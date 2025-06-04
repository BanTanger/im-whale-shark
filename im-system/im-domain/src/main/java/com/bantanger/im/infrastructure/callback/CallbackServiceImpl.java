package com.bantanger.im.infrastructure.callback;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.infrastructure.config.AppConfig;
//import com.bantanger.im.infrastructure.utils.HttpRequestUtils;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/30 18:58
 */
@Slf4j
@Component
public class CallbackServiceImpl implements CallbackService {

//    @Resource
//    HttpRequestUtils httpRequestUtils;

    @Resource
    AppConfig appConfig;

    @Override
    public ResponseVO beforeCallback(Integer appId, String callbackCommand, String jsonBody) {
        try {
//            ResponseVO responseVO = httpRequestUtils.doPost(
//                    // 方法回调地址
//                    // TODO 目前只是将回调地址存储在配置文件中，后续要将其存放在表里持久化
//                    appConfig.getCallbackUrl(),
//                    // 指定返回值类型
//                    ResponseVO.class,
//                    // 请求参数，内部集成了 appId 和 callbackCommand
//                    builderUrlParams(appId, callbackCommand),
//                    // 回调内容
//                    jsonBody,
//                    // 指定字符集，为 null 默认 UTF8
//                    null
//            );
//            return responseVO;
            return null;
        } catch (Exception e) {
            log.error("Callback 回调 {} : {} 出现异常 : {} ", callbackCommand, appId, e.getMessage());
            // 回调失败也需要放行，避免阻碍正常程序执行，运维通过最高级别日志快速定位问题所在
            return ResponseVO.successResponse();
        }
    }

    @Override
    public void afterCallback(Integer appId, String callbackCommand, String jsonBody) {
        try {
//            httpRequestUtils.doPost(appConfig.getCallbackUrl(),Object.class,builderUrlParams(appId,callbackCommand),
//                    jsonBody,null);
        }catch (Exception e){
            log.error("callback 回调 {} : {} 出现异常 : {} ",callbackCommand , appId, e.getMessage());
        }
    }

    public Map<String, Object> builderUrlParams(Integer appId, String command) {
        Map<String, Object> map = new HashMap<>();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }

}
