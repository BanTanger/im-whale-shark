package com.bantanger.design.bridge.bridge.abst.factory;

import com.bantanger.design.bridge.bridge.abst.AbstractRegisterLoginComponent;
import com.bantanger.design.bridge.bridge.abst.RegisterLoginComponent;
import com.bantanger.design.bridge.bridge.function.RegisterLoginFunc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:42
 */
public class RegisterLoginComponentFactory {

    /** 缓存 AbstractRegisterLoginComponent（左路）。根据不同的登录方式进行缓存 */
    public static Map<String, AbstractRegisterLoginComponent> componentMap
            = new ConcurrentHashMap<>();
    /** 缓存不同类型的实现类（右路），如：RegisterLoginByDefault,RegisterLoginByGithub */
    public static java.util.Map<String, RegisterLoginFunc> funcMap
            = new ConcurrentHashMap<>();

    /** 根据不同的登录类型，获取 AbstractRegisterLoginComponent */
    public static AbstractRegisterLoginComponent getComponent(String type) {
        //如果存在，直接返回
        AbstractRegisterLoginComponent component = componentMap.get(type);
        if(component == null) {
            //并发情况下，汲取双重检查锁机制的设计，如果 componentMap 中没有，则进行创建
            synchronized (componentMap) {
                component = componentMap.get(type);
                if(component == null) {
                    //根据不同类型的实现类（右路），创建 RegisterLoginComponent 对象，
                    //并 put 到 map 中缓存起来，以备下次使用。
                    component = new RegisterLoginComponent(funcMap.get(type));
                    componentMap.put(type, component);
                }
            }
        }
        return component;
    }

}
