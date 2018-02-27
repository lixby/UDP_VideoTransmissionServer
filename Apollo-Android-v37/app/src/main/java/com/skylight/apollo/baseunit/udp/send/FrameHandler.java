package com.skylight.apollo.baseunit.udp.send;


import com.skylight.apollo.baseunit.bean.PacketDateBean;
import com.skylight.apollo.baseunit.util.ChangeUtils;
import com.skylight.apollo.util.Constants;

/**
 * 帧处理器，用于分包等处理
 * @author gengwen
 *
 */
public class FrameHandler {

	private String TAG = "FrameHandler:";
	private final static int HEAD_LENGTH = 18;
	//private static final int PACKET_LENS = 1448;
	//private static final int PACKET_LENS = 8*1024;
	private static final int PACKET_LENS = 4*1024;
	//private int PACKET_LENS = 4*1472;
	private int repeatTime_frame = 33;
	
	public static byte[] HEAD_FLAG = new byte[]{00,0x16};            //udp包标记

	private long packageIndex = 0L;    //4字节
	private int frameIndex = 0;        //2字节

	/**
	 * handle frame data ：mainly to split  frame
	 * @param frameData
     */
	public void frameHandle(byte[] frameData,int frameLength,int channel,byte IBPtype){

		short totalCount = (short) Math.ceil(frameLength/(PACKET_LENS - HEAD_LENGTH)); // package counts of frames
		long repeatTime_package = 33 * 1000000 / totalCount;
		short currentCountIndex = 0;
		long startIndex = packageIndex;
		int startPosition = 0;

		while (currentCountIndex < totalCount){

			int packageLens  = currentCountIndex == totalCount - 1 ? frameLength % (PACKET_LENS - HEAD_LENGTH):PACKET_LENS - HEAD_LENGTH;
			outputPacket(frameData,frameLength,startPosition,packageIndex,packageLens,frameIndex,channel,startIndex,totalCount,IBPtype,repeatTime_package);

			startPosition +=   packageLens;

			packageIndex  = (packageIndex +1) & 0xffffffff;
			currentCountIndex ++ ;
		}

		frameIndex = (frameIndex+1) & 0xffff;

	}

	private void outputPacket(byte[] frameData,int frameLens,int startPosition,long packageIndex,int packageLength,int frameIndex,int frameType,long startIndex,int count,byte IBPtype,long repeatTime){
		byte[] outputData = new byte[HEAD_LENGTH + packageLength]; //头 + 负载长度
		int headLength = addHead(outputData,packageIndex,packageLength,frameIndex,frameType,startIndex,count,IBPtype);
		System.arraycopy(frameData , startPosition,outputData,headLength,packageLength);
//		CmLog.e("|"+frameLens+"|"+startPosition +"|"+packageIndex+"|"+packageLength+"|"+frameIndex+"|"+frameType+"|"+startIndex+"|"+count);
		//TODO 平缓输出
		PacketDateBean packetDateBean = new PacketDateBean(outputData,packageIndex,frameIndex,repeatTime);
		if(packageOutListener != null && packageIndex <= Constants.sendPacketSize)packageOutListener.onPacketOut(packetDateBean);
	}

	/**
	 * HEAD information :
	 * 		 |----Flag(2byte)--------|---Packet Index(4byte)--|--Packet Lens(2byte)----|
	 * 		 |---Frame Index(2byte)--|---Frame type(1byte)----|---Start Index(4byte)---|
	 * 		 |---Count(2byte)--------|----IBP type(1byte)-----|------video data--------|
	 * 
	 * byte:1-2   flag  |00 0x16|
	 * 
	 * byte:3-6   Packet Index 包序
	 *
	 * byte:7-8   Packet Lens  包负载长度
	 *
	 * byte:9-10  Frame Index 帧序
	 * 
	 * byte:11    Frame type 帧类型
	 *
	 * byte:12-15 Start Index 帧起始包序
	 *
	 * byte:16-17 Count 包数
	 *
	 * byte:18    IBP type 视频帧类型
	 *
	 */
	private int addHead(byte[] outputDate,long packageIndex,int packageLength,int frameIndex,int frameType,long startIndex,int count,byte IBPtype){
		byte[] b_packageIndex = ChangeUtils.data2byteArr(packageIndex,4);
		byte[] b_packageLebgth = ChangeUtils.data2byteArr(packageLength,2);
		byte[] b_frameIndex = ChangeUtils.data2byteArr(frameIndex,2);
		byte[] b_frameType = ChangeUtils.data2byteArr(frameType,1);
		byte[] b_startIndex = ChangeUtils.data2byteArr(startIndex,4);
		byte[] b_count = ChangeUtils.data2byteArr(count,2);

		System.arraycopy(HEAD_FLAG,
				0,
				outputDate,
				0,
				HEAD_FLAG.length);
		System.arraycopy(b_packageIndex,
				0,
				outputDate,
				HEAD_FLAG.length,
				b_packageIndex.length);
		System.arraycopy(b_packageLebgth,
				0,
				outputDate,
				HEAD_FLAG.length + b_packageIndex.length,b_packageLebgth.length);
		System.arraycopy(b_frameIndex,
				0,
				outputDate,
				HEAD_FLAG.length + b_packageIndex.length + b_packageLebgth.length,
				b_frameIndex.length);
		System.arraycopy(b_frameType,
				0,
				outputDate,
				HEAD_FLAG.length + b_packageIndex.length + b_packageLebgth.length +b_frameIndex.length,
				b_frameType.length);
		System.arraycopy(b_startIndex,
				0,outputDate,
				HEAD_FLAG.length+ b_packageIndex.length + b_packageLebgth.length +b_frameIndex.length
						+b_frameType.length ,
				b_startIndex.length);
		System.arraycopy(b_count,
				0,
				outputDate,
				HEAD_FLAG.length+ b_packageIndex.length + b_packageLebgth.length +b_frameIndex.length
						+b_frameType.length + b_startIndex.length,
				b_count.length);
		outputDate[HEAD_LENGTH -1] = IBPtype;
		return HEAD_LENGTH;
	}


	private PackageOutListener packageOutListener;

	public void setPackageOutListener(PackageOutListener packageOutListener){
		this.packageOutListener = packageOutListener;
	}

	public interface PackageOutListener{
//		void onPackageOut(byte[] packageBean, PackageInfo packageInfo);
		void onPacketOut(PacketDateBean packetDateBean);
	}


}
