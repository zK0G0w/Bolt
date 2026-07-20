# 内部域模型层设计

## 概述

为 Bolt RTB 竞价引擎设计内部域模型层，作为竞价编排的输入数据结构。利用 JDK 25 的 Sealed Interface + Record 特性建模，在有真正类型分支的地方使用 Sealed Interface，纯数据载体保持简单 Record。

## 设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 范围 | AdSource + SellPlatform（最小核心） | 足以驱动一轮完整竞价编排 |
| AdSource 建模 | Sealed Interface 按竞价模式分型 | RTB/固定出价有结构化数据差异，Pattern Matching 驱动逻辑分发 |
| 加价策略归属 | 嵌入 AdSource 内部 | 和广告源强绑定，无独立复用场景 |
| 结算模式 | 仅一价 | 去掉二价复杂度，竞价决策写死一价逻辑 |
| SellPlatform | 普通 Record | 无多态行为，纯配置载体 |

## 包结构

```
top.wain.bolt.model.domain/
├── AdSource.java           (sealed interface + RtbSource, FixedPriceSource, PriceMarkup)
└── SellPlatform.java       (record)
```

放在 `model.domain` 包下，与现有 `model.request`/`model.response`/`model.enums` 平级，语义上区分「协议模型」和「内部业务域模型」。

## 详细设计

### 1. AdSource（广告源配置）

广告源是竞价编排的最小调度单元，每个广告源绑定一个 DSP 平台和一个广告位。用 sealed interface 按竞价模式分型：

```java
public sealed interface AdSource {

    // 公共访问器 —— 所有广告源共享的属性
    String sourceId();          // 广告源唯一标识
    String adPositionId();      // 所属广告位ID
    String platformId();        // 绑定的DSP平台ID
    String platformSlotId();    // 平台侧广告位ID
    int timeoutMs();            // 请求超时，单位毫秒

    /**
     * RTB 竞价模式广告源
     * 携带底价、利润率、加价策略，竞价阶段参与实时竞拍
     */
    record RtbSource(
        String sourceId,
        String adPositionId,
        String platformId,
        String platformSlotId,
        int timeoutMs,
        long bidFloor,          // 底价，分/CPM
        int profitRatio,        // 利润率 0-100，用于从结算价中扣除引擎利润
        PriceMarkup markup      // 加价策略，发给DSP前对底价的调整方式
    ) implements AdSource {}

    /**
     * 固定出价模式广告源
     * 不参与实时竞价，以固定价格直接结算
     */
    record FixedPriceSource(
        String sourceId,
        String adPositionId,
        String platformId,
        String platformSlotId,
        int timeoutMs,
        long fixedBidPrice      // 固定出价，分/CPM
    ) implements AdSource {}

    /**
     * 加价策略，决定发给DSP的底价如何在原始底价基础上调整
     */
    sealed interface PriceMarkup {

        /** 比例加价：底价 * (100 + percent) / 100 */
        record Ratio(int percent) implements PriceMarkup {}

        /** 固定提交价：直接用此值作为发给DSP的底价 */
        record Fixed(long price) implements PriceMarkup {}
    }
}
```

**设计要点：**
- 公共字段通过 sealed interface 的抽象方法暴露，竞价编排时可统一访问 `sourceId()`、`platformId()` 等
- `RtbSource` 携带竞价相关参数（底价、利润率、加价策略）
- `FixedPriceSource` 只需一个固定出价值，结构极简
- `PriceMarkup` 嵌套在 `AdSource` 内，语义归属清晰

**竞价编排中的用法（Pattern Matching）：**
```java
switch (adSource) {
    case AdSource.RtbSource rtb -> {
        // 计算发给DSP的底价
        long dspFloor = switch (rtb.markup()) {
            case AdSource.PriceMarkup.Ratio(var percent) ->
                rtb.bidFloor() * (100 + percent) / 100;
            case AdSource.PriceMarkup.Fixed(var price) -> price;
        };
        // 发起竞价请求...
    }
    case AdSource.FixedPriceSource fixed -> {
        // 直接以固定价格结算，无需请求DSP
    }
}
```

### 2. SellPlatform（DSP 平台配置）

描述一个下游 DSP 平台的基本信息和流量控制参数，纯数据载体：

```java
public record SellPlatform(
    String platformId,              // 平台唯一标识
    String name,                    // 平台名称（如"华为ADX"）
    String platformCode,            // 适配器路由码，用于匹配DSP适配器实现
    String dockingUrl,              // 下游API地址
    int trafficQps,                 // 平台QPS限制，0表示不限
    int trafficFrequency            // 用户日频次上限，0表示不限
) {}
```

**设计要点：**
- 无 `balanceType` 字段（只保留一价结算）
- `platformCode` 用于竞价编排时路由到对应的 DSP 适配器
- 流量控制参数（QPS、频次）从参考实现的嵌套结构简化为扁平字段
- 去掉了 `supportedFormats`，广告形式匹配由 AdSource 与 AdPosition 的关联隐式保证

## 与现有模型的关系

```
model.request (协议入站)          model.domain (内部业务配置)
┌──────────────────┐            ┌──────────────────────┐
│ BidRequest       │            │ AdSource (sealed)    │
│ ├─ Imp           │            │ ├─ RtbSource         │
│ ├─ Device        │            │ └─ FixedPriceSource  │
│ ├─ App           │            │                      │
│ └─ User          │            │ SellPlatform         │
└──────────────────┘            └──────────────────────┘
         │                                │
         └──────────┬─────────────────────┘
                    ▼
          竞价编排 Service（下一步设计）
```

- `model.request/response` = 外部协议的内部表示，描述「这次请求要什么」
- `model.domain` = 系统配置的内部表示，描述「引擎有什么资源可用」
- 竞价编排 Service 将两者结合：用请求信息 + 域配置驱动竞价逻辑

## 测试策略

- 单元测试验证 sealed interface 的 Pattern Matching 分发正确性
- 测试 PriceMarkup 的两种加价计算逻辑
- 测试 Record 的 equals/hashCode/toString 行为（Record 自动生成）
- 用法示例测试确保 switch 表达式覆盖所有分支（编译器保证穷尽性）
