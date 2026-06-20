<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <img src="./assets/openlinkhub-icon.svg" alt="OpenLinkHub" />
        <div>
          <strong>OpenLinkHub</strong>
          <span>IoT Core</span>
        </div>
      </div>

      <nav>
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="{ active: activeView === item.key }"
          @click="switchView(item.key)"
          :title="item.label"
        >
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="edge-card">
        <div class="pulse"></div>
        <span>Core API</span>
        <strong>{{ health.status || 'CHECKING' }}</strong>
      </div>
    </aside>

    <main>
      <header class="topbar">
        <div>
          <p class="eyebrow">Signal Operations Console</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="top-actions">
          <button class="icon-button" @click="refreshAll" title="刷新数据">
            <RefreshCw :size="18" />
          </button>
          <button class="primary-button" @click="sendDemoTelemetry">
            <Radio :size="17" />
            模拟上报
          </button>
        </div>
      </header>

      <p v-if="error" class="notice error">{{ error }}</p>
      <p v-if="toast" class="notice success">{{ toast }}</p>

      <section v-if="activeView === 'overview'" class="view overview-grid">
        <div class="signal-panel">
          <div class="panel-title">
            <div>
              <p class="eyebrow">Connection Map</p>
              <h2>设备、传感器与数据流</h2>
            </div>
            <span class="live-dot">Live</span>
          </div>
          <div class="hub-map">
            <div class="node core">
              <img src="./assets/openlinkhub-icon.svg" alt="" />
              <strong>Hub</strong>
            </div>
            <div class="node device">Devices</div>
            <div class="node rule">Rules</div>
            <div class="node alarm">Alarms</div>
            <div class="node api">API</div>
          </div>
        </div>

        <div class="metric-grid">
          <article v-for="metric in metrics" :key="metric.label" class="metric-card">
            <component :is="metric.icon" :size="20" />
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </article>
        </div>

        <div class="panel span-2">
          <div class="panel-title">
            <h2>最近数据</h2>
            <button class="ghost-button" @click="switchView('device-status')">查看状态</button>
          </div>
          <div class="data-stream">
            <div v-for="point in summary.recentTelemetry || []" :key="`${point.deviceId}-${point.metric}-${point.time}`" class="stream-row">
              <span class="stream-time">{{ formatTime(point.time) }}</span>
              <strong>{{ point.metric }}</strong>
              <span>{{ displayValue(point) }}</span>
            </div>
            <div v-if="!summary.recentTelemetry?.length" class="empty">暂无数据，点击“模拟上报”开始。</div>
          </div>
        </div>
      </section>

      <section v-if="activeView === 'products'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Product Registry</p>
              <h2>产品列表</h2>
            </div>
            <button class="primary-button" @click="openProductModal()">
              <Plus :size="17" />
              新建产品
            </button>
          </div>
          <div class="filter-row product-filter">
            <input v-model="productQuery.keyword" placeholder="搜索名称 / 编码 / 描述" @keyup.enter="loadProducts(1)" />
            <input v-model="productQuery.category" placeholder="分类" @keyup.enter="loadProducts(1)" />
            <button class="primary-button" @click="loadProducts(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetProducts">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>产品名称</th>
                <th>编码</th>
                <th>分类</th>
                <th>默认协议</th>
                <th>描述</th>
                <th>创建时间</th>
                <th class="actions-col wide">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in products" :key="product.id">
                <td><strong>{{ product.name }}</strong></td>
                <td><code>{{ product.code }}</code></td>
                <td>{{ product.category }}</td>
                <td><span class="protocol-pill">{{ product.protocolType || 'HTTP' }}</span></td>
                <td>{{ product.description || '-' }}</td>
                <td>{{ formatDate(product.createdAt) }}</td>
                <td>
                  <button class="table-action" @click="openThingModel(product)">
                    <Eye :size="15" /> 查看
                  </button>
                  <button class="table-action" @click="openProductModal(product)">
                    <Edit3 :size="15" /> 编辑
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="productPage.page" :size="productPage.size" :total="productPage.total" @change="loadProducts" />
        </div>
      </section>

      <section v-if="activeView === 'device-management'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Device Registry</p>
              <h2>设备管理</h2>
            </div>
            <button class="primary-button" @click="openDeviceModal()">
              <Plus :size="17" />
              新建设备
            </button>
          </div>
          <div class="filter-row device-filter">
            <input v-model="deviceQuery.keyword" placeholder="搜索名称 / Key / 位置" @keyup.enter="loadDevices(1)" />
            <select v-model="deviceQuery.productId">
              <option value="">全部产品</option>
              <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
            </select>
            <select v-model="deviceQuery.status">
              <option value="">全部状态</option>
              <option value="online">online</option>
              <option value="offline">offline</option>
            </select>
            <button class="primary-button" @click="loadDevices(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetDevices">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>设备名称</th>
                <th>Device Key</th>
                <th>产品</th>
                <th>协议</th>
                <th>位置 / GPS</th>
                <th>状态</th>
                <th>最后上报</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="device in devices" :key="device.id">
                <td><strong>{{ device.name }}</strong></td>
                <td><code>{{ device.deviceKey }}</code></td>
                <td>{{ device.productName }}</td>
                <td><span class="protocol-pill">{{ device.productProtocolType || '-' }}</span></td>
                <td>{{ device.location || '-' }}</td>
                <td><span :class="['status-pill', device.status]">{{ device.status }}</span></td>
                <td>{{ formatDate(device.lastSeenAt) }}</td>
                <td>
                  <button class="table-action" @click="openDeviceModal(device)">
                    <Edit3 :size="15" /> 编辑
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="devicePage.page" :size="devicePage.size" :total="devicePage.total" @change="loadDevices" />
        </div>
      </section>

      <section v-if="activeView === 'sensors'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Product Sensor Model</p>
              <h2>传感器管理</h2>
            </div>
            <button class="primary-button" :disabled="!sensorQuery.productId" @click="openSensorModal()">
              <Plus :size="17" />
              新建传感器
            </button>
          </div>
          <div class="filter-row sensor-filter">
            <select v-model.number="sensorQuery.productId" @change="loadSensors(1)">
              <option disabled value="">选择产品</option>
              <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
            </select>
            <input v-model="sensorQuery.keyword" placeholder="搜索名称 / Key / 描述" @keyup.enter="loadSensors(1)" />
            <button class="primary-button" @click="loadSensors(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetSensors">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>传感器名称</th>
                <th>传感器 Key</th>
                <th>所属产品</th>
                <th>类型</th>
                <th>单位</th>
                <th>描述</th>
                <th class="actions-col wide">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sensor in sensorRecords" :key="sensor.id">
                <td><strong>{{ sensor.name }}</strong></td>
                <td><code>{{ sensor.sensorKey }}</code></td>
                <td>{{ productName(sensor.productId) }}</td>
                <td>{{ sensor.sensorType }}</td>
                <td>{{ sensor.unit || '-' }}</td>
                <td>{{ sensor.description || '-' }}</td>
                <td>
                  <button class="table-action" @click="openSensorModal(sensor)">
                    <Edit3 :size="15" /> 编辑
                  </button>
                  <button class="table-action danger" @click="deleteSensor(sensor.id)">删除</button>
                </td>
              </tr>
              <tr v-if="!sensorRecords.length">
                <td colspan="7" class="table-empty">请选择产品后维护该产品的传感器定义。</td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="sensorPage.page" :size="sensorPage.size" :total="sensorTotal" @change="changeSensorPage" />
        </div>
      </section>

      <section v-if="activeView === 'device-status'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Realtime State</p>
              <h2>设备状态查看</h2>
            </div>
          </div>
          <div class="filter-row device-filter">
            <input v-model="deviceQuery.keyword" placeholder="搜索名称 / Key / 位置" @keyup.enter="loadDevices(1)" />
            <select v-model="deviceQuery.status">
              <option value="">全部状态</option>
              <option value="online">online</option>
              <option value="offline">offline</option>
            </select>
            <button class="primary-button" @click="loadDevices(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetDevices">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>设备</th>
                <th>Device Key</th>
                <th>位置 / GPS</th>
                <th>在线状态</th>
                <th>最后上报</th>
                <th>最新传感器状态</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="device in devices" :key="device.id">
                <td><strong>{{ device.name }}</strong></td>
                <td><code>{{ device.deviceKey }}</code></td>
                <td>{{ device.location || '-' }}</td>
                <td><span :class="['status-pill', device.status]">{{ device.status }}</span></td>
                <td>{{ formatDate(device.lastSeenAt) }}</td>
                <td>{{ latestSummary(device.id) }}</td>
                <td>
                  <button class="table-action" @click="openDeviceStatus(device)">
                    <Eye :size="15" /> 查看状态
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="devicePage.page" :size="devicePage.size" :total="devicePage.total" @change="loadDevices" />
        </div>
      </section>

      <section v-if="activeView === 'history'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Telemetry History</p>
              <h2>历史数据分析</h2>
            </div>
            <button class="primary-button" :disabled="!historyRows.length" @click="exportHistoryCsv">
              <Download :size="17" />
              导出 CSV
            </button>
          </div>
          <div class="filter-row">
            <select v-model.number="historyFilter.productId" @change="onHistoryProductChange">
              <option disabled value="">选择产品</option>
              <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
            </select>
            <select v-model.number="historyFilter.deviceId" @change="loadHistorySensors">
              <option disabled value="">选择设备</option>
              <option v-for="device in historyDevices" :key="device.id" :value="device.id">{{ device.name }} / {{ device.deviceKey }}</option>
            </select>
            <select v-model="historyFilter.metric">
              <option value="">全部传感器</option>
              <option v-for="sensor in historySensors" :key="sensor.id" :value="sensor.sensorKey">
                {{ sensor.name }} / {{ sensor.sensorKey }}
              </option>
            </select>
            <input v-model="historyFilter.start" type="datetime-local" />
            <input v-model="historyFilter.end" type="datetime-local" />
            <button class="primary-button" @click="queryHistory">
              <Search :size="17" />
              查询
            </button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>设备 ID</th>
                <th>传感器 Key</th>
                <th>数值</th>
                <th>文本值</th>
                <th>类型</th>
                <th>质量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in historyRows" :key="`${row.deviceId}-${row.metric}-${row.time}`">
                <td>{{ formatDate(row.time) }}</td>
                <td>{{ row.deviceId }}</td>
                <td><code>{{ row.metric }}</code></td>
                <td>{{ row.numericValue ?? '-' }}</td>
                <td>{{ row.textValue || '-' }}</td>
                <td>{{ row.valueType }}</td>
                <td>{{ row.quality }}</td>
              </tr>
              <tr v-if="!historyRows.length">
                <td colspan="7" class="table-empty">请选择设备和时间范围查询历史数据。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="activeView === 'rules'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Automation Rules</p>
              <h2>阈值规则</h2>
            </div>
            <button class="primary-button" @click="openRuleModal()">
              <Plus :size="17" />
              新建规则
            </button>
          </div>
          <div class="filter-row rule-filter">
            <input v-model="ruleQuery.keyword" placeholder="搜索规则名称 / 描述" @keyup.enter="loadRules(1)" />
            <input v-model="ruleQuery.deviceKey" placeholder="设备 Key" @keyup.enter="loadRules(1)" />
            <input v-model="ruleQuery.metric" placeholder="传感器 Key" @keyup.enter="loadRules(1)" />
            <select v-model="ruleQuery.enabled">
              <option value="">全部状态</option>
              <option value="true">enabled</option>
              <option value="false">disabled</option>
            </select>
            <button class="primary-button" @click="loadRules(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetRules">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>规则名称</th>
                <th>设备范围</th>
                <th>传感器 Key</th>
                <th>条件</th>
                <th>等级</th>
                <th>状态</th>
                <th class="actions-col wide">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in rules" :key="rule.id">
                <td><strong>{{ rule.name }}</strong></td>
                <td>{{ rule.deviceKey || '所有设备' }}</td>
                <td><code>{{ rule.metric }}</code></td>
                <td>{{ rule.operator }} {{ rule.threshold }}</td>
                <td><span :class="['severity', rule.severity]">{{ rule.severity }}</span></td>
                <td><span :class="['status-pill', rule.enabled ? 'online' : 'offline']">{{ rule.enabled ? 'enabled' : 'disabled' }}</span></td>
                <td>
                  <button class="table-action" @click="openRuleModal(rule)">
                    <Edit3 :size="15" /> 编辑
                  </button>
                  <button class="table-action" @click="toggleRule(rule)">
                    {{ rule.enabled ? '停用' : '启用' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="rulePage.page" :size="rulePage.size" :total="rulePage.total" @change="loadRules" />
        </div>
      </section>

      <section v-if="activeView === 'alarms'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">Alarm Center</p>
              <h2>告警列表</h2>
            </div>
          </div>
          <div class="filter-row alarm-filter">
            <input v-model="alarmQuery.keyword" placeholder="搜索设备 / 传感器 / 消息" @keyup.enter="loadAlarms(1)" />
            <select v-model="alarmQuery.status">
              <option value="">全部状态</option>
              <option value="open">open</option>
              <option value="acknowledged">acknowledged</option>
            </select>
            <select v-model="alarmQuery.severity">
              <option value="">全部等级</option>
              <option value="critical">critical</option>
              <option value="warning">warning</option>
              <option value="info">info</option>
            </select>
            <button class="primary-button" @click="loadAlarms(1)"><Search :size="17" /> 查询</button>
            <button class="ghost-button" @click="resetAlarms">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>等级</th>
                <th>设备</th>
                <th>传感器 Key</th>
                <th>实际值</th>
                <th>条件</th>
                <th>状态</th>
                <th>发生时间</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="alarm in alarms" :key="alarm.id">
                <td><span :class="['severity', alarm.severity]">{{ alarm.severity }}</span></td>
                <td><code>{{ alarm.deviceKey }}</code></td>
                <td>{{ alarm.metric }}</td>
                <td>{{ alarm.value }}</td>
                <td>{{ alarm.operator }} {{ alarm.threshold }}</td>
                <td><span :class="['status-pill', alarm.status === 'open' ? 'offline' : 'acknowledged']">{{ alarm.status }}</span></td>
                <td>{{ formatDate(alarm.occurredAt) }}</td>
                <td>
                  <button class="table-action" @click="openAlarmModal(alarm)">
                    <Eye :size="15" /> 查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <Pagination :page="alarmPage.page" :size="alarmPage.size" :total="alarmPage.total" @change="loadAlarms" />
        </div>
      </section>

      <section v-if="activeView === 'ingest'" class="view">
        <div class="panel">
          <div class="table-toolbar">
            <div>
              <p class="eyebrow">HTTP Ingest</p>
              <h2>接入示例</h2>
            </div>
            <button class="primary-button" @click="openIngestModal">
              <Send :size="17" />
              上报测试
            </button>
          </div>
          <div class="code-panel">
            <pre>{{ curlExample }}</pre>
          </div>
        </div>
      </section>

      <div v-if="modal" class="modal-backdrop" @click.self="closeModal">
        <section class="modal-card" :class="{ large: ['device-status', 'ingest', 'thing-model'].includes(modal.type) }">
          <header class="modal-header">
            <div>
              <p class="eyebrow">{{ modal.eyebrow }}</p>
              <h2>{{ modal.title }}</h2>
            </div>
            <button class="icon-button" @click="closeModal" title="关闭">
              <X :size="18" />
            </button>
          </header>

          <form v-if="modal.type === 'product'" class="form-grid" @submit.prevent="saveProduct">
            <label class="field">
              <span>产品名称</span>
              <input v-model="productForm.name" />
            </label>
            <label class="field">
              <span>产品编码</span>
              <input v-model="productForm.code" />
              <small>用于系统识别，建议使用英文、数字或短横线。</small>
            </label>
            <label class="field">
              <span>产品分类</span>
              <input v-model="productForm.category" />
              <small>例如 sensor、gateway、meter。</small>
            </label>
            <label class="field">
              <span>默认接入协议</span>
              <select v-model="productForm.protocolType">
                <option v-for="type in protocolTypes" :key="type" :value="type">{{ type }}</option>
              </select>
              <small>设备默认继承产品协议，后续协议入口会据此校验。</small>
            </label>
            <label class="field">
              <span>产品描述</span>
              <textarea v-model="productForm.description"></textarea>
            </label>
            <label class="field full">
              <span>协议解析模板</span>
              <textarea v-model="productForm.protocolConfig"></textarea>
              <small>JSON 格式。HTTP 可留空，MQTT/TCP 后续用于 topic、报文格式和字段映射。</small>
            </label>
            <footer class="modal-actions">
              <button type="button" class="ghost-button" @click="closeModal">取消</button>
              <button class="primary-button">保存</button>
            </footer>
          </form>

          <form v-if="modal.type === 'device'" class="form-grid" @submit.prevent="saveDevice">
            <label class="field">
              <span>所属产品</span>
              <select v-model.number="deviceForm.productId">
                <option disabled value="">选择产品</option>
                <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
              </select>
              <small>设备会继承该产品下定义的传感器类型。</small>
            </label>
            <label class="field">
              <span>继承协议</span>
              <input :value="selectedDeviceProduct?.protocolType || 'HTTP'" disabled />
              <small>协议类型来自产品；设备只维护实例级连接参数。</small>
            </label>
            <label class="field">
              <span>设备名称</span>
              <input v-model="deviceForm.name" />
            </label>
            <label class="field">
              <span>设备 Key</span>
              <input v-model="deviceForm.deviceKey" />
              <small>设备上报时使用的唯一标识。</small>
            </label>
            <label class="field">
              <span>设备密钥</span>
              <input v-model="deviceForm.secret" />
              <small>HTTP 上报时通过 X-Device-Secret 校验。</small>
            </label>
            <label class="field full">
              <span>GPS / 安装位置</span>
              <input v-model="deviceForm.location" />
              <small>例如 31.2304,121.4737，或填写具体安装位置。</small>
            </label>
            <label class="field full">
              <span>连接参数</span>
              <textarea v-model="deviceForm.connectionConfig"></textarea>
              <small>JSON 格式。用于保存 MQTT clientId、TCP 地址、Modbus slaveId、OPC UA endpointUrl 等设备实例差异。</small>
            </label>
            <footer class="modal-actions">
              <button type="button" class="ghost-button" @click="closeModal">取消</button>
              <button class="primary-button">保存</button>
            </footer>
          </form>

          <form v-if="modal.type === 'sensor'" class="form-grid" @submit.prevent="saveSensor">
            <label class="field">
              <span>所属产品</span>
              <select v-model.number="sensorForm.productId">
                <option disabled value="">选择产品</option>
                <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
              </select>
            </label>
            <label class="field">
              <span>传感器名称</span>
              <input v-model="sensorForm.name" />
              <small>例如温度传感器、湿度传感器、电量。</small>
            </label>
            <label class="field">
              <span>传感器 Key</span>
              <input v-model="sensorForm.sensorKey" />
              <small>必须与设备回传数据里的字段名一致，例如 temperature。</small>
            </label>
            <label class="field">
              <span>数据类型</span>
              <select v-model="sensorForm.sensorType">
                <option>number</option>
                <option>string</option>
                <option>boolean</option>
              </select>
            </label>
            <label class="field">
              <span>单位</span>
              <input v-model="sensorForm.unit" />
              <small>例如 C、%、V；无单位可留空。</small>
            </label>
            <label class="field full">
              <span>传感器描述</span>
              <textarea v-model="sensorForm.description"></textarea>
            </label>
            <footer class="modal-actions">
              <button type="button" class="ghost-button" @click="closeModal">取消</button>
              <button class="primary-button">保存</button>
            </footer>
          </form>

          <form v-if="modal.type === 'rule'" class="form-grid" @submit.prevent="saveRule">
            <label class="field">
              <span>规则名称</span>
              <input v-model="ruleForm.name" />
            </label>
            <label class="field">
              <span>设备 Key</span>
              <input v-model="ruleForm.deviceKey" />
              <small>留空表示应用到所有设备。</small>
            </label>
            <label class="field">
              <span>传感器 Key</span>
              <input v-model="ruleForm.metric" />
              <small>例如 temperature，应与产品传感器定义一致。</small>
            </label>
            <label class="field">
              <span>判断条件</span>
              <select v-model="ruleForm.operator">
                <option v-for="op in ['>', '>=', '<', '<=', '==', '!=']" :key="op">{{ op }}</option>
              </select>
            </label>
            <label class="field">
              <span>阈值</span>
              <input v-model.number="ruleForm.threshold" type="number" step="0.01" />
            </label>
            <label class="field">
              <span>告警等级</span>
              <select v-model="ruleForm.severity">
                <option>warning</option>
                <option>critical</option>
                <option>info</option>
              </select>
            </label>
            <footer class="modal-actions">
              <button type="button" class="ghost-button" @click="closeModal">取消</button>
              <button class="primary-button">保存</button>
            </footer>
          </form>

          <div v-if="modal.type === 'device-status'" class="detail-layout">
            <div class="detail-card">
              <span>设备 Key</span>
              <strong>{{ modal.item.deviceKey }}</strong>
            </div>
            <div class="detail-card">
              <span>GPS / 位置</span>
              <strong>{{ modal.item.location || '-' }}</strong>
            </div>
            <div class="detail-card">
              <span>产品</span>
              <strong>{{ modal.item.productName }}</strong>
            </div>
            <div class="detail-card">
              <span>在线状态</span>
              <strong>{{ modal.item.status }}</strong>
            </div>
            <div class="latest-grid full">
              <div v-for="item in latest" :key="item.metric" class="latest-item">
                <span>{{ item.sensorName || item.metric }}<small v-if="item.unit"> {{ item.unit }}</small></span>
                <strong>{{ displayValue(item) }}</strong>
              </div>
              <div v-if="!latest.length" class="empty">暂无最新数据。</div>
            </div>
          </div>

          <div v-if="modal.type === 'thing-model'" class="model-view">
            <div class="model-summary">
              <div>
                <span>产品编码</span>
                <strong>{{ modal.item.code }}</strong>
              </div>
              <div>
                <span>产品分类</span>
                <strong>{{ modal.item.category || '-' }}</strong>
              </div>
              <div>
                <span>默认协议</span>
                <strong>{{ modal.item.protocolType || 'HTTP' }}</strong>
              </div>
              <div>
                <span>传感器属性</span>
                <strong>{{ productModelSensors.length }}</strong>
              </div>
            </div>

            <section class="model-section">
              <div class="table-toolbar compact">
                <div>
                  <p class="eyebrow">Protocol Template</p>
                  <h2>协议解析模板</h2>
                </div>
              </div>
              <pre class="model-code">{{ formatJson(modal.item.protocolConfig) }}</pre>
            </section>

            <section class="model-section">
              <div class="table-toolbar compact">
                <div>
                  <p class="eyebrow">Telemetry Properties</p>
                  <h2>传感器属性</h2>
                </div>
              </div>
              <table class="data-table">
                <thead>
                  <tr>
                    <th>名称</th>
                    <th>Key</th>
                    <th>类型</th>
                    <th>单位</th>
                    <th>说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="sensor in productModelSensors" :key="sensor.id">
                    <td><strong>{{ sensor.name }}</strong></td>
                    <td><code>{{ sensor.sensorKey }}</code></td>
                    <td>{{ sensor.sensorType }}</td>
                    <td>{{ sensor.unit || '-' }}</td>
                    <td>{{ sensor.description || '-' }}</td>
                  </tr>
                  <tr v-if="!productModelSensors.length">
                    <td colspan="5" class="table-empty">暂无传感器属性，请先在传感器管理中维护。</td>
                  </tr>
                </tbody>
              </table>
            </section>

            <div class="model-placeholder-grid">
              <div class="model-placeholder">
                <span>Control Commands</span>
                <strong>控制指令</strong>
                <p>后续增加设备控制能力后，将在这里展示可下发指令、参数和数据类型。</p>
              </div>
              <div class="model-placeholder">
                <span>Events</span>
                <strong>事件</strong>
                <p>后续增加事件能力后，将在这里展示事件 Key、事件等级和事件字段。</p>
              </div>
            </div>
          </div>

          <div v-if="modal.type === 'alarm'" class="detail-layout">
            <div class="alarm-message full">{{ modal.item.message }}</div>
            <div class="detail-card">
              <span>设备</span>
              <strong>{{ modal.item.deviceKey }}</strong>
            </div>
            <div class="detail-card">
              <span>传感器 Key</span>
              <strong>{{ modal.item.metric }}</strong>
            </div>
            <div class="detail-card">
              <span>实际值</span>
              <strong>{{ modal.item.value }}</strong>
            </div>
            <div class="detail-card">
              <span>状态</span>
              <strong>{{ modal.item.status }}</strong>
            </div>
            <footer class="modal-actions full">
              <button type="button" class="ghost-button" @click="closeModal">关闭</button>
              <button v-if="modal.item.status === 'open'" class="primary-button" @click="ackAlarm(modal.item.id)">
                <CheckCircle2 :size="17" />
                确认告警
              </button>
            </footer>
          </div>

          <form v-if="modal.type === 'ingest'" class="form-grid" @submit.prevent="sendCustomTelemetry">
            <label class="field">
              <span>设备 Key</span>
              <input v-model="ingestForm.deviceKey" />
            </label>
            <label class="field">
              <span>设备密钥</span>
              <input v-model="ingestForm.secret" />
            </label>
            <label class="field full">
              <span>上报 Payload</span>
              <textarea class="payload" v-model="ingestForm.payload"></textarea>
              <small>values 中的字段名会按产品传感器 Key 解析。</small>
            </label>
            <footer class="modal-actions">
              <button type="button" class="ghost-button" @click="closeModal">取消</button>
              <button class="primary-button">
                <Send :size="17" />
                发送数据
              </button>
            </footer>
          </form>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue';
import {
  Bell,
  Boxes,
  CheckCircle2,
  Cpu,
  Database,
  Download,
  Edit3,
  Eye,
  Gauge,
  GitBranch,
  Home,
  Plus,
  Radio,
  RefreshCw,
  Router,
  Search,
  Send,
  ShieldAlert,
  SlidersHorizontal,
  X,
} from 'lucide-vue-next';
import { api } from './api';

const viewKeys = ['overview', 'products', 'sensors', 'device-management', 'device-status', 'history', 'rules', 'alarms', 'ingest'];
const hashView = window.location.hash.replace('#', '');
const activeView = ref(viewKeys.includes(hashView) ? hashView : 'overview');
const error = ref('');
const toast = ref('');
const health = reactive({});
const summary = ref({});
const productPage = ref({ records: [], total: 0, page: 1, size: 10 });
const devicePage = ref({ records: [], total: 0, page: 1, size: 10 });
const latest = ref([]);
const latestByDevice = reactive({});
const sensors = ref([]);
const productModelSensors = ref([]);
const historySensors = ref([]);
const historyRows = ref([]);
const rulePage = ref({ records: [], total: 0, page: 1, size: 10 });
const alarmPage = ref({ records: [], total: 0, page: 1, size: 10 });
const selectedDevice = ref(null);
const modal = ref(null);
const protocolTypes = ['HTTP', 'MQTT', 'TCP', 'MODBUS', 'OPC_UA'];

const productForm = reactive({
  id: null,
  name: '',
  code: '',
  category: 'sensor',
  protocolType: 'HTTP',
  protocolConfig: '{}',
  description: '',
});
const deviceForm = reactive({ id: null, productId: '', name: '', deviceKey: '', secret: '', connectionConfig: '{}', location: '' });
const sensorForm = reactive({ id: null, productId: '', name: '', sensorKey: '', sensorType: 'number', unit: '', description: '' });
const ruleForm = reactive({
  id: null,
  name: '',
  deviceKey: '',
  metric: 'temperature',
  operator: '>',
  threshold: 35,
  severity: 'warning',
  enabled: true,
});
const historyFilter = reactive({ productId: '', deviceId: '', metric: '', start: '', end: '' });
const productQuery = reactive({ keyword: '', category: '', page: 1, size: 10 });
const deviceQuery = reactive({ keyword: '', productId: '', status: '', page: 1, size: 10 });
const sensorQuery = reactive({ productId: '', keyword: '', page: 1, size: 10 });
const ruleQuery = reactive({ keyword: '', deviceKey: '', metric: '', enabled: '', page: 1, size: 10 });
const alarmQuery = reactive({ keyword: '', status: '', severity: '', page: 1, size: 10 });
const ingestForm = reactive({
  deviceKey: 'demo-device-001',
  secret: 'demo-secret',
  payload: JSON.stringify({
    timestamp: new Date().toISOString(),
    values: { temperature: 36.8, humidity: 62, voltage: 220.1, battery: 87 },
  }, null, 2),
});

const navItems = [
  { key: 'overview', label: '总览', icon: Home },
  { key: 'products', label: '产品', icon: Boxes },
  { key: 'sensors', label: '传感器管理', icon: SlidersHorizontal },
  { key: 'device-management', label: '设备管理', icon: Cpu },
  { key: 'device-status', label: '设备状态', icon: Gauge },
  { key: 'history', label: '历史数据', icon: Database },
  { key: 'rules', label: '规则', icon: GitBranch },
  { key: 'alarms', label: '告警', icon: ShieldAlert },
  { key: 'ingest', label: '接入', icon: Router },
];

const titles = {
  overview: '连接态势',
  products: '产品与物模型',
  sensors: '传感器管理',
  'device-management': '设备管理',
  'device-status': '设备状态查看',
  history: '历史数据分析',
  rules: '规则编排',
  alarms: '告警中心',
  ingest: '数据接入',
};

const currentTitle = computed(() => titles[activeView.value]);
const products = computed(() => productPage.value.records || []);
const devices = computed(() => devicePage.value.records || []);
const rules = computed(() => rulePage.value.records || []);
const alarms = computed(() => alarmPage.value.records || []);
const filteredSensors = computed(() => {
  const keyword = sensorQuery.keyword.trim().toLowerCase();
  if (!keyword) return sensors.value;
  return sensors.value.filter((sensor) => [
    sensor.name,
    sensor.sensorKey,
    sensor.sensorType,
    sensor.unit,
    sensor.description,
  ].some((value) => String(value || '').toLowerCase().includes(keyword)));
});
const sensorTotal = computed(() => filteredSensors.value.length);
const sensorPage = computed(() => ({
  page: sensorQuery.page,
  size: sensorQuery.size,
  total: sensorTotal.value,
}));
const sensorRecords = computed(() => {
  const start = (sensorQuery.page - 1) * sensorQuery.size;
  return filteredSensors.value.slice(start, start + sensorQuery.size);
});
const historyDevices = computed(() => devices.value.filter((device) => !historyFilter.productId || device.productId === historyFilter.productId));
const selectedDeviceProduct = computed(() => products.value.find((product) => product.id === deviceForm.productId));
const metrics = computed(() => [
  { label: '产品', value: summary.value.productCount || 0, icon: Boxes },
  { label: '设备', value: summary.value.deviceCount || 0, icon: Cpu },
  { label: '在线', value: summary.value.onlineDeviceCount || 0, icon: Radio },
  { label: '今日数据', value: summary.value.todayTelemetryCount || 0, icon: Gauge },
  { label: '未确认告警', value: summary.value.openAlarmCount || 0, icon: Bell },
]);

const curlExample = computed(() => `curl -X POST http://localhost:18080/api/ingest/http/${ingestForm.deviceKey}/telemetry \\
  -H "Content-Type: application/json" \\
  -H "X-Device-Secret: ${ingestForm.secret}" \\
  -d '${ingestForm.payload.replaceAll("'", "'\\''")}'`);

function switchView(view) {
  activeView.value = view;
  window.history.replaceState(null, '', `#${view}`);
  if (view === 'sensors' && !sensorQuery.productId && products.value.length) {
    sensorQuery.productId = products.value[0].id;
    loadSensors(1);
  }
  if (view === 'history' && !historyFilter.deviceId && devices.value.length) {
    ensureHistoryDefaults();
    loadHistorySensors();
  }
}

async function refreshAll() {
  await run(async () => {
    Object.assign(health, await api.health());
    summary.value = await api.summary();
    await Promise.all([loadProducts(), loadDevices(), loadRules(), loadAlarms()]);
    ensureSensorProduct();
    if (activeView.value === 'sensors') {
      await loadSensors(sensorQuery.page);
    }
    ensureHistoryDefaults();
    if (!selectedDevice.value && devices.value.length) {
      selectedDevice.value = devices.value[0];
    }
    await refreshLatestForDevices();
  });
}

async function loadProducts(page = productQuery.page) {
  productQuery.page = page;
  productPage.value = await api.products(productQuery);
}

async function loadDevices(page = deviceQuery.page) {
  deviceQuery.page = page;
  devicePage.value = await api.devices(deviceQuery);
  await refreshLatestForDevices();
}

async function loadSensors(page = sensorQuery.page) {
  ensureSensorProduct();
  sensorQuery.page = page;
  if (!sensorQuery.productId) {
    sensors.value = [];
    return;
  }
  sensors.value = await api.sensors(sensorQuery.productId);
}

async function loadRules(page = ruleQuery.page) {
  ruleQuery.page = page;
  rulePage.value = await api.rules({
    ...ruleQuery,
    enabled: ruleQuery.enabled === '' ? '' : ruleQuery.enabled === 'true',
  });
}

async function loadAlarms(page = alarmQuery.page) {
  alarmQuery.page = page;
  alarmPage.value = await api.alarms(alarmQuery);
}

function resetProducts() {
  Object.assign(productQuery, { keyword: '', category: '', page: 1, size: 10 });
  loadProducts(1);
}

function resetDevices() {
  Object.assign(deviceQuery, { keyword: '', productId: '', status: '', page: 1, size: 10 });
  loadDevices(1);
}

function resetSensors() {
  Object.assign(sensorQuery, { productId: products.value[0]?.id || '', keyword: '', page: 1, size: 10 });
  loadSensors(1);
}

function resetRules() {
  Object.assign(ruleQuery, { keyword: '', deviceKey: '', metric: '', enabled: '', page: 1, size: 10 });
  loadRules(1);
}

function resetAlarms() {
  Object.assign(alarmQuery, { keyword: '', status: '', severity: '', page: 1, size: 10 });
  loadAlarms(1);
}

async function refreshLatestForDevices() {
  for (const device of devices.value) {
    latestByDevice[device.id] = await api.latest(device.id);
  }
  if (selectedDevice.value) {
    latest.value = latestByDevice[selectedDevice.value.id] || [];
  }
}

function ensureSensorProduct() {
  if (!sensorQuery.productId && products.value.length) {
    sensorQuery.productId = products.value[0].id;
  }
}

function productName(productId) {
  return products.value.find((product) => product.id === productId)?.name || '-';
}

function openProductModal(product) {
  Object.assign(productForm, {
    id: product?.id || null,
    name: product?.name || '',
    code: product?.code || '',
    category: product?.category || 'sensor',
    protocolType: product?.protocolType || 'HTTP',
    protocolConfig: product?.protocolConfig || '{}',
    description: product?.description || '',
  });
  modal.value = { type: 'product', title: product ? '编辑产品' : '新建产品', eyebrow: 'Product' };
}

async function saveProduct() {
  await run(async () => {
    const payload = { ...productForm };
    if (productForm.id) {
      await api.updateProduct(productForm.id, payload);
      notify('产品已更新');
    } else {
      await api.createProduct(payload);
      notify('产品已创建');
    }
    closeModal();
    await refreshAll();
  });
}

async function openThingModel(product) {
  await run(async () => {
    productModelSensors.value = await api.sensors(product.id);
    modal.value = { type: 'thing-model', title: `${product.name} / 物模型视图`, eyebrow: 'Thing Model View', item: product };
  });
}

function openDeviceModal(device) {
  Object.assign(deviceForm, {
    id: device?.id || null,
    productId: device?.productId || products.value[0]?.id || '',
    name: device?.name || '',
    deviceKey: device?.deviceKey || '',
    secret: device?.secret || '',
    connectionConfig: device?.connectionConfig || '{}',
    location: device?.location || '',
  });
  modal.value = { type: 'device', title: device ? '编辑设备' : '新建设备', eyebrow: 'Device' };
}

async function saveDevice() {
  await run(async () => {
    const payload = { ...deviceForm };
    if (deviceForm.id) {
      await api.updateDevice(deviceForm.id, payload);
      notify('设备已更新');
    } else {
      await api.createDevice(payload);
      notify('设备已创建');
    }
    closeModal();
    await refreshAll();
  });
}

function openSensorModal(sensor) {
  ensureSensorProduct();
  Object.assign(sensorForm, {
    id: sensor?.id || null,
    productId: sensor?.productId || sensorQuery.productId || products.value[0]?.id || '',
    name: sensor?.name || '',
    sensorKey: sensor?.sensorKey || '',
    sensorType: sensor?.sensorType || 'number',
    unit: sensor?.unit || '',
    description: sensor?.description || '',
  });
  modal.value = { type: 'sensor', title: sensor ? '编辑传感器' : '新建传感器', eyebrow: 'Sensor Definition' };
}

async function saveSensor() {
  await run(async () => {
    const payload = { ...sensorForm };
    if (sensorForm.id) {
      await api.updateSensor(sensorForm.id, payload);
      notify('传感器已更新');
    } else {
      await api.createSensor(sensorForm.productId, payload);
      notify('传感器已创建');
    }
    sensorQuery.productId = sensorForm.productId;
    closeModal();
    await loadSensors(1);
    await refreshLatestForDevices();
  });
}

async function deleteSensor(id) {
  await run(async () => {
    await api.deleteSensor(id);
    notify('传感器已删除');
    await loadSensors(sensorQuery.page);
    await refreshLatestForDevices();
  });
}

async function openDeviceStatus(device) {
  selectedDevice.value = device;
  latest.value = await api.latest(device.id);
  modal.value = { type: 'device-status', title: `${device.name} / 最新状态`, eyebrow: 'Realtime State', item: device };
}

function openRuleModal(rule) {
  Object.assign(ruleForm, {
    id: rule?.id || null,
    name: rule?.name || '',
    deviceKey: rule?.deviceKey || '',
    metric: rule?.metric || 'temperature',
    operator: rule?.operator || '>',
    threshold: rule?.threshold || 35,
    severity: rule?.severity || 'warning',
    enabled: rule?.enabled ?? true,
    description: rule?.description || '',
  });
  modal.value = { type: 'rule', title: rule ? '编辑规则' : '新建规则', eyebrow: 'Rule' };
}

async function saveRule() {
  await run(async () => {
    const payload = { ...ruleForm };
    if (ruleForm.id) {
      await api.updateRule(ruleForm.id, payload);
      notify('规则已更新');
    } else {
      await api.createRule(payload);
      notify('规则已创建');
    }
    closeModal();
    await refreshAll();
  });
}

async function toggleRule(rule) {
  await run(async () => {
    if (rule.enabled) {
      await api.disableRule(rule.id);
    } else {
      await api.enableRule(rule.id);
    }
    await refreshAll();
  });
}

async function loadHistorySensors() {
  historyFilter.metric = '';
  historyRows.value = [];
  const device = devices.value.find((item) => item.id === historyFilter.deviceId);
  if (device && !historyFilter.productId) {
    historyFilter.productId = device.productId;
  }
  if (historyFilter.productId) {
    historySensors.value = await api.sensors(historyFilter.productId);
  } else {
    historySensors.value = [];
  }
}

function onHistoryProductChange() {
  const firstDevice = historyDevices.value[0];
  historyFilter.deviceId = firstDevice?.id || '';
  loadHistorySensors();
}

function ensureHistoryDefaults() {
  if (!historyFilter.productId && products.value.length) {
    historyFilter.productId = products.value[0].id;
  }
  if (!historyFilter.deviceId) {
    const firstDevice = historyDevices.value[0] || devices.value[0];
    historyFilter.deviceId = firstDevice?.id || '';
    if (firstDevice && !historyFilter.productId) {
      historyFilter.productId = firstDevice.productId;
    }
  }
}

function changeSensorPage(page) {
  sensorQuery.page = page;
}

async function queryHistory() {
  await run(async () => {
    if (!historyFilter.deviceId) {
      throw new Error('请选择设备');
    }
    historyRows.value = await api.telemetry(historyFilter.deviceId, {
      metric: historyFilter.metric,
      start: toApiTime(historyFilter.start),
      end: toApiTime(historyFilter.end),
      limit: 500,
    });
  });
}

function exportHistoryCsv() {
  const header = ['time', 'deviceId', 'metric', 'numericValue', 'textValue', 'valueType', 'quality'];
  const rows = historyRows.value.map((row) => header.map((key) => csvCell(row[key])).join(','));
  const blob = new Blob([[header.join(','), ...rows].join('\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `openlinkhub-telemetry-${Date.now()}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function openAlarmModal(alarm) {
  modal.value = { type: 'alarm', title: `告警 #${alarm.id}`, eyebrow: 'Alarm Detail', item: alarm };
}

async function ackAlarm(id) {
  await run(async () => {
    await api.ackAlarm(id);
    notify('告警已确认');
    closeModal();
    await refreshAll();
  });
}

function openIngestModal() {
  ingestForm.payload = JSON.stringify({
    timestamp: new Date().toISOString(),
    values: { temperature: 36.8, humidity: 62, voltage: 220.1, battery: 87 },
  }, null, 2);
  modal.value = { type: 'ingest', title: 'HTTP 上报测试', eyebrow: 'Ingest' };
}

async function sendDemoTelemetry() {
  const temp = Number((32 + Math.random() * 8).toFixed(1));
  await sendTelemetry('demo-device-001', 'demo-secret', {
    timestamp: new Date().toISOString(),
    values: {
      temperature: temp,
      humidity: Math.round(48 + Math.random() * 22),
      voltage: Number((218 + Math.random() * 5).toFixed(1)),
      battery: Math.round(72 + Math.random() * 22),
    },
  });
}

async function sendCustomTelemetry() {
  await sendTelemetry(ingestForm.deviceKey, ingestForm.secret, JSON.parse(ingestForm.payload));
  closeModal();
}

async function sendTelemetry(deviceKey, secret, payload) {
  await run(async () => {
    const result = await api.ingest(deviceKey, secret, payload);
    notify(`已接收 ${result.acceptedMetrics} 个指标，触发 ${result.alarms.length} 条告警`);
    await refreshAll();
  });
}

function closeModal() {
  modal.value = null;
}

async function run(task) {
  error.value = '';
  try {
    await task();
  } catch (exception) {
    error.value = exception.message;
  }
}

function notify(message) {
  toast.value = message;
  window.setTimeout(() => {
    toast.value = '';
  }, 2600);
}

function latestSummary(deviceId) {
  const values = latestByDevice[deviceId] || [];
  if (!values.length) return '-';
  return values.slice(0, 4).map((item) => `${item.sensorName || item.metric}: ${displayValue(item)}`).join(' / ');
}

function displayValue(item) {
  if (item.numericValue !== null && item.numericValue !== undefined) {
    return Number(item.numericValue).toLocaleString();
  }
  return item.textValue || item.rawValue || '-';
}

function formatTime(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
}

function formatDate(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function toApiTime(value) {
  return value ? new Date(value).toISOString() : '';
}

function formatJson(value) {
  if (!value) return '{}';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function csvCell(value) {
  if (value === null || value === undefined) return '';
  return `"${String(value).replaceAll('"', '""')}"`;
}

onMounted(async () => {
  await refreshAll();
  if (activeView.value === 'history' && devices.value.length) {
    ensureHistoryDefaults();
    await loadHistorySensors();
  }
  if (activeView.value === 'sensors') {
    await loadSensors();
  }
});

const Pagination = defineComponent({
  props: {
    page: { type: Number, required: true },
    size: { type: Number, required: true },
    total: { type: Number, required: true },
  },
  emits: ['change'],
  setup(props, { emit }) {
    return () => {
      const totalPages = Math.max(1, Math.ceil(props.total / props.size));
      return h('div', { class: 'pagination' }, [
        h('span', `共 ${props.total} 条，第 ${props.page} / ${totalPages} 页`),
        h('button', {
          class: 'ghost-button',
          disabled: props.page <= 1,
          onClick: () => emit('change', props.page - 1),
        }, '上一页'),
        h('button', {
          class: 'ghost-button',
          disabled: props.page >= totalPages,
          onClick: () => emit('change', props.page + 1),
        }, '下一页'),
      ]);
    };
  },
});
</script>
