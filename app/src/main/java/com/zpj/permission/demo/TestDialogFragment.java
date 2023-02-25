package com.zpj.permission.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.zpj.permission.ZPermission;

public class TestDialogFragment extends DialogFragment {

    public TestDialogFragment() {
        // 测试在Fragment中请求权限
        ZPermission.with(this)
                .requestPermissions(new ZPermission.SimpleObserver() {
                    @Override
                    public void onGranted() {
                        Toast.makeText(getContext(), "TestDialogFragment 111111", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDenied() {
                        Toast.makeText(getContext(), "TestDialogFragment 222222", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = new View(getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1000));
        return view;
    }
}
