package apollo.decoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * TestDataDecoder used for testing only.
 * This decoder loads video frames from SD card
 * Created by dongfeng on 2016/9/12.
 */
public class TestDataDecoder extends DataDecoder {

    private final static String TAG = "TestDataDecoder";
    private final static String MIME_TYPE = "video/avc";
    private final static int VIDEO_WIDTH = 3040;
    private final static int VIDEO_HEIGHT = 1520;
    private final static int VIDEO_FRAMERATE = 30;
    private final static int VIDEO_IFRAME_INTERVAL = 60;

    PlayerThread playerThread;

    public TestDataDecoder(Context context){
    }

    @Override
    public void startThreadDecoding(){
        Log.d(TAG, "startThreadDecoding");
        // use a separate thread to decode frames
        playerThread = new PlayerThread();
        playerThread.start();
    }

    @Override
    public void stopDecoding(){
        playerThread.interrupt();
    }

    private class PlayerThread extends Thread {
        private MediaCodec decoder;
        private int frameIndex = 0;

        @Override
        @SuppressWarnings("deprecation")
        public void run() {

            Log.d(TAG, "start run");
            try {
                decoder = MediaCodec.createDecoderByType(MIME_TYPE);
            }
            catch (IOException e){
                e.printStackTrace();
                return;
            }

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);

            byte[] header_sps = { 0, 0, 0, 1, 39, 77, 0, 51, -102, 100, 1, 124, 5, -3, 53, 1, 1, 1, 64, 0, 0, -6, 64, 0, 58, -104, 58, 24, 0, 57, 57, 0, 0, -28, -30, 46, -14, -29, 67, 0, 7, 39, 32, 0, 28, -100, 69, -34, 92, 40 };
            byte[] header_pps = { 0, 0, 0, 1, 40, -18, 60, -128 };
            format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_WIDTH * VIDEO_HEIGHT);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAMERATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

            // start only when surface is not null
            while(getSurface() == null){
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }

            decoder.configure(format, getSurface(), null, 0);
            decoder.start();

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        try {
                            byte[] data = loadFrame(frameIndex);
                            buffer.put(data);
                            decoder.queueInputBuffer(inIndex, 0, data.length, frameIndex*33, 0);
                            frameIndex ++;
                        }
                        catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
//                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
        }

        /**
         * load data for a frame
         * @param frame
         * @return
         * @throws IOException
         */
        private byte[] loadFrame(int frame) throws IOException{
            int index = (frame%120) + 38;
            String filename = Environment.getExternalStorageDirectory() + "/test/frame" + index + ".jpg";
//            return IOUtil.readFile(filename);
            return null;
        }
    }
}
