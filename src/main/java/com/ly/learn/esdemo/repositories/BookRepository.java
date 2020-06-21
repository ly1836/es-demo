package com.ly.learn.esdemo.repositories;

import com.alibaba.fastjson.JSONObject;
import com.ly.learn.esdemo.models.User;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.RequestLine;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@Component
public class BookRepository{
    private final static String index = "/data/user/";

    @Autowired
    private RestClient client;

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    public void quey() throws IOException {
        Response response = client.performRequest(new Request("GET", index + "kRQvs3IBoNolw9TpM1CG"));
        RequestLine requestLine = response.getRequestLine();
        HttpHost host = response.getHost();
        int statusCode = response.getStatusLine().getStatusCode();
        Header[] headers = response.getHeaders();
        String string = EntityUtils.toString(response.getEntity());
        System.out.println(string);
        JSONObject jsonObject = JSONObject.parseObject(string);
        System.out.println(jsonObject.toJSONString());
    }

    public void add(Integer id) throws IOException {
        User user = new User();
        user.setName("王五");
        user.setAddress("深圳西乡");
        user.setAge(28);
        user.setInterests("唱歌，跳舞，游泳");
        user.setBirthday(new Date());
        String esindex = (id == null) ? index : index + id;

        Request post = new Request("POST", esindex);
        post.setJsonEntity(JSONObject.toJSONString(user));
        Response response = client.performRequest(post);
        String string = EntityUtils.toString(response.getEntity());
        System.out.println(string);
    }
}