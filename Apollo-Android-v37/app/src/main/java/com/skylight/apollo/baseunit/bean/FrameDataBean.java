package com.skylight.apollo.baseunit.bean;

/**
 * @Author: gengwen
 * @Date: 2018/1/3.
 * @Company:Skylight
 * @Description:
 */

public class FrameDataBean {
    //byte[] frameData, int dataLength,int channel,byte IBPtype
    public byte[] frameData;
    public int dataLength;
    public int channel;
    public byte IBPtype;

    public FrameDataBean(byte[] frameData, int dataLength, int channel, byte IBPtype) {
        this.frameData = frameData;
        this.dataLength = dataLength;
        this.channel = channel;
        this.IBPtype = IBPtype;
    }
}
