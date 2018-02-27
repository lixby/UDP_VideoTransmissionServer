package com.skylight.apollo.baseunit.tcp.bean;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * @Author: gengwen
 * @Date: 2018/1/2.
 * @Company:Skylight
 * @Description:
 */

public class Node {
    protected Node next; //指针域
    protected NodeData nodeData;//数据域

    public Node(NodeData nodeData){
        this.nodeData = nodeData;
        LinkedList ll = new LinkedList();

        LinkedHashMap lh = new LinkedHashMap();

    }
}
