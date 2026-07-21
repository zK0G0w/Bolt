package top.wain.bolt.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: DSP 客户端路由器，根据 DspPlatform.platformCode 分发到对应的 DspClient 实现
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Component
public class DspClientRouter {

    private final Map<String, DspClient> clientsByCode;
    private final DspClient defaultClient;

    public DspClientRouter(List<DspClient> clients) {
        // MockDspClient 作为默认兜底
        this.defaultClient = clients.stream()
                .filter(c -> c instanceof MockDspClient)
                .findFirst()
                .orElse(clients.getFirst());
        this.clientsByCode = Map.of();
    }

    public DspClient route(String platformCode) {
        return clientsByCode.getOrDefault(platformCode, defaultClient);
    }
}
