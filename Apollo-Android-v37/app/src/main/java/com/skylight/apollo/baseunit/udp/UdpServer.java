package com.skylight.apollo.baseunit.udp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class UdpServer {
	DatagramSocket udpSocket ;
	InetAddress clientAddr;
	int TargetPort;
	int localPort = 3456;

//	private LinkedBlockingQueue<byte[]> packets = new LinkedBlockingQueue<>();
	private boolean watching = true;
	public UdpServer() throws SocketException {
		udpSocket = new DatagramSocket(localPort);

		udpSocket.setSendBufferSize(5 * 1024 *1024);
		int bufferSize = udpSocket.getSendBufferSize();
		Log.e("---UdpServer","bufferSize:"+bufferSize);
	}
	public void setTargetIp(InetAddress TargetIp,int TargetPort)  {
		clientAddr = TargetIp;
		this.TargetPort = TargetPort;
	}



	public void sendData(final byte[] data,long packetIndex){
		long nano = System.nanoTime();
		DatagramPacket dataGramPacket = new DatagramPacket(data, data.length, clientAddr, TargetPort);
		try {
			udpSocket.send(dataGramPacket);
			Log.e("----","-----sendData:"+(System.nanoTime()-nano)+" | "+packetIndex);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public void sendDataDelay(byte[] data, final long waitTime, final long frameIndex)  {
//		try {
//			packets.put(data);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		//		executorService.execute(new Runnable() {
//									@Override
//									public void run() {
//										DatagramPacket dataGramPacket = new DatagramPacket(data, data.length, clientAddr, TargetPort);
//										try {
//											udpSocket.send(dataGramPacket);
//											Log.e("----", "-----sendData:" + (System.nanoTime() - lastTimeNano) + "|" + waitTime / 1000 + "|" + frameIndex);
//											lastTimeNano = System.nanoTime();
//
//										} catch (IOException e) {
//											e.printStackTrace();
//										}
//									}
//								});
//		});
//		},5, TimeUnit.MICROSECONDS);
	}
	private long lastTimeNano = 0;
	public void close(){
		udpSocket.close();  //关闭套接字
	}
	private UsbDataRateControler usbDataRateControler;
	public void addRateControler(UsbDataRateControler controler){
		this.usbDataRateControler = controler;
	}

}
