package com.example.apptracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;

import java.util.List;

public class AppUsageAdapter extends ArrayAdapter<MainActivity.AppUsageInfo> {

    private final PackageManager packageManager;
    private final Context context;
    private final List<MainActivity.AppUsageInfo> appUsageInfoList;

    public AppUsageAdapter(Context context, List<MainActivity.AppUsageInfo> appUsageInfoList) {
        super(context, 0, appUsageInfoList);
        this.context = context;
        this.appUsageInfoList = appUsageInfoList;
        this.packageManager = context.getPackageManager();
    }

    @NonNull
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.app_usage_item, parent, false);
        }

        MainActivity.AppUsageInfo appUsageInfo = appUsageInfoList.get(position);

        // Get references to the views in app_usage_item.xml
        ImageView appIcon = convertView.findViewById(R.id.appIcon);
        TextView appNameTextView = convertView.findViewById(R.id.appName);
        TextView timeTextView = convertView.findViewById(R.id.appUsageTime);

        // Set the usage time in seconds
        long totalTimeInForeground = appUsageInfo.getTotalTimeInForeground() / 1000;
        timeTextView.setText("Time used: " + totalTimeInForeground + " secs");

        // Set the app icon
        try {

            Drawable icon = packageManager.getApplicationIcon(appUsageInfo.getPackageName());
            appIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_app_icon); // Fallback icon
        }

        // Set the app name
        String appName;
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(appUsageInfo.getPackageName(), PackageManager.GET_META_DATA);
            appName = packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = "Unknown App";
        }
        appNameTextView.setText(appName);



        return convertView;
    }
}



