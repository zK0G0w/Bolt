package top.wain.bolt.model.request;

/**
 * @Description: 设备地理位置信息
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param lon 经度
 * @param lat 纬度
 * @param ip 客户端IP地址
 * @param country 国家代码，如 CN
 * @param region 地区/省份
 */
public record DeviceGeo(
        float lon,
        float lat,
        String ip,
        String country,
        String region
) {}
