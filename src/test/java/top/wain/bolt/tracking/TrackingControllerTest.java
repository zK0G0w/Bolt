package top.wain.bolt.tracking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @Description: 追踪埋点接口集成测试（AES 加密 + 时间戳防重放）
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@WebMvcTest(TrackingController.class)
@ComponentScan(basePackages = "top.wain.bolt")
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrackingUrlGenerator generator;

    @Test
    void impression_validPayload_returns1x1Gif() throws Exception {
        String url = generator.impressionUrl("bid-t1", "src-t1", 100);
        String p = url.substring(url.indexOf("p=") + 2);

        mockMvc.perform(get("/i").param("p", p))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_GIF))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void impression_tamperedPayload_returns403() throws Exception {
        mockMvc.perform(get("/i").param("p", "invalid-cipher-text"))
                .andExpect(status().isForbidden());
    }

    @Test
    void impression_expiredPayload_returns410() throws Exception {
        TrackingCipher cipher = generator.cipher();
        String payload = "bid-exp|src-exp|100|0";
        String p = cipher.encrypt(payload);

        mockMvc.perform(get("/i").param("p", p))
                .andExpect(status().isGone());
    }

    @Test
    void click_validPayload_withLandingUrl_returns302() throws Exception {
        String url = generator.clickUrl("bid-t2", "src-t2", 200, "https://landing.example.com");
        String p = url.substring(url.indexOf("p=") + 2);

        mockMvc.perform(get("/c").param("p", p))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://landing.example.com"));
    }

    @Test
    void click_validPayload_emptyLandingUrl_returns200() throws Exception {
        String url = generator.clickUrl("bid-t3", "src-t3", 300, "");
        String p = url.substring(url.indexOf("p=") + 2);

        mockMvc.perform(get("/c").param("p", p))
                .andExpect(status().isOk());
    }

    @Test
    void click_tamperedPayload_returns403() throws Exception {
        mockMvc.perform(get("/c").param("p", "bad-cipher"))
                .andExpect(status().isForbidden());
    }

    @Test
    void click_expiredPayload_returns410() throws Exception {
        TrackingCipher cipher = generator.cipher();
        String payload = "bid-exp|src-exp|100|0|https://example.com";
        String p = cipher.encrypt(payload);

        mockMvc.perform(get("/c").param("p", p))
                .andExpect(status().isGone());
    }
}