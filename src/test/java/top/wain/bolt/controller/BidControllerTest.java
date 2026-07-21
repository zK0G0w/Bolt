package top.wain.bolt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.wain.bolt.MockRedisTestConfig;
import top.wain.bolt.config.RedisConfig;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;
import top.wain.bolt.repository.AdSourceRepository;
import top.wain.bolt.repository.DspPlatformRepository;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidController.class)
@ComponentScan(basePackages = "top.wain.bolt",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisConfig.class))
@Import({MockRedisTestConfig.class, BidControllerTest.TestRepositoryConfig.class})
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestRepositoryConfig {
        @Bean
        public AdSourceRepository adSourceRepository() {
            List<AdSource> sources = List.of(
                    new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-hw-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20)),
                    new AdSource.RtbSource("src-002", "imp-001", "plat-002", "slot-gdt-001", 250, 180L, 10, new AdSource.PriceMarkup.Fixed(220L)),
                    new AdSource.FixedPriceSource("src-003", "imp-001", "plat-003", "slot-bd-001", 300, 500L)
            );
            return new AdSourceRepository() {
                @Override
                public List<AdSource> findByAdPositionId(String adPositionId) {
                    return "imp-001".equals(adPositionId) ? sources : List.of();
                }
                @Override
                public Optional<AdSource> findById(String sourceId) {
                    return sources.stream().filter(s -> s.sourceId().equals(sourceId)).findFirst();
                }
            };
        }

        @Bean
        public DspPlatformRepository dspPlatformRepository() {
            var platforms = java.util.Map.of(
                    "plat-001", new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50),
                    "plat-002", new DspPlatform("plat-002", "广点通", "gdt", "https://dsp.gdt.qq.com/bid", 2000, 100),
                    "plat-003", new DspPlatform("plat-003", "百度联盟", "baidu", "https://dsp.baidu.com/bid", 1500, 80)
            );
            return id -> Optional.ofNullable(platforms.get(id));
        }
    }

    @Test
    void bid_validRequest_withMatchingAdSources_returns200() throws Exception {
        String requestJson = """
                {
                    "id": "req-001",
                    "imp": {
                        "id": "imp-001",
                        "format": {"type": "native_feed", "imageCount": 1, "titleLen": 30, "textLen": 50},
                        "dealType": {"type": "rtb", "bidFloor": 200},
                        "bidFloor": 200,
                        "width": 640,
                        "height": 320
                    },
                    "app": {"id": "app-001", "name": "测试APP", "packageName": "com.test", "version": "1.0"},
                    "device": {
                        "id": {"oaid": "oaid-123"},
                        "hardware": {"type": "PHONE", "make": "Xiaomi", "model": "14", "os": "ANDROID", "osVersion": "15", "screenWidth": 1080, "screenHeight": 2400, "connection": "WIFI", "carrier": "CHINA_MOBILE"},
                        "geo": {"lon": 116.4, "lat": 39.9, "ip": "10.0.0.1", "country": "CN", "region": "北京"}
                    },
                    "user": {"id": "user-001", "interests": ["游戏"]},
                    "tmax": 500
                }
                """;

        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("req-001"))
                .andExpect(jsonPath("$.bidid").isString())
                .andExpect(jsonPath("$.cur").value("CNY"))
                .andExpect(jsonPath("$.bids").isArray())
                .andExpect(jsonPath("$.bids[0].impId").value("imp-001"))
                .andExpect(jsonPath("$.bids[0].price").isNumber());
    }

    @Test
    void bid_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bid_unknownImp_returnsNoBid204() throws Exception {
        String requestJson = """
                {
                    "id": "req-nobid",
                    "imp": {
                        "id": "unknown-imp",
                        "format": {"type": "native_feed", "imageCount": 1, "titleLen": 30, "textLen": 50},
                        "dealType": {"type": "rtb", "bidFloor": 200},
                        "bidFloor": 200,
                        "width": 640,
                        "height": 320
                    },
                    "app": {"id": "app-001", "name": "A", "packageName": "com.a", "version": "1"},
                    "device": {"id": {"oaid": "oaid-1"}, "hardware": {"type": "PHONE", "make": "X", "model": "Y", "os": "ANDROID", "osVersion": "1", "screenWidth": 1, "screenHeight": 1, "connection": "WIFI", "carrier": "CHINA_MOBILE"}, "geo": {"lon": 0, "lat": 0, "ip": "1.1.1.1", "country": "CN", "region": ""}},
                    "user": {"id": "u1", "interests": []},
                    "tmax": 100
                }
                """;

        mockMvc.perform(post("/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());
    }

    @Test
    void bid_xForwardedFor_extractsClientIp() throws Exception {
        String requestJson = """
                {
                    "id": "req-xff",
                    "imp": {
                        "id": "imp-001",
                        "format": {"type": "native_feed", "imageCount": 1, "titleLen": 30, "textLen": 50},
                        "dealType": {"type": "rtb", "bidFloor": 200},
                        "bidFloor": 200,
                        "width": 640,
                        "height": 320
                    },
                    "app": {"id": "app-001", "name": "A", "packageName": "com.a", "version": "1"},
                    "device": {"id": {"oaid": "oaid-1"}, "hardware": {"type": "PHONE", "make": "X", "model": "Y", "os": "ANDROID", "osVersion": "1", "screenWidth": 1, "screenHeight": 1, "connection": "WIFI", "carrier": "CHINA_MOBILE"}, "geo": {"lon": 0, "lat": 0, "ip": "1.1.1.1", "country": "CN", "region": ""}},
                    "user": {"id": "u1", "interests": []},
                    "tmax": 500
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
