package com.skylight.apollo.baseunit.tcp.bean;


import android.util.Log;

import com.skylight.apollo.baseunit.util.ChangeUtils;
import com.skylight.apollo.baseunit.util.Utils;

import java.util.Arrays;

public class TcpPackage2 {
    private String TAG = "howardTcpPackage2:";
    public static final int HEAD_FLAG_LENGTH = 4;
    /**
     * 缓存数据,用于存放半头数据
     */
    public byte[] cacheData;
    public byte[] tempCache;
    private int position = 0;
    public boolean isCompletePackage = false;//是否完整包
    private boolean getPackageLen = false;
    private OnFinishListener mFinishListener;
    public final int HEADLENGTH = 8;
    public int packetLens = -1;

    public TcpPackage2() {

    }

    public void add(byte[] source, int start, int end) {
        int len = end - start;
        if (!getPackageLen && len > 0 && len < 8) {
            if (tempCache == null) {
                tempCache = new byte[len];
                System.arraycopy(source, 0, tempCache, 0, len);
            }

        } else if (!getPackageLen && len > 7) {
			byte[] newData=new byte[7];
			System.arraycopy(source, start, newData, 0, 7);
			System.out.println("howard tcppackage = "+ Arrays.toString(newData));
            Log.d(TAG, "tcppackage packetLens = " + packetLens);
            if (tempCache != null && tempCache.length > 0) {
                byte[] bytes = Utils.byteMerger(tempCache, source, source.length);
                packetLens = ChangeUtils.byteArr2int(Utils.byteMerger(bytes, start + 6, 0, 2)) + HEADLENGTH;
                getPackageLen = true;
                cacheData = new byte[packetLens];
                    tempCache = null;
                if(packetLens>bytes.length){
                    System.arraycopy(bytes, start, cacheData, 0, packetLens);
                    position += packetLens;
                }else {
                    cacheData = new byte[bytes.length];
                    packetLens=bytes.length;
                    System.arraycopy(bytes, start, cacheData, 0, bytes.length);
                    position += bytes.length;
                }


            } else {
                packetLens = ChangeUtils.byteArr2int(Utils.byteMerger(source, start + 6, 0, 2)) + HEADLENGTH;
                getPackageLen = true;
                cacheData = new byte[packetLens];
                if (getPackageLen && position + len <= packetLens) {
                    System.arraycopy(source, start, cacheData, 0, len);
                    position += len;
                }

            }

        } else if (getPackageLen && position + len <= packetLens) {
            System.arraycopy(source, start, cacheData, position, len);
            position += len;
        } else if (getPackageLen && position + len > packetLens) {
            //TODO
            System.out.println("error error error error error error");
        }
        Log.d(TAG, "tcppackage position = " + position);
        if (getPackageLen && position >= packetLens) {
            if (mFinishListener != null) {
                position = 0;
                getPackageLen = false;
                PackageBean packageBean = parseTLV(cacheData);
                mFinishListener.onFinish(packageBean);
            }

        }

    }

    public interface OnFinishListener {
        void onFinish(PackageBean packageBean);
    }

    public void setOnFinishListener(OnFinishListener finishListener) {
        this.mFinishListener = finishListener;
    }

    public void reset() {
        position = 0;
        isCompletePackage = false;
        getPackageLen = false;
        cacheData = null;
    }

    public PackageBean parseTLV(byte[] packageData) {
        Log.d(TAG, packageData.toString());
        System.out.println("howard22222" + Arrays.toString(packageData));
        PackageBean packageBean = new PackageBean();
        packageBean.commandType = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, 4, 0, 1));
        Log.d(TAG, "packageBean.commandType = " + packageBean.commandType);
        packageBean.packageType = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, 5, 0, 1));
        Log.d(TAG, "packageBean.packageType = " + packageBean.packageType);

        int packageLength = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, 6, 0, 2)) + HEADLENGTH;
        Log.d(TAG, "packageLength = " + packageLength);
        int parsePosition = HEADLENGTH;
        while (parsePosition < packageLength) {
            TLVBean tlvBean = new TLVBean();
            int type = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, parsePosition, 0, 2));
            tlvBean.setType(type);
            parsePosition += 2;
            Log.d(TAG, "tlv type = " + type);
            int tlvlen = ChangeUtils.byteArr2int(Utils.byteMerger(packageData, parsePosition, 0, 2));
            tlvBean.setLength(tlvlen);
            parsePosition += 2;
            Log.d(TAG, "tlv tlvlen = " + tlvlen);
            byte[] tlvData = Utils.byteMerger(packageData, parsePosition, 0, tlvlen);
            tlvBean.setValue(tlvData);
            parsePosition += tlvlen;
            System.out.println("howard33333  = " + Arrays.toString(tlvData));
            Log.d(TAG, "tlv tlvData length = " + tlvData.length);
            try {
                packageBean.tlvLink.put(tlvBean);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return packageBean;
    }

}
