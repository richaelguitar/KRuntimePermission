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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.kad.kpermissions.service.IPermissionService;

import java.io.Serializable;

@RequiresApi(api = Build.VERSION_CODES.M)
public final class PermissionActivity extends Activity {

    public static final String KEY_INPUT_PERMISSIONS = "KEY_INPUT_PERMISSIONS";

    private static PermissionListener sPermissionListener;

    private IPermissionListener iPermissionListener;

    private  String[] permissions;

    private boolean isBind;

    /**
     * Request for permissions.
     */
    public static void requestPermission(Context context, String[] permissions, PermissionListener permissionListener) {

        Intent intent = new Intent(context, PermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_INPUT_PERMISSIONS, permissions);
        sPermissionListener = permissionListener;
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invasionStatusBar(this);
        Intent intent = getIntent();
        permissions = intent.getStringArrayExtra(KEY_INPUT_PERMISSIONS);

        if (permissions != null ) {

            if(sPermissionListener != null){//同一个进程
                requestPermissions(permissions, 1);
            }else{//不同进程
                if(!isBind&&iPermissionListener==null){
                    Intent serviceIntent = new Intent(this, IPermissionService.class);
                    bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
                }
            }
        }else{
            finish();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iPermissionListener = IPermissionListener.Stub.asInterface(service);
            requestPermissions(permissions, 1);
            isBind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iPermissionListener = null;
            isBind = false;
            finish();
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (sPermissionListener != null) {//同一个进程
            sPermissionListener.onRequestPermissionsResult(permissions);
        }else if(iPermissionListener !=null){//不同进程使用AIDL
            try {
                iPermissionListener.onRequestPermissionsResult(permissions);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sPermissionListener = null;
        if(serviceConnection!=null&&isBind){
            unbindService(serviceConnection);
        }
        isBind = false;
        iPermissionListener = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * permission callback.
     */
    interface PermissionListener  {
        void onRequestPermissionsResult(@NonNull String[] permissions);
    }

    /**
     * Set the content layout full the StatusBar, but do not hide StatusBar.
     */
    private static void invasionStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
