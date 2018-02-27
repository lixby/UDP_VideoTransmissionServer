package com.skylight.apollo.baseunit.biz;

import android.util.Log;

import com.skylight.apollo.baseunit.tcp.DataSegregator;
import com.skylight.apollo.baseunit.tcp.TcpDataPresenter;
import com.skylight.apollo.baseunit.tcp.bean.PackageBean;
import com.skylight.apollo.baseunit.tcp.bean.TLVBean;
import com.skylight.apollo.baseunit.udp.UdpFramePresenter;
import com.skylight.apollo.baseunit.util.ChangeUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: gengwen
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public abstract class CommonPresenter implements DataSegregator.GetCommonListener{
    protected TcpDataPresenter tcpDataPresenter;
    protected UdpFramePresenter udpFramePresenter;

    private static final int C_SEND_IP_PORT  = 0X20;
    private static final int C_SYN_WINDOWS   = 0X21;
    private static final int C_OBTAIN_STREAM = 0X22;
    private static final int C_STOP_STREAM   = 0X23;
    private static final int C_GET_LEN_PARAM = 0X24;

    protected boolean startObtain = false;

    public CommonPresenter() {
        tcpDataPresenter = new TcpDataPresenter();
        udpFramePresenter = new UdpFramePresenter();

        tcpDataPresenter.setGetCommonListener(this);
    }


    @Override
    public void onCommonout(PackageBean packageBean)  {
        Log.e("----packageBean",packageBean.commandType+".");
        switch(packageBean.commandType){
            case C_SEND_IP_PORT:
                TLVBean tlvBean = null;
                InetAddress ip = null;
                int port = -1;
                while (!packageBean.tlvLink.isEmpty()) {
                    try {
                        tlvBean = packageBean.tlvLink.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (tlvBean.getType() == 0x600) {
                        try {
                            ip = InetAddress.getByAddress(tlvBean.getValue());

                            Log.e("------onGetIP_PORT1111","----"+tlvBean.getValue()[0]+"|"+tlvBean.getValue()[1]+"|"+tlvBean.getValue()[2]+"|"+tlvBean.getValue()[3]+"|");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    } else if (tlvBean.getType() == 0x601) {
                        port = ChangeUtils.byteArr2int(tlvBean.getValue());
                    }
                }
                Log.e("------onGetIP_PORT1111",":"+port);
                onGetIP_PORT(ip,port);

                break;
            case C_SYN_WINDOWS:
                long boundary  = -1;
                long receivedIndex = -1;
                long[] missIndexs = null;
                TLVBean tlvBean1 = null;
                while (!packageBean.tlvLink.isEmpty()) {
                    try {
                       tlvBean1 = packageBean.tlvLink.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e("onCommonout","tvl-type:"+tlvBean1.getType());
                    if (tlvBean1.getType() == 0x602) {
                        boundary = ChangeUtils.bytes2Long(tlvBean1.getValue(),0,4);
                        Log.e("onCommonout","boundary:"+boundary);
                    }else if (tlvBean1.getType() == 0x603){
                        receivedIndex = ChangeUtils.bytes2Long(tlvBean1.getValue(),0,4);
                        Log.e("onCommonout","receivedIndex:"+receivedIndex);
                    }else if(tlvBean1.getType() == 0x604){
                        byte[] value = tlvBean1.getValue();
                        int size = value.length/4;
                        missIndexs = new long[size];
                        int startPosition = 0;
                        while (startPosition < value.length){
                            long missIndex = ChangeUtils.bytes2Long(value,startPosition,4);
                            missIndexs[startPosition/4] = missIndex;
                            startPosition += 4;
                            Log.e("onCommonout","missIndex:"+missIndex);
                        }
                    }
                }
                onGetSynWds(boundary,receivedIndex,missIndexs);
                break;
            case C_OBTAIN_STREAM:
                onGetObtainStream();
                break;
            case C_STOP_STREAM:
                onGetCloseStream();
                break;
            case C_GET_LEN_PARAM:
//                onGetLensParams();
                break;
        }
    }

    /**
     * get ip & port info
     * @param ip
     * @param port
     */
    abstract void onGetIP_PORT(InetAddress ip,int port);

    abstract void onGetSynWds(long boundary,long receivedIndex,long[] missIndexs);
    /**
     * get start stream common
     */
    abstract void onGetObtainStream();

    /**
     * get close stream common
     */
    abstract void onGetCloseStream();
    abstract void onGetLensParams(String params);

}
