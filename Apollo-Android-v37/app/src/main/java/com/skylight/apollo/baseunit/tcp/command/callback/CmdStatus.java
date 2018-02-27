package com.skylight.apollo.baseunit.tcp.command.callback;


/**
 * Created by xiaobin on 2017/11/30.
 * Description:Record the command request result,
 * You can deal with other business logic based on the results.
 */
public class CmdStatus {


    /**Command result code*/
    private int code;

    /**Command result code description*/
    private String message;

    /**
     *
     * @return code : 0 means successful, non-zero means failure.
     */
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    /**
     *
     * @return message : Response status description.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CmdStatus(){

    }

    public CmdStatus(int code){
        this.code = code;
    }

    public CmdStatus(int code, String msg){
        this.code = code;
        this.message  = msg;
    }

}
