package top.wain.bolt.tracking;

import java.util.Optional;

/**
 * @Description: 追踪链接负载，封装明文格式的编解码与时效判定，
 *               格式知识仅存在于本类内部：event|bidId|adSourceId|price|timestamp|landingUrl
 * @Author: WainZeng
 * @Date: 2026/07/27
 * @param event 事件类型，使展示与点击的负载不可互换
 * @param bidId 竞价响应唯一标识
 * @param adSourceId 胜出广告源ID
 * @param price 媒体结算价，分/CPM
 * @param timestamp 链接生成时间戳，单位毫秒，用于防重放
 * @param landingUrl 落地页地址，展示链接为空串
 */
public record TrackingPayload(
        Event event,
        String bidId,
        String adSourceId,
        long price,
        long timestamp,
        String landingUrl
) {

    private static final String SEPARATOR = "|";
    private static final int FIELD_COUNT = 6;

    /** 追踪事件类型，作为负载首字段参与加密，防止展示与点击 token 互换使用 */
    public enum Event {
        IMPRESSION("i"),
        CLICK("c");

        private final String code;

        Event(String code) {
            this.code = code;
        }

        static Event fromCode(String code) {
            for (Event e : values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }
            return null;
        }
    }

    public String encode() {
        return String.join(SEPARATOR, event.code,
                bidId, adSourceId, String.valueOf(price), String.valueOf(timestamp), landingUrl);
    }

    /**
     * 解析明文负载，字段缺失或数值非法时返回空，不抛异常
     * @param plainText 解密后的明文
     */
    public static Optional<TrackingPayload> decode(String plainText) {
        if (plainText == null) {
            return Optional.empty();
        }
        // limit=FIELD_COUNT 保留末尾空串，展示链接的空 landingUrl 才不会被丢弃
        String[] parts = plainText.split("\\" + SEPARATOR, FIELD_COUNT);
        if (parts.length < FIELD_COUNT) {
            return Optional.empty();
        }
        Event event = Event.fromCode(parts[0]);
        if (event == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TrackingPayload(event,
                    parts[1], parts[2], Long.parseLong(parts[3]), Long.parseLong(parts[4]), parts[5]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 是否已超出有效期 */
    public boolean isExpired(long expireMs) {
        return System.currentTimeMillis() - timestamp > expireMs;
    }

    public boolean hasLandingUrl() {
        return landingUrl != null && !landingUrl.isBlank();
    }
}
