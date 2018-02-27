
package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = false;

    private static final String MIME_TYPE = "video/avc";   // H.264
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 2;           // 2 seconds between I-frames

    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int CODEC_TIMEOUT = 10000;
    private static final int CHANNEL_COUNT = 2;

    // video encoder
    private Surface mInputSurface;
    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private int mVideoTrackIndex;
    private boolean mVideoEncoderInitialize = false;

    // audio encoder
    private MediaCodec mAudioEncoder;
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private ByteBuffer[] mAudioInputBuffers;
    private ByteBuffer[] mAudioOutputBuffers;
    private double mAudioTimeUs = 0;
    private int mAudioBytesRead = 0;
    private int mAudioTrackIndex = 0;

    // muxer
    private IMediaMuxer mMuxer;
    private boolean mStreaming = false;

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public VideoEncoderCore(EncodeFormat encodeFormat, String outputUri)
            throws IOException {

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
//        outputFile = "rtmp://r730.kandao.tech/live/demo";
        Log.d(TAG, "Recording uri = " + outputUri);
        if(outputUri.startsWith("rtmp://")){
            mMuxer = new YaseaStreamingMediaMuxer(outputUri, 2, MIME_TYPE);
            mStreaming = true;
        }
        else {
            mMuxer = new FileMediaMuxer(outputUri, 2);
            mStreaming = false;
        }

        createAudioEncoder(encodeFormat.channels, encodeFormat.sampleRate, encodeFormat.audioBitrate);
        createVideoEncoder(encodeFormat.width, encodeFormat.height, encodeFormat.videoBitrate);
    }

    public MediaFormat createVideoEncoder(int width, int height, int bitrate) throws IOException{

        Log.d(TAG, "createVideoEncoder");
        mVideoBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
//        MediaFormat.Key
//        format.setInteger(MediaFormat.KEY_PROFILE);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mVideoEncoder.createInputSurface();
        mVideoEncoder.start();

        return format;
    }

    @SuppressWarnings("deprecation")
    public MediaFormat createAudioEncoder(int channelCount, int sampleRate, int bitrate) throws IOException{
        Log.d(TAG, "createAudioEncoder");
        mAudioBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

        mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        mAudioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioEncoder.start();

        mAudioInputBuffers = mAudioEncoder.getInputBuffers();
        mAudioOutputBuffers = mAudioEncoder.getOutputBuffers();

        return format;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    public boolean isMuxerStarted(){
        return mMuxer != null && mMuxer.isStarted();
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null){
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    @SuppressWarnings("deprecation")
    public void drainEncoder(boolean endOfStream) {
//        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if(mVideoEncoder == null){
            return;
        }

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
        }

        try {
            ByteBuffer[] encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            while (true) {

                mVideoBufferInfo = new MediaCodec.BufferInfo();
                int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, CODEC_TIMEOUT);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (!endOfStream) {
                        break;      // out of while
                    } else {
                        if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "video format change, add video track");
                    // should happen before receiving buffers, and should only happen once
                    if (mVideoEncoderInitialize) {
                        throw new RuntimeException("format changed twice");
                    }
                    MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                    mVideoEncoderInitialize = true;

                    Log.d(TAG, "add track format = " + newFormat);


                    // now that we have the Magic Goodies, start the muxer
                    mVideoTrackIndex = mMuxer.addTrack(newFormat);
                    if (!mMuxer.start()) {
                        // we should wait until muxer is ready
                        synchronized (mMuxer) {
                            while (mMuxer != null && !mMuxer.isStarted()) {
                                try {
                                    mMuxer.wait(10);
                                } catch (final InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    int len = mVideoBufferInfo.size > 50 ? 50 : mVideoBufferInfo.size;
                    byte[] d = new byte[len];
                    encodedData.position(mVideoBufferInfo.offset);
                    encodedData.get(d, 0, len);
//                Log.d(TAG, "write video data = " + bytesToHex(d, len));

                    if (!mStreaming && MIME_TYPE.equals("video/avc")
                            && (mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it for H.264 mp4
                        if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mVideoBufferInfo.size = 0;
                    }

                    if (mVideoBufferInfo.size != 0) {
                        if (!mVideoEncoderInitialize) {
                            throw new RuntimeException("muxer hasn't started");
                        }

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);

                        mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mVideoBufferInfo);
                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mVideoBufferInfo.size + " bytes to muxer, ts=" +
                                    mVideoBufferInfo.presentationTimeUs + " nanoTime = " + System.nanoTime() + ", " + System.currentTimeMillis());
                        }
                    }

                    mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            if (VERBOSE) Log.d(TAG, "end of stream reached");
                        }
                        break;      // out of while
                    }
                }
            }
        }
        catch (Exception e){
            // there is nothing that we can do with this exception, so ignore it
            Log.e(TAG, "encodeVideo exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void encodeAudio(byte[] data, int dataLength, int sampleRate, long timestamp){
        if(mAudioEncoder == null){
            return;
        }
        try {
            int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(CODEC_TIMEOUT);
            if (inputBufferIndex >= 0) {
                ByteBuffer buffer = mAudioInputBuffers[inputBufferIndex];
                buffer.clear();

                int bytesRead = dataLength;
                mAudioBytesRead += bytesRead;
                buffer.put(data, 0, bytesRead);
                mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, bytesRead, timestamp, 0);
                mAudioTimeUs = 1000000L * (mAudioBytesRead / 2) / sampleRate / CHANNEL_COUNT;
            }

            // process output data
            int outputBufferIndex = 0;
            while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                mAudioBufferInfo = new MediaCodec.BufferInfo();
                outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, CODEC_TIMEOUT);
                if (outputBufferIndex >= 0) {
                    // process encoded data
                    ByteBuffer encodedData = mAudioOutputBuffers[outputBufferIndex];
                    encodedData.position(mAudioBufferInfo.offset);
                    encodedData.limit(mAudioBufferInfo.offset + mAudioBufferInfo.size);

                    if (!mStreaming && (mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && mAudioBufferInfo.size != 0) {
                        mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    } else {
                        mMuxer.writeSampleData(mAudioTrackIndex, mAudioOutputBuffers[outputBufferIndex], mAudioBufferInfo);
                        mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "audio format changed, add audio track");
                    MediaFormat format = mAudioEncoder.getOutputFormat();
                    mAudioTrackIndex = mMuxer.addTrack(format);
                    if (!mMuxer.start()) {
                        Log.d(TAG, "muxer not started. we need to wait");
                        // we should wait until muxer is ready
                        synchronized (mMuxer) {
                            while (mMuxer != null && !mMuxer.isStarted()) {
                                try {
                                    mMuxer.wait(10);
                                } catch (final InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e){
            // there is nothing that we can do with this exception, so ignore it
            Log.e(TAG, "encodeAudio exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
