package com.skylight.apollo.baseunit.tcp.mode;


import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvStringMode extends TlvMode {

    /**Tlv Integer value*/
    private String value;

    public TlvStringMode() {
    }

    public TlvStringMode(int type,int length,String value) {
        this.type=type;
        this.length=length;
        this.value = value;
    }

    public TlvStringMode(int type,String value) {
        this.value = value;
        setType(type);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    @Override
    public byte[] createContent() {
        byte[] bType=intToByte2Arr(this.type);
        byte[] bLen =intToByte2Arr(this.length);
        byte[] bValue=null;

        byte[] content =null;
        switch(this.type){
            case T_IP:
                int ipLen=4;
                int contentLen=T_LENGTH+L_LENGTH+ipLen;
                content=new byte[contentLen];
                InetAddress inetAddress= null;
                try {
                    inetAddress = InetAddress.getByName(this.getValue());
                    bValue=inetAddress.getAddress();

                    System.arraycopy(bType,0,content,0,bType.length);
                    System.arraycopy(bLen,0,content,T_LENGTH,bLen.length);
                    System.arraycopy(bValue,0,content,(T_LENGTH+L_LENGTH),bValue.length);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;

        }

        return  content;
    }
}
