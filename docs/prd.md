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
- 产品定义默认接入协议和解析规则。
- 设备继承产品协议能力，并保存实例级身份、连接或地址参数。

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
| protocolType | 默认接入协议：HTTP/MQTT/TCP/MODBUS/OPC_UA |
| protocolConfig | 产品级协议解析模板，后续支持 |
| description | 描述 |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |

业务规则：

- 产品编码唯一。
- 产品可以拥有多个传感器定义。
- 产品被设备引用后，仍允许编辑基础信息。
- 产品传感器定义变更应谨慎处理历史数据解释问题。
- 产品不维护自由格式的物模型 JSON；物模型视图由传感器定义生成。
- 物模型视图当前展示传感器属性，后续增加控制指令和事件后同步扩展。
- 产品应定义默认接入协议，协议类型属于产品能力的一部分。
- 产品级协议配置描述同类设备通用的解析规则，例如 MQTT topic 模板、TCP 报文解析规则、Modbus 寄存器映射、OPC UA node 映射。
- MVP 当前已支持 HTTP；下一阶段优先支持 MQTT 和 TCP。
- Modbus / OPC UA 属于后续网关或平台主动采集类协议，应在协议配置成熟后接入。

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
| connectionConfig | 设备级连接参数，后续支持 |
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
- 设备默认继承产品的协议类型和解析规则。
- 设备级配置只保存实例差异，例如 MQTT clientId、TCP 远端地址、Modbus slaveId、OPC UA endpointUrl 或认证信息。
- 第一阶段设备不单独选择协议；后续可允许设备覆盖少量连接参数，但不应随意绕过产品协议约束。

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

## 5. 协议接入模型

协议接入遵循“产品定义协议能力，设备保存实例参数”的原则。

### 5.1 协议绑定边界

产品层负责：

- 默认协议类型。
- 协议级解析模板。
- 数据格式约束。
- 传感器 key 与协议字段、寄存器、节点或报文位置的映射。

设备层负责：

- 设备身份标识。
- 密钥或认证信息。
- IP、端口、clientId、slaveId、endpointUrl 等实例级连接参数。
- 少量允许覆盖的连接配置。

平台协议入口负责：

- 接收或采集不同协议的数据。
- 根据协议上下文定位设备。
- 校验该设备所属产品是否允许对应协议。
- 将协议数据转换为统一遥测格式。

不采用：

```text
任何协议传入同一 deviceKey 都默认接受和解析
```

原因：

- 不利于安全控制。
- 不利于判断设备真实接入方式。
- 不利于定位协议错误和解析错误。
- Modbus / OPC UA 与 HTTP / MQTT / TCP 的连接模型差异较大。

### 5.2 协议类型

第一阶段已有：

```text
HTTP
```

下一阶段优先支持：

```text
MQTT
TCP
```

后续支持：

```text
MODBUS
OPC_UA
```

### 5.3 协议语义

| 协议 | 典型模式 | 产品配置 | 设备配置 |
|---|---|---|---|
| HTTP | 设备主动上报 | Payload 格式、字段映射 | deviceKey、secret |
| MQTT | 设备主动上报 | Topic 模板、Payload 格式、字段映射 | clientId、username、secret |
| TCP | 设备长连接或短连接上报 | 报文帧格式、解析规则、字段映射 | 连接标识、远端地址、密钥 |
| MODBUS | 平台主动采集 | 寄存器映射、数据类型、采集周期 | IP、端口、slaveId、串口参数 |
| OPC_UA | 平台主动采集或订阅 | Node 映射、订阅配置、字段映射 | endpointUrl、认证信息 |

### 5.4 演进阶段

第一阶段：

- 产品保留单一默认协议字段。
- 当前实现以 HTTP 为主。
- 设备继承产品协议，不单独选择协议。

第二阶段：

- 支持 MQTT 和 TCP。
- 增加产品级协议配置。
- 增加设备级连接配置。
- 协议入口收到数据后，必须验证设备所属产品允许该协议。

第三阶段：

- 支持一个产品多个协议通道。
- 引入产品协议配置表。
- 支持 Modbus / OPC UA 这类主动采集协议。

未来产品多协议配置可演进为：

```text
olh_product_protocol
- id
- product_id
- protocol_type
- enabled
- parser_config
- created_at
- updated_at
```

## 6. 数据上报与解析流程

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
1. 协议入口接收数据或执行采集
2. 根据协议上下文定位 deviceKey 或设备连接配置
3. 根据 deviceKey 查找设备
4. 校验设备 secret 或协议认证信息
5. 根据 device.productId 加载产品
6. 校验产品是否允许当前协议
7. 加载产品协议解析模板
8. 加载产品传感器定义
9. 将协议数据转换为统一 values 结构
10. 遍历 values 中的 key/value
11. 根据 sensorKey 匹配传感器定义
12. 写入 telemetry_data
13. 更新 telemetry_latest
14. 更新设备 lastSeenAt 和在线状态
15. 执行规则判断
16. 生成告警
```

## 7. 控制台功能规划

### 7.1 产品管理

功能：

- 产品列表
- 筛选查询
- 分页
- 新建产品
- 编辑产品
- 查看产品关联的传感器定义入口
- 设置默认接入协议
- 后续维护协议解析模板

### 7.2 传感器管理

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

### 7.3 设备管理

功能：

- 设备列表
- 筛选查询
- 分页
- 新建设备
- 编辑设备
- 选择所属产品
- 设置 GPS 或安装位置
- 后续维护设备级连接参数

不再提供：

```text
设备直接绑定传感器
```

### 7.4 设备状态查看

功能：

- 设备状态列表
- 按产品、设备状态、关键字筛选
- 查看设备最新传感器状态
- 展示设备所属产品定义的传感器集合
- 已上报传感器显示最新值
- 未上报传感器显示暂无数据

### 7.5 历史数据

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

### 7.6 规则管理

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

### 7.7 告警中心

功能：

- 告警列表
- 筛选查询
- 分页
- 查看告警详情
- 确认告警

## 8. 数据库调整建议

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

协议字段建议按阶段增加：

```text
olh_product
- protocol_type
- protocol_config

olh_device
- connection_config
```

后续支持一个产品多个协议时，再引入：

```text
olh_product_protocol
- product_id
- protocol_type
- enabled
- parser_config
```

遥测表建议字段命名逐步从 `metric` 统一为 `sensor_key`。如果短期不改表结构，可以保留 `metric` 字段，但业务语义应解释为：

```text
metric = sensorKey
```

## 9. API 调整建议

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

## 10. 迁移计划

建议按以下顺序执行：

1. 新增 `olh_sensor_definition` 表。
2. 将当前 `olh_sensor` 数据按设备所属产品迁移为产品传感器定义。
3. 去掉设备传感器绑定管理 UI。
4. 新增独立传感器管理 UI。
5. 调整设备状态页面，按产品传感器定义展示最新值。
6. 调整历史数据页面，按产品、设备、传感器筛选。
7. 调整规则管理，按产品传感器定义选择指标。
8. 后续再决定是否删除旧 `olh_sensor` 表。

## 11. 未来扩展

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

## 12. 当前决策

当前确认采用：

```text
产品定义传感器
设备归属产品
设备数据按产品传感器模型解析
产品定义默认接入协议
设备保存实例级连接参数
```

当前确认废弃默认模型：

```text
设备直接绑定传感器
任意协议传入都默认解析
```

后续所有相关迭代应以本 PRD 为准。
