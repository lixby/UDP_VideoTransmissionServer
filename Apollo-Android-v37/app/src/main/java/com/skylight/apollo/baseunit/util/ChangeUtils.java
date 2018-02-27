package com.skylight.apollo.baseunit.util;

public class ChangeUtils {
	public static byte[] longToByteArr(long src){
		byte[] byteNum = new byte[8];  
	    for (int ix = 0; ix < 8; ++ix) {  
	        int offset = 64 - (ix + 1) * 8;  
	        byteNum[ix] = (byte) ((src >> offset) & 0xff);  
//	        System.out.println(byteNum[ix]);
	    }  
	    return byteNum;  
	}
	public static long bytes2Long(byte[] byteNum,int offset) {  
	    long num = 0;  
	    for (int ix = offset; ix < offset + 8; ix++) {
	        num <<= 8;  
	        num |= (byteNum[ix] & 0xff);  
	    }  
	    return num;  
	}
	public static long bytes2Long(byte[] src,int offset,int size) {
		long num = 0;
		for (int ix = offset; ix < offset + size; ix++) {
			num <<= 8;
			num |= (src[ix] & 0xff);
		}
		return num;
	}
	public static int byteArr2int(byte[] src,int offset,int size){
		int value=0;

		for(int i = offset; i < offset + size; i++) {
			value <<= 8;
			value |= (src[i] & 0xff);
		}
		return value;
	}
	public static byte[] data2byteArr(long src,int lens){
		byte[] byteNum = new byte[lens];
		for (int ix = 0; ix < lens; ix++) {
			int offset = 8 * (lens - ix - 1) ;
			byteNum[ix] = (byte) ((src >> offset) & 0xff);
			//	        System.out.println(byteNum[ix]);
		}
		return byteNum;
	}
	public static byte[] data2byteArr(int src,int lens){
		byte[] byteNum = new byte[lens];
		for (int ix = 0; ix < lens; ix++) {
			int offset = 8 * (lens - ix - 1) ;
			byteNum[ix] = (byte) ((src >> offset) & 0xff);
			//	        System.out.println(byteNum[ix]);
		}
		return byteNum;
	}
	public static int byteArr2int(byte[] src){
		int value=0;  

        for(int i = 0; i < src.length; i++) {  
            int shift= (src.length-1-i) * 8;  
            value +=(src[i] & 0x000000FF) << shift;
        }  
        return value;  
	}
	public static int byteArr2int4(byte[] src,int offset){
		int value=0;  

        for(int i = 0; i < offset + 4; i++) {
            int shift= (offset + 4-1-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }  
        return value;  
	}
	public static int byteArr2int2(byte[] src,int offset){
		int value=0;  

        for(int i = offset; i < offset +2; i++) {  
            int shift= (offset +1-i) * 8;  
            value +=(src[i] & 0x000000FF) << shift;
        }  
        return value;  
	}
	public static long byteArr2long6(byte[] src,int offset){
		long value=0;
        for(int i = offset; i < offset +6; i++) {
            int shift= (offset +6-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }  
        return value;  
	}
}
