package com.zpj.permission;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ZPermission {

    public static Requester with(Context context) {
        if (context instanceof ComponentActivity) {
            return new ActivityRequester((ComponentActivity) context);
        } else {
            return new ContextRequester(context);
        }

//        throw new RuntimeException("not support! context=" + context);
    }

    public static Requester with(Fragment fragment) {
        return new FragmentRequester(fragment);
    }

    public static boolean isPermissionGranted(Context context, @PermissionGroup int type) {
        String[] group = PermissionConstants.getPermissions(type);
        return isPermissionGranted(context, group);
    }

    public static boolean isPermissionGranted(Context context, String...permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (!isPermissionGranted(context, permission)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isPermissionGranted(Context context, final String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(context, permission);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isGrantedWriteSettings(Context context) {
        return Settings.System.canWrite(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isGrantedDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (aom == null) return false;
            int mode = aom.checkOpNoThrow(
                    "android:system_alert_window",
                    android.os.Process.myUid(),
                    context.getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
        }
        return Settings.canDrawOverlays(context);
    }

    public static List<String> getAppPermissions(Context context) {
        return getAppPermissions(context, context.getPackageName());
    }

    public static List<String> getAppPermissions(Context context, final String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            String[] permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            if (permissions == null || permissions.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(permissions);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(ZPermission.class.getSimpleName(), "getAppPermissions failed!", e);
            return Collections.emptyList();
        }
    }

    private static boolean isIntentAvailable(Context context, final Intent intent) {
        try {
            return context
                    .getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .size() > 0;
        } catch (Exception e) {
            Log.e(ZPermission.class.getSimpleName(), "isIntentAvailable failed!", e);
            return false;
        }
    }



    public interface Observer {
        void onGranted(List<String> granted);

        void onDenied(List<String> deniedForever, List<String> denied);
    }

    public interface SimpleObserver {
        void onGranted();

        void onDenied();
    }

    public static abstract class Requester {

        interface PermissionCallback {
            void onResult(List<String> granted, List<String> deniedForever, List<String> denied);
        }

        private ActivityResultLauncher<String[]> mLauncher;

        private ActivityResultLauncher<Intent> mIntentLauncher;

        private Requester() {

        }

        public final void requestPermissions(@PermissionGroup int type) {
            requestPermissions(PermissionConstants.getPermissions(type));
        }

        public void requestPermissions(String... permissions) {
            requestPermissions((PermissionCallback) null, permissions);
        }

        public final void requestPermissions(SimpleObserver observer, @PermissionGroup int type) {
            requestPermissions(observer, PermissionConstants.getPermissions(type));
        }

        public final void requestPermissions(SimpleObserver observer, String... permissions) {
            requestPermissions(new PermissionCallback() {
                @Override
                public void onResult(List<String> granted, List<String> deniedForever, List<String> denied) {
                    if (observer == null) {
                        return;
                    }
                    if (denied.isEmpty() && deniedForever.isEmpty()) {
                        observer.onGranted();
                    } else {
                        observer.onDenied();
                    }
                }
            }, permissions);
        }

        public final void requestPermissions(Observer observer, @PermissionGroup int type) {
            requestPermissions(observer, PermissionConstants.getPermissions(type));
        }

        public final void requestPermissions(Observer observer, String... permissions) {
            requestPermissions(new PermissionCallback() {
                @Override
                public void onResult(List<String> granted, List<String> deniedForever, List<String> denied) {
                    if (observer == null) {
                        return;
                    }
                    if (!granted.isEmpty()) {
                        observer.onGranted(granted);
                    }
                    if (!deniedForever.isEmpty() || !denied.isEmpty()) {
                        observer.onDenied(deniedForever, denied);
                    }
                }
            }, permissions);
        }

        protected void requestPermissions(ComponentActivity activity, @Nullable PermissionCallback callback, String... permissions) {
            LifecycleEventObserver lifecycleEventObserver = new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        activity.getLifecycle().removeObserver(this);
                        if (mLauncher != null) {
                            mLauncher.unregister();
                            mLauncher = null;
                        }
                    }
                }
            };
            activity.getLifecycle().addObserver(lifecycleEventObserver);

            mLauncher = activity.getActivityResultRegistry()
                    .register(toString(), new ActivityResultContracts.RequestMultiplePermissions(),
                            result -> {
                                activity.getLifecycle().removeObserver(lifecycleEventObserver);
                                if (mLauncher != null) {
                                    mLauncher.unregister();
                                    mLauncher = null;
                                }

                                if (callback == null) {
                                    return;
                                }

                                List<String> granted = new ArrayList<>();
                                List<String> deniedForever = new ArrayList<>();
                                List<String> denied = new ArrayList<>();

                                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                                    if (entry.getValue()) {
                                        granted.add(entry.getKey());
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                            && activity.shouldShowRequestPermissionRationale(entry.getKey())) {
                                        denied.add(entry.getKey());
                                    } else {
                                        deniedForever.add(entry.getKey());
                                    }
                                }

                                callback.onResult(granted, deniedForever, denied);
                            });

            if (permissions == null || permissions.length == 0) {
                List<String> appPermissions = getAppPermissions(activity);
                if (appPermissions.isEmpty()) {
                    permissions = new String[0];
                } else {

                    List<String> availablePermissions = new ArrayList<>();
                    List<String> allPermissions = PermissionConstants.getAllPermissions();

                    for (String permission : appPermissions) {
                        if (allPermissions.contains(permission)) {
                            availablePermissions.add(permission);
                        }
                    }

                    permissions = availablePermissions.toArray(new String[0]);
                }
            }
            if (permissions.length == 0) {
                activity.getLifecycle().removeObserver(lifecycleEventObserver);
                mLauncher.unregister();
                mLauncher = null;
                if (callback != null) {
                    callback.onResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                }
                return;
            }
            mLauncher.launch(permissions);
        }

        abstract void requestPermissions(PermissionCallback callback, String... permissions);

        protected void startActivityResult(ComponentActivity activity, Intent intent, ActivityResultCallback<ActivityResult> callback) {

            LifecycleEventObserver observer = new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        activity.getLifecycle().removeObserver(this);
                        if (mIntentLauncher != null) {
                            mIntentLauncher.unregister();
                            mIntentLauncher = null;
                        }
                    }
                }
            };
            activity.getLifecycle().addObserver(observer);

            mIntentLauncher = activity.getActivityResultRegistry()
                    .register(toString(), new ActivityResultContracts.StartActivityForResult(), result -> {
                        activity.getLifecycle().removeObserver(observer);
                        if (mIntentLauncher != null) {
                            mIntentLauncher.unregister();
                            mIntentLauncher = null;
                        }
                        if (callback != null) {
                            callback.onActivityResult(result);
                        }
                    });

            mIntentLauncher.launch(intent);
        }

        public abstract void requestWriteSettings(SimpleObserver observer);

        @RequiresApi(api = Build.VERSION_CODES.M)
        void requestWriteSettings(Context context, final SimpleObserver observer) {
            if (isGrantedWriteSettings(context)) {
                if (observer != null) {
                    observer.onGranted();
                }
                return;
            }


            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (!isIntentAvailable(context, intent)) {
                requestAppDetailsSettings(context);
                return;
            }

            if (context instanceof ComponentActivity && observer != null) {
                startActivityResult((ComponentActivity) context, intent, result -> {
                    if (isGrantedWriteSettings(context)) {
                        observer.onGranted();
                    } else {
                        observer.onDenied();
                    }
                });
            } else {
                context.startActivity(intent);
            }
        }

        public abstract void requestDrawOverlays(final SimpleObserver observer);

        @RequiresApi(api = Build.VERSION_CODES.M)
        void requestDrawOverlays(Context context, final SimpleObserver observer) {
            if (isGrantedDrawOverlays(context)) {
                if (observer != null) {
                    observer.onGranted();
                }
                return;
            }

            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (!isIntentAvailable(context, intent)) {
                requestAppDetailsSettings(context);
                return;
            }
            if (context instanceof ComponentActivity && observer != null) {
                startActivityResult((ComponentActivity) context, intent, result -> {
                    if (isGrantedDrawOverlays(context)) {
                        observer.onGranted();
                    } else {
                        observer.onDenied();
                    }
                });
            } else {
                context.startActivity(intent);
            }
        }

        public abstract void requestAppDetailsSettings();

        public void requestAppDetailsSettings(Context context) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (!isIntentAvailable(context, intent)) {
                return;
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private static class ActivityRequester extends Requester {

        private final ComponentActivity mActivity;

        private ActivityRequester(ComponentActivity activity) {
            mActivity = activity;
        }

        @Override
        void requestPermissions(PermissionCallback callback, String... permissions) {
            requestPermissions(mActivity, callback, permissions);
        }

        @Override
        public void requestWriteSettings(SimpleObserver observer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestWriteSettings(mActivity, observer);
            } else {
                observer.onGranted();
            }
        }

        @Override
        public void requestDrawOverlays(SimpleObserver observer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestDrawOverlays(mActivity, observer);
            } else {
                observer.onGranted();
            }
        }

        @Override
        public void requestAppDetailsSettings() {
            requestAppDetailsSettings(mActivity);
        }

    }

    private static class FragmentRequester extends Requester {

        private final Fragment mFragment;

        private FragmentRequester(Fragment fragment) {
            mFragment = fragment;
        }

        @Override
        void requestPermissions(PermissionCallback callback, String... permissions) {
            run(activity -> requestPermissions(activity, callback, permissions));
        }

        @Override
        public void requestWriteSettings(SimpleObserver observer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                run(activity -> requestWriteSettings(activity, observer));
            } else {
                observer.onGranted();
            }
        }

        @Override
        public void requestDrawOverlays(SimpleObserver observer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                run(activity -> requestDrawOverlays(activity, observer));
            } else {
                observer.onGranted();
            }
        }

        @Override
        public void requestAppDetailsSettings() {
            run(this::requestAppDetailsSettings);
        }

        private void run(ActivityResultCallback<ComponentActivity> callback) {
            if (mFragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                ComponentActivity activity = mFragment.requireActivity();
                callback.onActivityResult(activity);
            } else {
                mFragment.getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_CREATE) {
                            mFragment.getLifecycle().removeObserver(this);
                            ComponentActivity activity = mFragment.requireActivity();
                            callback.onActivityResult(activity);
                        }
                    }
                });
            }
        }

    }

    private static class ContextRequester extends Requester {

        private static final SparseArray<ContextRequester> REQUESTER_SPARSE_ARRAY = new SparseArray<>();

        private final Context mContext;

        private ContextRequester(Context context) {
            mContext = context;

            REQUESTER_SPARSE_ARRAY.append(hashCode(), this);

            if (mContext instanceof LifecycleOwner) {
                ((LifecycleOwner) mContext).getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            ((LifecycleOwner) mContext).getLifecycle().removeObserver(this);
                            REQUESTER_SPARSE_ARRAY.remove(ContextRequester.this.hashCode());
                        }
                    }
                });
            }
        }

        private PermissionCallback mPermissionCallback;
        private String[] mPermissions;
        private SimpleObserver mObserver;

        @Override
        void requestPermissions(PermissionCallback callback, String... permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPermissionCallback = callback;
                mPermissions = permissions;
                PermissionActivity.start(mContext, hashCode(), PermissionActivity.TYPE_RUNTIME);
            } else {
                callback.onResult(permissions == null ? Collections.emptyList() : Arrays.asList(permissions),
                        Collections.emptyList(), Collections.emptyList());

            }
        }

        private void requestPermissionsInner(ComponentActivity activity) {
            requestPermissions(activity, mPermissionCallback, mPermissions);
        }

        @Override
        public void requestWriteSettings(SimpleObserver observer) {
            if (observer == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestWriteSettings(mContext, null);
                }
                onDestroy();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mObserver = observer;
                PermissionActivity.start(mContext, hashCode(), PermissionActivity.TYPE_WRITE_SETTINGS);
            } else {
                observer.onGranted();
                onDestroy();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void requestWriteSettingsInner(ComponentActivity activity) {
            requestWriteSettings(activity, new SimpleObserverDelegate(activity));
        }

        @Override
        public void requestDrawOverlays(SimpleObserver observer) {
            if (observer == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestDrawOverlays(mContext, null);
                }
                onDestroy();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mObserver = observer;
                PermissionActivity.start(mContext, hashCode(), PermissionActivity.TYPE_DRAW_OVERLAYS);
            } else {
                observer.onGranted();
                onDestroy();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void requestDrawOverlaysInner(ComponentActivity activity) {
            requestDrawOverlays(activity, new SimpleObserverDelegate(activity));
        }

        @Override
        public void requestAppDetailsSettings() {
            requestAppDetailsSettings(mContext);
        }

        private void onDestroy() {
            REQUESTER_SPARSE_ARRAY.remove(ContextRequester.this.hashCode());
        }

        private void initLifecycle(ComponentActivity activity) {
            activity.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        activity.getLifecycle().removeObserver(this);
                        onDestroy();
                    }
                }
            });
        }

        private class SimpleObserverDelegate implements SimpleObserver {

            private final ComponentActivity mActivity;

            private SimpleObserverDelegate(ComponentActivity activity) {
                mActivity = activity;
            }

            @Override
            public void onGranted() {
                if (mObserver != null) {
                    mObserver.onGranted();
                }
                mActivity.finish();
                onDestroy();
            }

            @Override
            public void onDenied() {
                if (mObserver != null) {
                    mObserver.onDenied();
                }
                mActivity.finish();
                onDestroy();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public static class PermissionActivity extends ComponentActivity {

            private static final String TYPE = "TYPE";
            private static final String REQUEST_CODE = "requestCode";
            public static final int TYPE_RUNTIME = 0x01;
            public static final int TYPE_WRITE_SETTINGS = 0x02;
            public static final int TYPE_DRAW_OVERLAYS = 0x03;

            public static void start(final Context context, int code, int type) {
                Intent starter = new Intent(context, PermissionActivity.class);
                starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                starter.putExtra(TYPE, type);
                starter.putExtra(REQUEST_CODE, code);
                context.startActivity(starter);
            }

            @Override
            protected void onCreate(@Nullable Bundle savedInstanceState) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                getWindow().getAttributes().alpha = 0;
                int byteExtra = getIntent().getIntExtra(TYPE, TYPE_RUNTIME);
                int requestCode = getIntent().getIntExtra(REQUEST_CODE, 0);

                ContextRequester contextRequester = REQUESTER_SPARSE_ARRAY.get(requestCode);
                if (contextRequester == null) {
                    finish();
                    return;
                }
                contextRequester.initLifecycle(this);
                if (byteExtra == TYPE_RUNTIME) {
                    super.onCreate(savedInstanceState);
                    contextRequester.requestPermissionsInner(this);
                } else if (byteExtra == TYPE_WRITE_SETTINGS) {
                    super.onCreate(savedInstanceState);
                    contextRequester.requestWriteSettingsInner(this);
                } else if (byteExtra == TYPE_DRAW_OVERLAYS) {
                    super.onCreate(savedInstanceState);
                    contextRequester.requestDrawOverlaysInner(this);
                }
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                finish();
                return true;
            }

        }

    }

}
