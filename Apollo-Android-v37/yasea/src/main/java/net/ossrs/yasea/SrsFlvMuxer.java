package net.ossrs.yasea;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.github.faucamp.simplertmp.DefaultRtmpPublisher;
import com.github.faucamp.simplertmp.RtmpHandler;

import net.ossrs.yasea.flv.SrsFlv;
import net.ossrs.yasea.flv.SrsFlvAvc;
import net.ossrs.yasea.flv.SrsFlvHevc;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by winlin on 5/2/15.
 * Updated by leoma on 4/1/16.
 * to POST the h.264/avc annexb frame to SRS over RTMP.
 * @see android.media.MediaMuxer https://developer.android.com/reference/android/media/MediaMuxer.html
 *
 * Usage:
 *      muxer = new SrsRtmp("rtmp://ossrs.net/live/yasea");
 *      muxer.start();
 *
 *      MediaFormat aformat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, asample_rate, achannel);
 *      // setup the aformat for audio.
 *      atrack = muxer.addTrack(aformat);
 *
 *      MediaFormat vformat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, vsize.width, vsize.height);
 *      // setup the vformat for video.
 *      vtrack = muxer.addTrack(vformat);
 *
 *      // encode the video frame from camera by h.264 codec to es and bi,
 *      // where es is the h.264 ES(element stream).
 *      ByteBuffer es, MediaCodec.BufferInfo bi;
 *      muxer.writeSampleData(vtrack, es, bi);
 *
 *      // encode the audio frame from microphone by aac codec to es and bi,
 *      // where es is the aac ES(element stream).
 *      ByteBuffer es, MediaCodec.BufferInfo bi;
 *      muxer.writeSampleData(atrack, es, bi);
 *
 *      muxer.stop();
 *      muxer.release();
 */
public class SrsFlvMuxer implements SrsFlv.FrameListener{
    private volatile boolean connected = false;
    private DefaultRtmpPublisher publisher;
    private RtmpHandler mHandler;

    private Thread worker;
    private final Object txFrameLock = new Object();

    private SrsFlv flv;
    private boolean needToFindKeyFrame = true;
    private SrsFlv.SrsFlvFrame videoSequenceHeader;
    private SrsFlv.SrsFlvFrame audioSequenceHeader;
    private ConcurrentLinkedQueue<SrsFlv.SrsFlvFrame> frameCache = new ConcurrentLinkedQueue<SrsFlv.SrsFlvFrame>();

    private static final int VIDEO_TRACK = 100;
    private static final int AUDIO_TRACK = 101;
    private static final String TAG = "SrsFlvMuxer";

    /**
     * constructor.
     * @param handler the rtmp event handler.
     */
    public SrsFlvMuxer(String codec, RtmpHandler handler) {
        mHandler = handler;
        publisher = new DefaultRtmpPublisher(handler);
        if(codec.equals("video/hevc")){
            flv = new SrsFlvHevc(this, handler);
        }
        else {
            flv = new SrsFlvAvc(this, handler);
        }
    }

    /**
     * get cached video frame number in publisher
     */
    public AtomicInteger getVideoFrameCacheNumber() {
        return publisher == null ? null : publisher.getVideoFrameCacheNumber();
    }

    /**
     * set video resolution for publisher
     * @param width width
     * @param height height
     */
    public void setVideoResolution(int width, int height) {
        if (publisher != null) {
            publisher.setVideoResolution(width, height);
        }
    }

    /**
     * Adds a track with the specified format.
     * @param format The media format for the track.
     * @return The track index for this newly added track.
     */
    public int addTrack(MediaFormat format) {
        if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
            flv.setVideoTrack(format);
            return VIDEO_TRACK;
        } else {
            flv.setAudioTrack(format);
            return AUDIO_TRACK;
        }
    }

    private void disconnect() {
        try {
            publisher.close();
        } catch (IllegalStateException e) {
            // Ignore illegal state.
        }
        connected = false;
        videoSequenceHeader = null;
        audioSequenceHeader = null;
        Log.i(TAG, "worker: disconnect ok.");
    }

    private boolean connect(String url) {
        if (!connected) {
            Log.i(TAG, String.format("worker: connecting to RTMP server by url=%s\n", url));
            if (publisher.connect(url)) {
                connected = publisher.publish("live");
            }
            videoSequenceHeader = null;
            audioSequenceHeader = null;
        }
        return connected;
    }

    public void frameAvailable(SrsFlv.SrsFlvFrame frame){

        if (frame.is_video()) {
            if (needToFindKeyFrame) {
                if (frame.is_keyframe()) {
                    needToFindKeyFrame = false;
                    flvFrameCacheAdd(frame);
                }
            } else {
                flvFrameCacheAdd(frame);
            }
        } else if (frame.is_audio()) {
            flvFrameCacheAdd(frame);
        }
    }

    private void flvFrameCacheAdd(SrsFlv.SrsFlvFrame frame) {
        frameCache.add(frame);
        if (frame.is_video()) {
            getVideoFrameCacheNumber().incrementAndGet();
        }
        synchronized (txFrameLock) {
            txFrameLock.notifyAll();
        }
    }

    private void sendFlvTag(SrsFlv.SrsFlvFrame frame) {
        if (!connected || frame == null) {
            return;
        }

        if (frame.is_video()) {
            publisher.publishVideoData(frame.flvTag.array(), frame.dts);
        } else if (frame.is_audio()) {
            publisher.publishAudioData(frame.flvTag.array(), frame.dts);
        }

        if (frame.is_keyframe()) {
            Log.i(TAG, String.format("worker: send frame type=%d, dts=%d, size=%dB",
                    frame.type, frame.dts, frame.flvTag.array().length));
        }
    }

    /**
     * start to the remote SRS for remux.
     */
    public void start(final String rtmpUrl) {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!connect(rtmpUrl)) {
                    return;
                }

                while (!Thread.interrupted()) {
                    while (!frameCache.isEmpty()) {
                        SrsFlv.SrsFlvFrame frame = frameCache.poll();
                        if (frame.is_sequenceHeader()) {
                            if (frame.is_video()) {
                                videoSequenceHeader = frame;
                                sendFlvTag(videoSequenceHeader);
                            } else if (frame.is_audio()) {
                                audioSequenceHeader = frame;
                                sendFlvTag(audioSequenceHeader);
                            }
                        } else {
                            if (frame.is_video() && videoSequenceHeader != null) {
                                sendFlvTag(frame);
                            } else if (frame.is_audio() && audioSequenceHeader != null) {
                                sendFlvTag(frame);
                            }
                        }
                    }
                    // Waiting for next frame
                    synchronized (txFrameLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            txFrameLock.wait(500);
                        } catch (InterruptedException ie) {
                            worker.interrupt();
                        }
                    }
                }
            }
        });
        worker.start();
    }

    /**
     * stop the muxer, disconnect RTMP connection from SRS.
     */
    public void stop() {
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                worker.interrupt();
            }
            frameCache.clear();
            worker = null;
        }
        flv.reset();
        needToFindKeyFrame = true;
        Log.i(TAG, "SrsFlvMuxer closed");

        new Thread(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        }).start();
    }

    /**
     * send the annexb frame to SRS over RTMP.
     * @param trackIndex The track index for this sample.
     * @param byteBuf The encoded sample.
     * @param bufferInfo The buffer information related to this sample.
     */
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (bufferInfo.offset > 0) {
            Log.w(TAG, String.format("encoded frame %dB, offset=%d pts=%dms",
                    bufferInfo.size, bufferInfo.offset, bufferInfo.presentationTimeUs / 1000
            ));
        }

        if (VIDEO_TRACK == trackIndex) {
            flv.writeVideoSample(byteBuf, bufferInfo);
        } else {
            flv.writeAudioSample(byteBuf, bufferInfo);
        }
    }


}
