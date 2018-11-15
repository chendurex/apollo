package com.ctrip.framework.apollo.common.advice;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * @author cheny.huang
 * @date 2018-09-07 15:44.
 */
public class CustomizePathLogInterceptor extends HandlerInterceptorAdapter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Gson gson = new Gson();
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        MDC.put("bsid", UUID.randomUUID().toString());
        log.info("Request Start And RequestPath Is: {}", getRealPath(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex) {
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            log.info("Params: {}", gson.toJson(params));
        }
        MDC.remove("bsid");
    }

    private String getRealPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return (path == null || path.trim().length() == 0) ? request.getRequestURI() : path;
    }
}
