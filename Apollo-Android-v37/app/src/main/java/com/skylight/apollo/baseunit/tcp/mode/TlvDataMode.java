package com.skylight.apollo.baseunit.tcp.mode;

import android.util.Log;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvDataMode extends TlvMode {

    /**
     * Tlv Integer value
     */
    private byte[] value;

    public TlvDataMode() {
    }

    public TlvDataMode(int type, int length, byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    public TlvDataMode(int type, byte[] value) {
        this.value = value;
        if (value != null) {
            this.length = value.length;
        }
        setType(type);
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }


    @Override
    public byte[] createContent() {
        byte[] bType = intToByte2Arr(this.type);
        byte[] bLen = null;
        byte[] content = null;
        switch (this.type) {
            case T_RESPONSE_OK:
                bLen = intToByte2Arr(this.length);
                content = new byte[4 + length];
                System.arraycopy(bType, 0, content, 0, bType.length);
                System.arraycopy(bLen, 0, content, 2, bLen.length);
                if (value != null) {
                    System.arraycopy(value, 0, content, 4, value.length);
                }
                break;
            case T_RESPONSE_ERROR:
                bLen = intToByte2Arr(this.length);
                content = new byte[4 + length];
                System.arraycopy(bType, 0, content, 0, bType.length);
                System.arraycopy(bLen, 0, content, 2, bLen.length);
                if (value != null) {
                    System.arraycopy(value, 0, content, 4, value.length);
                }
                break;
            default:
                break;

        }

        Log.d("", "Create TlvString content length:" + content.length);
        return content;
    }
}
