package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * 运维直接调用apollo接口操作数据
 * 聚合新增和发布接口
 * @author cheny.huang
 * @date 2018-12-18 11:23.
 */
@RestController
public class T8tController {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private ItemService itemService;
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private PortalConfig portalConfig;
    private static final String USER_ID = "yunwei";
    private static final String DEFAULT_NAMESPACE = "1.service-route-info";
    /**
     * 新增或者修改一条记录
     * 如果记录存在则修改，否则新增
     */
    @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/item/password/{password}", method = RequestMethod.POST)
    public HttpEntity<String> modifyItem(@PathVariable String appId, @PathVariable String env,
                              @PathVariable String clusterName, @PathVariable String namespaceName,@PathVariable String password,
                              @RequestBody ItemDTO item) {

        if (!password().equals(password)) {
            return new HttpEntity<>("password is not correctly");
        }
        if (!availableNamespace().contains(namespaceName)) {
            return new HttpEntity<>("namespace is not correctly");
        }
        if (!isValidItem(item)) {
            log.warn("请求数据有误");
            return new HttpEntity<>("key or value is not empty");
        }
        env = env.toUpperCase();
        ItemDTO origin = null;
        try {
            origin = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, item.getKey());
        } catch (Exception e) {
            String err = e.toString();
            if (err != null && err.contains("404")) {
                // ignore
            } else {
                log.error("查询数据失败,", e);
                return new HttpEntity<>("查询数据失败，错误信息"+Throwables.getStackTraceAsString(e));
            }
        }
        try {
            if (origin == null) {
                log.info("插入新的数据");
                createItem(appId, env, clusterName, namespaceName, item);
            } else {
                log.info("更新数据，原始数据为：{}", origin.getValue());
                updateItem(appId, env, clusterName, namespaceName, item, origin);
            }
            NamespaceReleaseModel releaseModel = new NamespaceReleaseModel();
            releaseModel.setReleaseTitle(releaseTitle());
            releaseModel.setReleaseComment("路由切换，数据修改");
            releaseModel.setReleasedBy(USER_ID);
            createRelease(appId, env, clusterName, namespaceName, releaseModel);
        } catch (Exception e) {
            log.error("操作apollo异常", e);
            return new HttpEntity<>("操作apollo异常"+ Throwables.getStackTraceAsString(e));
        }
        return new HttpEntity<>("ok");
    }

    private String password() {
        return  portalConfig.getValue("apollo.operator.password", USER_ID);
    }
    private String availableNamespace() {
        return  portalConfig.getValue("apollo.operator.namespace", DEFAULT_NAMESPACE);
    }
    private String releaseTitle() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-release";
    }

    private void createItem(String appId, String env,String clusterName, String namespaceName,ItemDTO item) {
        //protect
        item.setLineNum(0);
        item.setId(0);
        item.setDataChangeCreatedBy(USER_ID);
        item.setDataChangeLastModifiedBy(USER_ID);
        item.setDataChangeCreatedTime(null);
        item.setDataChangeLastModifiedTime(null);
        itemService.createItem(appId, Env.valueOf(env), clusterName, namespaceName, item);
    }

    private boolean isValidItem(ItemDTO item) {
        return Objects.nonNull(item) && !StringUtils.isContainEmpty(item.getKey()) && !StringUtils.isContainEmpty(item.getValue());
    }

    private void updateItem(String appId, String env, String clusterName, String namespaceName, ItemDTO item, ItemDTO toUpdateItem) {
        toUpdateItem.setComment(item.getComment());
        toUpdateItem.setValue(item.getValue());
        itemService.updateItem(appId, Env.fromString(env), clusterName, namespaceName, toUpdateItem);
    }

    private void createRelease(String appId, String env, String clusterName, String namespaceName, NamespaceReleaseModel model) {
        model.setAppId(appId);
        model.setEnv(env);
        model.setClusterName(clusterName);
        model.setNamespaceName(namespaceName);
        ReleaseDTO createdRelease = releaseService.publish(model);
        ConfigPublishEvent event = ConfigPublishEvent.instance();
        event.withAppId(appId)
                .withCluster(clusterName)
                .withNamespace(namespaceName)
                .withReleaseId(createdRelease.getId())
                .setNormalPublishEvent(true)
                .setEnv(Env.valueOf(env));
        publisher.publishEvent(event);
    }

    @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/item/password/{password}/{key:.+}", method = RequestMethod.POST)
    public HttpEntity<String> deleteItem(@PathVariable String appId, @PathVariable String env,@PathVariable String clusterName,
                           @PathVariable String namespaceName,@PathVariable String password, @PathVariable String key) {
        if (!password().equals(password)) {
            return new HttpEntity<>("password is not correctly");
        }
        if (!availableNamespace().contains(namespaceName)) {
            return new HttpEntity<>("namespace is not correctly");
        }
        ItemDTO toDeleteItem = null;
        env = env.toUpperCase();
        try {
            toDeleteItem = itemService.loadItem(Env.fromString(env), appId, clusterName, namespaceName, key);
        } catch (Exception e) {
            String err = e.toString();
            if (err != null && err.contains("404")) {
                // ignore
            } else {
                log.error("查询数据失败,", e);
                return new HttpEntity<>("查询数据失败，错误信息"+Throwables.getStackTraceAsString(e));
            }
        }
        if (toDeleteItem == null) {
            return new HttpEntity<>("ok");
        }
        try {
            itemService.deleteItem(Env.fromString(env), toDeleteItem.getId(), USER_ID);
            NamespaceReleaseModel releaseModel = new NamespaceReleaseModel();
            releaseModel.setReleaseTitle(releaseTitle());
            releaseModel.setReleaseComment("路由切换，数据修改");
            releaseModel.setReleasedBy(USER_ID);
            createRelease(appId, env, clusterName, namespaceName, releaseModel);
        } catch (Exception e) {
            log.error("删除apollo数据失败,", e);
            return new HttpEntity<>("delete fail, error trace:"+Throwables.getStackTraceAsString(e));
        }
        return new HttpEntity<>("ok");
    }

    @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/item/password/{password}/{key:.+}", method = RequestMethod.GET)
    public HttpEntity<Object> loadItem(@PathVariable String appId, @PathVariable String env,@PathVariable String clusterName,
                           @PathVariable String namespaceName,@PathVariable String key) {
        String err;
        try {
            return new HttpEntity<>(itemService.loadItem(Env.fromString(env.toUpperCase()), appId, clusterName, namespaceName, key).getValue());
        } catch (Exception e) {
            err = e.toString();
            if (err != null && err.contains("404")) {
                err = "数据不存在";
            } else {
                log.error("查询数据失败,", e);
                err = "查询数据失败，错误信息"+Throwables.getStackTraceAsString(e);
            }
        }
        return new HttpEntity<>("error:"+err);
    }
}
