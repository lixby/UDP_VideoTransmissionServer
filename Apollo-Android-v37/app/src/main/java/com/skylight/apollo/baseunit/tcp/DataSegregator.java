package com.skylight.apollo.baseunit.tcp;

import android.util.Log;

import com.skylight.apollo.baseunit.tcp.bean.PackageBean;
import com.skylight.apollo.baseunit.tcp.bean.TcpPackage2;
import com.skylight.apollo.baseunit.util.ChangeUtils;
import com.skylight.apollo.baseunit.util.Utils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Author: gengwen
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public class DataSegregator {
    private ConcurrentLinkedQueue<byte[]> dataQueue ;
    public TcpPackage2 halfPacket;
    private Executor handlePool = Executors.newSingleThreadExecutor();
    private boolean keepWatching = true;

    private static DataSegregator instance;
    private int mPacketLens;
    private DataSegregator() {
        halfPacket = new TcpPackage2();
        dataQueue = new ConcurrentLinkedQueue<>();
        keepWatching();
        Log.d("-------------","DataSegregator");
    }

    public static DataSegregator getInstance(){
        if (instance == null)instance = new DataSegregator();
        return instance;
    }



    /**
     *
     *<Pre>
     * Head information：
     * Head length=8
     * |--4字节(标记0x15(21))---|--1字节（Common type）----|
     *
     * |--1字节（ Packet type）--|--2字节（Packet length）--|
     *
     * |--------------content（携带数据）------------------|
     *
     * 第1-4字节：标记0x15，			|00 00 00 0x15|
     *
     * 第5字节：Common type    		命令类型
     *
     * 第6字节：  packet type			0x40发送包
     * 							    0x41 响应包
     *
     * 第7,8字节：Content length  	负载长度
     *
     * Content information：携带内容数据，为TLV格式：
     *
     * |--T(2byte)--|---L(2byte)---|
     * |------------V--------------|
     *
     * |--T(2byte)--|---L(2byte)---|
     * |------------V--------------|
     *
     *			  ......
     *
     *TLV格式定义如下：
     *	1)T：Type 数据类型
     *
     * 	2)L：Length Value的长度
     *
     *  3)V：value 携带的数据
     *</Pre>
     */
    private void splitPackage(byte[] source) {
        halfPacket.setOnFinishListener(new TcpPackage2.OnFinishListener() {
            @Override
            public void onFinish(PackageBean packageBean) {
                //TODO
                System.out.println("$$$$$$$$$$$$");
                if (getCommonListener != null) getCommonListener.onCommonout(packageBean);
            }
        });
        int lenStart = TcpUtils.findDatahead(source, 0);
        boolean startWhile = false;
        if (lenStart != -1) {
            System.out.println("parsedata ------1------");
            if (lenStart != 0) {
                halfPacket.add(source, 0, lenStart);
                startWhile = true;
            }else{
                System.out.println("parsedata ------2------");
                halfPacket.reset();
                if (source.length>halfPacket.HEADLENGTH){
                    mPacketLens = ChangeUtils.byteArr2int(Utils.byteMerger(source, 6, 0, 2))+halfPacket.HEADLENGTH;
                }
                lenStart = TcpUtils.findDatahead(source, TcpPackage2.HEAD_FLAG_LENGTH);
                while (mPacketLens!=0 && lenStart!=-1 && lenStart<mPacketLens){
                    System.out.println("parsedata ------17------");
                    lenStart = TcpUtils.findDatahead(source, lenStart+1);
                }
                if(lenStart!=-1){
                    System.out.println("parsedata ------3------");
                    //这是一个完整的包
                    mPacketLens = ChangeUtils.byteArr2int(Utils.byteMerger(source, 6, 0, 2))+halfPacket.HEADLENGTH;
                    byte[] packageData=new byte[mPacketLens];
                    System.arraycopy(source, 0, packageData, 0, mPacketLens);
                    PackageBean packageBean=halfPacket.parseTLV(packageData);
                    if (getCommonListener != null) getCommonListener.onCommonout(packageBean);
                    startWhile = true;
                }else{
                    System.out.println("parsedata ------4------");
                    if (source.length >= halfPacket.HEADLENGTH) {
                        System.out.println("parsedata ------5------");
                        int packetLens  = ChangeUtils.byteArr2int(Utils.byteMerger(source, 6, 0, 2))+halfPacket.HEADLENGTH;
                        System.out.println("packetLens ------5------"+packetLens);
                        if(source.length <packetLens){
                            System.out.println("parsedata ------6------");
                            // 当前数据小于发过来的整包长度 ,将当前整段数据加入半包中
                            halfPacket.add(source, 0, source.length);

                        }else{
                            System.out.println("parsedata ------7------");
                            byte[] packageData=new byte[packetLens];
                            System.arraycopy(source, 0, packageData, 0, packetLens);
                            PackageBean packageBean=halfPacket.parseTLV(packageData);
                            if (getCommonListener != null) getCommonListener.onCommonout(packageBean);
                            if(source.length-packetLens>0){
                                System.out.println("parsedata ------8------");
                                //如果数据比一个完整的包还大，说明还有剩余
                                halfPacket.reset();
                                halfPacket.add(source, packetLens, source.length);
                            }

                        }
                    }else{
                        System.out.println("parsedata ------9------");
                        halfPacket.add(source, 0, source.length);
                    }
                    startWhile = false;
                }
            }
            while (startWhile) {
                if (source.length>lenStart+halfPacket.HEADLENGTH){
                    mPacketLens  = ChangeUtils.byteArr2int(Utils.byteMerger(source, lenStart+6, 0, 2))+halfPacket.HEADLENGTH;
                }
                int midLenStart = TcpUtils.findDatahead(source, lenStart + halfPacket.HEADLENGTH);
                while (mPacketLens!=0 && midLenStart!=-1 && midLenStart-lenStart<mPacketLens){
                    System.out.println("parsedata ------18------");
                    byte[] bytes=new byte[source.length-lenStart];
                    System.arraycopy(source, lenStart, bytes, 0, source.length-lenStart);
                    System.out.println("howardparsedata = "+ Arrays.toString(bytes));
                    midLenStart = TcpUtils.findDatahead(source, midLenStart+1);
                }
                if (midLenStart != -1) {
                    System.out.println("parsedata ------10------");
                    mPacketLens  = ChangeUtils.byteArr2int(Utils.byteMerger(source, lenStart+6, 0, 2))+halfPacket.HEADLENGTH;
                    byte[] packageData=new byte[mPacketLens];
                    //					System.arraycopy(source, lenStart, packageData, 0, midLenStart-lenStart);
                    System.arraycopy(source, lenStart, packageData, 0, mPacketLens);
                    PackageBean packageBean=halfPacket.parseTLV(packageData);
                    if (getCommonListener != null) getCommonListener.onCommonout(packageBean);
                    lenStart=midLenStart;
                }else{
                    System.out.println("parsedata ------11------");
                    System.out.println("parsedata mPacketLens = "+mPacketLens);
                    System.out.println("parsedata LEN = "+(source.length-lenStart));
                    if (source.length-lenStart >= halfPacket.HEADLENGTH) {
                        System.out.println("parsedata ------12------");
                        int packetLens  = ChangeUtils.byteArr2int(Utils.byteMerger(source, lenStart+6, 0, 2))+halfPacket.HEADLENGTH;
                        if(source.length-lenStart <packetLens){
                            halfPacket.reset();
                            System.out.println("parsedata ------13------");
                            // 当前数据小于发过来的整包长度 ,将当前整段数据加入半包中
                            halfPacket.add(source, lenStart, source.length);

                        }else{
                            System.out.println("parsedata ------14------");
                            byte[] packageData=new byte[packetLens];
                            System.arraycopy(source, lenStart, packageData, 0, packetLens);
                            PackageBean packageBean=halfPacket.parseTLV(packageData);
                            if (getCommonListener != null) getCommonListener.onCommonout(packageBean);
                            if(source.length-lenStart-packetLens>0){
                                //如果数据比一个完整的包还大，说明还有剩余
                                halfPacket.reset();
                                halfPacket.add(source, packetLens+lenStart+packetLens, source.length);
                            }

                        }
                    }else{
                        System.out.println("parsedata ------15------");
                        halfPacket.reset();
                        halfPacket.add(source, lenStart, source.length);
                    }
                    startWhile=false;
                }
            }
        }else{
            System.out.println("parsedata ------16------");
            halfPacket.add(source, 0, source.length);
        }
    }

    private void keepWatching() {
        handlePool.execute(new Runnable() {
            @Override
            public void run() {
                while (keepWatching) {
                    if (dataQueue != null ) {

//                        try {
                            byte[] dataByte = dataQueue.poll();

                            if (dataByte != null ) {
                                splitPackage(dataByte);
                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }

                    }
                }
            }
        });
    }

    public void addTcpData(byte[] tcpData) throws InterruptedException {
        dataQueue.add(tcpData);
    }

    /**
     * 回调
     */
    private GetCommonListener getCommonListener;
    public void setListener(GetCommonListener getCommonListener){
        this.getCommonListener = getCommonListener;
    }
    public interface GetCommonListener{
        void onCommonout(PackageBean packageBean) ;
    }
}
