package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/9/28.
 */
public class RawMediaMuxer implements IMediaMuxer{

    private static final String TAG = "RawMuxerWrapper";
    private static final int VIDEO_TRACK = 1;
    private static final int AUDIO_TRACK = 2;
    private int current = 0;

    public RawMediaMuxer(String outputPath, int encoderCount) throws IOException{
    }

    public synchronized boolean isStarted() {
        return true;
    }

    public synchronized boolean start() {
        Log.d(TAG, "creating VP9 file");
//        openFile("vp9" + System.currentTimeMillis() + ".vp9");
        return true;
    }

    public synchronized void stop() {
        Log.d(TAG, "Closing VP9 file");
//        closeFile();
    }

    public synchronized int addTrack(final MediaFormat format) {
        if(format.getString(MediaFormat.KEY_MIME).startsWith("video")){
            return VIDEO_TRACK;
        }
        return AUDIO_TRACK;
    }

    /**
     * write encoded data to muxer
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
	public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {

        if(trackIndex == VIDEO_TRACK){
            Log.d(TAG, "receive video data " + bufferInfo.size);
            byte[] data = new byte[bufferInfo.size];
            byteBuf.get(data, 0, bufferInfo.size);
            openFile("vp9frame" + current++ + ".vp9");
            recordData(data);
            closeFile();
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
