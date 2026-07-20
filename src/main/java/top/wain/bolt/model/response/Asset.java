package top.wain.bolt.model.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * @Description: 广告素材，用 sealed interface 区分图片和视频两种形态
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Asset.Image.class, name = "image"),
        @JsonSubTypes.Type(value = Asset.Video.class, name = "video")
})
public sealed interface Asset {

    /**
     * 图片素材
     * @param urls 图片URL列表（三图广告会有多个）
     * @param mime MIME类型，如 image/jpeg
     * @param width 图片宽度
     * @param height 图片高度
     * @param title 广告标题
     * @param desc 广告描述文案
     */
    record Image(
            List<String> urls,
            String mime,
            int width,
            int height,
            String title,
            String desc
    ) implements Asset {}

    /**
     * 视频素材
     * @param url 视频URL
     * @param mime MIME类型，如 video/mp4
     * @param width 视频宽度
     * @param height 视频高度
     * @param title 广告标题
     * @param duration 视频时长，单位毫秒
     * @param coverUrl 封面图URL
     */
    record Video(
            String url,
            String mime,
            int width,
            int height,
            String title,
            long duration,
            String coverUrl
    ) implements Asset {}
}
