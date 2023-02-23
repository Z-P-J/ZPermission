package com.zpj.permission.demo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZPermission {

    public interface Observer {
        void onGranted(List<String> granted);

        void onDenied(List<String> deniedForever, List<String> denied);
    }

    public interface SimpleObserver {
        void onGranted();

        void onDenied();
    }

    public static abstract class Requester {


        private Set<String> mPermissions;

        private Observer mObserver;

        private Requester() {
            mPermissions = new LinkedHashSet<>();

        }

        public Requester setObserver(Observer observer) {
            mObserver = observer;
            return this;
        }

        public Requester setObserver(SimpleObserver observer) {
            if (observer != null) {
                mObserver = new Observer() {
                    @Override
                    public void onGranted(List<String> granted) {
                        observer.onGranted();
                    }

                    @Override
                    public void onDenied(List<String> deniedForever, List<String> denied) {
                        observer.onDenied();
                    }
                };
            }
            return this;
        }

        public final void request(String...permissions) {
            if (mObserver == null) {
                throw new RuntimeException("observer must be not null!");
            }
            request(mObserver, permissions);
        }

        abstract void request(Observer observer, String...permissions);

        ActivityResultLauncher<String[]> mLauncher;

//        private void requestPermission(ComponentActivity activity,
//                                       String permission,
//                                       Observer observer) {
//            mLauncher = activity.getActivityResultRegistry()
//                    .register(toString(), new ActivityResultContracts.RequestPermission(),
//                            new ActivityResultCallback<Boolean>() {
//                                @Override
//                                public void onActivityResult(Boolean result) {
//                                    if (mLauncher != null) {
//                                        mLauncher.unregister();
//                                    }
//                                    if (observer != null) {
//                                        List<String> resultList = Collections.singletonList(permission);
//                                        if (result) {
//                                            observer.onGranted(resultList);
//                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                                                && activity.shouldShowRequestPermissionRationale(permission)) {
//                                            observer.onDenied(Collections.emptyList(), resultList);
//                                        } else {
//                                            observer.onDenied(resultList, Collections.emptyList());
//                                        }
//                                    }
//                                }
//                            });
//            activity.getLifecycle().addObserver(new LifecycleEventObserver() {
//                @Override
//                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
//                    if (event == Lifecycle.Event.ON_DESTROY) {
//                        activity.getLifecycle().removeObserver(this);
//                        mLauncher.unregister();
//                    }
//                }
//            });
//            mLauncher.launch(permission);
//        }

        protected void requestPermissions(ComponentActivity activity, Observer observer, String...permissions) {
            mLauncher = activity.getActivityResultRegistry()
                    .register(toString(), new ActivityResultContracts.RequestMultiplePermissions(),
                            new ActivityResultCallback<Map<String, Boolean>>() {
                                @Override
                                public void onActivityResult(Map<String, Boolean> result) {
                                    List<String> granted = new ArrayList<>();
                                    List<String> deniedFor = new ArrayList<>();
                                    List<String> denied = new ArrayList<>();

                                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                                        if (entry.getValue()) {
                                            granted.add(entry.getKey());
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                                && activity.shouldShowRequestPermissionRationale(entry.getKey())) {
                                            denied.add(entry.getKey());
                                        } else {
                                            deniedFor.add(entry.getKey());
                                        }
                                    }

                                    if (!granted.isEmpty()) {
                                        observer.onGranted(granted);
                                    }
                                    if (!deniedFor.isEmpty() || !denied.isEmpty()) {
                                        observer.onDenied(deniedFor, denied);
                                    }
                                }
                            });
            activity.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        activity.getLifecycle().removeObserver(this);
                        mLauncher.unregister();
                    }
                }
            });
            if (permissions == null || permissions.length == 0) {
                permissions = getAppPermissions(activity);
            }
            mLauncher.launch(permissions);
        }


    }

    private static class ActivityRequester extends Requester {

        private final ComponentActivity mActivity;

        private ActivityRequester(ComponentActivity activity) {
            mActivity = activity;
        }

        @Override
        void request(Observer observer, String... permissions) {
            requestPermissions(mActivity, observer, permissions);
        }
    }

    private static class FragmentRequester extends Requester {

        private final Fragment mFragment;

        private FragmentRequester(Fragment fragment) {
            mFragment = fragment;
        }

        @Override
        void request(Observer observer, String... permissions) {
            if (mFragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                ComponentActivity activity = mFragment.requireActivity();
                requestPermissions(activity, observer, permissions);
            } else {
                mFragment.getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_CREATE) {
                            mFragment.getLifecycle().removeObserver(this);
                            ComponentActivity activity = mFragment.requireActivity();
                            requestPermissions(activity, observer, permissions);
                        }
                    }
                });
            }
        }
    }


    public static Requester with(Fragment fragment) {
        return new FragmentRequester(fragment);
    }

    public static Requester with(Context context) {

        if (context instanceof ComponentActivity) {
            return new ActivityRequester((ComponentActivity) context);
        }

        throw new RuntimeException("not support! context=" + context);
    }

    public static boolean isPermissionGranted(Context context, final String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(context, permission);
    }

    public static String[] getAppPermissions(Context context) {
        return getAppPermissions(context, context.getPackageName());
    }

    public static String[] getAppPermissions(Context context, final String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            String[] permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            if (permissions == null) {
                return new String[0];
            }
            return permissions;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

}
