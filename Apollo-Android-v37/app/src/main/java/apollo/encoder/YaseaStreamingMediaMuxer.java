package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.github.faucamp.simplertmp.RtmpHandler;

import net.ossrs.yasea.SrsFlvMuxer;

import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/11/11.
 */

public class YaseaStreamingMediaMuxer implements IMediaMuxer {
    private final static String TAG = "YaseaMediaMuxer";

    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private String mUrl;
    private RtmpHandler mRtmpHandler;
    private SrsFlvMuxer mFlvMuxer;

    public YaseaStreamingMediaMuxer(String url, int encoderCount, String videoMime){
        mRtmpHandler = new RtmpHandler(null);
        mFlvMuxer = new SrsFlvMuxer(videoMime, mRtmpHandler);
        mUrl = url;
        mEncoderCount = encoderCount;
        mStatredCount = 0;
        mIsStarted = false;
    }

    public synchronized boolean isStarted(){
        return mIsStarted;
    }

    public synchronized boolean start(){
        Log.v(TAG,  "start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mFlvMuxer.start(mUrl);
            mIsStarted = true;
            notifyAll();
            Log.d(TAG,  "MediaMuxer started:");
        }
        return mIsStarted;
    }

    public synchronized void stop(){
        Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        mStatredCount = 0;
        mFlvMuxer.stop();
        mIsStarted = false;
        Log.d(TAG,  "MediaMuxer stopped:");
    }

    public synchronized int addTrack(final MediaFormat format){
        return mFlvMuxer.addTrack(format);
    }

    public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo){
        if (mStatredCount <= 0){
            Log.d(TAG, "Skip data for track " + trackIndex);
            return;
        }

        byte[] data = new byte[bufferInfo.size];
        byteBuf.position(bufferInfo.offset);
        byteBuf.get(data, 0, bufferInfo.size);
        byteBuf.position(bufferInfo.offset);
        mFlvMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }


}
