package com.skylight.apollo.baseunit.tcp;


import android.util.Log;

import com.skylight.apollo.baseunit.util.CmLog;
import com.skylight.apollo.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;



public class TcpServer implements Runnable{

	private boolean startAccept = true;
	private ServerSocket serverSocket;
	private InputStream inputStream;
	private Socket mSocket;
	private OutputStream mOutputStream;
	private String TAG="TcpServer";
	private DataSegregator dataSegregator;
	private int tcpPort;
	
	public TcpServer(int tcpPort,DataSegregator dataSegregator ){
		this.tcpPort = tcpPort;
		this.dataSegregator = dataSegregator;
	}

	private int count = 0;
	public void run() {
		try {
//			if (serverSocket != null && !serverSocket.isClosed()){
//				serverSocket.close();
//			}
			serverSocket = new ServerSocket(tcpPort);
			serverSocket.setReceiveBufferSize(Constants.tcpBufferSize);

			while(startAccept){
				mSocket = serverSocket.accept();
				mSocket.setKeepAlive(true);
				inputStream = mSocket.getInputStream();
				mOutputStream = mSocket.getOutputStream();
				byte[] bytes = new byte[Constants.tcpReadSize];
				int lens = 0;
				Log.e("TcpSocket.read","start-");
				while ((lens = inputStream.read(bytes))!= -1) {
					if (count <= 2) {
						Log.e("TcpSocket.read", "start read-");
						byte[] commonData = Arrays.copyOf(bytes, lens);
						Log.e("TcpSocket.read", "end read-");
						dataSegregator.addTcpData(commonData);
						Log.e("TcpSocket.read", "end add-");
						count ++ ;
					}
				}
				Log.e("TcpSocket.read","end-");
			}
//			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setOnPacketOutListener(DataSegregator.GetCommonListener getCommonListener){
		if (dataSegregator != null)dataSegregator.setListener(getCommonListener);
	}
	public void sendCommand(byte[] data){

		dataSegregator.halfPacket.parseTLV(data);
		if(mSocket==null){
			CmLog.e("clientSocket=NULL");
			return;
		}

		if(!mSocket.isConnected()){
			CmLog.e("clientSocket is not Connected");
			return;
		}


		if(mOutputStream!=null&&mSocket.isConnected()){
			try {
				Log.i(TAG,"sendCommand ok");
				mOutputStream.write(data);
				mOutputStream.flush();
			}catch (SocketException e) {
				CmLog.e("SocketException:"+e.getMessage());
				e.printStackTrace();
			}catch (IOException e) {
				CmLog.e("IOException:"+e.getMessage());
				e.printStackTrace();
			}

		}

	}
}