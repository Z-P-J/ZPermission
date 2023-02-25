package com.zpj.permission.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zpj.permission.ZPermission;
import com.zpj.permission.demo.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.btnStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TestDialogFragment dialogFragment = new TestDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "tag");


//                ZPermission.with(MainActivity.this)
//                        .requestPermissions(new ZPermission.Observer() {
//                            @Override
//                            public void onGranted(List<String> granted) {
//                                Toast.makeText(MainActivity.this, "onGranted " + granted, Toast.LENGTH_SHORT).show();
//                            }
//
//                            @Override
//                            public void onDenied(List<String> deniedForever, List<String> denied) {
//                                Toast.makeText(MainActivity.this, "onDenied " + denied + " forever=" + deniedForever, Toast.LENGTH_SHORT).show();
//                            }
//                        });
            }
        });

        binding.btnWriteSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 测试通过ApplicationContext获取权限
                ZPermission.with(MainActivity.this.getApplicationContext())
                        .requestWriteSettings(new ZPermission.SimpleObserver() {
                            @Override
                            public void onGranted() {
                                Toast.makeText(MainActivity.this, "requestWriteSettings granted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDenied() {
                                Toast.makeText(MainActivity.this, "requestWriteSettings denied", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZPermission.with(MainActivity.this)
                        .requestDrawOverlays(new ZPermission.SimpleObserver() {
                            @Override
                            public void onGranted() {
                                Toast.makeText(MainActivity.this, "requestDrawOverlays granted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDenied() {
                                Toast.makeText(MainActivity.this, "requestDrawOverlays denied", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZPermission.with(MainActivity.this).requestAppDetailsSettings();
            }
        });



    }



}