package com.skylight.apollo.baseunit.tcp;

public class TcpUtils {
	/**
	 * Ѱ���Զ����ͷ |---- 00 00 00 0x15-----|
	 * @param data
	 * @param start
	 * @return
	 */
    public static int findDatahead(byte[] data,int start){
        if (data.length>8){
            for (int i=start;i < data.length - 6;i++){
                //        	System.out.println("findDatahead-"+i+":"+data[i] +".data.length:"+data.length);
                if (data[i]==00 && data[i+1]==00 && data[i+2] ==00 && data[i+3] ==21 && (data[i+5] == 64 || data[i+5] == 65)){
                    //            	System.out.println("findDatahead-"+i+".getHead");
                    return i;
                }
            }
        }

        return -1;
    }
}
