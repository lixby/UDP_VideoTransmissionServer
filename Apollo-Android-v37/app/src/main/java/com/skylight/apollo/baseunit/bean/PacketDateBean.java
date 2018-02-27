package com.skylight.apollo.baseunit.bean;

/**
 * @Author: gengwen
 * @Date: 2018/1/3.
 * @Company:Skylight
 * @Description:
 */

public class PacketDateBean {
    public byte[] packetData;
    public long packetIndex;
    public int frameIndex;
    public long repeatTime;
    public long sendTime;
    public boolean isRttCalculated = false;

    public PacketDateBean(byte[] packetData, long packetIndex,int frameIndex,long repeatTime) {
        this.packetData = packetData;
        this.packetIndex = packetIndex;
        this.frameIndex = frameIndex;
        this.repeatTime = repeatTime;
    }
}
