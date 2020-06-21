package com.ly.learn.esdemo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ly.learn.esdemo.models.User;
import com.ly.learn.esdemo.properties.ESProperties;
import com.ly.learn.esdemo.repositories.BookRepository;
import com.ly.learn.esdemo.repositories.ElasticClient;
import com.ly.learn.esdemo.repositories.ElasticsearchUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EsDemoApplicationTests {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ElasticClient elasticClient;

    @Test
    public void createIndex() {
        try {
            User user = new User();
            user.setName("王五");
            user.setAddress("深圳西乡");
            user.setAge(28);
            user.setInterests("唱歌，跳舞，游泳");
            user.setBirthday(new Date());


            elasticClient.addDocByJson("data","user","1", JSONObject.toJSONString(user));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void queryTest() {
        try {
            bookRepository.quey();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void addTest() {
        try {
            //bookRepository.add(null);
            bookRepository.add(666);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
