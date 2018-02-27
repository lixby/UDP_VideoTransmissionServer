package com.skylight.apollo.baseunit.tcp.handlercallback;


import com.skylight.apollo.baseunit.tcp.command.callback.ICmdHandlerCallback;

/**
 * Description:
 * Author: Created by lixby on 17-12-21.
 */

public abstract class TcpIpStatusHandlerCallback extends ICmdHandlerCallback {

    public abstract void inItSuccess();

    public abstract void inItFailed();

    public abstract void disConnected();




}
