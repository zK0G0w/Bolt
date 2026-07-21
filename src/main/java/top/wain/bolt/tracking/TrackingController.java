package top.wain.bolt.tracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: 追踪埋点接口，AES 解密参数 + 时间戳防重放 + 落地页服务端解出
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@RestController
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);

    private static final byte[] PIXEL_GIF = {
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
            0x01, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, 0x00, 0x00, 0x00, 0x21, (byte) 0xf9, 0x04,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00,
            0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02,
            0x02, 0x44, 0x01, 0x00, 0x3b
    };

    private final TrackingCipher cipher;
    private final long expireMs;

    public TrackingController(
            TrackingUrlGenerator generator,
            @Value("${bolt.tracking.expire-ms:300000}") long expireMs) {
        this.cipher = generator.cipher();
        this.expireMs = expireMs;
    }

    /**
     * 展示追踪：解密参数 → 校验时效 → 记录日志 → 返回 1x1 透明像素
     * 密文格式: bidId|adSourceId|price|timestamp
     */
    @GetMapping("/i")
    public ResponseEntity<byte[]> impression(@RequestParam String p) {
        String decrypted = cipher.decrypt(p);
        if (decrypted == null) {
            log.warn("展示追踪解密失败");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String[] parts = decrypted.split("\\|", 4);
        if (parts.length < 4) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String bid = parts[0];
        String src = parts[1];
        long price = Long.parseLong(parts[2]);
        long ts = Long.parseLong(parts[3]);

        if (isExpired(ts)) {
            log.warn("展示追踪已过期 bid={} ts={}", bid, ts);
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        log.info("展示上报 bid={} src={} price={} ts={}", bid, src, price, ts);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(PIXEL_GIF);
    }

    /**
     * 点击追踪：解密参数 → 校验时效 → 记录日志 → 302 重定向至落地页（从密文解出）
     * 密文格式: bidId|adSourceId|price|timestamp|landingUrl
     */
    @GetMapping("/c")
    public ResponseEntity<Void> click(@RequestParam String p) {
        String decrypted = cipher.decrypt(p);
        if (decrypted == null) {
            log.warn("点击追踪解密失败");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String[] parts = decrypted.split("\\|", 5);
        if (parts.length < 5) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String bid = parts[0];
        String src = parts[1];
        long price = Long.parseLong(parts[2]);
        long ts = Long.parseLong(parts[3]);
        String landingUrl = parts[4];

        if (isExpired(ts)) {
            log.warn("点击追踪已过期 bid={} ts={}", bid, ts);
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        log.info("点击上报 bid={} src={} price={} ts={}", bid, src, price, ts);

        if (landingUrl.isBlank()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, landingUrl)
                .build();
    }

    private boolean isExpired(long ts) {
        return System.currentTimeMillis() - ts > expireMs;
    }
}