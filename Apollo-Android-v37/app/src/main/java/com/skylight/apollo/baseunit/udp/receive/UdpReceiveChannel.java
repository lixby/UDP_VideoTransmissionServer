package com.skylight.apollo.baseunit.udp.receive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @Author: gengwen
 * @Date: 2018/1/19.
 * @Company:Skylight
 * @Description:
 */

public class UdpReceiveChannel implements Runnable {
    private DatagramChannel receiveChannel;
    private Selector selector;
    private int port = 3457;
    private boolean keepWatching = true;
    private int bufferSize = 1024;

    public UdpReceiveChannel() {

    }

    @Override
    public void run() {
        try {
            receiveChannel = DatagramChannel.open();
            selector = Selector.open();
            receiveChannel.bind(new InetSocketAddress(port));
            receiveChannel.register(selector, SelectionKey.OP_READ);

            ByteBuffer receiveBuffer = ByteBuffer.allocate(bufferSize);
            while (keepWatching){
                receiveBuffer.clear();
                InetSocketAddress client = (InetSocketAddress)receiveChannel.receive(receiveBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
