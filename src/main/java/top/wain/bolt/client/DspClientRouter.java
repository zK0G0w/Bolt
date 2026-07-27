package top.wain.bolt.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: DSP 客户端路由器，根据 DspPlatform.platformCode 分发到对应的 DspClient 实现。
 *               不做兜底降级：找不到适配器返回空，由调用方按配置错误处理。
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Component
public class DspClientRouter {

    private final Map<String, DspClient> clientsByCode;

    public DspClientRouter(List<DspClient> clients) {
        this.clientsByCode = clients.stream()
                .collect(Collectors.toUnmodifiableMap(DspClient::platformCode, Function.identity()));
    }

    /**
     * 按平台路由码查找适配器
     * @param platformCode DspPlatform 配置的路由码
     * @return 对应适配器，无匹配时为空
     */
    public Optional<DspClient> route(String platformCode) {
        return Optional.ofNullable(clientsByCode.get(platformCode));
    }

    /** 当前已部署的适配器路由码集合，即配置侧 platformCode 的合法取值范围 */
    public Set<String> supportedCodes() {
        return clientsByCode.keySet();
    }
}
