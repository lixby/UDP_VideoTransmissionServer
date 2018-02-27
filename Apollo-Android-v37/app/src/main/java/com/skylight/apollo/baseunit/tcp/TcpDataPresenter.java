package com.skylight.apollo.baseunit.tcp;

import android.util.Log;

import com.skylight.apollo.baseunit.tcp.mode.TlvModes;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Author: gengwen
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public class TcpDataPresenter {
    private TcpServer tcpServer;
    private DataSegregator dataSegregator;
    private DataSynthesiser dataSynthesiser;
    private  DataSynthesiser mDataSynthesiser;
    private Executor executor = Executors.newSingleThreadExecutor();

    public TcpDataPresenter() {
        dataSegregator = DataSegregator.getInstance();
        tcpServer = new TcpServer(6543,dataSegregator);
        mDataSynthesiser = new DataSynthesiser(tcpServer);
        Log.d("--------------------","TcpDataPresenter");
    }

    public void startRunTcp(){
        executor.execute(tcpServer);
    }
    /**
     * offer send common method
     */
    public void sendResPonseCommon( int commandType ,byte[] data){
        mDataSynthesiser.sendResPonseCommon(commandType,data);
    }
    public void sendNomalCommon(int commandType,int packetType ,TlvModes tlvModes){
        mDataSynthesiser.sendNomalCommon(commandType,packetType,tlvModes);
    }
    public void setGetCommonListener(DataSegregator.GetCommonListener getCommonListener){
        if (tcpServer != null) tcpServer.setOnPacketOutListener(getCommonListener);
    }

}
