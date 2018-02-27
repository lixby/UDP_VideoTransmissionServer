package apollo.decoder;

import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Read H264 frames from USB and decode using Mediacodec
 * Created by dongfeng on 2016/9/12.
 */
public class UsbDataDecoder extends DataDecoder {

    private final static String TAG = "UsbDataDecoder";
    private final static String MIME_TYPE = "video/avc";
    private final static int VIDEO_WIDTH = 3040;
    private final static int VIDEO_HEIGHT = 1520;
    private final static int VIDEO_FRAMERATE = 30;
    private final static int VIDEO_IFRAME_INTERVAL = 30;

    private Context mContext;

    private MediaCodec mDecoder;
    private boolean mDecoderInitialized = false;
    private boolean mDecoderStarted = false;
    private boolean mDecoderReady = false;
    private long mStartTime = 0;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    private static final Object obj = new Object();

    public UsbDataDecoder(Activity context){
        mContext = context;
    }

    @Override
    public void startThreadDecoding() {
        synchronized (obj){
            // create MediaCodec decoder
            // the decoder cannot be initialize here because we don't have SPS and PPS
            try {
                // That is, when the live stream is played and inserted when Cam, there is a great probability of MediaCode IllegalStateException exception
                if (!mDecoderInitialized){
                    mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public boolean isDecoderStarted() {
        return mDecoderStarted;
    }

    @Override
    public void stopDecoding(){
        synchronized (obj){
            if(mDecoderStarted) {
                mDecoder.stop();
            }
            if (mDecoder != null) {
                mDecoder.release();
                mDecoder = null;
            }

            mDecoderInitialized = false;
            mDecoderStarted = false;
            mDecoderReady = false;
        }
    }

    @SuppressWarnings("deprecation")
    public void onReadFrame(final byte[] frameData, int dataLength) {
        synchronized (obj){
            if(mDecoder == null){
                return;
            }

//            if(InfoExtractor.isIFrame(frameData)){
            if(true){
                Log.d(TAG, "found iframe");
                if(mDecoderStarted) {
                    Log.d(TAG, "decoder ready ");
                    // decoder ready at the first iframe after the decoder is started
                    mDecoderReady = true;
                }
                else if(!mDecoderInitialized && getSurface() != null){
                    Log.d(TAG, "initializing decoder");

                    // configure media format
//                    Pair<byte[], byte[]> info = InfoExtractor.extractSpsAndPps(frameData);
                    Pair<byte[], byte[]> info = null;
                    MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
                    format.setByteBuffer("csd-0", ByteBuffer.wrap(info.first));
                    format.setByteBuffer("csd-1", ByteBuffer.wrap(info.second));
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_WIDTH * VIDEO_HEIGHT);
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAMERATE);
                    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

                    mDecoder.configure(format, getSurface(), null, 0);
                    mDecoder.start();
                    mDecoderStarted = true;
                    // initialize the decoder
                    mDecoderInitialized = true;
                    Log.d(TAG, "decoder started");

                    inputBuffers = mDecoder.getInputBuffers();
                    outputBuffers = mDecoder.getOutputBuffers();
                    mStartTime = System.currentTimeMillis();
                }
            }

            if(mDecoderReady){
                try {
                    int inIndex = mDecoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        buffer.put(frameData);
                        mDecoder.queueInputBuffer(inIndex, 0, frameData.length, System.currentTimeMillis() - mStartTime, 0);
                    }

                    int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mDecoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d("DecodeActivity", "New format " + mDecoder.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                            break;
                        default:
//                        ByteBuffer buffer = outputBuffers[outIndex];
//                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);
                            mDecoder.releaseOutputBuffer(outIndex, true);
                            break;
                    }
                }
                catch (Exception e){
                    Log.d(TAG, "decoder exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

    }
}
