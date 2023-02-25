package com.zpj.permission;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
        PermissionGroup.CALENDAR, PermissionGroup.CAMERA, PermissionGroup.CONTACTS,
        PermissionGroup.LOCATION, PermissionGroup.MICROPHONE, PermissionGroup.PHONE,
        PermissionGroup.SENSORS, PermissionGroup.SMS, PermissionGroup.STORAGE,
        PermissionGroup.ACTIVITY_RECOGNITION
})
@Retention(RetentionPolicy.SOURCE)
public @interface PermissionGroup {

    int CALENDAR = 0;
    int CAMERA = 1;
    int CONTACTS = 2;
    int LOCATION = 3;
    int MICROPHONE = 4;
    int PHONE = 5;
    int SENSORS = 6;
    int SMS = 7;
    int STORAGE = 8;
    int ACTIVITY_RECOGNITION = 9;

}
