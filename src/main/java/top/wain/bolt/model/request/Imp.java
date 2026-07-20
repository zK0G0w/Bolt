package top.wain.bolt.model.request;

import top.wain.bolt.model.enums.AdFormat;
import top.wain.bolt.model.enums.DealType;

/**
 * @Description: 广告位，描述单个广告展示机会
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 广告位ID
 * @param format 广告形式（开屏/横幅/信息流/插屏/激励视频）
 * @param dealType 交易类型（RTB/PD/PDB）
 * @param bidFloor 底价，单位：分/CPM
 * @param width 广告位宽度，单位像素
 * @param height 广告位高度，单位像素
 */
public record Imp(
        String id,
        AdFormat format,
        DealType dealType,
        long bidFloor,
        int width,
        int height
) {}
