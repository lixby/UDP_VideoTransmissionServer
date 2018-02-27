package com.skylight.apollo.service;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.skylight.sd.DoubleEventFrameImpl;
import com.skylight.sd.ReadFrameImpl;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;



public class SkylightUsbDataService extends Service {
    private ReadFrameImpl frameImpl = null;
    private DoubleEventFrameImpl doubleEventImpl = null;
    private UsbManager manager = null;
    private UsbDevice usbDevice = null;
    private UsbInterface usbInterface = null;
    private UsbDeviceConnection deviceConnection = null;
    private UsbEndpoint sendEndPoint;
    private UsbEndpoint receiveEndPoint;
    private UsbEndpoint streamEndPoint;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.skylight.usb_app01.USB_PERMISSION";
    private static final String TAG = "SkylightUsbDataService";
    private com.skylight.apollo.service.SkylightUsbDataService.OnStatusChangedListener onStatusChangedListener;
    private boolean isInited = false;
    private com.skylight.apollo.service.SkylightUsbDataService.MyBinder binder;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if("com.skylight.usb_app01.USB_PERMISSION".equals(action)) {
                synchronized(this) {
                    if(!intent.getBooleanExtra("permission", false)) {
                        Toast.makeText(com.skylight.apollo.service.SkylightUsbDataService.this, "permission denied for accessory", 0).show();
                    } else {
                        if(com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice != null && com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice.getProductId() == 1 && com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice.getVendorId() == 16981) {
                            com.skylight.apollo.service.SkylightUsbDataService.this.deviceConnection = com.skylight.apollo.service.SkylightUsbDataService.this.manager.openDevice(com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice);
                            com.skylight.apollo.service.SkylightUsbDataService.this.initUsbEndpoint(com.skylight.apollo.service.SkylightUsbDataService.this.deviceConnection, com.skylight.apollo.service.SkylightUsbDataService.this.usbInterface);
                        } else {
                            com.skylight.apollo.service.SkylightUsbDataService.this.queryDevices();
                        }

                        com.skylight.apollo.service.SkylightUsbDataService.this.initialized360SDK();
                    }
                }
            }

        }
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra("device");
                Log.d("SkylightUsbDataService", device.toString());
                if(device != null && device.getProductId() == 1 && device.getVendorId() == 16981) {
                    com.skylight.apollo.service.SkylightUsbDataService.this.connect(device);
                    com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice = device;
                    if(com.skylight.apollo.service.SkylightUsbDataService.this.onStatusChangedListener == null) {
                        throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
                    }

                    com.skylight.apollo.service.SkylightUsbDataService.this.onStatusChangedListener.onStatusUpdate("UsbDevice attached");
                }
            } else if("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action) && com.skylight.apollo.service.SkylightUsbDataService.this.usbDevice != null) {
                Log.d("SkylightUsbDataService", "UsbDevice dettached isInited->" + com.skylight.apollo.service.SkylightUsbDataService.this.isInited);
                com.skylight.apollo.service.SkylightUsbDataService.this.destroy();
                if(com.skylight.apollo.service.SkylightUsbDataService.this.onStatusChangedListener == null) {
                    throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
                }

                com.skylight.apollo.service.SkylightUsbDataService.this.onStatusChangedListener.onStatusUpdate("UsbDevice detached");
                Toast.makeText(com.skylight.apollo.service.SkylightUsbDataService.this, "UsbDevice dettached", 0).show();
            }

        }
    };
    private static final int USB360_ERRCODE_OK = 0;
    private static final int USB360_ERRCODE_PROTOCOL_VERSION = -8000;
    private static final int USB360_ERRCODE_SEND = -8001;
    private static final int USB360_ERRCODE_RECEIVE_TIMEOUT = -8002;
    private static final int USB360_ERRCODE_CMD_SEQ = -8003;
    private static final int USB360_APIERR_NULL = -8004;
    private static final int USB360_APIERR_INVALIDPAR = -8005;
    private static final int USB360_APIERR_CALL = -8006;
    private static final int USB360_NO_VALID_DEVICE = -7000;

    static {
        System.loadLibrary("usb360-jni");
    }

    public SkylightUsbDataService() {
    }

    public void onCreate() {
        super.onCreate();
        Log.d("SkylightUsbDataService", "onCreate");
        this.manager = (UsbManager)this.getSystemService("usb");
        this.registUsbDetectReceiver();
        this.registPermissionReceiver();
        this.queryDevices();
    }

    private void registUsbDetectReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        this.registerReceiver(this.receiver, intentFilter);
    }

    private void registPermissionReceiver() {
        IntentFilter filter = new IntentFilter("com.skylight.usb_app01.USB_PERMISSION");
        this.registerReceiver(this.mUsbReceiver, filter);
    }

    public void setOnStatusChangedListener(com.skylight.apollo.service.SkylightUsbDataService.OnStatusChangedListener onStatusChangedListener) {
        this.onStatusChangedListener = onStatusChangedListener;
    }

    private void queryDevices() {
        if(this.manager != null) {
            HashMap hashMap = this.manager.getDeviceList();
            Iterator iterator = hashMap.values().iterator();

            while(iterator.hasNext()) {
                UsbDevice device = (UsbDevice)iterator.next();
                if(device.getProductId() == 1 && device.getVendorId() == 16981) {
                    this.usbDevice = device;
                    this.connect(this.usbDevice);
                    Log.i("SkylightUsbDataService", "找到匹配设备! ");
                    break;
                }
            }
        }

    }

    private void connect(UsbDevice usbDevice) {
        this.usbInterface = usbDevice.getInterface(0);
        if(this.usbInterface != null) {
            if(this.manager.hasPermission(usbDevice)) {
                this.deviceConnection = this.manager.openDevice(usbDevice);
                this.initUsbEndpoint(this.deviceConnection, this.usbInterface);
                if(this.frameImpl != null && this.onStatusChangedListener != null) {
                    this.initialized360SDK();
                }
            } else {
                this.mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.skylight.usb_app01.USB_PERMISSION"), 0);
                this.manager.requestPermission(usbDevice, this.mPermissionIntent);
            }
        }

    }

    private void initUsbEndpoint(UsbDeviceConnection usbConnection, UsbInterface intf) {
        try {
            this.sendEndPoint = intf.getEndpoint(1);
            this.streamEndPoint = intf.getEndpoint(2);
            this.receiveEndPoint = intf.getEndpoint(0);
        } catch (ArrayIndexOutOfBoundsException var4) {
            var4.printStackTrace();
        }

    }

    private void initialized360SDK() {
        if(this.streamEndPoint != null) {
            Log.d("SkylightUsbDataService", "初始化usb360sdk\t");
            if(!this.isInited) {
                this.nativeusb360_new();
                this.nativeusb360_setDeviceConnect(this.deviceConnection);
                Log.d("SkylightUsbDataService", "sendEndPoint attr -> " + this.sendEndPoint.toString());
                this.nativeusb360_AddSendEndPoint(this.sendEndPoint);
                Log.d("SkylightUsbDataService", "receiveEndPoint attr -> " + this.receiveEndPoint.toString());
                this.nativeusb360_AddReceiveEndPoint(this.receiveEndPoint);
                Log.d("SkylightUsbDataService", "streamEndPoint attr -> " + this.streamEndPoint.toString());
                this.nativeusb360_AddReceiveEndPoint(this.streamEndPoint);
                if(this.frameImpl == null) {
                    this.nativeusb360_destroy();
                    throw new RuntimeException("you should initialize ReadFrameImpl Object,then the live streaming data can be getted");
                }

                this.nativeusb360_setStreamObj(this.frameImpl);
                this.isInited = true;
                if(this.doubleEventImpl != null) {
                    this.nativeusb360_setDoubleClickObj(this.doubleEventImpl);
                }

                if(this.onStatusChangedListener == null) {
                    throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
                }

                this.onStatusChangedListener.onStatusUpdate("sdk initialized");
            }
        }

    }

    public IBinder onBind(Intent intent) {
        Log.d("SkylightUsbDataService", "IBinder onBind: ");
        this.binder = new com.skylight.apollo.service.SkylightUsbDataService.MyBinder();
        return this.binder;
    }

    public void init() {
        this.initialized360SDK();
    }

    public boolean isConnected() {
        return this.isInited;
    }

    public void setReadFrameImpl(ReadFrameImpl readFrameImpl) {
        this.frameImpl = readFrameImpl;
    }

    public void setDoubleEventFrameImpl(DoubleEventFrameImpl doubleEventImpl) {
        this.doubleEventImpl = doubleEventImpl;
    }

    public int obtainStream() {
        int responseCode = this.nativeusb360_sendStreamStart();
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int releaseStream() {
        int responseCode = this.nativeusb360_sendStreamStop();
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String requestFWVersion() {
        byte[] name = new byte[64];
        byte[] Sn = new byte[64];
        byte[] Fw = new byte[64];
        int responseCode = this.nativeusb360_sendGetDeviceInfo(name, Sn, Fw);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            String info = (new String(name, Charset.forName("utf-8"))).trim() + "\n" + (new String(Sn, Charset.forName("utf-8"))).trim() + "\n" + (new String(Fw, Charset.forName("utf-8"))).trim();
            return info;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getLensParam() {
        byte[] lens = new byte[512];
        int responseCode = this.nativeusb360_sendGetLens(lens);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(lens, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getStreamInfo() {
        byte[] info = new byte[256];
        int responseCode = this.nativeusb360_sendGetStreamInfo(info);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            String streamInfo = new String(info, Charset.forName("utf-8"));
            return streamInfo.trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setLensParam(String lens) {
        int responseCode = this.nativeusb360_sendSetLens(lens);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getCameraWorkingMode() {
        String mode = "capture";
        return mode;
    }

    public String getCameraBattery() {
        String mode = "10";
        return mode;
    }

    public void setCameraName(String cameraName) {
    }

    public int setCameraTime(String time) {
        int responseCode = this.nativeusb360_sendSetTime(time);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setIQParams(int iso, int awb, int ev, int st) {
        int responseCode = this.nativeusb360_sendSetIQ(iso, awb, ev, st);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setIQParamsByJson(String param) {
        int responseCode = this.nativeusb360_sendSetIQByJson(param);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getDefaultIQParams() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetIQ(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(params, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setDeviceId(String id) {
        int responseCode = this.nativeusb360_sendSetDeviceId(id);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setDeviceSN(String sn) {
        int responseCode = this.nativeusb360_sendSetSerialNumber(sn);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getDeviceSN() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetSerialNumber(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(params, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getDelayedTime() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetShutDownDelayedTime(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(params, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getBatteryPower() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetCameraPower(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(params, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setStopChargingPower(String power) {
        int responseCode = this.nativeusb360_sendLowPower(power);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getMCU_SN() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetMCU_SN(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return (new String(params, Charset.forName("utf-8"))).trim();
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setCameraLogPath(String path) {
        int responseCode = this.nativeusb360_sendGetCameraLog(path);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setWB(String wb) {
        int responseCode = this.nativeusb360_sendSetWB(wb);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getWB() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetWB(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            String wb = (new String(params, Charset.forName("utf-8"))).trim();
            return wb.startsWith("-1")?"-1":wb;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int setCircle_Calib(String Circle_Calib) {
        int responseCode = this.nativeusb360_sendSetCircle_Calib(Circle_Calib);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public String getCircle_Calib() {
        byte[] params = new byte[512];
        int responseCode = this.nativeusb360_sendGetCircle_Calib(params);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            String cc = (new String(params, Charset.forName("utf-8"))).trim();
            return cc.startsWith("-1")?"-1":cc;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public void setExposureMode(String mode, String param) {
        boolean responseCode = true;
    }

    public void setWhiteBalanceMode(String mode, String param) {
        boolean responseCode = true;
    }

    public int shutDown() {
        int responseCode = this.nativeusb360_sendShutDown();
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int postAutoShutDown(String min) {
        int responseCode = this.nativeusb360_sendShutDownDelayed(min);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int postAutoShutDownStart() {
        int responseCode = this.nativeusb360_sendShutDownDelayStart();
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int postAutoShutDownStop() {
        int responseCode = this.nativeusb360_sendShutDownDelayStop();
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int updateCameraFrameware(String filePath, String md5) {
        int responseCode = this.nativeusb360_sendUpdate(filePath, md5);
        if(this.onStatusChangedListener != null) {
            this.onStatusChangedListener.onStatusUpdate(this.handleResponseCode(responseCode));
            return responseCode;
        } else {
            throw new RuntimeException("service is unbinding or no calls the service.setStatusChangedListener method");
        }
    }

    public int getUpdateProgress() {
        return this.nativeusb360_getUpdateProgress();
    }

    public String getSdkVersion() {
        return this.nativeusb360_getSDKVersion();
    }

    public int getTemperature() {
        return this.nativeusb360_getCameraTemperature();
    }

    public byte[] frameDataFilter(byte[] data, int length) {
        Object frame = null;
        byte[] frame1;
        if((data[10] & 31) == 7) {
            frame1 = new byte[length - 6];
            System.arraycopy(data, 6, frame1, 0, length - 6);
        } else {
            frame1 = new byte[length - 24];
            System.arraycopy(data, 24, frame1, 0, length - 24);
        }

        return frame1;
    }

    public void onDestroy() {
        if(this.usbDevice != null && this.usbDevice.getProductId() == 1 && this.usbDevice.getVendorId() == 16981) {
            this.destroy();
        }

        if(this.receiver != null) {
            this.unregisterReceiver(this.receiver);
        }

        if(this.mUsbReceiver != null) {
            this.unregisterReceiver(this.mUsbReceiver);
        }

        super.onDestroy();
    }

    public int destroy() {
        int responseCode = 0;
        if(this.isInited) {
            responseCode = this.nativeusb360_destroy();
            if(this.deviceConnection != null) {
                this.deviceConnection.releaseInterface(this.usbInterface);
                this.usbDevice = null;
            }

            Log.d("SkylightUsbDataService", "nativeusb360_destroy");
            this.isInited = false;
        }

        return responseCode;
    }

    private native int nativeusb360_new();

    private native int nativeusb360_setDeviceConnect(UsbDeviceConnection var1);

    private native int nativeusb360_AddReceiveEndPoint(UsbEndpoint var1);

    private native int nativeusb360_AddSendEndPoint(UsbEndpoint var1);

    private native int nativeusb360_setStreamObj(Object var1);

    private native int nativeusb360_setDoubleClickObj(Object var1);

    private native int nativeusb360_sendStreamStop();

    private native int nativeusb360_sendStreamStart();

    private native int nativeusb360_sendGetDeviceInfo(byte[] var1, byte[] var2, byte[] var3);

    private native int nativeusb360_destroy();

    private native int nativeusb360_sendSetLens(String var1);

    private native int nativeusb360_sendGetLens(byte[] var1);

    private native int nativeusb360_sendGetStreamInfo(byte[] var1);

    private native int nativeusb360_sendUpdate(String var1, String var2);

    private native int nativeusb360_getUpdateProgress();

    private native int nativeusb360_sendSetTime(String var1);

    private native int nativeusb360_sendSetIQ(int var1, int var2, int var3, int var4);

    private native int nativeusb360_sendSetIQByJson(String var1);

    private native int nativeusb360_sendGetIQ(byte[] var1);

    private native int nativeusb360_sendSetDeviceId(String var1);

    private native int nativeusb360_sendSetSerialNumber(String var1);

    private native int nativeusb360_sendGetSerialNumber(byte[] var1);

    private native int nativeusb360_sendShutDown();

    private native int nativeusb360_sendShutDownDelayed(String var1);

    private native int nativeusb360_sendShutDownDelayStart();

    private native int nativeusb360_sendShutDownDelayStop();

    private native int nativeusb360_sendGetShutDownDelayedTime(byte[] var1);

    private native String nativeusb360_getSDKVersion();

    private native int nativeusb360_getCameraTemperature();

    private native int nativeusb360_sendGetCameraPower(byte[] var1);

    private native int nativeusb360_sendLowPower(String var1);

    private native int nativeusb360_sendGetMCU_SN(byte[] var1);

    private native int nativeusb360_sendGetCameraLog(String var1);

    private native int nativeusb360_sendSetWB(String var1);

    private native int nativeusb360_sendGetWB(byte[] var1);

    private native int nativeusb360_sendSetCircle_Calib(String var1);

    private native int nativeusb360_sendGetCircle_Calib(byte[] var1);

    private String handleResponseCode(int responseCode) {
        String status = null;
        switch(responseCode) {
            case -8006:
                status = "Call the wrong order or repeat calls";
                break;
            case -8005:
                status = "Parameter is invalid, beyond the scope of legal";
                break;
            case -8004:
                status = "The SDK  failed to initialize";
                break;
            case -8003:
                status = "Response sequence does not match";
                break;
            case -8002:
                status = "Receive data timeout";
                break;
            case -8001:
                status = "Command send fialed";
                break;
            case -8000:
                status = "Protocol version are not compatible";
        }

        return status;
    }

    public class MyBinder extends Binder {
        public MyBinder() {
        }

        public com.skylight.apollo.service.SkylightUsbDataService getSkylightUsbDataService() {
            return com.skylight.apollo.service.SkylightUsbDataService.this;
        }
    }

    public interface OnStatusChangedListener {
        void onStatusUpdate(String var1);
    }
}
