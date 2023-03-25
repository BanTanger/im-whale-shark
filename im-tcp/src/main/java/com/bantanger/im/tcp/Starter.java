package com.bantanger.im.tcp;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.service.rabbitmq.listener.MqMessageListener;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.strategy.command.factory.CommandFactoryConfig;
import com.bantanger.im.service.utils.MqFactory;
import com.bantanger.im.tcp.server.ImServer;
import com.bantanger.im.tcp.server.ImWebSocketServer;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

    private static void start(String path) {
        try {
            Yaml yaml = new Yaml();
            FileInputStream is = new FileInputStream(path);
            ImBootstrapConfig config = yaml.loadAs(is, ImBootstrapConfig.class);

            new ImServer(config.getIm()).start();
            new ImWebSocketServer(config.getIm()).start();

            // redisson 在系统启动之初就初始化
            RedisManager.init(config);
            // 策略工厂初始化
            CommandFactoryConfig.init();
            // MQ 工厂初始化
            MqFactory.init(config.getIm().getRabbitmq());
            // MQ 监听器初始化
            MqMessageListener.init();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // 程序退出
            System.exit(500);
        }
    }

}
