package top.wain.bolt.model.domain;

/**
 * @Description: DSP平台配置，描述下游DSP平台的基本信息和流量控制参数
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param platformId 平台唯一标识
 * @param name 平台名称（如"华为ADX"）
 * @param platformCode 适配器路由码，用于匹配DSP适配器实现
 * @param dockingUrl 下游API地址
 * @param trafficQps 平台QPS限制，0表示不限
 * @param trafficFrequency 用户日频次上限，0表示不限
 */
public record SellPlatform(
        String platformId,
        String name,
        String platformCode,
        String dockingUrl,
        int trafficQps,
        int trafficFrequency
) {}
