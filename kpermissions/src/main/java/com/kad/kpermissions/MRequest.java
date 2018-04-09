/*
 * Copyright © Yan Zhenjie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kad.kpermissions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.kad.kpermissions.checker.DoubleChecker;
import com.kad.kpermissions.checker.PermissionChecker;
import com.kad.kpermissions.checker.StandardChecker;
import com.kad.kpermissions.source.Source;
import com.kad.kpermissions.util.AppUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;


@RequiresApi(api = Build.VERSION_CODES.M)
public class MRequest implements Request, RequestExecutor {

    private static final PermissionChecker CHECKER = new StandardChecker();
    private static final PermissionChecker DOUBLE_CHECKER = new DoubleChecker();

    public static final String ACTION = MRequest.class.getSimpleName();


    private Source mSource;

    private String[] mPermissions;
    private Rationale mRationaleListener;
    private Action mGranted;
    private Action mDenied;

    private String[] mDeniedPermissions;

    private PermissionBroadcastReceiver permissionBroadcastReceiver;

    //获取当前进程名
    private AppUtils appUtils ;

    MRequest(Source source) {
        this.mSource = source;
        this.appUtils =  AppUtils.getInstance(mSource.getContext());
    }

    @NonNull
    @Override
    public Request permission(String... permissions) {
        this.mPermissions = permissions;
        return this;
    }

    @NonNull
    @Override
    public Request permission(String[]... groups) {
        List<String> permissionList = new ArrayList<>();
        for (String[] group : groups) {
            permissionList.addAll(Arrays.asList(group));
        }
        this.mPermissions = permissionList.toArray(new String[permissionList.size()]);
        return this;
    }


    @NonNull
    @Override
    public Request rationale(Rationale listener) {
        this.mRationaleListener = listener;
        return this;
    }

    @NonNull
    @Override
    public Request onGranted(Action granted) {
        this.mGranted = granted;
        return this;
    }

    @NonNull
    @Override
    public Request onDenied(Action denied) {
        this.mDenied = denied;
        return this;
    }

    @Override
    public void start() {
        List<String> deniedList = getDeniedPermissions(CHECKER, mSource, mPermissions);
        mDeniedPermissions = deniedList.toArray(new String[deniedList.size()]);
        if (mDeniedPermissions.length > 0) {
            List<String> rationaleList = getRationalePermissions(mSource, mDeniedPermissions);
            if (rationaleList.size() > 0 && mRationaleListener != null) {
                mRationaleListener.showRationale(mSource.getContext(), rationaleList, this);
            } else {
                execute();
            }
        } else {
            callbackSucceed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void execute() {

        if(!appUtils.isMainLooper()){
            permissionBroadcastReceiver = new PermissionBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION);
            mSource.getContext().registerReceiver(permissionBroadcastReceiver,filter);
        }

        PermissionActivity.requestPermission(mSource.getContext(), mDeniedPermissions, permissionListener);
    }

    private PermissionActivity.PermissionListener permissionListener = new PermissionActivity.PermissionListener() {
        @Override
        public void onRequestPermissionsResult(@NonNull String[] permissions) {
            List<String> deniedList = getDeniedPermissions(DOUBLE_CHECKER, mSource, permissions);
            if (deniedList.isEmpty()) {
                callbackSucceed();
            } else {
                callbackFailed(deniedList);
            }
        }
    };

    @Override
    public void cancel() {
        List<String> deniedList = getDeniedPermissions(DOUBLE_CHECKER, mSource, mDeniedPermissions);
        if (deniedList.isEmpty()) {
            callbackSucceed();
        } else {
            callbackFailed(deniedList);
        }
    }


    /**
     * Callback acceptance status.
     */
    private void callbackSucceed() {
        if (mGranted != null) {
            List<String> permissionList = asList(mPermissions);
            try {
                mGranted.onAction(permissionList);
            } catch (Exception e) {
                if (mDenied != null) {
                    mDenied.onAction(permissionList);
                }
            }
        }

        unregisterReceiver();
    }

    /**
     * Callback rejected state.
     */
    private void callbackFailed(@NonNull List<String> deniedList) {
        if (mDenied != null) {
            mDenied.onAction(deniedList);
        }
        unregisterReceiver();
    }

    /**
     * 反注册广播
     */
    private void unregisterReceiver(){
        if(!appUtils.isMainLooper()&&permissionBroadcastReceiver!=null){
            mSource.getContext().unregisterReceiver(permissionBroadcastReceiver);
            permissionBroadcastReceiver = null;
        }
    }

    /**
     * Get denied permissions.
     */
    private static List<String> getDeniedPermissions(PermissionChecker checker, @NonNull Source source, @NonNull String... permissions) {
        List<String> deniedList = new ArrayList<>(1);
        for (String permission : permissions) {
            if (!checker.hasPermission(source.getContext(), permission)) {
                deniedList.add(permission);
            }
        }
        return deniedList;
    }

    /**
     * Get permissions to show rationale.
     */
    private static List<String> getRationalePermissions(@NonNull Source source, @NonNull String... permissions) {
        List<String> rationaleList = new ArrayList<>(1);
        for (String permission : permissions) {
            if (source.isShowRationalePermission(permission)) {
                rationaleList.add(permission);
            }
        }
        return rationaleList;
    }

    private class PermissionBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
           String action = intent.getAction();
           if(ACTION.equalsIgnoreCase(action)){
               String[] permissions = intent.getStringArrayExtra(PermissionActivity.KEY_INPUT_PERMISSIONS);
               List<String> deniedList = getDeniedPermissions(DOUBLE_CHECKER, mSource, permissions);
               if (deniedList.isEmpty()) {
                   callbackSucceed();
               } else {
                   callbackFailed(deniedList);
               }
           }
        }
    }
}