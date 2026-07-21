package top.wain.bolt.service;

import org.springframework.stereotype.Service;
import top.wain.bolt.client.DspClient;
import top.wain.bolt.client.DspClientRouter;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.domain.DspPlatform;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.request.Imp;
import top.wain.bolt.repository.AdSourceRepository;
import top.wain.bolt.repository.DspPlatformRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @Description: DSP 并发扇出服务，根据广告位配置并发请求多个 DSP，收集出价结果。
 *               使用 Virtual Threads + invokeAll(timeout) 实现并发控制和超时兜底。
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
@Service
public class DspFanOutService {

    /** 上游未传 tmax 时的默认总超时（ms） */
    private static final int DEFAULT_TIMEOUT_MS = 150;
    /** 预留给响应序列化和网络传输的安全余量（ms） */
    private static final int SAFETY_MARGIN_MS = 20;

    private final AdSourceRepository adSourceRepository;
    private final DspPlatformRepository dspPlatformRepository;
    private final DspClientRouter dspClientRouter;

    public DspFanOutService(AdSourceRepository adSourceRepository,
                            DspPlatformRepository dspPlatformRepository,
                            DspClientRouter dspClientRouter) {
        this.adSourceRepository = adSourceRepository;
        this.dspPlatformRepository = dspPlatformRepository;
        this.dspClientRouter = dspClientRouter;
    }

    /**
     * 并发扇出请求 DSP，返回所有广告源的出价结果
     * @param request 上游竞价请求，取第一个 Imp 的 id 作为广告位标识
     * @return DSP 出价结果列表，可能包含 Success/NoBid/Timeout/Error 混合
     */
    public List<DspBidResult> fanOut(BidRequest request) {
        if (request.imps() == null || request.imps().isEmpty()) {
            return List.of();
        }

        Imp imp = request.imps().getFirst();
        List<AdSource> sources = adSourceRepository.findByAdPositionId(imp.id());
        if (sources.isEmpty()) {
            return List.of();
        }

        long deadlineMs = computeDeadline(request.tmax());

        List<Callable<DspBidResult>> tasks = sources.stream()
                .map(source -> (Callable<DspBidResult>) () -> callDsp(source, request))
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<DspBidResult>> futures = executor.invokeAll(tasks, deadlineMs, TimeUnit.MILLISECONDS);
            return collectResults(futures, sources);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return sources.stream()
                    .map(s -> (DspBidResult) new DspBidResult.Timeout(s.sourceId()))
                    .toList();
        }
    }

    /** 调用单个 DSP：查平台配置、计算加价底价、发送请求 */
    private DspBidResult callDsp(AdSource source, BidRequest request) {
        // TODO: 阶段六 — QPS 限流和频次控制检查

        DspPlatform platform = dspPlatformRepository.findById(source.platformId()).orElse(null);
        if (platform == null) {
            return new DspBidResult.Error(source.sourceId(), "platform not found: " + source.platformId());
        }

        long dspBidFloor = computeDspBidFloor(source);
        DspClient client = dspClientRouter.route(platform.platformCode());

        try {
            return client.sendBid(source, request, dspBidFloor);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DspBidResult.Timeout(source.sourceId());
        } catch (Exception e) {
            return new DspBidResult.Error(source.sourceId(), e.getMessage());
        }
    }

    /** 根据 AdSource 类型和加价策略计算发给 DSP 的底价 */
    private long computeDspBidFloor(AdSource source) {
        return switch (source) {
            case AdSource.RtbSource rtb -> switch (rtb.markup()) {
                case AdSource.PriceMarkup.Ratio(var percent) -> rtb.bidFloor() * (100 + percent) / 100;
                case AdSource.PriceMarkup.Fixed(var price) -> price;
            };
            case AdSource.FixedPriceSource fixed -> fixed.fixedBidPrice();
        };
    }

    /** 计算 invokeAll 的总超时：tmax - 安全余量，最低 10ms */
    private long computeDeadline(int tmax) {
        int effective = tmax > 0 ? tmax : DEFAULT_TIMEOUT_MS;
        return Math.max(effective - SAFETY_MARGIN_MS, 10);
    }

    /** 收集 invokeAll 结果，将 cancelled/exception 映射为对应的 DspBidResult 子类型 */
    private List<DspBidResult> collectResults(List<Future<DspBidResult>> futures, List<AdSource> sources) {
        List<DspBidResult> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            Future<DspBidResult> future = futures.get(i);
            String sourceId = sources.get(i).sourceId();
            try {
                if (future.isCancelled()) {
                    results.add(new DspBidResult.Timeout(sourceId));
                } else {
                    results.add(future.get());
                }
            } catch (CancellationException e) {
                results.add(new DspBidResult.Timeout(sourceId));
            } catch (ExecutionException e) {
                results.add(new DspBidResult.Error(sourceId, e.getCause().getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(new DspBidResult.Timeout(sourceId));
            }
        }
        return results;
    }
}
