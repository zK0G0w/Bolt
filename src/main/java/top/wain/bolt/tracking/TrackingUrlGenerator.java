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
     * 生成展示追踪 URL（参数全部加密，无落地页）
     */
    public String impressionUrl(String bidId, String adSourceId, long price) {
        return baseUrl + "/i?p=" + encrypt(bidId, adSourceId, price, "");
    }

    /**
     * 生成点击追踪 URL（含落地页 URL，加密进参数体内）
     */
    public String clickUrl(String bidId, String adSourceId, long price, String landingUrl) {
        return baseUrl + "/c?p=" + encrypt(bidId, adSourceId, price, landingUrl);
    }

    private String encrypt(String bidId, String adSourceId, long price, String landingUrl) {
        var payload = new TrackingPayload(
                bidId, adSourceId, price, System.currentTimeMillis(), landingUrl);
        return cipher.encrypt(payload.encode());
    }

    public TrackingCipher cipher() {
        return cipher;
    }
}
