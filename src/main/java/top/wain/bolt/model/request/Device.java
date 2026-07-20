package top.wain.bolt.model.request;

/**
 * @Description: 设备信息，组合设备标识、硬件参数、地理位置三个维度
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 设备标识（OAID/IMEI/IDFA等）
 * @param hardware 硬件与系统信息
 * @param geo 地理位置信息
 */
public record Device(
        DeviceId id,
        DeviceHardware hardware,
        DeviceGeo geo
) {}
