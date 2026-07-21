package top.wain.bolt.model.domain;

/**
 * @Description: DSP 出价结果，Sealed Interface 穷举竞价请求的所有可能结局，配合 Pattern Matching 做分发
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
public sealed interface DspBidResult {

    /** 关联的广告源ID，用于回溯配置 */
    String adSourceId();

    /**
     * 出价成功
     * @param adSourceId 广告源ID
     * @param price 出价金额，单位：分/CPM
     * @param adPayload 素材信息（JSON），阶段五响应组装时使用
     * @param rawResponse DSP 原始响应体，调试用
     */
    record Success(String adSourceId, long price, String adPayload, String rawResponse) implements DspBidResult {}

    /** DSP 明确不出价（HTTP 204 或空响应） */
    record NoBid(String adSourceId) implements DspBidResult {}

    /** 请求超时，被 invokeAll 或 OkHttp 超时机制取消 */
    record Timeout(String adSourceId) implements DspBidResult {}

    /** 网络异常或 DSP 返回错误 */
    record Error(String adSourceId, String reason) implements DspBidResult {}
}
