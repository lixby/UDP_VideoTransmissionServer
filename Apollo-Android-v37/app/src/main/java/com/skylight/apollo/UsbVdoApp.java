package com.skylight.apollo;

import android.app.Application;
import android.content.ServiceConnection;
import android.widget.Toast;

import com.skylight.apollo.receiver.CameraConnectionReceiver;
import com.skylight.sd.OnStatusChangedListener;
import com.skylight.sd.ReadFrameImpl;
import com.skylight.sd.SkylightUsbDataService;

/**
 * @Author: gengwen
 * @Date: 2017/6/20.
 * @Company:Skylight
 * @Description:
 */

public class UsbVdoApp extends Application{

    private CameraConnectionReceiver cameraConnectionReceiver;
    private static UsbVdoApp instance;
    private SkylightUsbDataService mSkylightService;
    private String TAG = "UsbVdoApp";
    private ServiceConnection mServiceConnection;
    private OnUsbListener onUsbListener;
    private ReadFrameImpl readFrameImpl;
    private OnStatusChangedListener onStatueChangedListener;


    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(USB_ATTACHED);
//        intentFilter.addAction(USB_DETACHED);
//        this.cameraConnectionReceiver = new CameraConnectionReceiver();
//        registerReceiver(this.cameraConnectionReceiver, intentFilter);
        bindservice();
    }
    public ReadFrameImpl getReadFrameImpl(){
        return readFrameImpl;
    }
    public OnStatusChangedListener getOnStatueChangedListener(){
        return onStatueChangedListener;
    }
    private void bindservice() {
         readFrameImpl = new ReadFrameImpl() {
            @Override
            public int onReadFrame(int i, byte[] bytes, int i1, long l) {
                if (onUsbListener != null) {
                    return onUsbListener.onReadFrame(i, bytes, i1, l);
                }else{
                    return  0;
                }
            }
        };

       onStatueChangedListener = new OnStatusChangedListener() {
           @Override
           public void onStatusUpdate(USB_STATUS usb_status) {
               String s = null;
               if (usb_status == USB_STATUS.USBDEVICE_ATTACHED){
                   s = "UsbDevice attached";
               }else if(usb_status == USB_STATUS.USBDEVICE_DETACHED){
                   s = "UsbDevice detached";
               }else if(usb_status == USB_STATUS.USB_FOR_USER_INIT){
                   s =  "sdk initialized";
               }else if(usb_status == USB_STATUS.USB_FOR_USER_INIT){
                   s =  "You can init first";
               }else if(usb_status == USB_STATUS.USBDEVICE_NEED_PERMISSION){
                   s = "Device need permission";
               }else if(usb_status == USB_STATUS.USBDEVICE_PERMISSION_REFUSEED){
                   s = "Device permission not set";
               }
               Toast.makeText(UsbVdoApp.this, ""+s, Toast.LENGTH_SHORT).show();
               onUsbListener.onStatusChanged(s);
           }

//           @Override
//            public void onStatusUpdate(String s) {
//                onUsbListener.onStatusChanged(s);
//            }
        };
//        mSkylightService = SkylightUsbDataService.instance(UsbVdoApp.getInstance());


//        mServiceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                // TODO Auto-generated method stub
//                if (mSkylightService != null) {
////                    mSkylightService.destroy();
//                    mSkylightService.release();
//                }
//            }
//
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder binder) {
//                Log.d(TAG, "onServiceConnected");
////                mSkylightService = ((SkylightUsbDataService.MyBinder) binder).getSkylightUsbDataService();
//                mSkylightService = SkylightUsbDataService.instance(UsbVdoApp.getInstance());
////                if (mSkylightService != null) {
////                    Log.d(TAG, "Skylight service is not null");
////                    //   Constants.FW = mSkylightService.requestFWVersion();
//                    mSkylightService.addOnStatusChangedListener(onStatueChangedListener);
//                    mSkylightService.setReadFrameImpl(readFrameImpl);
//
////                    mSkylightService.init();
////                    //    mSkylightService.obtainStream();
////                    Constants.wb_statue = mSkylightService.getWB();
////                    Constants.cc_statue = mSkylightService.getCircle_Calib();
////                    Constants.FW = mSkylightService.requestFWVersion();
////                    initStatue();
////                }
////                else{
////                    Toast.makeText(TestActivity.this,"Skylight service is null",Toast.LENGTH_SHORT).show();
////                }
//            }
//        };
//        Intent intent = new Intent(this, SkylightUsbDataService.class);
//        boolean bindSuccess = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    //    public CameraConnectionReceiver getCameraConnectionReceiver() {
//        return cameraConnectionReceiver;
//    }
    public static UsbVdoApp getInstance(){
        return instance;
    }
    public SkylightUsbDataService getUsbService(){
        return mSkylightService;
    }
    public void setSkylightUsbDataService(SkylightUsbDataService skylightUsbDataService){
        this.mSkylightService = skylightUsbDataService;
        mSkylightService.addOnStatusChangedListener(onStatueChangedListener);
        mSkylightService.setReadFrameImpl(readFrameImpl);
    }
    interface   OnUsbListener{
        int onReadFrame(int i, byte[] bytes, int i1, long l);
        void onStatusChanged(String s);
    }
    public void setOnReadFrameListener(OnUsbListener onUsbListener){
        this.onUsbListener = onUsbListener;
    }
}
