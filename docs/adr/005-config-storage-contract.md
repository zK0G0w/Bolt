# ADR-005: 配置存储契约（Bolt 只读，管理端在外）

## 状态

已采纳 (2026-07-27)

## 背景

ADR-004 确定了 Redis + Caffeine + Pub/Sub 的配置热更新架构，但把"管理端"当作一个未实现的外部角色，没有约定双方之间的数据格式。

配置管理后台确定**不进入 Bolt**：Bolt 只从 Redis 读配置，写侧由独立服务承担。这意味着 Redis key 格式、JSON 结构、Pub/Sub 消息格式成为两个独立部署单元之间的契约。契约只存在于 Bolt 的代码里而没有文档，管理端实现时只能靠读源码推断。

本 ADR 把该契约固化，作为管理端的实现依据。

## 决策

Bolt 侧**永不写入** Redis 配置数据（`DataSeeder` 仅在 `dev` profile 下播种样本，不属于生产写路径）。
两个 Repository 接口保持只读，不提供 `save` / `delete`。

契约由以下四部分组成。

### 一、Redis Key 格式

| Key | 类型 | 内容 | 读取方 |
|-----|------|------|--------|
| `bolt:dsp:{platformId}` | String | 单个 `DspPlatform` 的 JSON | `RedisDspPlatformRepository` |
| `bolt:adsource:{sourceId}` | String | 单个 `AdSource` 的 JSON | `RedisAdSourceRepository` |
| `bolt:adsource:index:{adPositionId}` | String | 该广告位下的 sourceId 数组 JSON | `RedisAdSourceRepository` |

约束：

- 全部使用 String 类型存 JSON，不用 Hash。原因是 Caffeine 缓存的粒度就是整个对象，部分字段读取没有意义。
- `bolt:adsource:index:` 是 `bolt:adsource:` 的前缀子集。引擎预热时用 `KEYS bolt:adsource:*` 扫描后**依赖前缀判断剔除索引键**，因此禁止再新增其他以 `bolt:adsource:` 开头的键。
- 不设置 TTL。配置是长期数据，过期由管理端显式删除。

### 二、JSON 结构

`AdSource` 与 `AdSource.PriceMarkup` 是 sealed 类型，靠 `type` 字段判别子类型（Jackson `@JsonTypeInfo(Id.NAME, property = "type")`）。**`type` 字段缺失或取值未知会导致反序列化失败，该广告源被静默跳过。**

RTB 广告源（`type = "rtb"`），`markup` 取 `ratio` 或 `fixed`：

```json
{
  "type": "rtb",
  "sourceId": "src-001",
  "adPositionId": "imp-001",
  "platformId": "plat-001",
  "platformSlotId": "slot-hw-001",
  "timeoutMs": 200,
  "bidFloor": 150,
  "profitRatio": 15,
  "markup": { "type": "ratio", "percent": 20 }
}
```

固定出价广告源（`type = "fixed_price"`），无 `bidFloor` / `profitRatio` / `markup`：

```json
{
  "type": "fixed_price",
  "sourceId": "src-003",
  "adPositionId": "imp-001",
  "platformId": "plat-003",
  "platformSlotId": "slot-bd-001",
  "timeoutMs": 300,
  "fixedBidPrice": 500
}
```

DSP 平台（无 `type` 字段，非 sealed 类型）：

```json
{
  "platformId": "plat-001",
  "name": "Mock-ADX-A",
  "platformCode": "mock",
  "dockingUrl": "https://adx.mock-a.com/bid",
  "trafficQps": 1000,
  "trafficFrequency": 50
}
```

广告位索引（值为 JSON 数组，非 Redis List）：

```json
["src-001", "src-002", "src-003"]
```

字段约束：

| 字段 | 约束 | 违反后果 |
|------|------|---------|
| 所有价格字段 | **单位为分/CPM 的整数**，不接受小数 | 反序列化失败或精度丢失 |
| `markup.percent` | `>= 0` | `Ratio` 构造器抛 `IllegalArgumentException`，该广告源被跳过 |
| `profitRatio` | `0-100` | 无校验，超范围会算出负结算价 |
| `platformCode` | 见第四节 | 扇出返回 `Error`，该广告源无出价 |

### 三、Pub/Sub 失效消息

频道：`bolt:cache:invalidate`（可由 `bolt.cache.invalidation-channel` 覆盖）

消息体：

```json
{ "entity": "adsource", "id": "src-001", "action": "update" }
```

| 字段 | 取值 | 说明 |
|------|------|------|
| `entity` | `adsource` / `dsp` | 其他取值被记 WARN 后忽略 |
| `id` | 实体主键 | `entity=adsource` 时为 sourceId，`entity=dsp` 时为 platformId |
| `action` | `update` / `delete` | **当前实现未使用该字段**，两种操作都只做失效 |

行为约定：

- `entity=adsource` 时，引擎失效该实体缓存，并**全量失效广告位索引缓存**（因为无法从 sourceId 反推它属于哪些广告位）。
- 消息丢失不会导致永久不一致，Caffeine 有 5 分钟 `expireAfterWrite` 兜底（ADR-004）。

### 四、platformCode 的合法取值

`platformCode` 的合法值**由 Bolt 侧已部署的 `DspClient` 实现决定**，不是自由文本。当前为 `mock`、`mock2`。

引擎侧 `DspClientRouter.route()` 找不到匹配适配器时**不做兜底降级**，直接产出 `DspBidResult.Error`。这是刻意选择：一旦配置可被人工编辑，静默 fallback 到 mock 适配器意味着线上返回假出价且无任何告警。

因此管理端**不得**把 `platformCode` 做成自由输入框。合法取值以 `DspClientRouter.supportedCodes()` 为准，两种获取方式：

- 短期：人工同步，Bolt 新增适配器时更新管理端的枚举
- 长期：Bolt 暴露一个只读接口返回该集合，管理端下拉框取远端数据（**尚未实现**）

### 五、管理端写入顺序要求

新增或修改一个 `AdSource`，必须按此顺序完成三步：

1. `SET bolt:adsource:{sourceId}` — 写实体
2. `SET bolt:adsource:index:{adPositionId}` — 写/更新索引（**改了 `adPositionId` 时还要从旧广告位的索引里移除**）
3. `PUBLISH bolt:cache:invalidate` — 通知失效

三步之间**没有原子性保证**，Bolt 侧也没有一致性校验。中途失败会留下脏数据，典型症状：

- 只做了 1 没做 2 → 广告源存在但不参与任何广告位的竞价
- 只做了 2 没做 1 → 索引指向不存在的实体，引擎按 `findById` 返回空静默跳过（`RedisAdSourceRepository.findByAdPositionId` 会过滤掉 null）
- 做了 1、2 没做 3 → 最多 5 分钟后生效

管理端应自行用 Lua 脚本或 `MULTI` 把 1、2 合并为一次原子写入。这是**管理端的责任，不是 Bolt 的**。

## 否决方案

| 方案 | 否决理由 |
|------|---------|
| 管理后台并入 Bolt（加 `/admin/**` 端点） | 配置管理与投放执行是两个不同的限界上下文，塞进同一进程会稀释引擎的轻量定位；且引擎需要为写侧引入持久存储 |
| Repository 预留 `save` / `delete` | 当前无调用方，属于投机设计。Repository 是接口，将来真需要写侧时新增不破坏既有代码 |
| 用 Protobuf / Avro 定义契约 | 需要引入 schema 仓库和代码生成，对单人学习项目是纯运维成本；JSON + 本文档已够 |
| 由 Bolt 校验配置合法性后拒绝加载 | 引擎在竞价热路径上，校验成本应由写侧承担；且"拒绝加载"和"跳过"对竞价结果的影响相同 |

## 后果

- 管理端可独立实现，无需读 Bolt 源码
- `AdSource` / `DspPlatform` / `CacheInvalidationMessage` 的字段结构成为对外契约,**改字段即破坏兼容**，需同步更新本文档并协调管理端
- 价格字段确定为整数分，因此不引入 `Money` 值对象（会改变 JSON 形态，破坏本契约）；若将来要做，需作为一次显式的契约变更
- 索引与实体的一致性没有任何机制保障，是已知的架构缺口，责任归属写侧
- `platformCode` 合法值的同步目前靠人工，长期需要 Bolt 暴露只读接口
