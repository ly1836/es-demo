package com.ly.learn.esdemo.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties({
        ESProperties.class
})
public class AutoConfiguration {

}
