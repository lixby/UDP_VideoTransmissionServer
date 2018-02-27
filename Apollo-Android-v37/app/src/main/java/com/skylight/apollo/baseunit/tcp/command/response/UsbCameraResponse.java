package com.skylight.apollo.baseunit.tcp.command.response;


import com.skylight.apollo.baseunit.tcp.command.callback.CmdStatus;

/**
 * Created by xiaobin on 2017/12/1.
 */
public class UsbCameraResponse extends CameraResponse {


    @Override
    public CmdStatus handleCommandStatus(int code) {
        String status = null;
        switch (code) {
            case 0:
                status = "the request is successful.";
                break;
            case -1:
                status = "Error updating file.";
                break;
            case -2:
                status = "File upgrade.";
                break;
            case -100:
                status = "Device disconnected.";
                break;
            case -9018:
                status = "Error update packet.";
                break;
            case -9017:
                status = "update err version identifier";
                break;
            case -9008:
                status = "Error params";
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
                break;
            default:
                status="No message";
                break;
        }


        return this.cmdStatus=new CmdStatus(code,status);

    }

}
