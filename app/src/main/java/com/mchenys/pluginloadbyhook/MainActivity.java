package com.mchenys.pluginloadbyhook;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.mchenys.pluginloadbyhook.bean.PluginItem;
import com.mchenys.pluginloader.core.Constants;
import com.mchenys.pluginloader.core.LoadedPlugin;
import com.mchenys.pluginloader.core.PluginManager;
import com.mchenys.pluginloader.core.ResourcesManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private List<PluginItem> mData = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private TextView mNoPluginTextView;
    private static final String PLUGIN_HOME = "DynamicPlugin";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanOrLoadPlugin();
        } else {
            Toast.makeText(this, "请先授权读取存储卡", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        mNoPluginTextView = findViewById(R.id.no_plugin);
        mRecyclerView = findViewById(R.id.plugin_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new PluginAdapter());
    }

    // 启动插件Activity
    public void startPluginActivity(View view) {
        Intent intent = new Intent();
        intent.setClassName(this, "com.mchenys.plugina.MainActivity");
        startActivity(intent);
    }


    // 启动插件Fragment
    public void loadPluginFragment(View view) {
        Fragment fragment = PluginManager.getInstance().getPluginFragment("com.mchenys.plugina", "com.mchenys.plugina.AFragment");
        if (null == fragment) {
            Toast.makeText(this, "创建失败", Toast.LENGTH_SHORT).show();
            return;
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container, fragment)
                .commitAllowingStateLoss();

    }

    // 扫描+加载插件
    private void scanOrLoadPlugin() {
        mData.clear();
        File file = new File(Environment.getExternalStorageDirectory(), PLUGIN_HOME);
        File[] plugins = file.listFiles();
        if (plugins == null || plugins.length == 0) {
            // 加载内置插件
            mNoPluginTextView.setVisibility(View.VISIBLE);

            Map<String, LoadedPlugin> loadedPlugins = PluginManager.getInstance().getLoadedPlugins();
            if (loadedPlugins.size() > 0) {
                mNoPluginTextView.setText(R.string.find_inner_plugins);
                for (Map.Entry<String, LoadedPlugin> entry : loadedPlugins.entrySet()) {
                    PluginItem item = new PluginItem();
                    LoadedPlugin pluginPackage = entry.getValue();
                    item.packageInfo = pluginPackage.mPackageInfo;
                    item.launcherActivityName = pluginPackage.mLaunchIntent.getComponent().getClassName();
                    item.pluginPath = pluginPackage.mLocation;
                    mData.add(item);
                }
                mRecyclerView.getAdapter().notifyDataSetChanged();
            } else {
                mNoPluginTextView.setText(getString(R.string.no_plugin));
            }

        } else {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("开始扫码外置插件");
            progressDialog.show();
            mNoPluginTextView.setVisibility(View.VISIBLE);
            mNoPluginTextView.setText(getString(R.string.find_outer_plugins));

            new Thread(new Runnable() {

                @Override
                public void run() {
                    for (File plugin : plugins) {
                        try {
                            LoadedPlugin pluginPackage = PluginManager.getInstance().addPlugin(plugin);
                            PluginItem item = new PluginItem();
                            item.packageInfo = pluginPackage.mPackageInfo;
                            item.launcherActivityName = pluginPackage.mLaunchIntent.getComponent().getClassName();
                            item.pluginPath = pluginPackage.mLocation;
                            mData.add(item);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            progressDialog.dismiss();
                        }
                    });
                }
            }).start();
        }

    }

    public void scanOrLoadPlugin(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 110);
                return;
            }
        }
        scanOrLoadPlugin();
    }

    public void goPluginServiceManager(View view) {
        // startActivity(new Intent(this, ServiceDemoActivity.class));
    }

    public void goPluginProviderManager(View view) {
        // startActivity(new Intent(this, ProviderDemoActivity.class));
    }

    public void GoPluginFragmentManager(View view) {
        //  startActivity(new Intent(this,FragmentDemoActivity.class));
    }


    class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item_plugin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView appIcon;
            private TextView appName, apkName, packageName, appVersion, launchName;
            private Button uninstallBtn;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                appIcon = (ImageView) itemView.findViewById(R.id.app_icon);
                appName = (TextView) itemView.findViewById(R.id.app_name);
                appVersion = (TextView) itemView.findViewById(R.id.app_version);
                apkName = (TextView) itemView.findViewById(R.id.apk_name);
                packageName = (TextView) itemView.findViewById(R.id.package_name);
                launchName = (TextView) itemView.findViewById(R.id.launch_activity_name);
                uninstallBtn = itemView.findViewById(R.id.btn_uninstall);
            }

            public void bind(PluginItem item) {
                PackageInfo packageInfo = item.packageInfo;
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                PackageManager pm = getPackageManager();
                Drawable icon = pm.getApplicationIcon(appInfo);
                CharSequence label = pm.getApplicationLabel(appInfo);
                appIcon.setImageDrawable(icon);
                appName.setText("插件名称:" + label);
                apkName.setText("插件文件名:" + item.pluginPath.substring(item.pluginPath.lastIndexOf(File.separatorChar) + 1));
                appVersion.setText("插件版本:" + packageInfo.versionName);
                packageName.setText("插件包名:" + packageInfo.applicationInfo.packageName);
                final String launcherActivityName = item.launcherActivityName;
                launchName.setText("插件启动类:" + launcherActivityName);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClassName(MainActivity.this, launcherActivityName);
                        startActivity(intent);
                    }
                });
                uninstallBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean result = PluginManager.getInstance().uninstallPlugin(item.packageInfo.packageName);
                        if (result) {
                            mData.remove(item);
                            mRecyclerView.getAdapter().notifyItemRemoved(getAdapterPosition());
                            Toast.makeText(MainActivity.this, "卸载成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "卸载失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}