package top.wain.bolt.repository;

import org.junit.jupiter.api.Test;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoryTest {

    private final InMemoryAdSourceRepository adSourceRepo = new InMemoryAdSourceRepository();
    private final InMemoryDspPlatformRepository platformRepo = new InMemoryDspPlatformRepository();

    @Test
    void adSourceRepo_knownPosition_returnsMultipleSources() {
        List<AdSource> sources = adSourceRepo.findByAdPositionId("imp-001");
        assertEquals(3, sources.size());
        assertTrue(sources.stream().anyMatch(s -> s instanceof AdSource.RtbSource));
        assertTrue(sources.stream().anyMatch(s -> s instanceof AdSource.FixedPriceSource));
    }

    @Test
    void adSourceRepo_unknownPosition_returnsEmpty() {
        List<AdSource> sources = adSourceRepo.findByAdPositionId("unknown-pos");
        assertTrue(sources.isEmpty());
    }

    @Test
    void platformRepo_knownId_returnsPlatform() {
        var platform = platformRepo.findById("plat-001");
        assertTrue(platform.isPresent());
        assertEquals("华为ADX", platform.get().name());
        assertEquals("huawei", platform.get().platformCode());
    }

    @Test
    void platformRepo_unknownId_returnsEmpty() {
        var platform = platformRepo.findById("unknown-plat");
        assertTrue(platform.isEmpty());
    }

    @Test
    void adSource_platformId_matchesPlatformRepo() {
        List<AdSource> sources = adSourceRepo.findByAdPositionId("imp-001");
        for (AdSource source : sources) {
            var platform = platformRepo.findById(source.platformId());
            assertTrue(platform.isPresent(), "AdSource.platformId should match a DspPlatform: " + source.platformId());
        }
    }
}
