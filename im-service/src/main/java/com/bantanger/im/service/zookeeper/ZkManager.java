package com.bantanger.im.service.zookeeper;

import com.bantanger.im.common.comstant.Constants;
import org.I0Itec.zkclient.ZkClient;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 11:19
 */
public class ZkManager extends Constants.ZkConstants {

    private ZkClient zkClient;

    public ZkManager(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    /**
     * 创建父类节点，格式：im-coreRoot/tcp/ip:port
     * 如果没有就新建
     */
    public void createRootNode() {
        boolean rootExists = zkClient.exists(ImCoreZkRoot);
        if (!rootExists) {
            zkClient.createPersistent(ImCoreZkRoot);
        }
        boolean tcpExists = zkClient.exists(ImCoreZkRoot + ImCoreZkRootTcp);
        if (!tcpExists) {
            zkClient.createPersistent(ImCoreZkRoot + ImCoreZkRootTcp);
        }
        boolean webExists = zkClient.exists(ImCoreZkRoot + ImCoreZkRootWeb);
        if (!webExists) {
            zkClient.createPersistent(ImCoreZkRoot + ImCoreZkRootWeb);
        }
    }

    /**
     * 创建节点, 格式：ip + port
     * @param path
     */
    public void createNode(String path) {
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path);
        }
    }
}
