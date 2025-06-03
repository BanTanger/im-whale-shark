package com.bantanger.im.infrastructure.zookeeper;

import com.alibaba.fastjson.JSON;
import com.bantanger.im.common.constant.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 11:19
 */
@Slf4j
@Getter
@Setter
@Component
public class ZkManager extends Constants.ZkConstants {

    public CuratorZkClient zkClient;

    /**
     * 从 Zk 获取所有 TCP 服务节点地址
     *
     * @return
     */
    public List<String> getAllTcpNode() {
        List<String> children = zkClient.getChildren(ImCoreZkRoot + ImCoreZkRootTcp);
        log.info("Query all [TCP] node =[{}] success.", JSON.toJSONString(children));
        return children;
    }

    /**
     * 从 Zk 获取所有 WEB 服务节点地址
     *
     * @return
     */
    public List<String> getAllWebNode() {
        List<String> children = zkClient.getChildren(ImCoreZkRoot + ImCoreZkRootWeb);
        log.info("Query all [WEB] node =[{}] success.", JSON.toJSONString(children));
        return children;
    }
}
