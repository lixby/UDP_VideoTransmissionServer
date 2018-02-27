package com.skylight.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;

/**
 * Created by HoangHo on 6/14/17.
 */

public class CameraConnectionReceiver extends BroadcastReceiver {
    public static final String USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";


    protected ICameraListener iListener;

    public void setListener(ICameraListener iListener) {
        this.iListener = iListener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (iListener != null) {
            String action = intent.getAction();
            if (USB_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra("device");
                if (device != null && device.getProductId() == 1 && device.getVendorId() == 16981) {
                    // TODO Hoang will refactor this
                    try {
                      //  LogUtil.i("onCameraConnected");
                        iListener.onCameraConnected(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } else if (USB_DETACHED.equals(action)) {
                // TODO Hoang will refactor this
                try {
              //      LogUtil.i("onCameraDisconnect");
                    iListener.onCameraDisconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
