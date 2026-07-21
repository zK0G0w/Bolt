package top.wain.bolt.tracking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Description: TrackingUrlGenerator URL 格式与加密验证测试
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
class TrackingUrlGeneratorTest {

    private final TrackingUrlGenerator generator =
            new TrackingUrlGenerator("http://localhost:9292", "test-secret");

    @Test
    void impressionUrl_startsWithCorrectPath() {
        String url = generator.impressionUrl("bid-001", "src-001", 350);
        assertTrue(url.startsWith("http://localhost:9292/i?p="));
    }

    @Test
    void clickUrl_startsWithCorrectPath() {
        String url = generator.clickUrl("bid-002", "src-002", 500, "https://landing.com");
        assertTrue(url.startsWith("http://localhost:9292/c?p="));
    }

    @Test
    void impressionUrl_decryptable() {
        String url = generator.impressionUrl("bid-003", "src-003", 200);
        String p = url.substring(url.indexOf("p=") + 2);
        String decrypted = generator.cipher().decrypt(p);

        assertNotNull(decrypted);
        assertTrue(decrypted.startsWith("bid-003|src-003|200|"));
    }

    @Test
    void clickUrl_containsLandingUrl() {
        String url = generator.clickUrl("bid-004", "src-004", 100, "https://example.com/lp");
        String p = url.substring(url.indexOf("p=") + 2);
        String decrypted = generator.cipher().decrypt(p);

        assertNotNull(decrypted);
        assertTrue(decrypted.endsWith("|https://example.com/lp"));
    }
}
