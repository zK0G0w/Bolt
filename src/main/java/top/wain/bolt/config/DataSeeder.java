package top.wain.bolt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.List;

/**
 * @Description: 开发环境数据播种器，Redis 无数据时写入样本配置
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DataSeeder(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (Boolean.TRUE.equals(redisTemplate.hasKey("bolt:dsp:plat-001"))) {
            log.info("Redis 已有数据，跳过播种");
            return;
        }
        seedDspPlatforms();
        seedAdSources();
        log.info("开发环境样本数据播种完成");
    }

    private void seedDspPlatforms() throws Exception {
        var platforms = List.of(
                new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50),
                new DspPlatform("plat-002", "广点通", "gdt", "https://dsp.gdt.qq.com/bid", 2000, 100),
                new DspPlatform("plat-003", "百度联盟", "baidu", "https://dsp.baidu.com/bid", 1500, 80)
        );
        for (DspPlatform p : platforms) {
            redisTemplate.opsForValue().set("bolt:dsp:" + p.platformId(), objectMapper.writeValueAsString(p));
        }
    }

    private void seedAdSources() throws Exception {
        var sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-hw-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20)),
                new AdSource.RtbSource("src-002", "imp-001", "plat-002", "slot-gdt-001", 250, 180L, 10, new AdSource.PriceMarkup.Fixed(220L)),
                new AdSource.FixedPriceSource("src-003", "imp-001", "plat-003", "slot-bd-001", 300, 500L)
        );
        for (AdSource s : sources) {
            redisTemplate.opsForValue().set("bolt:adsource:" + s.sourceId(), objectMapper.writeValueAsString(s));
        }
        // 广告位 → 广告源ID 索引
        List<String> sourceIds = sources.stream().map(AdSource::sourceId).toList();
        redisTemplate.opsForValue().set("bolt:adsource:index:imp-001", objectMapper.writeValueAsString(sourceIds));
    }
}

