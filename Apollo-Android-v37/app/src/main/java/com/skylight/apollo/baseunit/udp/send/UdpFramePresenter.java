package com.skylight.apollo.baseunit.udp.send;


import android.util.Log;

import com.skylight.apollo.baseunit.bean.FrameDataBean;
import com.skylight.apollo.baseunit.bean.PacketDateBean;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * 帧处理器，用于分包等处理
 * @author gengwen
 *
 */
public class UdpFramePresenter implements UsbDataRateControler.OnRateEvent{
	private String TAG = "UdpFramePresenter:";


	private UdpServer udpServer;

	private UsbDataRateControler usbDataRateControler;
	private HashMap<Long,Long> rttMap = new HashMap<>();


	public UdpFramePresenter() {
		usbDataRateControler = new UsbDataRateControler();
		usbDataRateControler.setOnPacketOut(this);
		Log.e("-----111","UdpFramePresenter");
		try {
			udpServer = new UdpServer();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * set target client s ip & port
	 * @throws UnknownHostException
     */
	public void setTargetInfo(InetAddress targetIp, int targetPort){
		if (udpServer != null)udpServer.setTargetIp(targetIp,targetPort);
	}

	/**
	 * video data in
	 * @param frameData
	 * @param dataLength
	 * @param channel
     */
	public void onReadFrame(byte[] frameData, int dataLength,int channel,byte IBPtype) {
		FrameDataBean frameDataBean = new FrameDataBean(frameData,dataLength,channel,IBPtype);
		usbDataRateControler.addFrame(frameDataBean);

	}

	/**
	 * 包输出回调
     */
	@Override
	public void onPacketOut(PacketDateBean packetDateBean) {
		if (udpServer != null){
			udpServer.sendData(packetDateBean.packetData,packetDateBean.packetIndex);
			packetDateBean.sendTime = System.currentTimeMillis();
		}
	}

	@Override
	public void onDelePacket(long packetIndex) {
		if (rttMap.containsKey(packetIndex))rttMap.remove(packetIndex);
	}

	public void delStaleWindows(long packetIndex){
		usbDataRateControler.delStaleWindows(packetIndex);
	}
	public void delreceivedIndex(long packetIndex){
		usbDataRateControler.delreceivedIndex(packetIndex);
	}
	public void resendPacket(long packetIndex) throws IOException {
		if (rttMap.containsKey(packetIndex)) { //有rtt时间
			long currentTime = System.currentTimeMillis();
			long lastSendTime = rttMap.get(packetIndex);
			long time = currentTime - lastSendTime;   //
			if (time >=usbDataRateControler.getRttTime() ){//
				PacketDateBean packetDateBean = usbDataRateControler.getPacket(packetIndex);
				if (packetDateBean != null) {
//					Log.e("----resendPacket0:", "has rtt " + packetIndex + "found. rttTime = " +usbDataRateControler.getRttTime());
//					udpServer.sendData(packetDateBean.packetData,packetIndex);
					//modify : don`t send right now,just add priorityQueue and wait to keep the rate
					usbDataRateControler.addPriorityQueue(packetDateBean);
					rttMap.put(packetIndex,currentTime);
				}else {
//					Log.e("----resendPacket0:", "has rtt " + packetIndex + "not found. rttTime = "+usbDataRateControler.getRttTime());
				}
			}else{
//				Log.e("----resendPacket:", packetIndex + "isSending"+"| time = "+time+",rttTime = "+usbDataRateControler.getRttTime());
			}
		}else{									//无rtt时间
			PacketDateBean packetDateBean = usbDataRateControler.getPacket(packetIndex);
			if (packetDateBean != null) {
//				Log.e("----resendPacket:", "no rtt : " + packetIndex + "found");
//				udpServer.sendData(packetDateBean.packetData,packetIndex);
				usbDataRateControler.addPriorityQueue(packetDateBean);
				rttMap.put(packetIndex,System.currentTimeMillis());
			}else {
//				Log.e("----resendPacket:", "no rtt : " + packetIndex + " not found");
			}
		}
	}
	public void onGetReceivedPacket(long packetIndex){
		usbDataRateControler.updateRttTime(packetIndex);
	}
	public void onClose(){
		usbDataRateControler.stop();
		rttMap.clear();
//		udpServer.close();
	}
}
