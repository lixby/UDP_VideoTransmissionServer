package com.skylight.apollo.baseunit.tcp.command;

import com.skylight.apollo.baseunit.tcp.TcpServer;

/**
 * Author: Created by lixby on 17-12-15.
 *
 * <Pre>
 * Head information：
 * Head length=7
 * |----4字节 (标记0x15)----|--1字节(Command type)--|
 * |--1字节(Packet Type)--|--2字节(packet length)--|
 * |----------------content(携带数据)--------------|
 *
 *
 *
 * 第1-4字节：标记0x15，占用4个字节|00 00 00 0x15|
 * 第5字节：Command type  命令类型
 * 第6字节：Packet type  0x40  发送包,0x41 响应包
 * 第7,8字节：Packet length  负载长度(Content_length=N*TLV)
 *
 * content information：携带内容数据
 *
 * |--T（2字节数据类型)--|--L（2字节数据值长度）--|
 * |----------------V（4字节）-----------------|
 *
 *</Pre>
 *
 */


public abstract class PacketCmdHandler<T> extends CommandHandler {

    public static final String TAG="PacketHandler";

    /**Head length 8byte*/
    public static final int HEAD_LENGTH=8;

    /**TCP 指令固定值0x15*/
    public static final int SEND_PACKET_MARK=0X15;

    /**Packet 类型，占1个字节*/
    public int commandType;


    /**请求包*/
    public static final int PACKET_QUEST=0x40;
    /**响应包*/
    public static final int PACKET_RESPONSE=0x41;

    /**指令响应类型，占1个字节*/
    private int packetType=PACKET_QUEST;

    /**executor*/
    private TcpServer tcpClient;
    /**Content data*/
    private byte[] message;

    /**packet length*/
    private int contentLength;


    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
        if(message!=null){
            this.contentLength=message.length;

        }
    }

    public int getCommandType() {
        return commandType;
    }

    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }

    public int getPacketType() {
        return packetType;
    }

    public void setPacketType(int packetType) {
        this.packetType = packetType;
    }

    public TcpServer getTcpClient() {
        return tcpClient;
    }

    public void setTcpClient(TcpServer tcpClient) {
        this.tcpClient = tcpClient;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public abstract  void createMessage(T t);

    /**
     * 大端格式long->8bytes
     * @param src
     * @return byte[]
     */
    protected byte[] longToByte8Arr(long src){
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }
        return byteNum;
    }

    /**
     * 大端格式int->4bytes
     * @param src
     * @return byte[]
     */
    protected byte[] intToByte4Arr(int src){
        byte[] byteNum = new byte[4];
        for (int ix = 0; ix < 4; ++ix) {
            int offset = 32 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }

        return byteNum;
    }

    /**
     * 大端格式int->2bytes
     * @param src
     * @return byte[]
     */
    protected byte[] intToByte2Arr(int src){
        byte[] byteNum = new byte[2];
        for (int ix = 0; ix < 2; ++ix) {
            int offset = 16 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }

        return byteNum;
    }

    /**
     * 计算对应byte[]->long
     * @param byteNum
     * @param offset
     * @param byteSize
     * @return
     */
    protected long bytes2Long(byte[] byteNum,int offset,int byteSize) {
        long num = 0;
        for (int ix = offset; ix < offset + byteSize; ++ix) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    /**
     * Create data for tcp send
     * @param content
     */
    protected byte[] createSendPacket(byte[] content){

        byte[] head_mark=intToByte4Arr(SEND_PACKET_MARK);
        byte head_cmdType= (byte) getCommandType();
        byte head_pkType= (byte) getPacketType();
        byte[] head_contentLen= intToByte2Arr(getContentLength());

        int packetLength=HEAD_LENGTH+getContentLength();
        byte[] result=new byte[packetLength];

        //4byte Head mark
        System.arraycopy(head_mark,0,result,0,head_mark.length);
        //1byte cmd type
        result[4]=head_cmdType;
        //1byte pocket type
        result[5]=head_pkType;
        //2byte Packet length
        System.arraycopy(head_contentLen,0,result,6,head_contentLen.length);

        //Add to TcpPacketReceiver
        if(getPacketType()==PACKET_QUEST){

        }else{

        }

        if(content!=null&&content.length>0){
            System.arraycopy(content,0,result,8,content.length);
        }

//        CmLog.d("tcp send packet len="+result.length);

        return result;
    }


}
