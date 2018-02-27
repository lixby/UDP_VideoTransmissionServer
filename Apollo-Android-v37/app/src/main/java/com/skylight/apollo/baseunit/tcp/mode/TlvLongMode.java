package com.skylight.apollo.baseunit.tcp.mode;

import android.util.Log;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvLongMode extends TlvMode {

    /**Tlv Integer value*/
    private long value;

    public TlvLongMode() {
    }

    public TlvLongMode(int type, int length, long value) {
        this.type=type;
        this.length=length;
        this.value = value;
    }

    public TlvLongMode(int type, long value) {
        this.value = value;
        setType(type);
    }

    public long getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public byte[] createContent(){
        byte[] bType=intToByte2Arr(this.type);
        byte[] bLen =intToByte2Arr(this.length);

        byte[] content =null;
        switch(this.type){
            case T_WINDOW_NUMBER:
            case T_POCKET_CONFIRM:
                int vLen=4;
                int contentLen=T_LENGTH+L_LENGTH+vLen;
                content=new byte[contentLen];
                byte[] bValue=long2byteArr(this.value,4);
                System.arraycopy(bType,0,content,0,bType.length);
                System.arraycopy(bLen,0,content,T_LENGTH,bLen.length);
                System.arraycopy(bValue,0,content,(T_LENGTH+L_LENGTH),bValue.length);
                break;
            default:
                break;

        }

        Log.d("TlvLongMode","Create TlvInteger content type:"+this.type+"|len:"+content.length);
        return  content;
    }


}
