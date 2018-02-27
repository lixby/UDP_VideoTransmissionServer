package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/9/28.
 */
public class WebmMediaMuxer implements IMediaMuxer{

    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "WebmMuxerWrapper";

    private String mOutputPath;
    private MediaMuxer mMediaMuxer;
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;

    private int mVideoTrackId = 0;

    public WebmMediaMuxer(String outputPath, int encoderCount) throws IOException{
        mOutputPath = outputPath + ".webm";
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM);
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

//            openFile("movie" + System.currentTimeMillis() + ".h265");
        }
        Log.d(TAG, "Started count = " + mStatredCount);
//        return true;// return true so that we do not block
        return mIsStarted;
    }

    public synchronized void stop() {
//        closeFile();
        if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        mStatredCount = 0;
        mMediaMuxer.stop();
        mMediaMuxer.release();
        mIsStarted = false;
        if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
    }

    public synchronized int addTrack(final MediaFormat format) {

        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackId = mMediaMuxer.addTrack(format);
        if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackId + ",format=" + format);

        if(format.getString(MediaFormat.KEY_MIME).startsWith("video")){
            mVideoTrackId = trackId;
        }
        return trackId;
    }

    /**
     * write encoded data to muxer
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
	public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {

//        if(trackIndex == mVideoTrackId){
//            byte[] data = new byte[bufferInfo.size];
//            byteBuf.get(data, 0, bufferInfo.size);
//            recordData(data);
//            byteBuf.position(0);
//        }

        if(mIsStarted){
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
            return;
        }
        else {
            Log.e(TAG, "receive writeSampleData when not started");
            return;
        }

    }



    // for recording
    private FileOutputStream outputStream = null;
    public void openFile(String fileName)
    {
        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals("mounted"))
        {
            Log.d("TestFile", "SD card is not avaiable/writeable right now.");
            return;
        }
        try
        {
            File file = new File(Environment.getExternalStorageDirectory(),
                    fileName);
            if (!file.exists())
            {
                Log.d("TestFile", "Create the file:" + fileName);
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file, true);
        }
        catch (Exception e)
        {
            Log.e("TestFile", "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void recordData(byte[] data){
        if(outputStream == null){
            return;
        }

        try
        {
            outputStream.write(data, 0 ,data.length);
        }
        catch (Exception e)
        {
            Log.e("TestFile", "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void closeFile(){
        try
        {
            if(outputStream != null){
                outputStream.close();
            }
        }
        catch(Exception e){
            Log.e("UsbActivity", "Error closing file: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            outputStream = null;
        }
    }

}
