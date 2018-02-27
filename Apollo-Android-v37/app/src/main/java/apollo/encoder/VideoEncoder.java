
package apollo.encoder;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.kandaovr.sdk.gles.EglCore;
import com.kandaovr.sdk.gles.RectShape;
import com.kandaovr.sdk.gles.WindowSurface;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create VideoEncoder object
 * <li>create an EncoderConfig
 * <li>call VideoEncoder#setTextureId() with the texture object that receives frames
 * <li>call VideoEncoder#startRecording() with the config
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 *     call VideoEncoder#frameAvailable().
 * </ul>
 *
 */
public class VideoEncoder implements Runnable {
    private static final String TAG = "VideoEncoder";
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;
    private static final int MSG_ENCODE_AUDIO = 6;
    private static final int MSG_ENCODE_VIDEO = 7;

    private float[] mTransform = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private RectShape mRectShape;
    private int mTextureId;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private AudioHandler mAudioHandler;
    private VideoHandler mVideoHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private long mAudioTime = -1;
    private long mAudioTotal = 0;

    // ---- recording parameters ----
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private boolean mRecordingEnabled;
    private int mRecordingStatus = RECORDING_OFF;
    private String mRecordFile;
    private EncodeFormat mEncoderConfig;
    private RecordErrorListener mRecordListener;
    private RecordCompleteListener mRecordCompleteListener;
    private Queue<AudioFrame> mAudioQueue;

    public interface RecordErrorListener {
        void onError(final String recordPath, final String exception);
    }

    public interface RecordCompleteListener {
        void onComplete(final String recordPath);
    }

    private static class AudioFrame{
        byte[] frameData;
        int dataLength;
        int sampleRate;
        public AudioFrame(byte[] frameData, int dataLength, int sampleRate){
            this.frameData = frameData;
            this.dataLength = dataLength;
            this.sampleRate = sampleRate;
        }
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     */
    public static class EncoderConfig {
        final String mOutputUri;
        final EncodeFormat mEncodeFormat;
        final EGLContext mEglContext;

        public EncoderConfig(String outputUri, EncodeFormat encodeFormat, EGLContext eglContext) {
            mOutputUri = outputUri;
            mEncodeFormat = encodeFormat;
            mEglContext = eglContext;
        }
    }

    public VideoEncoder(){
        mAudioQueue = new LinkedList<>();
    }

    public void startRecord(String savePath, EncodeFormat encoderConfig, RecordErrorListener listener){

        mRecordListener = listener;
        mRecordFile = savePath;
        mEncoderConfig = encoderConfig;
        mRecordingStatus = RECORDING_OFF;
        mRecordingEnabled = true;
    }

    public void stopRecord(RecordCompleteListener listener){
        mRecordingEnabled = false;
        if(mRecordFile != null && !mRecordFile.startsWith("rtmp://")
                && !isEncoderStarted() && mRecordListener != null){
            mRecordListener.onError(mRecordFile, "your video was too short so we couldn't capture anything");
            return;
        }
        onVideoFrame(); //call this method to stop record video
        mRecordCompleteListener = listener;
        if(mRecordCompleteListener != null){
            mRecordCompleteListener.onComplete(mRecordFile);
        }
    }

    public void onVideoFrame(){
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    startRecording(mRecordFile, mEncoderConfig, EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
            frameAvailable(System.nanoTime());
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }
    }

    public void onAudioFrame(byte[] frameData, int dataLength, int sampleRate){

        mAudioQueue.add(new AudioFrame(frameData, dataLength, sampleRate));
        if(mAudioQueue.size() < 11){
            return;
        }
        AudioFrame audioFrame = mAudioQueue.remove();

        if(mRecordingStatus == RECORDING_ON && mRecordingEnabled) {
            encodeAudio(audioFrame.frameData, audioFrame.dataLength, audioFrame.sampleRate);
        }
    }


    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(String uri, EncodeFormat encodeFormat, EGLContext eglContext) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "VideoEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        EncoderConfig config = new EncoderConfig(uri, encodeFormat, eglContext);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AudioThread starts");
                Looper.prepare();
                mAudioHandler = new AudioHandler();
                Log.d(TAG, "AudioThread looping");
                Looper.loop();
                Log.d(TAG, "AudioThread stops");
            }
        }, "AudioEncoder").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "VideoThread starts");
                Looper.prepare();
                mVideoHandler = new VideoHandler();
                Log.d(TAG, "VideoThread looping");
                Looper.loop();
                Log.d(TAG, "VideoThread stops");
            }
        }, "VideoEncoder").start();


    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     */
    public void stopRecording() {
        Log.d(TAG, "stopRecording");
        if(mHandler != null) {
            // mHandler could be null
            Message message = mHandler.obtainMessage(MSG_STOP_RECORDING);
            boolean result = mHandler.sendMessage(message);
            Log.d(TAG, "message sent " + message.what + " result = " + result);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
            // We don't know when these will actually finish (or even start).  We don't want to
            // delay the UI thread though, so we return immediately.
        }
    }

    public boolean isEncoderStarted(){
        return mVideoEncoder != null && mVideoEncoder.isMuxerStarted();
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     */
    public void frameAvailable(long timestamp) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        boolean res = mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, mTransform));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     */
    public void setTextureId(int id) {
        mTextureId = id;
    }

    public void encodeAudio(byte[] audioData, int dataLength, int sampleRate){
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        if(mVideoEncoder == null){
            Log.d(TAG, "encodeAudio encoder is null");
            return;
        }

        AudioData record = new AudioData(audioData, dataLength, sampleRate);
        if(mAudioTime < 0){
            mAudioTime = System.nanoTime() / 1000L;
            mAudioTotal = 0;
        }
        long timestamp = mAudioTime + 1000000L * mAudioTotal / sampleRate / 4;
        mAudioTotal += dataLength;
        mAudioHandler.sendMessage(mAudioHandler.obtainMessage(MSG_ENCODE_AUDIO,
                    (int) (timestamp >> 32), (int) timestamp, record));
    }


    private class AudioData{
        public byte[] data;
        public int dataLength;
        public int sampleRate;
        public AudioData(byte[] data, int dataLength, int sampleRate){
            this.data = data;
            this.dataLength = dataLength;
            this.sampleRate = sampleRate;
        }
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<VideoEncoder> mWeakEncoder;

        public EncoderHandler(VideoEncoder encoder) {
            mWeakEncoder = new WeakReference<VideoEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            VideoEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    Log.d(TAG, "receive stop recording message");
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Log.d(TAG, "looper quit");
                    Looper.myLooper().quit();
                    encoder.handleQuit();
                    break;
                default:
                    Log.d(TAG, "Unhandled msg " + what);
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private class AudioHandler extends Handler{
        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            switch(what){
                case MSG_ENCODE_AUDIO:
                    long timestamp2 = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    handleEncodeAudio((AudioData)inputMessage.obj, timestamp2);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
            }
        }
    }

    private class VideoHandler extends Handler{
        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            switch(what){
                case MSG_ENCODE_VIDEO:
                    if(mVideoEncoder != null) {
                        mVideoEncoder.drainEncoder(false);
                    }
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mAudioTime = -1;
        prepareEncoder(config.mEglContext, config.mEncodeFormat, config.mOutputUri);
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     * @param transform The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) Log.d(TAG, "record handleFrameAvailable tr=" + transform + " textureId = " + mTextureId);
        if(mVideoHandler != null) {
            mVideoHandler.sendMessage(mVideoHandler.obtainMessage(MSG_ENCODE_VIDEO));
        }

        if(mRectShape != null) {
            mRectShape.draw(mTextureId);
        }

        if(mInputWindowSurface != null) {
            mInputWindowSurface.setPresentationTime(timestampNanos);
            mInputWindowSurface.swapBuffers();
        }
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();

        if(mRecordCompleteListener != null){
            mRecordCompleteListener.onComplete(mRecordFile);
        }
    }

    public void handleEncodeAudio(AudioData audioData, long timestamp){
        if(mVideoEncoder != null){

            // break into chunksq
            byte[] chunk = new byte[2048*2];
            int processed = 0;
            while(true){
                int current = processed + chunk.length <= audioData.dataLength ? chunk.length : audioData.dataLength - processed;
                if(current <= 0){
                    break;
                }
                System.arraycopy(audioData.data, processed, chunk, 0, current);
                mVideoEncoder.encodeAudio(chunk, current, audioData.sampleRate, timestamp + 1000000L*processed/audioData.sampleRate/4);
                processed += current;
            }
        }
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mRectShape.release();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        mRectShape = new RectShape();
    }

    private void handleQuit(){
        if(mAudioHandler != null){
            mAudioHandler.sendMessage(mAudioHandler.obtainMessage(MSG_QUIT));
        }
        if(mVideoHandler != null){
            mVideoHandler.sendMessage(mVideoHandler.obtainMessage(MSG_QUIT));
        }
    }

    private void prepareEncoder(EGLContext sharedContext, EncodeFormat encodeFormat,
                                String outputFile) {
        try {
            Log.d(TAG, "Recording create encoder " + outputFile);
            mVideoEncoder = new VideoEncoderCore(encodeFormat, outputFile);
        } catch (Exception ioe) {
            Log.e(TAG, "Cannot record video: " + ioe.getMessage());
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        mRectShape = new RectShape();
    }

    private void releaseEncoder() {
        Log.d(TAG, "releaseEncoder");
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if(mRectShape != null){
            mRectShape.release();
            mRectShape = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}
