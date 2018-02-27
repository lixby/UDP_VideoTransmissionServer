package com.skylight.apollo.baseunit.biz;


import android.content.Context;
import android.util.Log;

import com.skylight.apollo.baseunit.tcp.command.PacketCmdHandler;
import com.skylight.apollo.baseunit.tcp.command.TcpCommandType;
import com.skylight.apollo.baseunit.tcp.mode.TlvIntegerMode;
import com.skylight.apollo.baseunit.tcp.mode.TlvMode;
import com.skylight.apollo.baseunit.tcp.mode.TlvModes;
import com.skylight.apollo.baseunit.tcp.mode.TlvStringMode;
import com.skylight.apollo.baseunit.udp.receive.UdpReceiver;
import com.skylight.apollo.baseunit.util.Utils;
import com.skylight.apollo.decoder.util.InfoExtractor;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @Author: gengwen
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public class SendVideoPresenter extends CommonPresenter{

    public void setView(Context context){

    }

    public void startRunTcp(){
        if (tcpDataPresenter != null) tcpDataPresenter.startRunTcp();
    }
    private boolean isFirsIFrame = false;
    private boolean isChecked = false;
    /**
     * video data in
     * @param frameData
     * @param dataLength
     * @param channel
     */
    public void onReadFrame(byte[] frameData, int dataLength,int channel) {
        if (startObtain) {
            if (!isChecked) {
                isFirsIFrame =  InfoExtractor.isIFrame(frameData);
            }
            if (udpFramePresenter != null  && isFirsIFrame){
                isChecked = true;
                byte IBPtype;
                if (InfoExtractor.isIFrame(frameData)){
                    IBPtype = 0x20;
                }else{
                    IBPtype = 0x21;
                }

                udpFramePresenter.onReadFrame(frameData, dataLength, channel, IBPtype);
            }
        }
    }
    @Override
    void onGetIP_PORT(InetAddress ip, int port) {
        udpFramePresenter.setTargetInfo(ip,port);

        try {


            TlvModes tlvModes=new TlvModes();
            tlvModes.setTlvMode(new TlvIntegerMode(TlvMode.T_PORT,UdpReceiver.localPort));
            tlvModes.setTlvMode(new TlvStringMode(TlvMode.T_IP,Utils.getLocalAddress()));
            tcpDataPresenter.sendNomalCommon(TcpCommandType.COMMAND_UDP_IPPORT, PacketCmdHandler.PACKET_RESPONSE,tlvModes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    void onGetSynWds(long boundary, long receivedIndex, long[] missIndexs) {
        udpFramePresenter.delStaleWindows(boundary);
        Log.e("onGetSynWds","boundary:"+boundary +",receivedIndex:"+receivedIndex);
        if (receivedIndex != -1) { udpFramePresenter.delStaleWindows(boundary);
            udpFramePresenter.onGetReceivedPacket(receivedIndex);
            udpFramePresenter.delreceivedIndex(receivedIndex);

            if (missIndexs != null) {     //有丢包信息
                for (int i = 0; i < missIndexs.length; i++) {
                    try {
                        Log.e("onGetSynWds", "missIndex:" + missIndexs[i]);
                        udpFramePresenter.resendPacket(missIndexs[i]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }else{
            Log.e("onGetSynWds", "just windows");
        }
    }
    public void onClose(){
        udpFramePresenter.onClose();
    }

    @Override
    public void onGetObtainStream() {
        startObtain = true;
    }

    @Override
    public void onGetCloseStream() {
//        startObtain = false;
//        udpFramePresenter.onClose();
    }

    @Override
    void onGetLensParams(String params) {

    }


}
