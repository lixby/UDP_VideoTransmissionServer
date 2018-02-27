package com.skylight.apollo.baseunit.udp.send;

import android.util.Log;
import com.skylight.apollo.baseunit.bean.FrameDataBean;
import com.skylight.apollo.baseunit.bean.PacketDateBean;
import com.skylight.apollo.baseunit.udp.send.module.RttTimeModule;
import com.skylight.apollo.util.Constants;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @Author: gengwen
 * @Date: 2018/1/3.
 * @Company:Skylight
 * @Description:
 */

public class UsbDataRateControler implements  FrameHandler.PackageOutListener{

    private Executor executor  = Executors.newFixedThreadPool(2);
    private LinkedBlockingQueue<FrameDataBean> frameDatas  = new LinkedBlockingQueue<>();
    private ConcurrentLinkedQueue<PacketDateBean> priorityQueue = new ConcurrentLinkedQueue<>();

    private LinkedHashMap<Long ,PacketDateBean> packetDatas = new LinkedHashMap();

    private long expectedIndex = 0;

    private boolean watchingFrame = true;
    private boolean watchingPacket = true;
    private FrameHandler frameHandler ;

    private Object lock = new Object();
    private long windowSize = -1;
    private RttTimeModule rttTimeModule;
    private long send_Interval;

    public UsbDataRateControler() {
        send_Interval=Constants.repeatTime;
        frameHandler = new FrameHandler();
        frameHandler.setPackageOutListener(this);
        rttTimeModule = new RttTimeModule(10);
        rttTimeModule.startCalculate();
        rttTimeModule.setSendIntervalListener(sendIntervalListener);
        keepWatchingFrame();
        keepWatchingPacket();

    }

    public void updateRttTime(long packetIndex){
        PacketDateBean packetDateBean = packetDatas.get(packetIndex);
        if (packetDateBean != null && !packetDateBean.isRttCalculated){
            long rttTime = System.currentTimeMillis() - packetDateBean.sendTime;
            Log.d("lixby","rttTime="+rttTime+"--|--packetIndex="+packetIndex);
            rttTimeModule.addRttTime(rttTime);
            packetDateBean.isRttCalculated = true;
        }

    }

    public long getRttTime(){
        return rttTimeModule.getAverageTime();
    }

    private void keepWatchingPacket() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                while (watchingPacket) {
                    try {
                        PacketDateBean packet = null;
                        if (!priorityQueue.isEmpty()){
                            packet = priorityQueue.poll();
                            packet.isRttCalculated = true;
                        }else {
                            Log.e("---keepWatchingPacket:","packetDatas.size:"+packetDatas.size());
                            packet = pollPacket();
                        }

                        if (packet != null) {
                            if (onRateEvent != null ){
                                onRateEvent.onPacketOut(packet);
                            }

                        }

                        TimeUnit.MICROSECONDS.sleep(send_Interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    RttTimeModule.SendIntervalListener sendIntervalListener=new RttTimeModule.SendIntervalListener() {
        @Override
        public void SendIntervalChanged(long interval) {
            send_Interval=interval;
        }
    };

    public void addFrame(FrameDataBean frameDataBean){
        if (packetDatas.size() <= 20000) {
            try {
                frameDatas.put(frameDataBean);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void keepWatchingFrame(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (watchingFrame){
                        try {
                            FrameDataBean frameDataBean = frameDatas.take();
                            if (frameDataBean != null){
                                frameHandler.frameHandle(frameDataBean.frameData,
                                        frameDataBean.dataLength,
                                        frameDataBean.channel,
                                        frameDataBean.IBPtype);  //handle frame to packet

                            }
                            frameDataBean = null;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                }
            }
        });

    }

    public void onPacketOut(PacketDateBean packetDateBean) {
        synchronized (lock) {
            packetDatas.put(packetDateBean.packetIndex, packetDateBean);
        }

    }

    private OnRateEvent onRateEvent;

    public void setOnPacketOut(OnRateEvent onRateEvent){
        this.onRateEvent = onRateEvent;
    }

    public interface OnRateEvent{
        void onPacketOut(PacketDateBean packetDateBean);
        void onDelePacket(long packetIndex);
    }

    public  void delStaleWindows(long packetIndex){

        long startTime0 = System.currentTimeMillis();
            windowSize = packetIndex;
            if (expectedIndex < packetIndex) {
                expectedIndex = packetIndex;
            }

            Long[] indexs;
            synchronized (lock) {
                indexs = new Long[packetDatas.size()];
                packetDatas.keySet().toArray(indexs);
            }
            long startTime1 = System.currentTimeMillis();

            for (int i = 0; i < indexs.length; i++) {
                if (indexs[i]<packetIndex){
                    PacketDateBean packetDateBean  = packetDatas.remove(indexs[i]);
                    Log.e("----delStaleWindows","delStaleWindows in"+packetIndex+",dele packet:"+indexs[i]);
                    if (priorityQueue.contains(packetDateBean)) {
                        priorityQueue.remove(packetDateBean);

                    }
                    onRateEvent.onDelePacket(indexs[i]);
                }else {
                    Log.e("----delStaleWindows","delStaleWindows stop:"+indexs[i]);
                    break;
                }
            }

    }

    public void delreceivedIndex(long packetIndex){
            synchronized (lock) {
                PacketDateBean packetDateBean = packetDatas.remove(packetIndex);
                if (packetDateBean != null){
                    priorityQueue.remove(packetDateBean);
                }

            }

    }


   public PacketDateBean pollPacket(){
       if (packetDatas != null){
           PacketDateBean packetDateBean = packetDatas.get(expectedIndex);
           if (packetDateBean != null) {
               expectedIndex = (expectedIndex + 1) & 0xffffffff;
               return packetDateBean;
           }
       }

       return null;
   }

    public PacketDateBean getPacket(long packetIndex){
        if (packetDatas != null){
            PacketDateBean packetDateBean = packetDatas.get(packetIndex);
            return packetDateBean;
        }
        return null;
    }

    public void addPriorityQueue(PacketDateBean packetDateBean){
        priorityQueue.remove(packetDateBean);
        priorityQueue.add(packetDateBean);
    }

    public void stop(){
        watchingPacket = false;
        watchingFrame = false;
        frameDatas.clear();
        synchronized (lock) {
            packetDatas.clear();
        }
        priorityQueue.clear();
    }


}
