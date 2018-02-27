package com.skylight.apollo.baseunit.udp.mode;

import com.skylight.apollo.baseunit.tcp.bean.TLVBean;

import java.util.ArrayList;

public class PackageBean {
	public int commandType=0x00;
	public int packageType=0x00;
	public ArrayList<TLVBean> tlvLink=new ArrayList<>();
}
