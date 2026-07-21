package top.wain.bolt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.wain.bolt.service.BidService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Description: 竞价入口 Controller 集成测试，验证请求解析与 ScopedValue 上下文注入
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@WebMvcTest(BidController.class)
@Import(BidService.class)
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void bid_validRequest_returns200WithResponseId() throws Exception {
        String requestJson = """
                {
                    "id": "req-001",
                    "imps": [{
                        "id": "imp-001",
                        "format": {"type": "native_feed", "imageCount": 1, "titleLen": 30, "textLen": 50},
                        "dealType": {"type": "rtb", "bidFloor": 200},
                        "bidFloor": 200,
                        "width": 640,
                        "height": 320
                    }],
                    "app": {"id": "app-001", "name": "测试APP", "packageName": "com.test", "version": "1.0"},
                    "device": {
                        "id": {"oaid": "oaid-123"},
                        "hardware": {"type": "PHONE", "make": "Xiaomi", "model": "14", "os": "ANDROID", "osVersion": "15", "screenWidth": 1080, "screenHeight": 2400, "connection": "WIFI", "carrier": "CHINA_MOBILE"},
                        "geo": {"lon": 116.4, "lat": 39.9, "ip": "10.0.0.1", "country": "CN", "region": "北京"}
                    },
                    "user": {"id": "user-001", "interests": ["游戏"]},
                    "tmax": 100
                }
                """;

        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("req-001"))
                .andExpect(jsonPath("$.bids").isArray())
                .andExpect(jsonPath("$.bids").isEmpty());
    }

    @Test
    void bid_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bid_xForwardedFor_extractsClientIp() throws Exception {
        String requestJson = """
                {
                    "id": "req-xff",
                    "imps": [],
                    "app": {"id": "app-001", "name": "A", "packageName": "com.a", "version": "1"},
                    "device": {"id": {}, "hardware": {"type": "PHONE", "make": "X", "model": "Y", "os": "ANDROID", "osVersion": "1", "screenWidth": 1, "screenHeight": 1, "connection": "WIFI", "carrier": "CHINA_MOBILE"}, "geo": {"lon": 0, "lat": 0, "ip": "1.1.1.1", "country": "CN", "region": ""}},
                    "user": {"id": "u1", "interests": []},
                    "tmax": 50
                }
                """;

        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.50, 70.41.3.18")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("req-xff"));
    }
}
