package com.skylight.apollo.baseunit.udp.send.module;

import android.util.Log;
import com.skylight.apollo.util.Constants;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author: gengwen
 * @Date: 2018/1/20.
 * @Company:Skylight
 * @Description:rtt时间模块
 */

public class RttTimeModule {

    private String TAG = "RttTimeModule";

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture calculateFuture;
    private Object lock = new Object();

    private Long[] rttTimes;
    private int desIndex = 0;
    private int limit = -1;
    private int currentSize = 0;
    /**是否超过limit*/
    private boolean isExceed = false;

    private long rttTime_average = 0;
    private long rttMin=-1;
    private long rttMax=-1;
    private long rttRecent=-1;
    private long last_rttTime_average = 0;
    private long fluctuation_rttTime = 0;
    private long INTERVAL_TIME=1000;

    /**网络未拥塞*/
    private static final double T_UNLOADED=0.05;
    /**网络拥塞*/
    private static final double T_LOADED=0.1;

    /**最大发送间隔*/
    private long maxSendInterval=Constants.repeatTime*10;
    /**最小发送间隔*/
    private long minSendInterval=Constants.repeatTime;
    /**当前发送间隔*/
    private long curSendInterval= Constants.repeatTime;
    /**和式增量I,每次减少1ms,缩短发送间隔时间.*/
    private int Increment_I=1;
    /**每次乘2,增大发送间隔，减小发送频率.*/
    private int Decrement_D=2;

    private boolean sendIntervalChanaged=false;

    /**弱化因子*/
    private static final double G=0.125;

    private Runnable calculateTask = new Runnable() {

        @Override
        public void run() {
            synchronized (lock) {
                if (currentSize > 0){
                    calculateAverageTime();
                }

            }
        }
    };

    public RttTimeModule(int limit) {
        this.limit = limit;
        rttTimes = new Long[limit];

    }

    public void startCalculate(){
        INTERVAL_TIME=Constants.repeatTime*10;
        calculateFuture = executorService.
                scheduleAtFixedRate(calculateTask,INTERVAL_TIME,INTERVAL_TIME, TimeUnit.MILLISECONDS);

    }

    public void stopCalculate(){
        if (calculateFuture != null){
            calculateFuture.cancel(true);
        }

    }

    public  void addRttTime(long rttTime){
        synchronized (lock) {
            /*rttRecent=rttTime;
            if(rttMin==-1&&rttMax==-1){
                rttMin=rttTime;
                rttMax=rttTime;
            }

            if(rttTime<rttMin){
                rttMin=rttTime;
            }

            if(rttTime>rttMax){
                rttMax=rttTime;
            }*/

            //坐标 size计算
            if (desIndex < limit) {
                rttTimes[desIndex] = rttTime;
                desIndex++;
                if (!isExceed){
                    currentSize = desIndex;
                }

            } else {
                isExceed = true;
                desIndex = 0;
                rttTimes[desIndex] = rttTime;

            }
        }
    }

    /**
     * 计算平均值
     * @return
     */
    private void calculateAverageTime(){
        int sum = 0;
        for (int i = 0; i < currentSize; i++) {
            sum += rttTimes[i];
        }

        rttTime_average = sum / currentSize;
        fluctuation_rttTime = rttTime_average - last_rttTime_average;
        last_rttTime_average = rttTime_average;

        /*float next_Rtt=predictionNextRtt(rttTime_average,rttRecent);
        float p=predictionNetLoad(next_Rtt,rttMin,rttMax);
        judgeNetStatus(p);
        Log.i("lixby","rttTime_average="+rttTime_average+"--|next_Rtt="+next_Rtt+"--|p="+p);*/

        Log.i("lixby","rttTime_average="+rttTime_average);

    }

    public long getAverageTime(){
        return rttTime_average;
    }

    /**
     *Predict RTT time for next packet
     * @param Rtt_n  Rtt平均值
     * @param Rtt_m  最近一次Rtt
     * @return
     */
    private float predictionNextRtt(long Rtt_n,long Rtt_m){
        double c_G=(Rtt_m-Rtt_n)*G;
        double next_Rtt=Rtt_n+c_G;

        BigDecimal a = new BigDecimal(next_Rtt);
        float new_next_Rtt = a.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

        return new_next_Rtt;
    }

    /**
     * Predict network load status
     * @param next_Rtt 预测的那下一个Rtt
     * @param minRtt 最小Rtt时间
     * @return
     */
    private float predictionNetLoad(float next_Rtt,long minRtt,long maxRtt){
        float P=(next_Rtt-minRtt)/(maxRtt-minRtt);
        BigDecimal a = new BigDecimal(P);
        //保留两位小数
        float p = a.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

        return p;
    }

    /**
     * Judge the network status
     * @param p 网络拥塞密度
     */
    private void judgeNetStatus(float p){
        if(sendIntervalChanaged){
            return;
        }

        if(p<=T_UNLOADED){//网络未拥塞，需要减小发送间隔.
            Log.i("lixby","----网络状况未拥塞----");
            curSendInterval=Math.max(curSendInterval-Increment_I,minSendInterval);

        }else if(p>=T_UNLOADED&&p<=T_LOADED){//网络正常，不需要改变发送间隔.
            Log.i("lixby","----网络状况正常----");

        }else{//网络拥塞，需要减小发送间隔.
            Log.i("lixby","----网络状况拥塞----");
            //curSendInterval=curSendInterval+Increment_I;
            curSendInterval=Math.min(curSendInterval*Decrement_D,maxSendInterval);
        }

        Log.e("lixby","----esend-interval----"+curSendInterval);
        if(sendIntervalListener!=null){
            sendIntervalListener.SendIntervalChanged(curSendInterval);
        }

        sendIntervalChanaged=true;

        /*INTERVAL_TIME=Math.max(curSendInterval*10,100);
        if(calculateFuture!=null){
            calculateFuture.cancel(true);
            calculateFuture=null;
            calculateFuture = executorService.
                    scheduleAtFixedRate(calculateTask,INTERVAL_TIME,INTERVAL_TIME, TimeUnit.MILLISECONDS);
        }*/

        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                sendIntervalChanaged=false;

            }

        },5,TimeUnit.SECONDS);

    }


    private SendIntervalListener sendIntervalListener;

    public void setSendIntervalListener(SendIntervalListener sendIntervalListener) {
        this.sendIntervalListener = sendIntervalListener;
    }

    public interface SendIntervalListener{
        void SendIntervalChanged(long interval);

    }




}
