package com.skylight.apollo.baseunit.bean;

public class PackageInfo {
	public long headflag;
	public int currentCountIndex;
	public long frameLens;
	public int totalPackage;
	public long frameIndex;
	public int packageLens;
	public long timestamp;
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof PackageInfo) {
//			CmLog.d("----------------PackageInfo.equals()");
			PackageInfo packageinfo = (PackageInfo) obj;
//			return headflag == packageinfo.headflag && currentCountIndex == packageinfo.currentCountIndex && frameLens == packageinfo.frameLens
//					&& totalPackage == packageinfo.totalPackage && frameIndex == packageinfo.frameIndex && packageLens == packageinfo.packageLens;
			return  currentCountIndex == packageinfo.currentCountIndex 
//					&& totalPackage == packageinfo.totalPackage 
					&& frameIndex == packageinfo.frameIndex && timestamp == packageinfo.timestamp ;
		}else{
			return false;
		}
	}
//	@Override
//	public int hashCode() {
//		CmLog.d("----------------PackageInfo.hashCode()");
//		return currentCountIndex * (int)frameIndex;
//	}
}
