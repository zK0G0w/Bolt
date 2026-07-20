package top.wain.bolt.model.request;

/**
 * @Description: 设备标识信息，聚合各平台设备ID
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param oaid OAID，Android 匿名设备标识
 * @param imei IMEI，Android 国际移动设备识别码
 * @param idfa IDFA，iOS 广告标识符
 * @param androidId Android ID
 */
public record DeviceId(
        String oaid,
        String imei,
        String idfa,
        String androidId
) {}
