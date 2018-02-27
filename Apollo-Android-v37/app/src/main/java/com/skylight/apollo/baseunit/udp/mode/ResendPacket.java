package com.skylight.apollo.baseunit.udp.mode;

/**
 * @Author: gengwen
 * @Date: 2018/1/23.
 * @Company:Skylight
 * @Description:
 */

public class ResendPacket {
    public long packetIndex = -1;
    public int sendedTimes = 0;
    public static int limit = 3;
    public long rttTime  = -1;
    public long backUpRttime = -1;
}
