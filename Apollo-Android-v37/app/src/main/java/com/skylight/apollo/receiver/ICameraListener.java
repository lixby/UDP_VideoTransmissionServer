package com.skylight.apollo.receiver;

import android.hardware.usb.UsbDevice;

/**
 * Created by HoangHo on 6/14/17.
 */

public interface ICameraListener {
    void onCameraConnected(UsbDevice device);

    void onCameraDisconnect();
}
