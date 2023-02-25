package com.zpj.permission;


import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Description: copy from https://github.com/Blankj/AndroidUtilCode
 */
@SuppressLint("InlinedApi")
public final class PermissionConstants {

    private static final String[] GROUP_CALENDAR = {
            permission.READ_CALENDAR, permission.WRITE_CALENDAR
    };
    private static final String[] GROUP_CAMERA = {
            permission.CAMERA
    };
    private static final String[] GROUP_CONTACTS = {
            permission.READ_CONTACTS, permission.WRITE_CONTACTS, permission.GET_ACCOUNTS
    };
    private static final String[] GROUP_LOCATION = {
            permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION
    };
    private static final String[] GROUP_MICROPHONE = {
            permission.RECORD_AUDIO
    };
    private static final String[] GROUP_PHONE = {
            permission.READ_PHONE_STATE, permission.READ_PHONE_NUMBERS, permission.CALL_PHONE,
            permission.READ_CALL_LOG, permission.WRITE_CALL_LOG, permission.ADD_VOICEMAIL,
            permission.USE_SIP, permission.PROCESS_OUTGOING_CALLS, permission.ANSWER_PHONE_CALLS
    };
    private static final String[] GROUP_PHONE_BELOW_O = {
            permission.READ_PHONE_STATE, permission.READ_PHONE_NUMBERS, permission.CALL_PHONE,
            permission.READ_CALL_LOG, permission.WRITE_CALL_LOG, permission.ADD_VOICEMAIL,
            permission.USE_SIP, permission.PROCESS_OUTGOING_CALLS
    };
    private static final String[] GROUP_SENSORS = {
            permission.BODY_SENSORS
    };
    private static final String[] GROUP_SMS = {
            permission.SEND_SMS, permission.RECEIVE_SMS, permission.READ_SMS,
            permission.RECEIVE_WAP_PUSH, permission.RECEIVE_MMS,
    };
    private static final String[] GROUP_STORAGE = {
            permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final String[] GROUP_ACTIVITY_RECOGNITION = {
            permission.ACTIVITY_RECOGNITION,
    };

    public static String[] getPermissions(@PermissionGroup final int type) {
        switch (type) {
            case PermissionGroup.CALENDAR:
                return GROUP_CALENDAR;
            case PermissionGroup.CAMERA:
                return GROUP_CAMERA;
            case PermissionGroup.CONTACTS:
                return GROUP_CONTACTS;
            case PermissionGroup.LOCATION:
                return GROUP_LOCATION;
            case PermissionGroup.MICROPHONE:
                return GROUP_MICROPHONE;
            case PermissionGroup.PHONE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    return GROUP_PHONE_BELOW_O;
                } else {
                    return GROUP_PHONE;
                }
            case PermissionGroup.SENSORS:
                return GROUP_SENSORS;
            case PermissionGroup.SMS:
                return GROUP_SMS;
            case PermissionGroup.STORAGE:
                return GROUP_STORAGE;
            case PermissionGroup.ACTIVITY_RECOGNITION:
                return GROUP_ACTIVITY_RECOGNITION;
        }
        return new String[0];
    }

    public static String[] getPermissions(@PermissionGroup final int...types) {
        if (types != null) {
            List<String> list = new ArrayList<>();
            for (int type : types) {
                list.addAll(Arrays.asList(getPermissions(type)));
            }
            return list.toArray(new String[0]);
        }
        return new String[0];
    }

    static List<String> getAllPermissions() {
        return Arrays.asList(getPermissions(PermissionGroup.CALENDAR, PermissionGroup.CAMERA, PermissionGroup.CONTACTS,
                PermissionGroup.LOCATION, PermissionGroup.MICROPHONE, PermissionGroup.PHONE,
                PermissionGroup.SENSORS, PermissionGroup.SMS, PermissionGroup.STORAGE,
                PermissionGroup.ACTIVITY_RECOGNITION));
    }

}