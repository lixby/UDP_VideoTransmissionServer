package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/9/28.
 */
public class FileMediaMuxer implements IMediaMuxer{

    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MediaMuxerWrapper";

    private String mTempPath;
    private String mOutputPath;
    private MediaMuxer mMediaMuxer;
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;

    public FileMediaMuxer(String outputPath, int encoderCount) throws IOException{
        mOutputPath = outputPath;
        mTempPath = mOutputPath + ".mp4";
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = encoderCount;
        mStatredCount = 0;
        mIsStarted = false;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    public synchronized boolean start() {
        if (DEBUG) Log.v(TAG,  "start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
        }
        Log.d(TAG, "Started count = " + mStatredCount);
        return mIsStarted;
    }

    public synchronized void stop(){
        if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        mStatredCount = 0;
        if(!mIsStarted){
            mMediaMuxer.release();
            return;
        }
        mIsStarted = false;
        try {
            mMediaMuxer.stop();
        }catch (Exception e){
            Log.e(TAG, "MediaMuxer stop() exception: " + e.getMessage());
//            FileUtil.deleteFile(mOutputPath);
            return;
        }
        mMediaMuxer.release();
        if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");

//        try {
//            Log.d(TAG, "Writing metadata");
//            MetadataUtil.writeVideo(mTempPath, mOutputPath);
//            FileUtil.deleteFile(mTempPath);
//            Log.d(TAG, "finish writing video");
//        }
//        catch(Exception e){
//            Log.d(TAG, "meta exception: " + e.getMessage());
//            e.printStackTrace();
//        }
    }

    public synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackId = mMediaMuxer.addTrack(format);
        if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackId + ",format=" + format);
        return trackId;
    }

    /**
     * write encoded data to muxer
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
	public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {

        if (mIsStarted) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
            return;
        }
    }
}
