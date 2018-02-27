package com.skylight.apollo.baseunit.udp.send;

import com.skylight.apollo.baseunit.bean.FrameBean;
import com.skylight.apollo.baseunit.util.Utils;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author gengwen
 *
 */
public class FramePaser {
	//当前项目类型
	String projectPath = System.getProperty("user.dir");
	String frameDir = projectPath +"/assets";
	File targetFile; //目标文件
	private long frameIndex = 0;
	private FrameHandler frameHandler; //帧处理类，用于udp分包处理
	public FramePaser(){
		targetFile = getFrameFilePath();
	}
	
	//获取视频文件
	public File getFrameFilePath(){
		File frameFile = new File(frameDir);
		File[] childFile = frameFile.listFiles();
		return childFile[0];
	}
	public void readFrameFromFile(){
		try {
			FileInputStream fin = new FileInputStream(targetFile);
			ObjectInputStream oin = new ObjectInputStream(fin);
			try{
			Object frameObject =null;
			if(readFrameListener != null)readFrameListener.onStartFrame();
			while ((frameObject = oin.readObject() ) != null) {
					FrameBean frame = (FrameBean)frameObject;
					frame.setFrameIndex(frameIndex);
					if(readFrameListener != null)readFrameListener.onReadFrame(frame);
				}
			}catch(EOFException e){
				System.out.println("end");
				e.printStackTrace();
				if(readFrameListener != null)readFrameListener.onEndFrame();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	FileInputStream fin;
	ObjectInputStream oin;
	public void prepareGetFrame()throws Exception{
		fin = new FileInputStream(targetFile);
		oin = new ObjectInputStream(fin);
	}
	public FrameBean getNextFrame(){
		FrameBean frame = null;
		try {
			frame =(FrameBean) oin.readObject();
			System.out.println(Utils.getCurrentTime()+":----------frame Lens-----------------:"+frame.getLength());
			
			frame.setFrameIndex(frameIndex++);
		}catch(EOFException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return frame;
	}
	public void close(){
		try {
			oin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {fin.close();} catch (IOException e) {}
		}
		
	}
	
	
	private ReadFrameListener readFrameListener;
	public void setReadFrameListener(ReadFrameListener readFrameListener){
		this.readFrameListener = readFrameListener;
	}
	public interface ReadFrameListener{
		void onStartFrame();
		void onReadFrame(FrameBean frame);
		void onEndFrame();
	}
	
}
