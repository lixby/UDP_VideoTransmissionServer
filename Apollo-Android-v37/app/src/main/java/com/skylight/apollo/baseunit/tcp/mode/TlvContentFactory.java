package com.skylight.apollo.baseunit.tcp.mode;


/**
 * Description: this is used create TLV mode,and return byte[].
 * Author: Created by lixby on 17-12-28.
 */

public class TlvContentFactory {

    public static byte[] createTlvContent(TlvMode tlvMode){
        if(tlvMode==null){
            return new byte[0];
        }

        if(tlvMode instanceof  TlvIntegerMode){
            TlvIntegerMode tlvIntegerMode= (TlvIntegerMode) tlvMode;
            return tlvIntegerMode.createContent();

        }else if(tlvMode instanceof  TlvStringMode){
            TlvStringMode tlvStringMode= (TlvStringMode) tlvMode;
            return tlvStringMode.createContent();

        }else if(tlvMode instanceof TlvPacketLostMode){
            TlvPacketLostMode tlvPacketLostMode= (TlvPacketLostMode) tlvMode;
            return tlvPacketLostMode.createContent();
        }

        return new byte[0];
    }


    public static byte[] createTlvContent(TlvModes tlvMOdes){
        if(tlvMOdes==null){
            return new byte[0];
        }
        return tlvMOdes.createTlvListContent();
    }


}
