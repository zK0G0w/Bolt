package top.wain.bolt.repository;

import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.Map;
import java.util.Optional;

/**
 * @Description: DspPlatformRepository 内存实现，写死测试数据，后续阶段六切换为 Redis
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Component
public class InMemoryDspPlatformRepository implements DspPlatformRepository {

    private final Map<String, DspPlatform> store = Map.of(
            "plat-001", new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50),
            "plat-002", new DspPlatform("plat-002", "广点通", "gdt", "https://dsp.gdt.qq.com/bid", 2000, 100),
            "plat-003", new DspPlatform("plat-003", "百度联盟", "baidu", "https://dsp.baidu.com/bid", 1500, 80)
    );

    @Override
    public Optional<DspPlatform> findById(String platformId) {
        return Optional.ofNullable(store.get(platformId));
    }
}
