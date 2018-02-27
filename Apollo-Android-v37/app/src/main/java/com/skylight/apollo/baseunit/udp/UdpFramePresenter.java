package com.skylight.apollo.baseunit.udp;


import android.util.Log;
import com.skylight.apollo.baseunit.bean.FrameDataBean;
import com.skylight.apollo.baseunit.bean.PacketDateBean;
import com.skylight.apollo.baseunit.tcp.bean.TLVBean;
import com.skylight.apollo.baseunit.udp.mode.PackageBean;
import com.skylight.apollo.baseunit.udp.mode.ResendPacket;
import com.skylight.apollo.baseunit.udp.receive.DataSegregator;
import com.skylight.apollo.baseunit.udp.receive.UdpReceiver;
import com.skylight.apollo.baseunit.udp.send.UdpServer;
import com.skylight.apollo.baseunit.udp.send.UsbDataRateControler;
import com.skylight.apollo.baseunit.util.ChangeUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author gengwen
 *
 */
public class UdpFramePresenter implements UsbDataRateControler.OnRateEvent, DataSegregator.ReceiveListener{

	private String TAG = "UdpFramePresenter:";

	private UdpServer udpServer;
	private UdpReceiver udpReceiver;

	private UsbDataRateControler usbDataRateControler;
	private ConcurrentHashMap<Long,ResendPacket> missMap = new ConcurrentHashMap<>();

	private long windowSize = 0;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	private Runnable autoSendTask = new Runnable() {

		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			long rttTime = usbDataRateControler.getRttTime();
			if (missMap.size()>0 && rttTime > 0) {
				Long[] indexs = new Long[missMap.size()];
				missMap.keySet().toArray(indexs);

				for (int i=0;i < indexs.length;i++){
					if (indexs[i]!= null) {
						ResendPacket resendPacket = missMap.get(indexs[i]);
						if (resendPacket != null) {
							long newRttTime = resendPacket.rttTime - 5;
							if (newRttTime > 0) {
								resendPacket.rttTime = newRttTime;
							} else {
//								resendPacket.rttTime = resendPacket.backUpRttime;
								resendPacket.rttTime = usbDataRateControler.getRttTime();
								resendPacket.sendedTimes++;
								PacketDateBean packetDateBean = usbDataRateControler.getPacket(resendPacket.packetIndex);
								addPriorityQueue(packetDateBean);
								if (resendPacket.sendedTimes > ResendPacket.limit)missMap.remove(indexs[i]);
							}
						}
					}
				}

			}

		}
	};


	public UdpFramePresenter() {
		usbDataRateControler = new UsbDataRateControler();
		usbDataRateControler.setOnPacketOut(this);
		try {
			udpServer = new UdpServer();
			udpReceiver = new UdpReceiver();
			udpReceiver.setOnReceivedListener(this);
			udpReceiver.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		executorService.scheduleWithFixedDelay(autoSendTask,5,5, TimeUnit.MILLISECONDS);
	}

	/**
	 * set target client s ip & port
	 * @throws UnknownHostException
     */
	public void setTargetInfo(InetAddress targetIp, int targetPort){
		if (udpServer != null){
			udpServer.setTargetIp(targetIp,targetPort);
		}

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
	public void onPacketOut(final PacketDateBean packetDateBean) {
		if (udpServer != null){
			udpServer.sendData(packetDateBean.packetData,packetDateBean.packetIndex);
		}
		packetDateBean.sendTime = System.currentTimeMillis();

	}

	@Override
	public void onDelePacket(long packetIndex) {
		missMap.remove(packetIndex);
	}

	public void delStaleWindows(long packetIndex){
		if (packetIndex > windowSize) {
			windowSize = packetIndex;
			Log.e("delStaleWindows", "windowSize:" + windowSize);
			usbDataRateControler.delStaleWindows(packetIndex);
		}

	}

	public void delreceivedIndex(long packetIndex){
		usbDataRateControler.delreceivedIndex(packetIndex);
		missMap.remove(packetIndex);

	}

	public void resendPacket(long packetIndex) throws IOException {
		if (packetIndex >= windowSize) {      //未过期的包
			if (!missMap.containsKey(packetIndex)) {
				PacketDateBean packetDateBean = usbDataRateControler.getPacket(packetIndex);
				if (packetDateBean != null) {
					//				udpServer.sendData(packetDateBean.packetData,packetIndex);
					usbDataRateControler.addPriorityQueue(packetDateBean);
					ResendPacket resendPacket = new ResendPacket();
					resendPacket.packetIndex = packetIndex;
					resendPacket.sendedTimes  = 1;
					resendPacket.backUpRttime =resendPacket.rttTime  = usbDataRateControler.getRttTime();
					missMap.put(packetIndex, resendPacket);
				}
			}
		}

	}

	public void addPriorityQueue(PacketDateBean packetDateBean){
		usbDataRateControler.addPriorityQueue(packetDateBean);
	}

	public void onGetReceivedPacket(long packetIndex){
		usbDataRateControler.updateRttTime(packetIndex);
	}

	public void onClose(){
		udpServer.close();
		usbDataRateControler.stop();
		missMap.clear();
	}

	/**
	 * udp收包回调
	 * @param packageBean
     */
	@Override
	public void onReceived(PackageBean packageBean) {
		if (packageBean != null ){
			for (int i = 0;i < packageBean.tlvLink.size();i++){
				TLVBean tlvBean =packageBean.tlvLink.get(i);
				switch (tlvBean.getType()){
					case 0x602://更新边界值
						long boundary = ChangeUtils.bytes2Long(tlvBean.getValue(),0,4);
						delStaleWindows(boundary);

						break;
					case 0x603://处理确认包
						long receivedIndex = ChangeUtils.bytes2Long(tlvBean.getValue(),0,4);
						onGetReceivedPacket(receivedIndex);
						delreceivedIndex(receivedIndex);

						break;
					case 0x604://处理丢包
						byte[] value = tlvBean.getValue();
						int size = value.length/4;
						long[] missIndexs  = new long[size];
						int startPosition = 0;

						while (startPosition < value.length){
							long missIndex = ChangeUtils.bytes2Long(value,startPosition,4);
							missIndexs[startPosition/4] = missIndex;
							startPosition += 4;
							try {
								resendPacket(missIndex);

							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						break;
					default:
						break;
				}

			}
		}

	}


}
