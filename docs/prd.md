# OpenLinkHub IoT Core PRD

## 1. 文档目的

本文档用于沉淀 OpenLinkHub IoT Core 的核心业务模型、功能边界和后续迭代规则。后续产品、后端、前端、数据库和 API 设计均应以本文档为主要依据持续维护。

## 2. 产品定位

OpenLinkHub IoT Core 是 OpenLinkHub 生态的第一阶段核心模块，目标是提供一套开源的设备接入、产品建模、传感器数据采集、状态查看、历史分析和规则告警能力。

第一阶段聚焦：

- 产品管理
- 传感器定义管理
- 设备管理
- HTTP 数据上报
- 设备状态查看
- 历史数据查询与导出
- 阈值规则与告警
- Web 控制台

暂不纳入第一阶段：

- MQTT Broker
- Modbus / OPC UA / TCP 网关
- 多租户
- 复杂权限体系
- 拖拽式规则编排
- 边缘节点
- AI 分析
- 插件市场

## 3. 核心业务原则

OpenLinkHub IoT Core 的设备建模采用“产品定义能力，设备归属产品”的模式。

标准关系为：

```text
Product 1 ── N SensorDefinition
Product 1 ── N Device
Device  1 ── N TelemetryData
```

即：

- 产品定义传感器种类和数据结构。
- 同一产品下的设备拥有相同传感器种类。
- 设备不直接绑定传感器。
- 设备上报数据时，平台根据设备所属产品解析传感器数据。
- 遥测数据按设备、产品、传感器 key 和时间写入时序表。

不再采用：

```text
Device 1 ── N Sensor
```

设备级传感器绑定仅作为未来“设备能力覆盖”场景考虑，不作为第一阶段默认模型。

## 4. 领域对象

### 4.1 产品 Product

产品表示一类设备型号或设备模板，例如：

- 温湿度监测设备 TH-100
- 电表采集终端 EM-200
- 网关型环境监测设备 GW-ENV

产品核心字段：

| 字段 | 说明 |
|---|---|
| id | 产品 ID |
| name | 产品名称 |
| code | 产品编码，唯一 |
| category | 产品分类 |
| protocolType | 协议类型，第一阶段可选 |
| description | 描述 |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |

业务规则：

- 产品编码唯一。
- 产品可以拥有多个传感器定义。
- 产品被设备引用后，仍允许编辑基础信息。
- 产品传感器定义变更应谨慎处理历史数据解释问题。

### 4.2 传感器定义 Sensor Definition

传感器定义描述某类产品具备的可观测指标或能力，例如：

- GPS 位置
- 温度
- 湿度
- 电量
- 电压
- 开关状态

传感器定义核心字段：

| 字段 | 说明 |
|---|---|
| id | 传感器定义 ID |
| productId | 所属产品 ID |
| name | 传感器名称 |
| sensorKey | 传感器标识，上报数据中的 key |
| dataType | 数据类型：number/string/boolean |
| unit | 单位，如 C、%、V |
| precision | 数值精度，可选 |
| required | 是否必填 |
| alarmEnabled | 是否允许用于规则告警 |
| description | 描述 |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |

业务规则：

- 同一产品下 `sensorKey` 唯一。
- `sensorKey` 必须与设备上报数据中的 key 一致。
- 传感器定义属于产品，不属于单台设备。
- 同一产品下的所有设备默认具备该产品定义的传感器集合。
- 删除传感器定义前，需要检查是否已被规则或历史数据使用。

示例：

```text
产品：温湿度监测设备 TH-100

传感器定义：
- gps          GPS 位置       string
- temperature  温度          number  C
- humidity     湿度          number  %
- battery      电量          number  %
```

### 4.3 设备 Device

设备表示真实接入平台的物理设备实例。

设备核心字段：

| 字段 | 说明 |
|---|---|
| id | 设备 ID |
| productId | 所属产品 ID |
| name | 设备名称 |
| deviceKey | 设备唯一标识 |
| secret | 设备密钥 |
| location | GPS 或安装位置 |
| status | 在线状态 |
| lastSeenAt | 最后上报时间 |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |

业务规则：

- 设备必须归属一个产品。
- 设备通过所属产品继承传感器定义。
- 设备不直接维护传感器绑定列表。
- 设备上报数据时，通过 `deviceKey` 定位设备，再通过 `productId` 加载产品传感器定义。
- 设备状态由最近上报行为更新。

### 4.4 遥测数据 Telemetry

遥测数据表示设备某个传感器在某个时间点上报的值。

核心字段：

| 字段 | 说明 |
|---|---|
| time | 数据时间 |
| deviceId | 设备 ID |
| productId | 产品 ID |
| sensorKey | 传感器 key |
| numericValue | 数值型值 |
| textValue | 文本型值 |
| valueType | 值类型 |
| quality | 数据质量 |
| rawValue | 原始值 |
| createdAt | 写入时间 |

业务规则：

- 每个传感器值独立写入一条时序数据。
- `sensorKey` 来自产品传感器定义。
- 第一阶段允许接收未定义 `sensorKey`，但应标记为未建模数据或在后续配置中决定是否拒绝。
- 最新值表按 `deviceId + sensorKey` 保留最新状态。
- 历史数据表用于时间范围查询、趋势分析和导出。

### 4.5 规则 Rule

规则用于对传感器数据进行条件判断并触发告警。

规则核心字段：

| 字段 | 说明 |
|---|---|
| id | 规则 ID |
| name | 规则名称 |
| productId | 产品范围，可选 |
| deviceKey | 指定设备，可选 |
| sensorKey | 传感器 key |
| operator | 操作符：>、>=、<、<=、==、!= |
| threshold | 阈值 |
| severity | 告警等级 |
| enabled | 是否启用 |
| description | 描述 |

业务规则：

- 规则应优先基于产品传感器定义创建。
- 可支持产品级规则：同一产品下所有设备生效。
- 可支持设备级规则：仅对指定设备生效。
- 规则判断发生在遥测数据写入后。

### 4.6 告警 Alarm

告警由规则触发产生。

业务规则：

- 告警关联规则、设备、传感器 key 和实际值。
- 告警状态至少包括 `open` 和 `acknowledged`。
- 告警支持列表筛选、查看详情和确认处理。

## 5. 数据上报与解析流程

设备上报示例：

```json
{
  "timestamp": "2026-06-20T10:30:00+08:00",
  "values": {
    "temperature": 28.6,
    "humidity": 63,
    "battery": 88,
    "gps": "31.2304,121.4737"
  }
}
```

平台处理流程：

```text
1. 根据 deviceKey 查找设备
2. 校验设备 secret
3. 根据 device.productId 加载产品
4. 加载产品传感器定义
5. 遍历 values 中的 key/value
6. 根据 sensorKey 匹配传感器定义
7. 写入 telemetry_data
8. 更新 telemetry_latest
9. 更新设备 lastSeenAt 和在线状态
10. 执行规则判断
11. 生成告警
```

## 6. 控制台功能规划

### 6.1 产品管理

功能：

- 产品列表
- 筛选查询
- 分页
- 新建产品
- 编辑产品
- 查看产品关联的传感器定义入口

### 6.2 传感器管理

功能：

- 传感器定义列表
- 按产品筛选
- 按传感器 key 搜索
- 新建传感器定义
- 编辑传感器定义
- 删除传感器定义
- 查看该传感器是否被规则使用

推荐作为独立菜单：

```text
传感器管理
```

同时在产品详情中提供传感器定义入口。

### 6.3 设备管理

功能：

- 设备列表
- 筛选查询
- 分页
- 新建设备
- 编辑设备
- 选择所属产品
- 设置 GPS 或安装位置

不再提供：

```text
设备直接绑定传感器
```

### 6.4 设备状态查看

功能：

- 设备状态列表
- 按产品、设备状态、关键字筛选
- 查看设备最新传感器状态
- 展示设备所属产品定义的传感器集合
- 已上报传感器显示最新值
- 未上报传感器显示暂无数据

### 6.5 历史数据

功能：

- 选择产品
- 选择设备
- 选择传感器
- 设置时间范围
- 查询历史数据
- 导出 CSV

历史数据用于分析：

- 某个设备温度在一段时间内的变化
- 某个设备电量下降趋势
- 某产品下同类设备传感器数据对比

### 6.6 规则管理

功能：

- 规则列表
- 筛选查询
- 分页
- 新建规则
- 编辑规则
- 启用/停用规则
- 按产品传感器定义选择 `sensorKey`

规则范围：

- 产品级规则
- 设备级规则

### 6.7 告警中心

功能：

- 告警列表
- 筛选查询
- 分页
- 查看告警详情
- 确认告警

## 7. 数据库调整建议

当前需要从设备传感器绑定模型迁移到产品传感器定义模型。

建议新增或调整为：

```text
olh_sensor_definition
- id
- product_id
- name
- sensor_key
- data_type
- unit
- precision
- required
- alarm_enabled
- description
- created_at
- updated_at
```

设备表保持：

```text
olh_device
- product_id
```

遥测表建议字段命名逐步从 `metric` 统一为 `sensor_key`。如果短期不改表结构，可以保留 `metric` 字段，但业务语义应解释为：

```text
metric = sensorKey
```

## 8. API 调整建议

### 产品传感器定义

```text
GET    /api/products/{productId}/sensors
POST   /api/products/{productId}/sensors
PUT    /api/sensor-definitions/{id}
DELETE /api/sensor-definitions/{id}
```

### 设备

设备管理不再提供设备传感器绑定接口。

```text
GET  /api/devices
POST /api/devices
PUT  /api/devices/{id}
```

### 状态

```text
GET /api/devices/{id}/latest
```

返回时应结合产品传感器定义，包含已定义但未上报的传感器。

### 历史数据

```text
GET /api/devices/{id}/telemetry?sensorKey=temperature&start=...&end=...&page=1&size=100
```

## 9. 迁移计划

建议按以下顺序执行：

1. 新增 `olh_sensor_definition` 表。
2. 将当前 `olh_sensor` 数据按设备所属产品迁移为产品传感器定义。
3. 去掉设备传感器绑定管理 UI。
4. 新增独立传感器管理 UI。
5. 调整设备状态页面，按产品传感器定义展示最新值。
6. 调整历史数据页面，按产品、设备、传感器筛选。
7. 调整规则管理，按产品传感器定义选择指标。
8. 后续再决定是否删除旧 `olh_sensor` 表。

## 10. 未来扩展

未来如果同一产品下不同设备存在选配或校准差异，可增加设备能力覆盖表：

```text
olh_device_sensor_override
- device_id
- sensor_definition_id
- enabled
- custom_name
- calibration_offset
- calibration_factor
```

该能力不属于第一阶段默认模型。

## 11. 当前决策

当前确认采用：

```text
产品定义传感器
设备归属产品
设备数据按产品传感器模型解析
```

当前确认废弃默认模型：

```text
设备直接绑定传感器
```

后续所有相关迭代应以本 PRD 为准。
