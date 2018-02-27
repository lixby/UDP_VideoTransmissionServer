package com.skylight.apollo.baseunit.tcp.bean;

/**
 * @Author: gengwen
 * @Date: 2018/1/2.
 * @Company:Skylight
 * @Description:
 */

public class NodeData {
    public long packetIndex;
    public byte[] packetData;

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NodeData){
            NodeData nodeData = (NodeData) obj;
            if (packetIndex == nodeData.packetIndex)return true;
        }
        return false;
    }
}
