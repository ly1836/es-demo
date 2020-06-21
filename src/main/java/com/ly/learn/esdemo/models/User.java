package com.ly.learn.esdemo.models;

import lombok.Data;
import java.util.Date;

@Data
public class User {

    /**
     * 姓名
     */
    private String name;

    /**
     * 住址
     */
    private String address;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 兴趣
     */
    private String interests;

    /**
     * 生日
     */
    private Date birthday;
}
