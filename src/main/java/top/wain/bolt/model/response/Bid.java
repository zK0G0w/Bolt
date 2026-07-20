package top.wain.bolt.model.response;

import java.util.List;

/**
 * @Description: 单条出价，DSP对某个广告位的竞价出价
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param impId 对应的广告位ID
 * @param dspId 出价的DSP标识
 * @param price 出价金额，单位：分/CPM
 * @param nurl Win Notice URL，竞价胜出时回调通知DSP
 * @param lurl Loss Notice URL，竞价失败时回调通知DSP
 * @param asset 广告素材
 * @param landingUrl 落地页URL
 * @param impressionUrls 曝光监测URL列表
 * @param clickUrls 点击监测URL列表
 */
public record Bid(
        String impId,
        String dspId,
        long price,
        String nurl,
        String lurl,
        Asset asset,
        String landingUrl,
        List<String> impressionUrls,
        List<String> clickUrls
) {}
