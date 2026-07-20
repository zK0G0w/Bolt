package top.wain.bolt.model.enums;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @Description: 广告形式，用 sealed interface + record 替代传统 int 魔数
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AdFormat.Splash.class, name = "splash"),
        @JsonSubTypes.Type(value = AdFormat.Banner.class, name = "banner"),
        @JsonSubTypes.Type(value = AdFormat.NativeFeed.class, name = "native_feed"),
        @JsonSubTypes.Type(value = AdFormat.Interstitial.class, name = "interstitial"),
        @JsonSubTypes.Type(value = AdFormat.RewardVideo.class, name = "reward_video")
})
public sealed interface AdFormat {

    /** 开屏广告 */
    record Splash(int width, int height) implements AdFormat {}

    /** 横幅广告 */
    record Banner(int width, int height) implements AdFormat {}

    /**
     * 原生信息流广告
     * @param imageCount 图片数量（单图=1，三图=3）
     * @param titleLen 标题字数限制
     * @param textLen 广告语字数限制
     */
    record NativeFeed(int imageCount, Integer titleLen, Integer textLen) implements AdFormat {}

    /** 插屏广告 */
    record Interstitial(int width, int height) implements AdFormat {}

    /**
     * 激励视频广告
     * @param duration 视频时长，单位秒
     */
    record RewardVideo(int duration) implements AdFormat {}
}
