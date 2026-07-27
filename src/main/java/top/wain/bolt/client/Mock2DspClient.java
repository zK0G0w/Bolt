package top.wain.bolt.client;

import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.BidRequest;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description: 第二个 DSP Mock 实现，出价保守、响应更快、不出价概率更高，
 *               与 MockDspClient 形成可观测差异以验证 platformCode 路由生效
 * @Author: WainZeng
 * @Date: 2026/07/27
 */
@Component
public class Mock2DspClient implements DspClient {

    public static final String PLATFORM_CODE = "mock2";

    @Override
    public String platformCode() {
        return PLATFORM_CODE;
    }

    @Override
    public DspBidResult sendBid(AdSource source, BidRequest request, long dspBidFloor) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 模拟网络延迟 5-60ms，比 mock 更快
        try {
            Thread.sleep(random.nextInt(5, 60));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DspBidResult.Timeout(source.sourceId());
        }

        // 40% 概率不出价，比 mock 更保守
        if (random.nextInt(100) < 40) {
            return new DspBidResult.NoBid(source.sourceId());
        }

        // 出价：底价的 100%~130% 浮动，加价幅度低于 mock
        long price = dspBidFloor + random.nextLong(0, Math.max(dspBidFloor * 30 / 100, 1));
        String adPayload = "{\"creative\":\"mock2-ad-" + source.sourceId() + "\"}";
        String rawResponse = "{\"bid\":" + price + ",\"source\":\"" + source.sourceId() + "\",\"dsp\":\"mock2\"}";

        return new DspBidResult.Success(source.sourceId(), price, adPayload, rawResponse);
    }
}
