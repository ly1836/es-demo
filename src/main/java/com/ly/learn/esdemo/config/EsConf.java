package com.ly.learn.esdemo.config;

import com.ly.learn.esdemo.properties.ESProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;


@Configuration
public class EsConf {

    @Autowired
    ESProperties esProperties;

    @Bean
    public RestClientBuilder getRestClientBuilder() {
        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(esProperties.getUser(), esProperties.getPassword()));
        return RestClient.builder(new HttpHost(esProperties.getHost(), esProperties.getProt(), "http"))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.disableAuthCaching();
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });
    }

    @Bean
    public RestClient elasticsearchClient(@Qualifier("getRestClientBuilder") RestClientBuilder restClientBuilder) {
        return restClientBuilder.build();
    }


    @Bean
    public RestHighLevelClient getRestHighLevelClient(@Qualifier("getRestClientBuilder") RestClientBuilder restClientBuilder) {
        return new RestHighLevelClient(restClientBuilder);
    }

}
