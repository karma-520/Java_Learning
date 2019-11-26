package com.consumer.my;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.consul.discovery.ConsulServerUtils.findHost;
import static org.springframework.cloud.consul.discovery.ConsulServerUtils.getMetadata;

/**
 * @author: create by wangmh
 * @name: MyConsulDiscoveryClient.java
 * @description: 自定义ConsulDiscoveryClient
 * @date:2019/11/26
 **/
@Component
public class MyConsulDiscoveryClient implements DiscoveryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyConsulDiscoveryClient.class);
    private final ConsulClient client;
    private final ConsulDiscoveryProperties properties;

    public MyConsulDiscoveryClient(ConsulClient client, ConsulDiscoveryProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String description() {
        return "Spring Cloud Consul Discovery Client";
    }

    @Override
    public List<ServiceInstance> getInstances(final String serviceId) {

        return getInstances(serviceId, QueryParams.DEFAULT);
    }

    public List<ServiceInstance> getInstances(final String serviceId,
                                              final QueryParams queryParams) {
        LOGGER.info("********************serviceId:" + serviceId);
        List<ServiceInstance> instances = new ArrayList<>();
        addInstancesToList(instances, serviceId, queryParams);

        return instances;
    }

    private void addInstancesToList(List<ServiceInstance> instances, String serviceId,
                                    QueryParams queryParams) {


        String aclToken = properties.getAclToken();
        Response<List<HealthService>> services;
        if (StringUtils.hasText(aclToken)) {
            //此处getTag(serviceId) 此处由获取默认tag改为获取指定tag
            services = client.getHealthServices(serviceId, getTag(serviceId), this.properties.isQueryPassing(), queryParams, aclToken);
        } else {
            services = client.getHealthServices(serviceId, getTag(serviceId), this.properties.isQueryPassing(), queryParams);
        }
        for (HealthService service : services.getValue()) {
            String host = findHost(service);

            Map<String, String> metadata = getMetadata(service);
            boolean secure = false;
            if (metadata.containsKey("secure")) {
                secure = Boolean.parseBoolean(metadata.get("secure"));
            }
            instances.add(new DefaultServiceInstance(serviceId, host, service
                    .getService().getPort(), secure, metadata));
        }
    }

    protected String getTag(String serviceId) {
        return this.properties.getQueryTagForService(serviceId);
    }

    @Override
    public List<String> getServices() {
        String aclToken = properties.getAclToken();

        if (StringUtils.hasText(aclToken)) {
            return new ArrayList<>(client.getCatalogServices(QueryParams.DEFAULT, aclToken).getValue()
                    .keySet());
        } else {
            return new ArrayList<>(client.getCatalogServices(QueryParams.DEFAULT).getValue()
                    .keySet());
        }
    }

    public List<ServiceInstance> getAllInstances() {
        List<ServiceInstance> instances = new ArrayList<>();

        Response<Map<String, List<String>>> services = client
                .getCatalogServices(QueryParams.DEFAULT);
        for (String serviceId : services.getValue().keySet()) {
            addInstancesToList(instances, serviceId, QueryParams.DEFAULT);
        }
        return instances;
    }
}
