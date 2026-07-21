# ADR-001: Sealed Interface + Record 建模 DSP 竞价结果

**状态**: 已采纳  
**日期**: 2025-06  

## 背景

DSP 竞价有四种终态：成功出价、不出价、超时、错误。每种终态携带不同信息（Success 有 price + adPayload，Error 有 reason）。需要一种模型既能穷举所有状态，又能让每种状态携带差异化数据。

## 决策

使用 Sealed Interface `DspBidResult`，四个 Record 实现：

```java
sealed interface DspBidResult permits Success, NoBid, Timeout, Error {
    String adSourceId();  // 公共字段，所有子类型都能关联回广告源
}
record Success(String adSourceId, long price, String adPayload, String rawResponse) implements DspBidResult {}
record NoBid(String adSourceId) implements DspBidResult {}
record Timeout(String adSourceId) implements DspBidResult {}
record Error(String adSourceId, String reason) implements DspBidResult {}
```

## 否决方案

| 方案 | 否决理由 |
|------|---------|
| Enum + payload 字段 | 所有状态被迫携带相同字段，Success 的 price 在 NoBid 时无意义 |
| 抽象类 + 子类 | 无编译期穷举保证，switch 可能遗漏新增状态 |
| Result<T, E> 泛型 | 只能表达二态（成功/失败），无法区分 NoBid/Timeout/Error |

## 约束

- `price` 使用 `long`（分为单位）而非 BigDecimal，接受学习项目中的精度取舍
- Sealed 要求所有实现类与接口在同一编译单元（同文件或同包）
