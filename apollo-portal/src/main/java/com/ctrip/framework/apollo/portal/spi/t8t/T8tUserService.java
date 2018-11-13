package com.ctrip.framework.apollo.portal.spi.t8t;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.HttpPost;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sean.liu
 * @version 2017/9/18 14:15
 */
public class T8tUserService implements UserService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private T8tCallService t8TCallService;

    @Override
    public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        ObjectNode search = OBJECT_MAPPER.createObjectNode();
        search.put("name", keyword);
        args.set("search", search);
        args.put("page", offset);
        args.put("size", limit);
        HttpPost httpPost = t8TCallService.createAccPost(T8tConstants.API_QUERY_FULL, args.toString());
        JsonNode result = t8TCallService.post(httpPost);
        if (result != null) {
            JsonNode rows = result.get("rows");

            List<UserInfo> users = new ArrayList<>(rows.size());
            rows.forEach(e -> users.add(convertJson2User(e)));

            return users;
        }
        return Collections.emptyList();
    }

    @Override
    public UserInfo findByUserId(String userId) {
        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        ObjectNode search = OBJECT_MAPPER.createObjectNode();
        search.put("enName", userId);
        args.set("search", search);

        HttpPost httpPost = t8TCallService.createAccPost(T8tConstants.API_QUERY, args.toString());

        JsonNode result = t8TCallService.post(httpPost);

        if (result != null) {
            JsonNode rows = result.get("rows");

            List<UserInfo> users = new ArrayList<>(rows.size());
            rows.forEach(e -> users.add(convertJson2User(e)));

            if (!users.isEmpty()) {
                return users.get(0);
            }
        }

        return null;
    }

    @Override
    public List<UserInfo> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        ObjectNode search = OBJECT_MAPPER.createObjectNode();
        ArrayNode ids = OBJECT_MAPPER.valueToTree(userIds);
        search.putArray("enName_in").addAll(ids);
        args.set("search", search);

        HttpPost httpPost = t8TCallService.createAccPost(T8tConstants.API_QUERY, args.toString());

        JsonNode result = t8TCallService.post(httpPost);

        if (result != null) {
            JsonNode rows = result.get("rows");

            List<UserInfo> users = new ArrayList<>(rows.size());
            rows.forEach(e -> users.add(convertJson2User(e)));

            return users;
        }
        return Collections.emptyList();
    }

    private UserInfo convertJson2User(JsonNode jsonNode) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(jsonNode.get("enName").asText());
        userInfo.setName(jsonNode.get("name").asText());
        userInfo.setEmail(jsonNode.get("email").asText());
        return userInfo;
    }
}
