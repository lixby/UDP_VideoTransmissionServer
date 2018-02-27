package com.skylight.apollo.baseunit.tcp.mode;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Description:
 * Author: Created by lixby on 17-12-21.
 */

public class TcpIpInformation {

    /**Tcp server ip*/
    //private String serverTcp_IP="192.168.3.3";
    private String serverTcp_IP="192.168.3.42";
    //private String serverTcp_IP="192.168.0.104";
    /**Tcp server port*/
    private int serverTcp_Port=6543;

    /**Udp server ip*/
    private String serverUdp_IP="192.168.0.100";
    /**Udp server port*/
    private int serverUdp_Port=3456;

    /**Udp server ip*/
    private String clientUdp_IP="192.168.3.11";
    /**Udp server port*/
    private int clientUdp_Port=3364;

    private static TcpIpInformation tcpIpInformation;

    public static synchronized TcpIpInformation getInstance(){
        if(tcpIpInformation==null){
            tcpIpInformation=new TcpIpInformation();
        }

        return tcpIpInformation;
    }


    public String getServerTcp_IP() {
        return serverTcp_IP;
    }

    public void setServerTcp_IP(String serverTcp_IP) {
        this.serverTcp_IP = serverTcp_IP;
    }

    public int getServerTcp_Port() {
        return serverTcp_Port;
    }

    public void setServerTcp_Port(int serverTcp_Port) {
        this.serverTcp_Port = serverTcp_Port;
    }

    public String getServerUdp_IP() {
        return serverUdp_IP;
    }

    public void setServerUdp_IP(String serverUdp_IP) {
        this.serverUdp_IP = serverUdp_IP;
    }

    public int getServerUdp_Port() {
        return serverUdp_Port;
    }

    public void setServerUdp_Port(int serverUdp_Port) {
        this.serverUdp_Port = serverUdp_Port;
    }

    public String getClientUdp_IP() {
        return clientUdp_IP;
    }

    public void setClientUdp_IP(String clientUdp_IP) {
        this.clientUdp_IP = clientUdp_IP;
    }

    public int getClientUdp_Port() {
        return clientUdp_Port;
    }

    public void setClientUdp_Port(int clientUdp_Port) {
        this.clientUdp_Port = clientUdp_Port;
    }

    /**
     * Get local address
     * @return Local IP
     * @throws IOException
     */
    public String  getLocalAddress() throws IOException {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces(); enNetI.hasMoreElements();){
                NetworkInterface netI=enNetI.nextElement();

                for(Enumeration<InetAddress> enAddress = netI.getInetAddresses(); enAddress.hasMoreElements();){
                    InetAddress inetAddress=enAddress.nextElement();
                    if(inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()){
                        clientUdp_IP=inetAddress.getHostAddress();
                        return clientUdp_IP;
                    }

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        throw new IOException("Did not get to the local IP address");
    }



}
