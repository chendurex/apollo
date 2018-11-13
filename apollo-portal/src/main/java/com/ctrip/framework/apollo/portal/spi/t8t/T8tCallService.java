package com.ctrip.framework.apollo.portal.spi.t8t;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author sean.liu
 * @version 2017/9/22 16:02
 */
@Component
public class T8tCallService {
    private static final Logger logger = LoggerFactory.getLogger(T8tCallService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private String ESB_URL;

    public T8tCallService() {
        ESB_URL = System.getProperty("esb_url");
        if (StringUtils.isBlank(ESB_URL)) {
            logger.error("Please set esb_url when using t8t profile!");
            System.exit(-1);
        }
    }

    public HttpPost createAccPost(String method, String args) {//{"enName":%s}
        HttpPost httpPost = new HttpPost(ESB_URL);
        httpPost.setHeader("s", T8tConstants.ACC_SERVICE);
        httpPost.setHeader("m", method);
        httpPost.setEntity(new StringEntity( String.format("{'args':%s}", args), "utf-8"));
        return httpPost;
    }

    /**
     *
     * @param httpPost
     * @return nullable
     */
    public JsonNode post(HttpPost httpPost) {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse resp = httpClient.execute(httpPost);

            HttpEntity respEntity = resp.getEntity();
            String respBody = EntityUtils.toString(respEntity, "utf-8");

            JsonNode json = OBJECT_MAPPER.readTree(respBody);
            int code = json.get("status").asInt();
            JsonNode result = json.get("result");
            if (code == 200) {
                return result;
            } else {
                logger.warn("call t8t service return not 200, headers, resp:{}", httpPost.getAllHeaders(), respBody);
            }

        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
        return null;
    }
}
