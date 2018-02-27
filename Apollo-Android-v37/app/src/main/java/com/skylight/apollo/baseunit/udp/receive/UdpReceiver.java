package com.skylight.apollo.baseunit.udp.receive;

import android.util.Log;

import com.skylight.apollo.baseunit.util.ChangeUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class UdpReceiver implements Runnable{
	DatagramSocket udpSocket ;
	public static int localPort = 3457;
	private int bufferSize =  8 *1024 * 1024;
	private boolean keepWatching = true;
	private DataSegregator dataSegregator = DataSegregator.getInstance();
	private Executor executor = Executors.newSingleThreadExecutor();

	public UdpReceiver() throws SocketException {
		if (udpSocket != null && !udpSocket.isClosed()){
			udpSocket.close();
			udpSocket = null;
		}
		udpSocket = new DatagramSocket(localPort);
	}

	public void setOnReceivedListener(DataSegregator.ReceiveListener receivedListener){
		dataSegregator.setListener(receivedListener);
	}

	public void close(){
		udpSocket.close();  //关闭套接字
	}
	@Override
	public void run() {
		byte[] buf = new byte[bufferSize];//接受内容的大小，注意不要溢出

		DatagramPacket datagramPacket = new DatagramPacket(buf,0,buf.length);//定义一个接收的包
		while (keepWatching){
			try {
				long startTime = System.currentTimeMillis();
				udpSocket.receive(datagramPacket);
				Log.w("UdpReceiver","receive time:"+(System.currentTimeMillis()-startTime));
				int packageLength = ChangeUtils.byteArr2int(datagramPacket.getData(),6, 2) + 8;
				byte[] receivedData = new byte[packageLength];
				System.arraycopy(datagramPacket.getData(),0,receivedData,0,packageLength);
				try {
					dataSegregator.addUdpData(receivedData);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void start(){
		executor.execute(this);
	}
}
