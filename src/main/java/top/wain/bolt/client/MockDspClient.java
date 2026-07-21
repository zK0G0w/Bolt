package top.wain.bolt.client;

import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.BidRequest;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description: DSP 客户端 Mock 实现，内存模拟出价（随机价格、随机延迟），用于开发阶段跑通链路
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Component
public class MockDspClient implements DspClient {

    @Override
    public DspBidResult sendBid(AdSource source, BidRequest request, long dspBidFloor) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 模拟网络延迟 10-350ms
        int delayMs = random.nextInt(10, 350);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DspBidResult.Timeout(source.sourceId());
        }

        // 20% 概率不出价
        if (random.nextInt(100) < 20) {
            return new DspBidResult.NoBid(source.sourceId());
        }

        // 出价：底价的 100%~180% 浮动
        long price = dspBidFloor + random.nextLong(0, dspBidFloor);
        String adPayload = "{\"creative\":\"mock-ad-" + source.sourceId() + "\"}";
        String rawResponse = "{\"bid\":" + price + ",\"source\":\"" + source.sourceId() + "\"}";

        return new DspBidResult.Success(source.sourceId(), price, adPayload, rawResponse);
    }
}
