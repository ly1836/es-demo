package com.ly.learn.esdemo.util;

import com.alibaba.fastjson.JSONObject;

public class JsonValidator {

    /**
     * 判断是否为正常的json字符串
     * @param jsonStr
     * @return
     */
    public static boolean validate(String jsonStr){
        try {
            JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        }catch (Exception ex){
            return false;
        }

        return true;
    }
}
