package com.skylight.apollo.baseunit.bean;

import java.io.Serializable;

public class FrameBean implements Serializable{
 	private static final long serialVersionUID = 5281156714316445330L;
    private int channel;
    private long pts;
    private int length;
    private byte[] frameData;
    private long frameIndex;

    public void setFrameIndex(long frameIndex){
    	this.frameIndex = frameIndex;
    }
    
    public long getFrameIndex(){
    	return frameIndex;
    }
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public long getPts() {
        return pts;
    }

    public void setPts(long pts) {
        this.pts = pts;
    }

    public byte[] getFrameData() {
        return frameData;
    }

    public void setFrameData(byte[] frameData) {
        this.frameData = frameData;
    }
}
