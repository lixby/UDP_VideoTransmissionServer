package com.skylight.apollo.baseunit.tcp.mode;

import android.util.Log;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvPacketLostMode extends TlvMode {

    /**TLV T length*/
    public static final int V_LENGTH=4;

    /**Many packages may be lost */
    private Long[] values;

    public TlvPacketLostMode() {

    }

    public TlvPacketLostMode(int type,int length,Long[] values) {
        this.type=type;
        this.length=length;
        this.values = values;
    }

    public TlvPacketLostMode(int type,Long[] values) {
        this.type=type;
        this.values = values;
    }

    public Long[] getValues() {
        return values;
    }

    public void setValues(Long[] values) {
        this.values = values;
    }

    @Override
    public byte[] createContent() {
        int valueLen=values.length*V_LENGTH;

        byte[] bType=intToByte2Arr(this.type);
        byte[] bLen =intToByte2Arr(valueLen);

        byte[] content =new byte[T_LENGTH+L_LENGTH+valueLen];

        int offset=0;
        System.arraycopy(bType,0,content,offset,bType.length);
        offset+=T_LENGTH;
        System.arraycopy(bLen,0,content,offset,bLen.length);

        int startPos=offset+L_LENGTH;
        for (int i = 0; i <values.length ; i++) {
            byte[] bValue=long2byteArr(values[i],4);
            System.arraycopy(bValue,0,content,startPos,bValue.length);
            startPos=startPos+bValue.length;
        }

        //Log.d("TlvSAKMode","Create TlvPacketLost content length:"+content.length);
        return content;
    }
}
