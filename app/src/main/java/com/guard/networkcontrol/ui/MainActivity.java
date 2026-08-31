package com.guard.networkcontrol.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.guard.networkcontrol.R;
import com.guard.networkcontrol.core.AppInfo;
import com.guard.networkcontrol.core.ConfigStore;
import com.guard.networkcontrol.core.PolicyManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 主界面：状态卡 + 应用白名单列表 + 搜索筛选 + 帮助入口。 */
public class MainActivity extends Activity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_ALLOWED = 1;
    private static final int FILTER_DENIED = 2;

    private TextView statusText;
    private TextView statsText;
    private ListView appList;
    private EditText searchInput;
    private Spinner filterSpinner;

    private AppListAdapter adapter;
    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> shown = new ArrayList<>();
    private int filterMode = FILTER_ALL;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        statsText = findViewById(R.id.stats_text);
        appList = findViewById(R.id.app_list);
        searchInput = findViewById(R.id.search_input);
        filterSpinner = findViewById(R.id.filter_spinner);

        Button checkOwner = findViewById(R.id.btn_check_owner);
        Button allowAll = findViewById(R.id.btn_allow_all);
        Button disallowAll = findViewById(R.id.btn_disallow_all);
        Button apply = findViewById(R.id.btn_apply);
        Button help = findViewById(R.id.btn_help);

        adapter = new AppListAdapter(getLayoutInflater(), shown, this::onCheckChanged);
        appList.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterMode = position;
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                query = s == null ? "" : s.toString().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        checkOwner.setOnClickListener(v -> refreshAll());
        allowAll.setOnClickListener(v -> {
            for (AppInfo a : allApps) a.allowMetered = true;
            applyFilter();
            updateStats();
        });
        disallowAll.setOnClickListener(v -> {
            for (AppInfo a : allApps) {
                if (!a.packageName.equals(getPackageName())) a.allowMetered = false;
            }
            applyFilter();
            updateStats();
        });
        apply.setOnClickListener(v -> applyPolicy());
        help.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HelpActivity.class)));

        refreshAll();
    }

    private void onCheckChanged() {
        updateStats();
        applyFilter();
    }

    private void refreshAll() {
        boolean owner = PolicyManager.isDeviceOwner(this);
        statusText.setText(owner ? R.string.status_owner_ok : R.string.status_owner_no);
        statusText.setTextColor(getColor(owner ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        loadInstalledApps();
        updateStats();
        applyFilter();
    }

    private void loadInstalledApps() {
        allApps.clear();
        Set<String> wl = ConfigStore.loadWhitelist(this);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed;
        try {
            installed = pm.getInstalledApplications(0);
        } catch (Exception e) {
            installed = new ArrayList<>();
        }
        for (ApplicationInfo ai : installed) {
            if (ai.packageName == null) continue;
            CharSequence label = ai.loadLabel(pm);
            String name = TextUtils.isEmpty(label) ? ai.packageName : label.toString();
            AppInfo info = new AppInfo(ai.packageName, name, wl.contains(ai.packageName));
            info.icon = ai.loadIcon(pm);
            allApps.add(info);
        }
    }

    private void applyFilter() {
        shown.clear();
        for (AppInfo a : allApps) {
            if (filterMode == FILTER_ALLOWED && !a.allowMetered) continue;
            if (filterMode == FILTER_DENIED && a.allowMetered) continue;
            if (!query.isEmpty()) {
                String q = query.toLowerCase(Locale.ROOT);
                String label = a.label == null ? "" : a.label.toLowerCase(Locale.ROOT);
                String pkg = a.packageName.toLowerCase(Locale.ROOT);
                if (!label.contains(q) && !pkg.contains(q)) continue;
            }
            shown.add(a);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateStats() {
        int allow = 0;
        for (AppInfo a : allApps) if (a.allowMetered) allow++;
        int denied = allApps.size() - allow;
        statsText.setText(getString(R.string.stats_line, denied, allow, allApps.size()));
    }

    private void applyPolicy() {
        if (!PolicyManager.isDeviceOwner(this)) {
            Toast.makeText(this, R.string.toast_failed_not_owner, Toast.LENGTH_LONG).show();
            return;
        }
        Set<String> whitelist = new HashSet<>();
        for (AppInfo a : allApps) {
            if (a.allowMetered || a.packageName.equals(getPackageName())) whitelist.add(a.packageName);
        }
        ConfigStore.saveWhitelist(this, whitelist);
        PolicyManager.ApplyResult res = PolicyManager.applyPolicy(this, whitelist);
        if (res != null && res.success) {
            Toast.makeText(this, R.string.toast_applied, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.toast_failed_not_owner, Toast.LENGTH_LONG).show();
        }
        updateStats();
    }
}