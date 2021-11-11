package com.lewis.utils;

import java.io.*;
import java.util.Properties;

/**
 * 读取配置文件工具类
 *
 * @author Lewis
 */
public class ReadPropertiesUtils {

    private static final Properties properties = new Properties();

    /**
     * 初始化配置文件到Properties中
     * @param fileName
     * @return
     */
    public static int init(String fileName) {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;

        // 结果标志 1：成功 -1：失败
        int flag = 1;
        try {
            // 获取指定文件路径的输入流
            // 外置型
            inputStream = new BufferedInputStream(new FileInputStream(fileName));
            // 不为空则用inputStreamReader获取内容
            // 1.8 写法
//            inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            // 1.6 写法
            inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            // 加载到properties里提供外部调用
            properties.load(inputStreamReader);
        } catch (Exception e) {
            e.printStackTrace();
            flag = -1;
        } finally {
            // 关闭流
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 根据key获取value
     * @param key
     * @return value
     */
    public static String getValue(String key) {
        String value = "";
        try {
            value = properties.getProperty(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
