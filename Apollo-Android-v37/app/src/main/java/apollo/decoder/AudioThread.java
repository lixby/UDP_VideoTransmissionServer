package apollo.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Duc Nguyen
 * @version 1.0
 * @since 1/23/17
 */
public class AudioThread extends Thread {
    private final String TAG = AudioThread.class.getSimpleName();
    ByteBuffer[] audioInputBuffers;
    ByteBuffer[] audioOutputBuffers;
    MediaCodec.BufferInfo audioInfo;
    private MediaExtractor audioExtractor;
    private MediaCodec audioDecoder;
    private String mFilePath;
    private boolean mPlaying;
    private final PlaybackTimer mTimer;
    private int currentPosition = 0;
    private int mSeekToMs = -1;
    private long totalSample = 0;

    public AudioThread(String filePath){
        this.mFilePath = filePath;
        mTimer = new PlaybackTimer();
    }

    @Override
    public void run() {
            try {
                sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
//        super.run();
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                8192 * 2,
                AudioTrack.MODE_STREAM);

        // extract media information
        audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(mFilePath);
        } catch (Exception e) {
            Log.e(TAG, "Cannot setDataSource: " + e.getMessage());
            e.printStackTrace();
        }

        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            if(audioDecoder != null){
                audioDecoder.release();
                audioDecoder = null;
            }
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                audioExtractor.selectTrack(i);
                try {
                    audioDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                    stopThread();
                    return;
                }
                audioDecoder.configure(format, null, null, 0);
                break;
            }
        }

        if (audioDecoder == null) {
            Log.e(TAG, "Can't find audio info!");
            stopThread();
            return;
        }

        Log.d(TAG, "//////////////Start decoding");
        audioDecoder.start();

        audioInputBuffers = audioDecoder.getInputBuffers();
        audioOutputBuffers = audioDecoder.getOutputBuffers();
        audioInfo = new MediaCodec.BufferInfo();
        boolean isEOS = false;


        boolean threadIsInterrupted = Thread.interrupted();
        Log.d(TAG, "//////////////threadIsInterrupted:" + threadIsInterrupted);
        Log.d(TAG, "//////////////mPlaying:" + mPlaying);
        while (!Thread.interrupted()) {
            if(mPlaying){
                if (!isEOS) {

                    if (isEOS) {
                        seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        isEOS = false;
                    }

                    if (mSeekToMs != -1) {
                        seekTo(mSeekToMs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        mSeekToMs = -1;
                    }

                    long timertime = mTimer.getTime();

                    if (timertime < currentPosition) {
                        continue;
                    }


                    int inIndex = audioDecoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = audioInputBuffers[inIndex];
                        int sampleSize = audioExtractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to videoDecoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d(TAG, "xdf InputBuffer BUFFER_FLAG_END_OF_STREAM " + totalSample);
                            audioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            audioDecoder.queueInputBuffer(inIndex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                            audioExtractor.advance();
                        }
                    }
                }

                int outIndex = audioDecoder.dequeueOutputBuffer(audioInfo, 10000);
                currentPosition = (int) (audioInfo.presentationTimeUs / 1000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        audioInputBuffers = audioDecoder.getInputBuffers();
                        audioOutputBuffers = audioDecoder.getOutputBuffers();

                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "xdf audio format " + audioDecoder.getOutputFormat());
                        audioTrack.setPlaybackRate(audioDecoder.getOutputFormat().getInteger(MediaFormat.KEY_SAMPLE_RATE));
                        audioTrack.play();
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        Log.d(TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = audioOutputBuffers[outIndex];
                        final byte[] chunk = new byte[audioInfo.size];
                        buffer.get(chunk); // Read the buffer all at once
                        buffer.clear();

                        totalSample += audioInfo.size;
                        Log.d(TAG, "xdf audio pts " + audioInfo.presentationTimeUs + " total sample = " + totalSample);

                        audioTrack.write(chunk, audioInfo.offset, audioInfo.size);

//                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
//                        while (audioInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
//                            try {
//                                sleep(10);
//                            } catch (InterruptedException e) {
//                                Log.d(TAG, "interrupted exception");
//                                e.printStackTrace();
//                                audioDecoder.stop();
//                                audioDecoder.release();
//                                return;
//                            }
//                        }
                        audioDecoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

            }

        }

        Log.d(TAG, "//////////////Stop Thread");
        audioDecoder.stop();
        audioDecoder.release();
        audioExtractor.release();
    }

    public void seekTo(int position) {
        mSeekToMs = position;
    }

    public void pause() {
        mPlaying = false;
        mTimer.stop();
    }

    public void play() {
        mPlaying = true;
        mTimer.start();
    }

    public void seekTo(long ms, int seekMode) {

        audioExtractor.seekTo(ms * 1000, seekMode);
        currentPosition = (int) (audioExtractor.getSampleTime() / 1000);
        Log.d(TAG, "currentPosition:" + currentPosition);
        mTimer.setTime(currentPosition);
        audioDecoder.flush();
        audioInputBuffers = audioDecoder.getInputBuffers();
        audioOutputBuffers = audioDecoder.getOutputBuffers();
        audioInfo = new MediaCodec.BufferInfo();

    }

    public void reset() {
//        super.reset();

        stopThread();
        mTimer.reset();
        mSeekToMs = -1;
        currentPosition = 0;
        mPlaying = false;
    }

    private void stopThread(){
        interrupt();
        mPlaying = false;
    }


}
