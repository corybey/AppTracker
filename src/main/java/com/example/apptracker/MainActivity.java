package com.example.apptracker;

import android.os.Bundle;

import androidx.appcompat.widget.SearchView;
import android.app.AppOpsManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Handler;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private UsageStatsManager usageStatsManager;
    private ListView appUsageListView;
    private TextView timeTextView;
    private static final String PERMISSION_CHECK_DONE_KEY = "permission_check_done";
    private final Handler timeHandler = new Handler();
    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            // Update the time display
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            timeTextView.setText(currentTime);

            // Re-run this runnable in 1 second
            timeHandler.postDelayed(this, 1000);
        }
    };
    private static final String ACCESS_TOKEN = "sl.CAVXdHflkPIlFqp3xOV3UltOKQrOv0hmwyQZnt-TNgB6kZhjkLSTM9x1Dy2ngaf2iBH7Cx7H8fLnQMgTdGVAkJevBQNKEnV6P8yoOu8sJzTdA2dbIWQFKvPExVNxON4JFz26_JT_M_u6";

    private List<AppUsageInfo> appUsageInfoList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appUsageInfoList = new ArrayList<>();

        SearchView appUsageSearch = findViewById(R.id.appUsageSearch);
        timeTextView = findViewById(R.id.timeTextView);
        appUsageListView = findViewById(R.id.appUsageListView);
        Button syncButton = findViewById(R.id.syncButton);

        appUsageSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // No action needed when the query is submitted
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the list based on the query text
                filterAppUsageData(newText);
                return true;
            }
        });



            // Initialize UsageStatsManager
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);



        // Only request permission if it hasn't been checked before
        if (shouldCheckPermission() && !hasUsageStatsPermission()) {
            requestPermissionIfNecessary();
        } else {
            displayAppUsageData();
        }

            // Set up Dropbox sync
        syncButton.setOnClickListener(v -> syncDataWithDropbox());
        //Start time update handler
        timeHandler.post(timeRunnable);
    }

    private void filterAppUsageData(String query) {
        List<AppUsageInfo> filteredList = new ArrayList<>();

        // Loop through the app usage list and check if the app name matches the query
        for (AppUsageInfo appUsageInfo : appUsageInfoList) {
            if (appUsageInfo.getAppName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(appUsageInfo);
            }
        }

        // Update the adapter with the filtered list
        AppUsageAdapter adapter = new AppUsageAdapter(this, filteredList);
        appUsageListView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop updating the time when the activity is destroyed
        timeHandler.removeCallbacks(timeRunnable);
    }
    // Check if the permission check has been done before
    private boolean shouldCheckPermission() {
        SharedPreferences prefs =  getSharedPreferences("AppManaging", Context.MODE_PRIVATE);
        return !prefs.getBoolean(PERMISSION_CHECK_DONE_KEY, false);
    }

    private void requestPermissionIfNecessary() {
        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

            SharedPreferences prefs =  getSharedPreferences("AppManaging1", Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PERMISSION_CHECK_DONE_KEY, true).apply();
        }
    }
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    public static class AppUsageInfo {
        private final String packageName;
        private final String appName;
        private final long totalTimeInForeground;

        public AppUsageInfo(String packageName, String appName, long totalTimeInForeground) {
            this.packageName = packageName;
            this.appName = appName;
            this.totalTimeInForeground = totalTimeInForeground;
        }

        public String getPackageName(){
            return packageName;
        }
        public String getAppName() {
            return appName;
        }

        public long getTotalTimeInForeground() {
            return totalTimeInForeground;
        }

        @Override
        public String toString() {
            return appName + " - " + totalTimeInForeground / 1000 + " seconds";
        }
    }

    private void displayAppUsageData() {
        // Define time range (past day)
        Calendar calendar = Calendar.getInstance();
        long endTime = System.currentTimeMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = endTime - 1000 * 3600 * 24;

        // Fetch usage stats
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Log.d("AppUsage", "No usage stats data available.");
            return; // Exit if there's no data
        }

        // Process and display usage data
        for (UsageStats usageStats : usageStatsList) {
            long totalTimeInForeground = usageStats.getTotalTimeInForeground();
            if (totalTimeInForeground > -1 ) {
                String packageName = usageStats.getPackageName();
                String appName = getAppNameFromPackage(packageName); // Get app name
                try {
                    ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
                appUsageInfoList.add(new AppUsageInfo(packageName,appName, totalTimeInForeground));
            }
        }

        // Sort the usage stats list by total time in foreground (descending)
        Collections.sort(appUsageInfoList, new Comparator<AppUsageInfo>() {
            @Override
            public int compare(AppUsageInfo o1, AppUsageInfo o2) {
                return Long.compare(o2.getTotalTimeInForeground(), o1.getTotalTimeInForeground());
            }
        });
        Log.d("App Usage", "Sorting completed");
        if (appUsageInfoList.isEmpty()) {
            appUsageInfoList.add(new AppUsageInfo("","No app usage data available", 0));
        }
        // Populate listview
        AppUsageAdapter adapter = new AppUsageAdapter(this, appUsageInfoList);
        appUsageListView.setAdapter(adapter);
    }


    private String getAppNameFromPackage(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("AppUsageAdapter", "Unknown app or system app for package: " + packageName);
            return packageName; // Show package name if app name can't be found
        }
    }



    private void syncDataWithDropbox() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("AppUsageTracker").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        // Save app usage data to a file
        String filename = "app_usage_data.json";
        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write("{ 'sample': 'data' }".getBytes()); // Replace with real data
            FileInputStream fis = openFileInput(filename);

            // Upload file to Dropbox
            client.files().uploadBuilder("/" + filename).uploadAndFinish(fis);
            Log.d("DropboxSync", "Data synced with Dropbox at");
        } catch (DbxException | IOException e) {
            e.printStackTrace();
        }
    }
}
