package com.skylight.apollo.baseunit.tcp.command;

/**
 * Description:
 * Author: Created by lixby on 17-12-20.
 */

public class TcpCommandType {

    /**Send udp ip and port*/
    public static final int COMMAND_UDP_IPPORT=0x20;

    /**Synchronize window size*/
    public static final int COMMAND_SYNC_WIN_INFRO=0x21;

    /**Start stream*/
    public static final int COMMAND_START_STREAM=0x22;
    /**Stop stream*/
    public static final int COMMAND_STOP_STREAM=0x23;

    /**get lens params*/
    public static final int COMMAND_GET_LENS=0x24;
    /**reponse command*/
    public static final int COMMAND_REPONSE=0x41;




}
