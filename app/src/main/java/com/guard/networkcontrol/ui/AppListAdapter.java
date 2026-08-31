package com.guard.networkcontrol.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.guard.networkcontrol.R;
import com.guard.networkcontrol.core.AppInfo;

import java.util.List;

/**
 * 应用列表适配器：每行一个「移动」开关 — 勾选＝允许移动数据，未勾选＝禁止。
 */
public class AppListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<AppInfo> items;
    private final OnCheckChangedListener listener;

    public interface OnCheckChangedListener {
        void onChanged();
    }

    public AppListAdapter(LayoutInflater inflater, List<AppInfo> items, OnCheckChangedListener listener) {
        this.inflater = inflater;
        this.items = items;
        this.listener = listener;
    }

    public List<AppInfo> getItems() { return items; }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app_row, parent, false);
            h = new ViewHolder();
            h.icon = convertView.findViewById(R.id.app_icon);
            h.name = convertView.findViewById(R.id.app_name);
            h.pkg = convertView.findViewById(R.id.app_pkg);
            h.mobile = convertView.findViewById(R.id.app_allow_mobile);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        final AppInfo info = items.get(position);
        h.name.setText(info.label);
        h.pkg.setText(info.packageName);
        h.icon.setImageDrawable(info.icon);

        h.mobile.setOnCheckedChangeListener(null);
        h.mobile.setChecked(info.allowMetered);
        h.mobile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            info.allowMetered = isChecked;
            if (listener != null) listener.onChanged();
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView pkg;
        CheckBox mobile;
    }
}