package com.skylight.apollo.baseunit.tcp.command;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Description: TcpCommandManager for send command to tcp service
 * Author: Created by lixby on 17-12-18.
 *
 */

public class TcpCommandManager implements Runnable{

    //Message command queue
    private ConcurrentLinkedQueue<PacketCmdHandler> cmdQueue = new ConcurrentLinkedQueue();
    private ExecutorService service;
    private Future future;
    private boolean executing=false;


    public TcpCommandManager(){
        //Init tcp packet receiver
    }



    public void startRun(){
        if(service==null){
            service= Executors.newCachedThreadPool();
        }

        if(!executing){
            executing=true;
            future=service.submit(this);
        }

    }



    public void addCommand(PacketCmdHandler handler){
        cmdQueue.add(handler);
    }

    /**
     *
     * @param handler PacketCmdHandler
     * @param isResponse  Need to respond to Tcp requests ,True :response,False:no
     */
    public void addCommand(PacketCmdHandler handler,boolean isResponse){
        addCommand(handler);

    }

    public void stopRun(){
        executing=false;
        if(future!=null){
            future.cancel(true);
            future=null;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }
        cmdQueue.clear();

    }

    @Override
    public void run() {
        while (executing){
            PacketCmdHandler packetHandler = cmdQueue.poll();
            if(packetHandler!=null){
                    packetHandler.execute();
            }
        }

    }




}
