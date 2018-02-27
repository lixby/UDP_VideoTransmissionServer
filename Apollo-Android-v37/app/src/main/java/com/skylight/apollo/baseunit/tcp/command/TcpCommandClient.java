package com.skylight.apollo.baseunit.tcp.command;

import android.util.Log;

import com.skylight.apollo.baseunit.tcp.mode.TcpIpInformation;
import com.skylight.apollo.baseunit.util.CmLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * TcpClient 用于消息传输
 * @author gengwen
 *
 */
public class TcpCommandClient implements Runnable {

	private String TAG = "TcpCommandClient";

	private Socket clientSocket;
	private InputStream inputStream;
	private OutputStream outputStream;

	private static final int TIME_OUT = 5000;
	private boolean keepWatching = false;

	public TcpCommandClient(TcpStatusCallback tcpStatusCallback){
		this.tcpStatusCallback=tcpStatusCallback;
		clientSocket = new Socket();
		new Thread(this).start();

	}

	@Override
	public void run() {
		connectService();
	}

	/**connect service*/
	private void connectService() {
		try {
			keepWatching=false;
			clientSocket.setSoTimeout(TIME_OUT);
			clientSocket.setKeepAlive(true);
			String ip= TcpIpInformation.getInstance().getServerTcp_IP();
			int port=TcpIpInformation.getInstance().getServerTcp_Port();

			clientSocket.connect(new InetSocketAddress(ip,port));
			outputStream = clientSocket.getOutputStream();
			inputStream = clientSocket.getInputStream();
			startWatching();
		} catch (IOException e) {
			CmLog.e("IOException:"+e.getMessage());
			release();
			if(tcpStatusCallback!=null){
				tcpStatusCallback.connected(TcpStatusCallback.TcpStatus.CONNECTION_FAILED);
			}
			//e.printStackTrace();
		}

	}


	/**start watcher*/
	private void startWatching(){
		//Start udp after the tcp connection is successful
		if(tcpStatusCallback!=null){
			Log.i(TAG,"keepWatching tcp connected");
			tcpStatusCallback.connected(TcpStatusCallback.TcpStatus.CONNECTION_SUCCEEDED);
		}

		keepWatching=true;
		while (keepWatching) {
			//receive service data
			try {
				byte[] rdBuffer=new byte[1024];
				int lens = 0;
				while ((lens = inputStream.read(rdBuffer))!= -1) {


                }
			} catch (IOException e) {}
		}

	}

	/**
	 * send command data to tcp service
	 * @param data you will send
	 */
	public void sendCommand(byte[] data){
		if(clientSocket==null){
			CmLog.e("clientSocket=NULL");
			return;
		}

		if(!clientSocket.isConnected()){
			CmLog.e("clientSocket is not Connected");
			return;
		}


		if(outputStream!=null&&clientSocket.isConnected()){
			try {
				Log.i(TAG,"sendCommand ok");
				outputStream.write(data);
				outputStream.flush();
			}catch (SocketException e) {
				CmLog.e("SocketException:"+e.getMessage());
				release();
				tcpStatusCallback.connected(TcpStatusCallback.TcpStatus.DISCONNECTED);
				e.printStackTrace();
			}catch (IOException e) {
				CmLog.e("IOException:"+e.getMessage());
				e.printStackTrace();
			}

		}

	}


	private TcpStatusCallback tcpStatusCallback;

	public void setTcpStatusCallback(TcpStatusCallback tcpStatusCallback) {
		this.tcpStatusCallback = tcpStatusCallback;
	}

	public interface  TcpStatusCallback{

		public enum TcpStatus{
			CONNECTION_SUCCEEDED,
			CONNECTION_FAILED,
			DISCONNECTED
		}

		/**
		 * Output port number after successful connection
		 */
		void connected(TcpStatus tcpStatus);

		/**
		 * Succeddful receiving data from the opposition sender
		 * @param packet
		 */
		void outPutData(byte[] packet);
	}


	/**release tcp source*/
	public void release(){
		try {
			keepWatching=false;
			if(tcpStatusCallback!=null){
				tcpStatusCallback=null;
			}
			if(clientSocket!=null&&!clientSocket.isClosed()){
				clientSocket.shutdownOutput();
				clientSocket.shutdownInput();
				clientSocket.close();
				CmLog.d("Tcp socket closed");
			}
		} catch (IOException e) {
			CmLog.w(e.getMessage());
		}

	}

}