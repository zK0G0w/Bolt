package top.wain.bolt.tracking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description: 追踪 URL 生成器，将参数 AES 加密为单个 p 参数，防篡改 + 隐藏结算价
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class TrackingUrlGenerator {

    private final String baseUrl;
    private final TrackingCipher cipher;

    public TrackingUrlGenerator(
            @Value("${bolt.tracking.base-url:http://localhost:9292}") String baseUrl,
            @Value("${bolt.tracking.secret:bolt-dev-secret}") String secret) {
        this.baseUrl = baseUrl;
        this.cipher = new TrackingCipher(secret);
    }

    /**
     * 生成展示追踪 URL（参数全部加密）
     */
    public String impressionUrl(String bidId, String adSourceId, long price) {
        long ts = System.currentTimeMillis();
        String payload = bidId + "|" + adSourceId + "|" + price + "|" + ts;
        return baseUrl + "/i?p=" + cipher.encrypt(payload);
    }

    /**
     * 生成点击追踪 URL（含落地页 URL，加密进参数体内）
     */
    public String clickUrl(String bidId, String adSourceId, long price, String landingUrl) {
        long ts = System.currentTimeMillis();
        String payload = bidId + "|" + adSourceId + "|" + price + "|" + ts + "|" + landingUrl;
        return baseUrl + "/c?p=" + cipher.encrypt(payload);
    }

    public TrackingCipher cipher() {
        return cipher;
    }
}
