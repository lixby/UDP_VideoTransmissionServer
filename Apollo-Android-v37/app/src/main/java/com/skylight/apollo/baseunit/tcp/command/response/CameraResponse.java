package com.skylight.apollo.baseunit.tcp.command.response;


import com.skylight.apollo.baseunit.tcp.command.callback.CmdStatus;

/**
 * Created by xiaobin on 2017/12/1.
 */
public abstract class CameraResponse<T> {

    /**Command request successful*/
    public static final int CMD_STATUS_SUCCESS=0;
    /**camera disconnected*/
    public static final int CAM_DISCONNECTED=-100;


    /**
     * Frameware upgrade status
     */

    /**Fw status updating */
    public static final int FW_FILE_UPDATING=1;

    /**Fw update status finished */
    public static final int FW_FILE_SUCCESS=0;

    /**Fw update status finished */
    public static final int FW_FILE_FAILED=-1;

    /**Error updating file*/
    public static final int UPGRADE_FILE_ERROR=-10;



    public abstract CmdStatus handleCommandStatus(int code);

    
    /*Command status*/
    protected CmdStatus cmdStatus;

    protected T result;


    public CmdStatus getCmdStatus() {
        return cmdStatus;
    }

    public void setCmdStatus(CmdStatus cmdStatus) {
        this.cmdStatus = cmdStatus;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }




}
