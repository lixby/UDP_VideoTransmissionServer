package com.skylight.apollo.baseunit.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

public class Utils {
	public static String getCurrentTime(){
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		return sdf.format(d);
	}
	
	//System.arraycopy()方法
		public static synchronized byte[] byteMerger(byte[] bt1,int srcPos,int desPos,int lens){
			byte[] bt2 = new byte[lens];
//			System.out.println("srcPos:"+srcPos+"-desPos:"+desPos+"-lens:"+lens + "-bt1,length:"+bt1.length);
			System.arraycopy(bt1, srcPos, bt2, desPos,lens);
			return bt2;
		}
		public static synchronized byte[] byteMerger(byte[] bt1,byte[] bt2,int spilt_lens){
			byte[] bt3 = new byte[bt1.length + spilt_lens];
//			System.out.println("bt1.length:"+bt1.length+"-bt2.length:"+bt1.length+"-spilt_lens:"+spilt_lens);
			System.arraycopy(bt1, 0, bt3, 0,bt1.length);
			System.arraycopy(bt2, 0, bt3, bt1.length, spilt_lens);
			return bt3;
		}
		public static String timestamp2time(long timeStamp){
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
			String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));
			return sd;
		}
		public static long compareTimeStamp(long timeStamp1,long timeStamp2){
			return timeStamp2 - timeStamp1;
		}

	public static String  getLocalAddress() throws IOException {
		try {
			for (Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces(); enNetI.hasMoreElements();){
				NetworkInterface netI=enNetI.nextElement();

				for(Enumeration<InetAddress> enAddress = netI.getInetAddresses(); enAddress.hasMoreElements();){
					InetAddress inetAddress=enAddress.nextElement();
					if(inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()){
						String clientUdp_IP=inetAddress.getHostAddress();
						CmLog.i("Local clientUdp_IP address :"+clientUdp_IP);
						return clientUdp_IP;
					}

				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		throw new IOException("Did not get to the local IP address");
	}
	public static String  getLocalAddressBytes() throws IOException {
		try {
			for (Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces(); enNetI.hasMoreElements();){
				NetworkInterface netI=enNetI.nextElement();

				for(Enumeration<InetAddress> enAddress = netI.getInetAddresses(); enAddress.hasMoreElements();){
					InetAddress inetAddress=enAddress.nextElement();
					if(inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()){
						String clientUdp_IP=inetAddress.getHostAddress().getBytes().toString();
						CmLog.i("Local clientUdp_IP address :"+clientUdp_IP);
						return clientUdp_IP;
					}

				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		throw new IOException("Did not get to the local IP address");
	}
}
