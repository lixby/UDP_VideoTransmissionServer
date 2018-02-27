package com.skylight.apollo.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Play a video in a surface
 * Created by dongfeng on 2016/9/12.
 */
public class VideoDataDecoder extends DataDecoder {

    private final static String TAG = "FileDataDecoder";
    private String mFilePath;

    VideoPlayerThread videoPlayerThread;
    AudioPlayerThread audioPlayerThread;

    public VideoDataDecoder(Context context, String videoPath){
        mFilePath = videoPath;
    }

    @Override
    public void startDecoding(){
        // use a separate thread to decode the video
        videoPlayerThread = new VideoPlayerThread();
        videoPlayerThread.start();

        audioPlayerThread = new AudioPlayerThread();
        audioPlayerThread.start();
    }

    @Override
    public void stopDecoding(){
        videoPlayerThread.interrupt();
        audioPlayerThread.interrupt();
    }

    private class VideoPlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;

        @Override
        @SuppressWarnings("deprecation")
        public void run() {

            try {
                // start only when surface is not null
                while (getSurface() == null) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                // extract media information
                extractor = new MediaExtractor();
                extractor.setDataSource(mFilePath);
                Log.d(TAG, "xdf track count = " + extractor.getTrackCount());

                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    Log.d(TAG, "mime = " + format);
                    if (mime.startsWith("video/")) {
                        extractor.selectTrack(i);
                        decoder = MediaCodec.createDecoderByType(mime);
                        decoder.configure(format, getSurface(), null, 0);
                        break;
                    }
                }

                if (decoder == null) {
                    Log.e(TAG, "Can't find video info!");
                    return;
                }

                Log.d(TAG, "Start decoding");
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
                            int sampleSize = extractor.readSampleData(buffer, 0);
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                                extractor.advance();
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
                            Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            ByteBuffer buffer = outputBuffers[outIndex];
//                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                            Log.d(TAG, "xdf video pts " + info.presentationTimeUs);

                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                sleep(10);
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
            catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "Error processing video");
            }
            finally {
                if(decoder != null){
                    try {
                        decoder.stop();
                        decoder.release();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.d(TAG, "error closing video decoder");
                    }
                }
            }
        }
    }


    private class AudioPlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private long totalSample = 0;

        public void setSurface(){
        }

        @Override
        @SuppressWarnings("deprecation")
        public void run() {

            try {
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        8192 * 2,
                        AudioTrack.MODE_STREAM);

                // extract media information
                extractor = new MediaExtractor();
                extractor.setDataSource(mFilePath);

                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("audio/")) {
                        extractor.selectTrack(i);
                        decoder = MediaCodec.createDecoderByType(mime);
                        decoder.configure(format, null, null, 0);
                        break;
                    }
                }

                if (decoder == null) {
                    Log.e(TAG, "Can't find audio info!");
                    return;
                }

                Log.d(TAG, "Start decoding");
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
                            int sampleSize = extractor.readSampleData(buffer, 0);
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d(TAG, "xdf InputBuffer BUFFER_FLAG_END_OF_STREAM " + totalSample);
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
//                                totalSample += sampleSize;
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                                extractor.advance();
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
                            Log.d("DecodeActivity", "xdf audio format " + decoder.getOutputFormat());
                            audioTrack.setPlaybackRate(decoder.getOutputFormat().getInteger(MediaFormat.KEY_SAMPLE_RATE));
                            audioTrack.play();
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            ByteBuffer buffer = outputBuffers[outIndex];
                            final byte[] chunk = new byte[info.size];
                            buffer.get(chunk); // Read the buffer all at once
                            buffer.clear();

                            totalSample += info.size;
                            Log.d(TAG, "xdf audio pts " + info.presentationTimeUs + " total sample = " + totalSample);

                            audioTrack.write(chunk, info.offset, info.size);

//                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                sleep(10);
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
            catch(Exception e){
                e.printStackTrace();
                Log.e(TAG, "Error processing audio");
            }
            finally {
                if(decoder != null){
                    try {
                        decoder.stop();
                        decoder.release();
                    }
                    catch (Exception e){
                        Log.e(TAG, "Error closing decoder");
                    }
                }
            }
        }
    }
}
