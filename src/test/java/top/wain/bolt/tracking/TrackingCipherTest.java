package top.wain.bolt.tracking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Description: TrackingCipher AES 加密/解密单元测试
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
class TrackingCipherTest {

    private final TrackingCipher cipher = new TrackingCipher("test-secret");

    @Test
    void encrypt_decrypt_roundTrip() {
        String payload = "bid-001|src-001|350|1000000";
        String encrypted = cipher.encrypt(payload);
        String decrypted = cipher.decrypt(encrypted);
        assertEquals(payload, decrypted);
    }

    @Test
    void decrypt_tamperedCipherText_returnsNull() {
        String encrypted = cipher.encrypt("bid-001|src-001|350|1000");
        String tampered = encrypted + "x";
        assertNull(cipher.decrypt(tampered));
    }

    @Test
    void decrypt_differentSecret_returnsNull() {
        String encrypted = cipher.encrypt("bid-001|src-001|350|1000");
        TrackingCipher other = new TrackingCipher("other-secret");
        assertNull(other.decrypt(encrypted));
    }

    @Test
    void encrypt_producesUrlSafeBase64() {
        String encrypted = cipher.encrypt("bid|src|100|999");
        // URL-safe Base64 不含 +, /, =
        assertFalse(encrypted.contains("+"));
        assertFalse(encrypted.contains("/"));
        assertFalse(encrypted.contains("="));
    }
}
