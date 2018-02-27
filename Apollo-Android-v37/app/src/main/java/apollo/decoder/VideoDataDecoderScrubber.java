package apollo.decoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Duc Nguyen
 * @version 1.0
 * @since 1/23/17
 */
public class VideoDataDecoderScrubber extends DataDecoder {


    private final static String TAG = "FileDataDecoder";
    private String mFilePath;

    VideoDataDecoderScrubber.VideoPlayerThread videoPlayerThread;
    //AudioThread audioPlayerThread;
    AudioThread2 audioPlayerThread;
    private long videoDuration = 0;
    private int currentPosition = 0;
    private int mSeekToMs = -1;
    boolean isRunningThread = false;
    boolean mPlaying;


    private final PlaybackTimer mTimer;

    public VideoDataDecoderScrubber(Context context, String videoPath) {
        mFilePath = videoPath;
        mTimer = new PlaybackTimer();
    }

    @Override
    public int getCurrentPosition() {
        return (int) mTimer.getTime();
    }

    @Override
    public void startThreadDecoding() {

        // use a separate thread to decode the video
        videoPlayerThread = new VideoDataDecoderScrubber.VideoPlayerThread();
        //audioPlayerThread = new AudioThread(mFilePath);
        audioPlayerThread = new AudioThread2(mFilePath);

        videoPlayerThread.start();
        //audioPlayerThread.start();
        audioPlayerThread.startThread();
        isRunningThread = true;

    }

    @Override
    public boolean isRunningThread() {
        return isRunningThread;
    }

    @Override
    public void stopDecoding() {
        if (videoPlayerThread != null)
            videoPlayerThread.interrupt();

        if (audioPlayerThread != null)
            //audioPlayerThread.interrupt();
            audioPlayerThread.stopThread();

        isRunningThread = false;
    }

    public long getVideoDuration() {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(mFilePath);
            String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mediaMetadataRetriever.release();

            videoDuration = Long.parseLong(durationStr);
        } catch (Exception e) {
            e.printStackTrace();
            videoDuration = 0;
        }
        return videoDuration;
    }


    private class VideoPlayerThread extends Thread {

        private ByteBuffer[] videoInputBuffers;
        private ByteBuffer[] videoOutputBuffers;
        private MediaCodec.BufferInfo videoInfo;
        private MediaExtractor videoExtractor;
        private MediaCodec videoDecoder;

        @Override
        @SuppressWarnings("deprecation")
        public void run() {
            while (getSurface() == null) {
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }

            videoExtractor = new MediaExtractor();
            try {
                videoExtractor.setDataSource(mFilePath);
            } catch (Exception e) {
                Log.e(TAG, "Cannot setDataSource: " + e.getMessage());
                e.printStackTrace();
                stopDecoding();
            }
            Log.d(TAG, "xdf track count = " + videoExtractor.getTrackCount());

            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                if (videoDecoder != null) {
                    videoDecoder.release();
                    videoDecoder = null;
                }
                MediaFormat format = videoExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "mime = " + format);
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i);
                    try {
                        videoDecoder = MediaCodec.createDecoderByType(mime);
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopDecoding();
                        return;
                    }
                    try {
                        videoDecoder.configure(format, getSurface(), null, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        stopDecoding();
                        return;
                    }
                    break;
                }
            }

            if (videoDecoder == null) {
                Log.e(TAG, "Can't find video info!");
                return;
            }

            videoDecoder.start();

            videoInputBuffers = videoDecoder.getInputBuffers();
            videoOutputBuffers = videoDecoder.getOutputBuffers();
            videoInfo = new MediaCodec.BufferInfo();


            boolean isEOS = false;


            while (!Thread.interrupted()) {
                if (mPlaying) {

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

                    int inIndex = videoDecoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = videoInputBuffers[inIndex];
                        int sampleSize = videoExtractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to videoDecoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            videoDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            long timeSampleTime = videoExtractor.getSampleTime();
//                                timeSampleTime = 1969733;
                            videoDecoder.queueInputBuffer(inIndex, 0, sampleSize, timeSampleTime, 0);
                            videoExtractor.advance();
                        }
                    }


                }

                int outIndex = videoDecoder.dequeueOutputBuffer(videoInfo, 10000);
                currentPosition = (int) (videoInfo.presentationTimeUs / 1000);
//                    Log.d("DUC", "currentPosition:" + currentPosition);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        videoInputBuffers = videoDecoder.getInputBuffers();
                        videoOutputBuffers = videoDecoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + videoDecoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
//                        Log.d("DecodeActivity", "releaseOutputBuffer");
                        videoDecoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    isEOS = true;
//                        break;
                }

                if (isEOS) {
//                    pause();

                    Message message = messageHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("VIDEO_EOS", true);
                    message.setData(bundle);
                    messageHandler.sendMessage(message);

                    reset();
                }


            }

            videoDecoder.stop();
            videoDecoder.release();
            videoExtractor.release();
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
            videoExtractor.seekTo(ms * 1000, seekMode);
            currentPosition = (int) (videoExtractor.getSampleTime() / 1000);
            mTimer.setTime(currentPosition);
            videoDecoder.flush();
            videoInputBuffers = videoDecoder.getInputBuffers();
            videoOutputBuffers = videoDecoder.getOutputBuffers();
            videoInfo = new MediaCodec.BufferInfo();

        }

    }

    @Override
    public void seekTo(int position) {
        audioPlayerThread.seekTo(position);
        mSeekToMs = position;
    }

    @Override
    public void pauseSeekTo(int position) {
        if (!mTimer.isRunning() && position != -1) {
            mSeekToMs = position;
            videoPlayerThread.seekTo(position, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioPlayerThread.seekTo(position, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }
    }

    @Override
    public void pause() {
//        mTimer.stop();
        videoPlayerThread.pause();
        audioPlayerThread.pause();
    }

    @Override
    public void play() {
//        mTimer.start();
        videoPlayerThread.play();
        audioPlayerThread.play();
    }

    @Override
    public boolean isPlaying() {
        return mTimer.isRunning();
    }

    @Override
    public void reset() {
//        super.reset();

        stopDecoding();
        mTimer.reset();
        mSeekToMs = -1;
        currentPosition = 0;
        mPlaying = false;

        audioPlayerThread.reset();
    }
}
