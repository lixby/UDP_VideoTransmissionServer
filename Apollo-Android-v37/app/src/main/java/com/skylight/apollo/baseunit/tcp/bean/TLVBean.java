package com.skylight.apollo.baseunit.tcp.bean;

public class TLVBean {
	private int type;
	private int length;
	private byte[] value;
	public void setType(int type){
		this.type=type;
	}
	public void setLength(int length){
		this.length=length;
	}
	public void setValue(byte[] value){
		this.value=value;
	}
	public int getType(){
		return type;
	}
	public int getLength(){
		return length;
	}
	public byte[] getValue(){
		return value;
	}

}
