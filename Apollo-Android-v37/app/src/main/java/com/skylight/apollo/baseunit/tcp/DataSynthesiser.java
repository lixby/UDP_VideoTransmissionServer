package com.skylight.apollo.baseunit.tcp;

import com.skylight.apollo.baseunit.tcp.command.PacketCmdHandler;
import com.skylight.apollo.baseunit.tcp.command.TcpCommandManager;
import com.skylight.apollo.baseunit.tcp.command.TcpInitCmdHandler;
import com.skylight.apollo.baseunit.tcp.mode.TlvContentFactory;
import com.skylight.apollo.baseunit.tcp.mode.TlvDataMode;
import com.skylight.apollo.baseunit.tcp.mode.TlvMode;
import com.skylight.apollo.baseunit.tcp.mode.TlvModes;

/**
 * @Author: gengwen
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public class DataSynthesiser {
    private TcpCommandManager commandManager;
    private TcpServer tcpServer;
    public DataSynthesiser(TcpServer tcpServer){
        this.tcpServer=tcpServer;
        commandManager=new TcpCommandManager();
        commandManager.startRun();
    }
    /**
     * offer send common method
     */
    public void sendResPonseCommon(int commandType ,byte[] data){
        switch (commandType){
            case TlvMode.T_RESPONSE_OK:
                sendSuccessInfo(commandType,data);
                break;
            case TlvMode.T_RESPONSE_ERROR:
                sendErrorInfo(commandType,data);
                break;
            default:
                break;
        }
    }


    public void sendNomalCommon(int commandType,int packetType , TlvModes tlvModes){
        PacketCmdHandler handler = new TcpInitCmdHandler();
        handler.setTcpClient(tcpServer);
        handler.setCommandType(commandType);
        handler.setPacketType(packetType);
        handler.setMessage(TlvContentFactory.createTlvContent(tlvModes));
        if (commandManager != null)commandManager.addCommand(handler);
    }

    private void sendErrorInfo(int commandType ,byte[] data) {
        PacketCmdHandler handler = new TcpInitCmdHandler();
        handler.setTcpClient(tcpServer);
        TlvModes tlvModes=new TlvModes();
        tlvModes.setTlvMode(new TlvDataMode(TlvMode.T_RESPONSE_ERROR,data));
        handler.setCommandType(commandType);
        handler.setPacketType(PacketCmdHandler.PACKET_RESPONSE);
        handler.setMessage(TlvContentFactory.createTlvContent(tlvModes));
        if(commandManager!=null){
            commandManager.addCommand(handler);
        }
    }

    private void sendSuccessInfo(int commandType ,byte[] data){
        PacketCmdHandler handler = new TcpInitCmdHandler();
        handler.setTcpClient(tcpServer);
        TlvModes tlvModes=new TlvModes();
        tlvModes.setTlvMode(new TlvDataMode(TlvMode.T_RESPONSE_OK,data));
        handler.setCommandType(commandType);
        handler.setMessage(TlvContentFactory.createTlvContent(tlvModes));
        if(commandManager!=null){
            commandManager.addCommand(handler);
        }
    }
}
