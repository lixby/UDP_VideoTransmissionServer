package com.skylight.apollo.baseunit.tcp.bean;

import java.util.concurrent.LinkedBlockingDeque;

public class PackageBean {
	public int commandType=0x00;
	public int packageType=0x00;
	public LinkedBlockingDeque<TLVBean> tlvLink=new LinkedBlockingDeque<>();
//	public void setCommandType(int commandType){
//		this.commandType=commandType;
//	}
//	public int getCommandType(){
//		return commandType;
//	}
//	
}
