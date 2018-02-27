package com.skylight.apollo.baseunit.udp.receive;

import com.skylight.apollo.baseunit.tcp.bean.TLVBean;
import com.skylight.apollo.baseunit.udp.mode.PackageBean;
import com.skylight.apollo.baseunit.util.ChangeUtils;
import com.skylight.apollo.baseunit.util.Utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Date: 2017/12/29.
 * @Company:Skylight
 * @Description:
 */

public class DataSegregator {
    private ConcurrentLinkedQueue<byte[]> dataQueue ;
    private Executor handlePool = Executors.newSingleThreadExecutor();
    private boolean keepWatching = true;
    public final int HEADLENGTH = 8;
    private static DataSegregator instance;
    private DataSegregator() {
        dataQueue = new ConcurrentLinkedQueue<>();
        keepWatching();
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


    private void keepWatching() {
        handlePool.execute(new Runnable() {
            @Override
            public void run() {
                while (keepWatching) {
                    if (dataQueue != null ) {
//                        try {
                            byte[] dataByte = dataQueue.poll();
                            if (dataByte != null ) {
//                                long timeStart = System.currentTimeMillis();
                                PackageBean packageBean = parseTLV(dataByte,dataByte.length);
//                                Log.e("DataSegregator","size:"+dataQueue.size() +"| parseTLV time:"+(System.currentTimeMillis()-timeStart));
                                if (receiveListener != null)receiveListener.onReceived(packageBean);
                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    }
                }
            }
        });
    }

    public PackageBean parseTLV(byte[] packageData,int packageLength) {
        PackageBean packageBean = new PackageBean();
//        packageBean.commandType = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, 4, 0, 1));
//        packageBean.packageType = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, 5, 0, 1));
        packageBean.commandType = ChangeUtils.byteArr2int(packageData, 4,1);
        packageBean.packageType = ChangeUtils.byteArr2int(packageData, 5,1);

        int parsePosition = HEADLENGTH;
        while (parsePosition < packageLength) {
            TLVBean tlvBean = new TLVBean();
            int type = ChangeUtils.byteArr2int(packageData, parsePosition, 2);
            tlvBean.setType(type);
            parsePosition += 2;
            int tlvlen = ChangeUtils.byteArr2int(packageData, parsePosition,2);
            tlvBean.setLength(tlvlen);
            parsePosition += 2;
            byte[] tlvData = Utils.byteMerger(packageData, parsePosition, 0, tlvlen);
            tlvBean.setValue(tlvData);
            parsePosition += tlvlen;
                packageBean.tlvLink.add(tlvBean);
        }
        return packageBean;
    }

    public void addUdpData(byte[] udpData) throws InterruptedException {
        dataQueue.add(udpData);
    }

    /**
     * 回调
     */
    private ReceiveListener receiveListener;
    public void setListener(ReceiveListener receiveListener){
        this.receiveListener = receiveListener;
    }
    public interface ReceiveListener{
        void onReceived(PackageBean packageBean) ;
    }
}
