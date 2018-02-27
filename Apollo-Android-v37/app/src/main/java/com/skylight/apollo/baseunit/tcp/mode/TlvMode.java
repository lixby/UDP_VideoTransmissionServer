package com.skylight.apollo.baseunit.tcp.mode;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvMode {

    /**UDP ip address*/
    public static final int DATA_TEXT=0X1;
    public static final int DATA_INT=0X2;



    /**TLV T length*/
    public static final int T_LENGTH=2;
    /**TLV L length*/
    public static final int L_LENGTH=2;


    /**UDP ip address*/
    public static final int T_IP=0X600;
    /**UDP port*/
    public static final int T_PORT=0X601;
    /**UDP window number*/
    public static final int T_WINDOW_NUMBER=0X602;
    /**UDP packet receive confirm number*/
    public static final int T_POCKET_CONFIRM=0X603;
    /**UDP packet lost*/
    public static final int T_POCKET_LOST=0X604;

    /**TCP response state*/
    public static final int T_RESPONSE_OK=0X605;
    public static final int T_RESPONSE_ERROR=0X606;

    /**TLV response result code length*/
    public static final int V_RESCODE_LEN=1;

    public static final int V_SERUDP_PORT_LEN=2;
    public static final int V_SERUDP_IP_LEN=4;




    /**Tlv type*/
    protected int type;

    /**Tlv 所带Valur的长度*/
    protected int length;

    /**Tlv 生成结果*/
    protected  byte[] tlvData;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;

        switch (this.type){
            case T_PORT: //2 bytes
                this.length=2;
                break;
            case T_IP: //4 bytes
            case T_WINDOW_NUMBER:
            case T_POCKET_CONFIRM:
                this.length=4;
                break;
        }
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getTlvData() {
        return tlvData;
    }

    public void setTlvData(byte[] tlvData) {
        this.tlvData = tlvData;
    }

    public byte[] createContent(){
        return new byte[0];
    }


    /**
     * 大端格式int->4bytes
     * @param src
     * @return byte[]
     */
    public byte[] intToByte4Arr(int src){
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
    public byte[] intToByte2Arr(int src){
        byte[] byteNum = new byte[2];
        for (int ix = 0; ix < 2; ++ix) {
            int offset = 16 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }

        return byteNum;
    }

    /**
     * 大端格式long->8bytes
     * @param src
     * @return byte[]
     */
    public byte[] longToByte4Arr(long src){
        byte[] byteNum = new byte[4];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 32 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }
        return byteNum;
    }

    public byte[] long2byteArr(long src,int lens){
        byte[] byteNum = new byte[lens];
        for (int ix = 0; ix < lens; ix++) {
            int offset = 8 * (lens - ix - 1) ;
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
    public long bytes2Long(byte[] byteNum,int offset,int byteSize) {
        long num = 0;
        for (int ix = offset; ix < offset + byteSize; ix++) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    /**
     * 计算对应byte[]->int
     * @param byteNum
     * @param offset
     * @param byteSize
     * @return
     */
    public int bytes2Int(byte[] byteNum,int offset,int byteSize) {
        int num = 0;
        for (int ix = offset; ix < offset + byteSize; ix++) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }


}
