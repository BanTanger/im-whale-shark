package com.bantanger.im.infrastructure.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.BaseErrorCode;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.error.GateWayErrorCode;
import com.bantanger.im.common.exception.ApplicationExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 22:14
 */
@Slf4j
@Component
public class GateWayInterceptor implements HandlerInterceptor {

    @Resource
    IdentityCheck identityCheck;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 先放权进行内部代码测试
        if (true) {
            return true;
        }
        //获取 appId 操作人 userSign
        String appIdStr = request.getParameter("appId");
        if (StringUtils.isBlank(appIdStr)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .APPID_NOT_EXIST), response);
            return false;
        }

        String identifier = request.getParameter("identifier");
        if (StringUtils.isBlank(identifier)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .OPERATER_NOT_EXIST), response);
            return false;
        }

        String userSign = request.getParameter("userSign");
        if (StringUtils.isBlank(userSign)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode
                    .USERSIGN_NOT_EXIST), response);
            return false;
        }

        // 签名和操作人和 appid 是否匹配
        ApplicationExceptionEnum applicationExceptionEnum = identityCheck.checkUserSig(identifier, appIdStr, userSign);
        if (applicationExceptionEnum != BaseErrorCode.SUCCESS) {
            resp(ResponseVO.errorResponse(applicationExceptionEnum), response);
            return false;
        }

        return true;
    }

    private void resp(ResponseVO respVo ,HttpServletResponse response){

        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            String resp = JSONObject.toJSONString(respVo);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-type", "application/json;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Origin","*");
            response.setHeader("Access-Control-Allow-Credentials","true");
            response.setHeader("Access-Control-Allow-Methods","*");
            response.setHeader("Access-Control-Allow-Headers","*");
            response.setHeader("Access-Control-Max-Age","3600");

            writer = response.getWriter();
            writer.write(resp);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(writer != null){
                writer.checkError();
            }
        }

    }

}
