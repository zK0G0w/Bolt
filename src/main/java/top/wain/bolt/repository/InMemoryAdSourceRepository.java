package top.wain.bolt.repository;

import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Description: AdSourceRepository 内存实现，写死测试数据，后续阶段六切换为 Redis
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Component
public class InMemoryAdSourceRepository implements AdSourceRepository {

    private final Map<String, List<AdSource>> store = Map.of(
            "imp-001", List.of(
                    new AdSource.RtbSource(
                            "src-001", "imp-001", "plat-001", "slot-hw-001",
                            200, 150L, 15, new AdSource.PriceMarkup.Ratio(20)
                    ),
                    new AdSource.RtbSource(
                            "src-002", "imp-001", "plat-002", "slot-gdt-001",
                            250, 180L, 10, new AdSource.PriceMarkup.Fixed(220L)
                    ),
                    new AdSource.FixedPriceSource(
                            "src-003", "imp-001", "plat-003", "slot-bd-001",
                            300, 500L
                    )
            )
    );

    @Override
    public List<AdSource> findByAdPositionId(String adPositionId) {
        return store.getOrDefault(adPositionId, List.of());
    }

    @Override
    public Optional<AdSource> findById(String sourceId) {
        return store.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.sourceId().equals(sourceId))
                .findFirst();
    }
}
