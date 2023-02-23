package com.zpj.permission.demo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.zpj.permission.demo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


//        ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
//                getActivityResultRegistry(), new ActivityResultCallback<Boolean>() {
//                    @Override
//                    public void onActivityResult(Boolean result) {
//
//
//
//                        if (result.equals(true)) {
//                            //权限获取到之后的动作
//                        } else {
//                            //权限没有获取到的动作
//                        }
//                    }
//                });


        ZPermission.with(this)
                .setObserver(new ZPermission.SimpleObserver() {
                    @Override
                    public void onGranted() {
                        Toast.makeText(MainActivity.this, "onGranted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDenied() {
                        Toast.makeText(MainActivity.this, "onDenied", Toast.LENGTH_SHORT).show();
                    }
                })
                .request();


    }

}