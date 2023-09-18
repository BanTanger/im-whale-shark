package com.bantanger.im.tcp;

import ch.qos.logback.classic.util.ContextInitializer;
import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.service.rabbitmq.MqFactory;
import com.bantanger.im.service.rabbitmq.listener.MqMessageListener;
import com.bantanger.im.service.redis.RedissonManager;
import com.bantanger.im.service.strategy.command.CommandFactoryConfig;
import com.bantanger.im.service.strategy.login.factory.LoginStatusFactoryConfig;
import com.bantanger.im.service.zookeeper.ZkManager;
import com.bantanger.im.service.zookeeper.ZkRegistry;
import com.bantanger.im.tcp.server.ImServer;
import com.bantanger.im.tcp.server.ImWebSocketServer;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 15:24
 */
public class Starter {

    public static void main(String [] args) {
        if (args.length > 0) {
            start(args[0]);
        }
    }

    public static void start(String path) {
        try {
            Yaml yaml = new Yaml();
            FileInputStream is = new FileInputStream(path);
            ImBootstrapConfig config = yaml.loadAs(is, ImBootstrapConfig.class);

            new ImServer(config.getIm()).start();
            new ImWebSocketServer(config.getIm()).start();

            // redisson 在系统启动之初就初始化
            RedissonManager.init(config);

            // 策略工厂初始化
            CommandFactoryConfig.init();
            LoginStatusFactoryConfig.init();
            // MQ 工厂初始化
            MqFactory.init(config.getIm().getRabbitmq());
            // MqFactory.createExchange();
            // MQ 监听器初始化
            MqMessageListener.init(String.valueOf(config.getIm().getBrokerId()));
            // 每个服务器都注册 Zk
            registerZk(config);
        } catch (FileNotFoundException | UnknownHostException e) {
            e.printStackTrace();
            // 程序退出
            System.exit(500);
        }
    }

    /**
     * 对于每一个 IP 地址，都开启一个线程去启动 Zk
     *
     * @param config
     * @throws UnknownHostException
     */
    public static void registerZk(ImBootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(config.getIm().getZkConfig().getZkAddr(),
                config.getIm().getZkConfig().getZkConnectTimeOut());
        ZkManager zkManager = new ZkManager(zkClient);
        ZkRegistry zkRegistry = new ZkRegistry(zkManager, hostAddress, config.getIm());
        Thread thread = new Thread(zkRegistry);
        thread.start();
    }

}
