package com.lhs.common.util;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigUtil implements InitializingBean {

    @Value("${resourcesPath.penguin}")
    private String penguin;  //    企鹅物流数据文件位置
    @Value("${resourcesPath.frontEnd}")
    private String frontEnd;  //    前端数据文件位置
    @Value("${resourcesPath.item}")
    private String item;  //    材料相关数据文件位置
    @Value("${resourcesPath.backup}")
    private String backup;  //    备份文件路径
    @Value("${resourcesPath.schedule}")
    private String schedule;
    @Value("${resourcesPath.secret}")
    private String secret;
    @Value("${resourcesPath.signKey}")
    private String signKey;

    public static String Penguin;
    public static String Item;
    public static String FrontEnd;
    public static String Backup;
    public static String Schedule;
    public static String Secret;
    public static String SignKey;


    @Override
    public void afterPropertiesSet() {
        Penguin = penguin;
        Item = item;
        FrontEnd = frontEnd;
        Backup = backup;
        Schedule = schedule;
        Secret = secret;
        SignKey = signKey;
    }
}