package apollo.decoder;

import android.os.Handler;
import android.view.Surface;

/**
 * Created by dongfeng on 2016/9/12.
 */
public abstract class DataDecoder {
    /**
     * The surface for holding the decoded image frame
     */
    private Surface mSurface;
    protected Handler messageHandler;

    public void setSurface(Surface surface){
        mSurface = surface;
    }

    public Surface getSurface(){
        return mSurface;
    }

    public abstract void startThreadDecoding();

    public abstract void stopDecoding();

    public long getVideoDuration(){
        return 0;
    }

    public int getCurrentPosition(){
        return 0;
    }

    public void seekTo(int position){

    }

    public void pause(){

    }

    public void play(){}

    public boolean isPlaying(){
        return false;
    }

    public void setMessageHandler(Handler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void reset(){}

    public void pauseSeekTo(int position){}

    public boolean isRunningThread(){
        return false;
    }
}
