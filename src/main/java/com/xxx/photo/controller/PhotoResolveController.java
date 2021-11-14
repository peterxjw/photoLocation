package com.xxx.photo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * com.xxx.photo.controller
 *
 * @author xiejinwei
 * @date 2021/11/14
 */
@RestController
@Slf4j
public class PhotoResolveController {

    private static final Long CHANGE_SIZE = 1024L;

    private static final String KEY = "ef53121db59d05665c0715a767db2c85";

    private static final String REGEOCODE = "regeocode";

    @RequestMapping("test")
    public String test() throws ImageProcessingException, IOException {
        log.info("=============logTest================");
        File file = new File("D:\\Program Files\\Tencent\\QQFile\\1106972295\\FileRecv\\MobileFile\\IMG_20211024_154259.jpg");
        readImageInfo(file);
        return "success";
    }

    /**
     * 获取图片信息
     *
     * @param file 图片文件
     * @throws ImageProcessingException 图片信息读取异常
     * @throws IOException              图片文件流读取异常
     */
    private void readImageInfo(File file) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(file);

        log.info("=========全部详情=========");
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                System.out.format("[%s] - %s = %s\n", directory.getName(), tag.getTagName(), tag.getDescription());
            }

            if (directory.hasErrors()) {
                for (String error : directory.getErrors()) {
                    System.err.format("ERROR: %s", error);
                }
            }
        }

        log.info("=========常用信息=========");
        Double lat = null;
        Double lng = null;
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                // 标签名
                String tagName = tag.getTagName();
                // 标签信息
                String description = tag.getDescription();
                switch (tagName) {
                    case "Detected File Type Name":
                        System.out.println("图片类型: " + description);
                        break;
                    case "File Size":
                        System.out.println("图片大小: " + getImageSize(description.split(" bytes")[0]));
                        break;
                    case "File Modified Date":
                        System.out.println("图片创建时间: " + description);
                        break;
                    case "Image Height":
                        System.out.println("图片高度: " + description);
                        break;
                    case "Image Width":
                        System.out.println("图片宽度: " + description);
                        break;
                    case "Date/Time Original":
                        System.out.println("拍摄时间: " + description);
                        break;
                    case "GPS Latitude":
                        System.out.println("纬度 : " + description);
                        System.out.println("纬度(度分秒格式) : " + pointToLatLong(description));
                        lat = latLng2Decimal(description);
                        break;
                    case "GPS Longitude":
                        System.out.println("经度: " + description);
                        System.out.println("经度(度分秒格式): " + pointToLatLong(description));
                        lng = latLng2Decimal(description);
                        break;
                    default:
                }
            }
        }
        if (lat != null && lng != null) {
            System.out.println(getAddressByLocation(lng, lat));
        }
    }

    /**
     * 经纬度转换成° ' " 形式
     *
     * @param point 经纬度
     * @return ° ' " 形式的经纬度
     */
    private String pointToLatLong(String point) {
        double du = Double.parseDouble(point.substring(0, point.indexOf("°")).trim());
        double fen = Double.parseDouble(point.substring(point.indexOf("°") + 1, point.indexOf("'")).trim());
        double miao = Double.parseDouble(point.substring(point.indexOf("'") + 1, point.indexOf("\"")).trim());
        double duStr = du + fen / 60 + miao / 60 / 60;
        return duStr + "";
    }

    /**
     * 经纬度转换成数字
     *
     * @param gps 经纬度
     * @return 数字经纬度
     */
    private double latLng2Decimal(String gps) {
        String a = gps.split("°")[0].replace(" ", "");
        String b = gps.split("°")[1].split("'")[0].replace(" ", "");
        String c = gps.split("°")[1].split("'")[1].replace(" ", "").replace("\"", "");
        return Double.parseDouble(a) + Double.parseDouble(b) / 60 + Double.parseDouble(c) / 60 / 60;
    }

    /**
     * 获取图片大小
     *
     * @param sizeStr 图片大小（bytes）
     * @return 图片大小（B、KB、M、G）
     */
    private String getImageSize(String sizeStr) {
        long size = Long.parseLong(sizeStr);

        if (size < CHANGE_SIZE) {
            return size + "B";
        } else {
            size = size / CHANGE_SIZE;
        }

        if (size < CHANGE_SIZE) {
            return size + "KB";
        } else {
            size = size / CHANGE_SIZE;
        }

        if (size < CHANGE_SIZE) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return (size / 100) + "." + (size % 100) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / CHANGE_SIZE;
            return (size / 100) + "." + (size % 100) + "GB";
        }
    }

    /**
     * 根据图片经纬度获取地理位置
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 地理位置
     */
    private String getAddressByLocation(Double lng, Double lat) {
        String location = lng + "," + lat;
        String getUrl = "https://restapi.amap.com/v3/geocode/regeo?key=" + KEY + "&location=" + location;
        String address = "";
        try {
            // 把字符串转换为URL请求地址
            URL url = new URL(getUrl);
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 连接会话
            connection.connect();
            // 获取输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            // 循环读取流
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            // 关闭流
            br.close();
            // 断开连接
            connection.disconnect();
            JSONObject a = JSON.parseObject(sb.toString());
            // 判断输入的位置点是否存在
            System.out.println(sb.toString());
            if (a.getJSONObject(REGEOCODE).size() > 0) {
                address = a.getJSONObject(REGEOCODE).get("formatted_address").toString();
            }
            System.out.println(location);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("失败!");
        }
        return address;
    }

}
