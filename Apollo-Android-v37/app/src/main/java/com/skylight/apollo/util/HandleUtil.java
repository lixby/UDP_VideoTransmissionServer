package com.skylight.apollo.util;

import com.kandaovr.sdk.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @Author: gengwen
 * @Date: 2017/9/15.
 * @Company:Skylight
 * @Description:
 */

public class HandleUtil {
    public static String handleVersion(String usdVersion,String usb_sdk){
        String version = null;
        if (usdVersion != null) {
            version = handleJsonVersion(usdVersion) + ";\nusb_sdk_version:" + usb_sdk + ";\nKD_version:" + Constants.SDK_VERSION;
        }else{
            version ="fw_Version:null;\nusb_sdk_version:" + usb_sdk  + ";\nKD_version:" + Constants.SDK_VERSION;
        }
        return version;
    }
    private static String handleJsonVersion(String usbVersion){
        String cameraV = null;
        String caseV = null;
        try {
            JSONObject jsAllVersion = new JSONObject(usbVersion);

            JSONObject jsCamera     = jsAllVersion.getJSONObject("camera");
            cameraV                 = jsCamera.getString("version");

            JSONObject jsCase       = jsAllVersion.getJSONObject("case");
            caseV                   = jsCase.getString("version");
        } catch (JSONException e) {
            e.printStackTrace();
            cameraV = null;
            caseV   = null;
        }
        return "camera_version:" + cameraV +";\ncase_verrsion:" + caseV;
    }
}
