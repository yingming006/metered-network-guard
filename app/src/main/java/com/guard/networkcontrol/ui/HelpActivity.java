package com.guard.networkcontrol.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.guard.networkcontrol.R;

/** 应用内置帮助文档。 */
public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Button back = findViewById(R.id.btn_help_back);
        back.setOnClickListener(v -> finish());

        TextView text = findViewById(R.id.help_text);
        text.setText(buildHelpText());
    }

    private String buildHelpText() {
        String[] lines = {
        "【本工具解决什么】",
        "免 Root：让设备上除了白名单以外的应用无法使用流量（手机卡流量），",
        "避免后台偷偷跑流量。",
        "",
        "【第一步：一次性激活（仅首次需要）】",
        "1. 安装本应用；",
        "2. 手机连电脑并开启 USB 调试；",
        "3. 退出设备上所有账号（手机厂商账号、Google 等），确保只有一个用户；",
        "4. 电脑执行激活命令：",
        "   adb shell dpm set-device-owner com.guard.networkcontrol/.receiver.AdminReceiver",
        "5. 主界面点「重新检测」，显示「Device Owner 已激活」即成功。",
        "",
        "【白名单怎么用】",
        "每个应用只有一个开关：",
        "· 勾选＝允许使用流量；未勾选（含以后新装）＝禁止使用流量。",
        "调整完点「保存并应用」，立即生效并长期保存。",
        "",
        "【重启 / 升级 / 新装】",
        "策略保存在设备系统中，重启手机依然生效；应用升级后自动重新同步；",
        "新安装的应用如果不在白名单，会自动进入禁止使用流量名单。",
        "",
        "【多开 / 双开应用（多用户）】",
        "· 双开（分身）应用运行在独立的用户空间（Android 多用户），与主空间相互独立；",
        "· 本工具只能管控【当前用户空间】（你的桌面主空间）里的应用，双开应用的流量无法被本工具管控；",
        "· 如需约束，请在手机的「多开/双开」设置里关闭该应用的多开，",
        "· 使其回到主空间后，再交由本工具统一管控。",
        "",
        "【如需转移/停止使用】",
        "1. 连接电脑执行：",
        "   adb shell dpm remove-active-admin com.guard.networkcontrol/.receiver.AdminReceiver",
        "2. 之后即可在应用信息页正常卸载本应用。",
        "",
        "【适用范围】",
        "· 需要 Android 9.0 及以上；",
        "· 不同厂商对即时生效时机略有差异，重启或网络切换后会自动再次同步；",
        "· 本工具不收集任何数据，也不接入外部网络。",
        };
        StringBuilder sb = new StringBuilder();
        for (String line : lines) sb.append(line).append("\n");
        return sb.toString();
    }
}
