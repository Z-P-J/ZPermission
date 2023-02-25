package com.zpj.permission.demo;

import android.app.Application;
import android.widget.Toast;

import com.zpj.permission.ZPermission;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 测试在Application中请求权限（不建议使用）
//        ZPermission.with(this).requestPermissions(new ZPermission.SimpleObserver() {
//            @Override
//            public void onGranted() {
//                Toast.makeText(App.this, "111111111111111", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onDenied() {
//                Toast.makeText(App.this, "222222222222222", Toast.LENGTH_SHORT).show();
//            }
//        });

    }
}
