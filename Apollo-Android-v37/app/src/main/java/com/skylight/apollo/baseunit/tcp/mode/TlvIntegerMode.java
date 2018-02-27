package com.skylight.apollo.baseunit.tcp.mode;


/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvIntegerMode extends TlvMode {

    /**Tlv Integer value*/
    private int value;

    public TlvIntegerMode() {
    }

    public TlvIntegerMode(int type,int length,int value) {
        this.type=type;
        this.length=length;
        this.value = value;
    }

    public TlvIntegerMode(int type,int value) {
        this.value = value;
        setType(type);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public byte[] createContent(){
        byte[] bType=intToByte2Arr(this.type);
        byte[] bLen =intToByte2Arr(this.length);
        byte[] bValue=null;

        byte[] content =null;
        switch(this.type){
            case T_PORT:
                int portLen=2;
                int contentLen=T_LENGTH+L_LENGTH+portLen;
                content=new byte[contentLen];
                bValue=intToByte2Arr(this.value);

                System.arraycopy(bType,0,content,0,bType.length);
                System.arraycopy(bLen,0,content,T_LENGTH,bLen.length);
                System.arraycopy(bValue,0,content,(T_LENGTH+L_LENGTH),bValue.length);
                break;
            default:
                break;

        }

        return  content;
    }


}
