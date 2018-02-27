package com.skylight.apollo.baseunit.udp;

import android.util.Log;

import com.skylight.apollo.baseunit.bean.FrameDataBean;
import com.skylight.apollo.baseunit.bean.PacketDateBean;
import com.skylight.apollo.util.Constants;

import java.util.LinkedHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @Author: gengwen
 * @Date: 2018/1/3.
 * @Company:Skylight
 * @Description:
 */

public class UsbDataRateControler implements  FrameHandler.PackageOutListener{
    private Executor executor = Executors.newFixedThreadPool(2);
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private LinkedBlockingQueue<FrameDataBean> frameDatas  = new LinkedBlockingQueue<>();
//    ConcurrentSkipListMap<Long,PacketDateBean> concurrentSkipListMap = new ConcurrentSkipListMap();

    private LinkedHashMap<Long ,PacketDateBean> packetDatas = new LinkedHashMap(500,0.75f);

//    {
//        private int MAX_ENTRIES = 80000;
//        @Override
//        protected boolean removeEldestEntry(Entry eldest) {
//            return size() > MAX_ENTRIES;
//        }
//    };

    private long expectedIndex = 0;

    private boolean watchingFrame = true;
    private boolean watchingPacket = true;
    private FrameHandler frameHandler ;

    private long currentTime = 0;
    private long expectedTime = 0;
    private long lastRunTime = 0;
    private long waitTime = 0;
    private Object lock = new Object();

    /**
     * about rtt
     */
    private long rttTime_average = 0;
    private long rttTime_min = 0;
    private long rttTime_max = 20;

    public UsbDataRateControler() {
        frameHandler = new FrameHandler();
        frameHandler.setPackageOutListener(this);
        keepWatchingFrame();
        keepWatchingPacket();
    }
    public void updateRttTime(long packetIndex){
        if (packetDatas.containsKey(packetIndex)) {
            PacketDateBean packetDateBean = packetDatas.get(packetIndex);
            if (packetDateBean != null && !packetDateBean.isRttCalculated){
                long currentTimeMill = System.currentTimeMillis();
                long rttTime = currentTimeMill - packetDateBean.sendTime;
                if (rttTime > rttTime_max)rttTime_max = rttTime;
                if (rttTime < rttTime_min)rttTime_min = rttTime;
                rttTime_average = (rttTime_max+rttTime_min)/2;
                packetDateBean.isRttCalculated = true;
                Log.e("---updateRttTime",packetIndex+"|"+rttTime_min+"|"+rttTime_max);
            }
        }
    }

    public long getRttTime(){
        return rttTime_average;
    }
    long nano1,nano2,nano3;
//    private void keepWatchingPacket() {
//        Log.e("-------","keepWatchingPacket");
//        executorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                Log.e("-------","keepWatchingPacket 1 "+(nano1-nano2));
//                nano1 = System.nanoTime();
//                PacketDateBean packet = pollPacket();
//                if (packet != null){
//                    if (onPacketOut != null)onPacketOut.onPacketOut(packet,10);
//                }
//                nano2 = System.nanoTime();
//                Log.e("-------","keepWatchingPacket 2 --"+(nano2-nano1));
//            }
//        }, Constants.repeatTime_mill, Constants.repeatTime_mill, TimeUnit.MILLISECONDS);
//    }
    private void keepWatchingPacket() {
        Log.e("-------","keepWatchingPacket");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (watchingPacket) {
                    try {
                        Thread.sleep(Constants.repeatTime_mill);
//                        Thread.sleep(0,Constants.repeatTime_nano);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    nano1 = System.nanoTime();
                    Log.e("-------","keepWatchingPacket 1 ");
                    PacketDateBean packet = pollPacket();
                    nano2 = System.nanoTime();
                    Log.e("-------","keepWatchingPacket 2 ");
                    if (packet != null) {
                        if (onPacketOut != null)
                            onPacketOut.onPacketOut(packet, 10);
                        nano3 = System.nanoTime();
                        Log.e("-------","keepWatchingPacket 3 --");
                        Log.e("-------","keepWatchingPacket 4 --");
                    }
//                    Log.e("-------", "keepWatchingPacket22");
                }
            }
        });
    }
    public void addFrame(FrameDataBean frameDataBean){
        try {
            if (packetDatas.size() <= 80000) {
                frameDatas.put(frameDataBean);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void keepWatchingFrame(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (watchingFrame){
                        try {
                            FrameDataBean frameDataBean = frameDatas.take();
//                            Log.e("------size","frameDatas:"+frameDatas.size());
                            if (frameDataBean != null )frameHandler.frameHandle(frameDataBean.frameData,frameDataBean.dataLength,frameDataBean.channel,frameDataBean.IBPtype);  //handle frame to packet
                            frameDataBean = null;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                }
            }
        });

    }

//    private void keepWatchingPacket(){
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//                while (watchingPacket){
////                    try {
//
////                        if (packetDatas.size() != 0) {
////                            Log.e("----packetDatas","1"+packetDatas.size());
////                            PacketDateBean packetDateBean = packetDatas.get(packetDatas.entrySet().iterator().next());
//                            //                        Log.e("------size","packetDatas:"+packetDatas.size()+"..");
//
//                            PacketDateBean packetDateBean = packetDatas.remove(expectedIndex);
//
//
//                            if (packetDateBean != null) {
//                                expectedIndex = (expectedIndex+1)&0xffffffff;
//                                Log.e("----get","g:"+expectedIndex);
//                                //TODO 计算等待取包时间
//                                expectedTime = lastRunTime + packetDateBean.repeatTime;
//                                currentTime = System.nanoTime();
//
//                                waitTime = expectedTime - currentTime;
//                                //                            while (waitTime < 0){
//                                //                                waitTime += packetDateBean.repeatTime;
//                                //                            }
//                                try {
//                                    Thread.sleep(1);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                if (waitTime > 0) {
//                                    Log.e("run", "------waitTime:" + (int) waitTime + "|" + packetDateBean.repeatTime + "|" + packetDateBean.frameIndex + "|--" + packetDatas.size() + "--|" + packetDateBean.packetIndex + "|" + System.nanoTime());
////                                    long waitTime1 = waitTime / 1000000;
////                                    int waitTime2 = (int) (waitTime % 1000000);
//                                    //                                Log.e("run:", "------sleep.b:" + waitTime1 + "|" + waitTime2 + "|" +
////                                    long nano1 = System.nanoTime();
//                                    //                                Thread.sleep(waitTime1, waitTime2);
////                                    Log.e("run:", "------sleep.a:" + packetDateBean.packetIndex + "|" + (System.nanoTime() - nano1));
//                                } else if (waitTime < 0) {
////                                        Log.e("run:", "------sleep.b" + packetDateBean.packetIndex + "|");
//                                        waitTime += packetDateBean.repeatTime;
//                                }
////                                Log.e("run:", "------sleep.c" + packetDatas.size() + "|");
//                                if (onPacketOut != null)
//                                    onPacketOut.onPacketOut(packetDateBean, waitTime);//发送包
//                                lastRunTime = System.nanoTime();
//                                packetDateBean = null;
//                            }
////                        }
//
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                }
//            }
//        });
//    }
//    @Override
    public void onPacketOut(PacketDateBean packetDateBean) {
        synchronized (lock) {
            packetDatas.put(packetDateBean.packetIndex, packetDateBean);
        }
    }
    private OnPacketOut onPacketOut;
    public void setOnPacketOut(OnPacketOut onPacketOut){
        this.onPacketOut = onPacketOut;
    }
    public interface OnPacketOut{
        void onPacketOut(PacketDateBean packetDateBean,long waitTime);
    }
    public void delStaleWindows(long packetIndex){
        if (packetDatas.containsKey(packetIndex)) {
            Long[] indexs = new Long[packetDatas.size()];
            synchronized (lock) {
                packetDatas.keySet().toArray(indexs);
            }
                for (int i = 0; i < indexs.length; i++) {
                    Log.e("----delStaleWindows",indexs[i]+"..."+packetDatas.size());
                    if (indexs[i] != null && indexs[i] != packetIndex) {
                        packetDatas.remove(indexs[i]);
                        Log.e("----delStaleWindows1",indexs[i]+"..."+packetDatas.size() +"|");
                    } else {
                        break;
                    }
                }

        }
    }
    public void delreceivedIndex(long packetIndex){
        if (packetDatas.containsKey(packetIndex)) {
            Log.e("--received.packetIndex",packetIndex +"|"+packetDatas.size());
            synchronized (lock) {
                packetDatas.remove(packetIndex);
            }
            Log.e("--received.packetIndex",packetIndex +"|"+packetDatas.size());
        }
    }
    //---------------队列
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
            Log.e("--getPacket.packetIndex","----packetIndex:"+packetIndex);
            return packetDateBean;
        }
        return null;
    }
}
