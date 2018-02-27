package com.skylight.apollo.decoder;

import android.view.Surface;

/**
 * Created by dongfeng on 2016/9/12.
 */
public abstract class DataDecoder {
    /**
     * The surface for holding the decoded image frame
     */
    private Surface mSurface;

    public void setSurface(Surface surface){
        mSurface = surface;
    }

    public Surface getSurface(){
        return mSurface;
    }

    public abstract void startDecoding();

    public abstract void stopDecoding();
}
