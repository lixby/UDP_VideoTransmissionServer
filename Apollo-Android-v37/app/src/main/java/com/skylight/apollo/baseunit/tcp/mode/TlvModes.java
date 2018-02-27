package com.skylight.apollo.baseunit.tcp.mode;


import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * Author: Created by lixby on 17-12-28.
 */

public class TlvModes {

    private List<TlvMode> tlvModes;
    private int contentLength=0;

    public TlvModes(){
        if(tlvModes==null){
            tlvModes=new ArrayList<>();
        }
    }

    public void setTlvMode(TlvMode tlvMode){
        byte[] tlvData=tlvMode.createContent();
        tlvMode.setTlvData(tlvData);
        contentLength=contentLength+tlvData.length;
        tlvModes.add(tlvMode);
    }

    public byte[] createTlvListContent(){
        byte[] content=new byte[contentLength];
        int contentPos=0;
        for (int i = 0; i <tlvModes.size() ; i++) {
            int tlvLen=tlvModes.get(i).getTlvData().length;
            System.arraycopy(tlvModes.get(i).getTlvData(),0,content,contentPos,tlvLen);
            contentPos=contentPos+tlvLen;
        }

        tlvModes.clear();
        contentLength=0;
        return content;
    }

}
