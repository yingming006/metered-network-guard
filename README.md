```markdown
# 流量守护 (NetworkGuard)

基于 Android Device Owner（设备所有者）架构的系统级移动流量白名单管控工具。

通过 Android 原生企业级设备管理策略，仅允许白名单内的应用使用移动数据，其余所有应用（含后续新安装应用）全自动禁止使用移动流量，Wi-Fi 联网不受任何影响。

项目地址：https://github.com/yingming006/metered-network-guard

---

## 概述

NetworkGuard 基于 Android 原生企业级设备管理接口（`DevicePolicyManager`），在系统底层实现计费网络（移动流量）的严格隔离与管控。

配置完成后由系统底层策略自动执行，无需应用常驻后台，设备重启后策略依然保持生效，支持新安装应用的自动化静默管控。

---

## 核心特性

* 免 Root、免 VPN：基于 Android 原生 DPC 架构，不占用系统 VPN 槽位，不修改系统底层文件。
* 零功耗、无后台：管控规则直接下发至系统内核执行，应用本身无需常驻后台消耗资源。
* 规则持久化：配置完成后，设备重启、系统清理、电量优化均不影响策略生效。
* 新装应用自动管控：内置应用生命周期监听机制，新安装或更新的应用自动加入流量限制名单。
* 防卸载保护：具备 Device Owner 权限，系统自动禁用普通桌面的直接卸载，防止误操作。
* 零第三方依赖：纯原生 Android SDK 开发，无任何第三方依赖库，体积轻量、安全透明。

---

## 工作机制

```text
               ┌───────────────────────────────┐
               │ 设备开机 / 应用安装 / 策略更新 │
               └───────────────┬───────────────┘
                               │ (系统广播触发)
                               ▼
               ┌───────────────────────────────┐
               │     AppLifecycleReceiver      │
               └───────────────┬───────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
     【 白名单应用 】                      【 其余所有应用 / 新装应用 】
   (如: 关键通信组件)                          (静默加入限制列表)
            │                                     │
            │ 放行流量                            ▼
            │               DevicePolicyManager.setMeteredDataDisabledPackages()
            │                                     │
            └──────────────────┬──────────────────┘
                               ▼
               ┌───────────────────────────────┐
               │   Android 底层内核直接拦截     │
               │   (移动流量断开 / Wi-Fi 正常)  │
               └───────────────────────────────┘
```

---

## 快速上手与激活

注意：本工具仅需在首次配置时连接电脑完成一次性激活，激活后即可脱离电脑独立运行。

### 1. 安装应用

### 2. 前置准备
为了顺利将应用设置为设备所有者（Device Owner），请确保：
1. 设备已开启「开发者选项」与「USB 调试」。
2. 进入系统「设置」->「账号」，临时退出/移除设备内已登录的所有账号（如 Google 账号、厂商账号等），激活完成后可重新登录。
3. 确保设备未开启多用户模式、访客模式或应用双开分身。

### 3. 执行激活命令
将设备通过 USB 数据线连接至电脑，在终端中执行以下命令：

```bash
adb shell dpm set-device-owner com.guard.networkcontrol/.receiver.AdminReceiver
```

当控制台输出 `Success: Device owner set to ...` 时，代表权限激活成功。

### 4. 策略配置
1. 打开应用。
2. 在应用列表中勾选允许使用移动数据的白名单应用。
3. 点击「保存并应用」。
4. 断开电脑连接，配置完成。

---

## 解除与卸载

如需取消管控或卸载本软件，连接电脑执行以下命令即可：

```bash
# 1. 移除设备所有者权限
adb shell dpm remove-active-admin com.guard.networkcontrol/.receiver.AdminReceiver

# 2. 卸载应用
adb uninstall com.guard.networkcontrol
```

---

## 常见问题 (FAQ)

### Q: 激活时提示 `Trying to set device owner but device already has accounts`？
这是 Android 系统的安全限制。请前往系统的「设置 -> 账号与同步」，将所有登录的账号临时退出，然后再执行激活命令。激活成功后可重新登录账号。

### Q: 激活时提示 `Not allowed to set the device owner because which there are already some users`？
说明设备开启了「多用户」、「访客模式」或「应用双开/分身」。请在系统设置中关闭应用分身并删除多余用户后重试。

### Q: 被限制移动流量的应用，连接 Wi-Fi 后能否正常联网？
可以正常联网。该方案调用的是系统的 Metered Data（计费网络）限制策略，仅在蜂窝移动数据网络下切断连接，Wi-Fi 流量不受任何影响。

---

## 源码结构与构建

```text
app/src/main/java/com/guard/networkcontrol/
├── core/         # 核心策略引擎、持久化存储 (ConfigStore, PolicyManager)
├── receiver/     # 系统广播监听 (AdminReceiver, AppLifecycleReceiver, BootReceiver)
└── ui/           # 控制面板与应用列表界面 (MainActivity, AppListAdapter)
```

### 构建环境要求
* JDK: 17+
* Android SDK: API Level 35 (Build-tools 35.0.0)
* minSdk: 28 (Android 9.0+)

### 编译命令
```bash
# 克隆仓库
git clone https://github.com/yingming006/metered-network-guard.git
cd metered-network-guard

# 编译生成 Release APK
./gradlew :app:assembleRelease
```

---

## 授权协议

本项目基于 [MIT License](LICENSE) 开源。
```