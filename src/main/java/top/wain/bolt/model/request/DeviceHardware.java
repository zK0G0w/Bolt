package top.wain.bolt.model.request;

import top.wain.bolt.model.enums.Carrier;
import top.wain.bolt.model.enums.ConnectionType;
import top.wain.bolt.model.enums.DeviceType;
import top.wain.bolt.model.enums.OsType;

/**
 * @Description: 设备硬件信息，包含设备类型、品牌型号、系统版本、屏幕参数、网络状态
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param type 设备类型（手机/平板/PC/电视）
 * @param make 设备制造商，如 Xiaomi、Apple
 * @param model 设备型号，如 Mi 14、iPhone 16
 * @param os 操作系统类型
 * @param osVersion 操作系统版本号
 * @param screenWidth 屏幕宽度，单位像素
 * @param screenHeight 屏幕高度，单位像素
 * @param connection 网络连接类型（WIFI/4G/5G等）
 * @param carrier 运营商
 */
public record DeviceHardware(
        DeviceType type,
        String make,
        String model,
        OsType os,
        String osVersion,
        int screenWidth,
        int screenHeight,
        ConnectionType connection,
        Carrier carrier
) {}
