package com.skylight.apollo.baseunit.tcp.mode;

import android.util.Log;


/**
 * Description:
 * Author: Created by lixby on 18-1-4.
 */

public class TlvSAKMode extends TlvMode{

    private long edgeNum;

    private long rightNum;

    private Long[] lostNum;


    public TlvSAKMode(){

    }

    public TlvSAKMode(long edgeNum,long rightNum,Long[] lostNum){
        this.edgeNum=edgeNum;
        this.rightNum=rightNum;
        this.lostNum=lostNum;

    }

    public long getEdgeNum() {
        return edgeNum;
    }

    public void setEdgeNum(long edgeNum) {
        this.edgeNum = edgeNum;
    }

    public long getRightNum() {
        return rightNum;
    }

    public void setRightNum(long rightNum) {
        this.rightNum = rightNum;
    }

    public Long[] getLostNum() {
        return lostNum;
    }

    public void setLostNum(Long[] lostNum) {
        this.lostNum = lostNum;
    }


    @Override
    public byte[] createContent() {
        Log.d("TlvSAKMode","Create TlvSAK createContent-edgeNum---"+edgeNum+"|rightNum="+rightNum);
        TlvLongMode edgeTlv;
        byte[] edges=null;
        int content=0;
        if(edgeNum>=0){
            edgeTlv=new TlvLongMode(T_WINDOW_NUMBER,edgeNum);
            edges=edgeTlv.createContent();
            content=edges.length;
        }

        Log.d("TlvSAKMode","-------------------------0-------------------"+rightNum);
        TlvLongMode rightTlv;
        byte[] rights=null;
        if(rightNum>=0){
            rightTlv=new TlvLongMode(T_POCKET_CONFIRM,rightNum);
            rights=rightTlv.createContent();
            content=content+rights.length;
        }

        Log.d("TlvSAKMode","-------------------------2-------------------"+lostNum);
        TlvPacketLostMode lostTlv;
        byte[] losts=null;
        if(lostNum!=null){
            if(lostNum.length>0){
                lostTlv=new TlvPacketLostMode(T_POCKET_LOST,lostNum);
                losts=lostTlv.createContent();
                content=content+losts.length;

            }
        }

        /*******************************Create SAK Content***********************/
        Log.d("TlvSAKMode","-------------------------3-------------------");
        byte[] result=new byte[content];
        int offset=0;
        if(edges!=null){
            System.arraycopy(edges,0,result,offset,edges.length);
            offset=offset+edges.length;
        }

        if(rights!=null){
            System.arraycopy(rights,0,result,offset,rights.length);
            offset=offset+rights.length;
        }

        if(losts!=null){
            System.arraycopy(losts,0,result,offset,losts.length);
        }

        Log.d("TlvSAKMode","Create TlvSAK result length:"+result.length);
        return result;
    }
}
