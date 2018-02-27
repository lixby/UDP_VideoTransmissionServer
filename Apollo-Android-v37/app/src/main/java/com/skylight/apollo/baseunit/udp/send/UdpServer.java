package com.skylight.apollo.baseunit.udp.send;

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

	public UdpServer() throws SocketException {
		if (udpSocket != null && !udpSocket.isClosed()){
			udpSocket.close();
			udpSocket = null;
		}
		udpSocket = new DatagramSocket(localPort);

	}

	public void setTargetIp(InetAddress TargetIp,int TargetPort)  {
		clientAddr = TargetIp;
		this.TargetPort = TargetPort;
	}

	public void sendData(final byte[] data,long packetIndex){
		try {
			if(!udpSocket.isClosed()){
				DatagramPacket dataGramPacket = new DatagramPacket(data, data.length, clientAddr, TargetPort);
				udpSocket.send(dataGramPacket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close(){
		udpSocket.close();  //关闭套接字
	}

}
