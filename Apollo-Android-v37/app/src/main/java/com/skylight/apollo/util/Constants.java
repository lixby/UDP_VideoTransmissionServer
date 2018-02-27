package com.skylight.apollo.util;

import java.util.concurrent.TimeUnit;

/**
 * Created by dongfeng on 2016/11/23.
 */

public class Constants {
//    public final static String RTMP_ENDPOINT = "rtmp://us-central.rtmp.wemersive.com/4xh3BAfywi1Qpm0iKBxYPrFzqDP9/test";
//    public final static String RTMP_ENDPOINT = "rtmp://r730.kandao.tech/live/demo";
    public final static String RTMP_ENDPOINT = "rtmp://pili-publish.live4.kandaovr.com/kandaovr4/demo5";
//    public final static String RTMP_ENDPOINT = "rtmp://192.168.0.25/live/test";
//    public final static String RTMP_ENDPOINT = "rtmp://r730.kandao.tech:18888/4xh3BAfywi1Qpm0iKBxYPrFzqDP9/kandao";
//    public final static String RTMP_ENDPOINT = "rtmp://a.rtmp.youtube.com/live2/cbqw-fwks-stsb-3737";

    // the following are for testing
    public final static boolean DEBUG_MODE = false;
    public final static String TEST_LENS_PARAM = "00014570000044F0000044168EE14476911B4473075F45337D36447423033F8000000000000000000000000000003F8000000000000000000000000000003F800000BF7FF7063C7B09203BCDC8753C7A97E13F7FF7ADBB900C52BBCFFA7FBB8CE704BF7FFE193D35286B3D50A99BBF88CA603D4D6D3BBD66A65DBF88B8FABC23CEA7BC3749AE3B3C382AB9E06530FFFF";

    public static String FW = null;
    public static String USB_SDK = null;
    public static  String wb_statue = null;
    public static  String cc_statue = null;
    public static String version = "lgw_20170919-0";
    public static long repeatTime_mill =1;
    public static int repeatTime_nano =500000;
    public static int repeatTime_mucro =200;
    public static long repeatTime = 1;
    public static long repeatTime_dt = 1;
    public static TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    public static int tcpBufferSize = 64 * 1024;
    public static int tcpReadSize = 1024;
    public static int sendPacketSize = 100;
}
