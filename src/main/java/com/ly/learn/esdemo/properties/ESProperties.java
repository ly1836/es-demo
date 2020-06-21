package com.ly.learn.esdemo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@Data
@ConfigurationProperties(prefix = "es")
public class ESProperties {
    /**
     * es集群主机IP
     */
    private String host;

    /**
     * es tcp端口
     */
    private Integer prot;

    /**
     * es 集群名称
     */
    private String clusterName;

    /**
     * es x-pack 认证名称
     */
    private String user;

    /**
     * es x-pack 认证秘钥
     */
    private String password;

    @PostConstruct
    public void init(){
        System.out.println("ES 配置信息初始化完成:" + this.toString());
    }

}
