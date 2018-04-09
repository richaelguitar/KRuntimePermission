package com.kad.kpermissions.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.kad.kpermissions.IPermissionListener;
import com.kad.kpermissions.MRequest;
import com.kad.kpermissions.PermissionActivity;

public class IPermissionService extends Service {
    public IPermissionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return stub.asBinder();
    }

    private IPermissionListener stub = new IPermissionListener.Stub() {
        @Override
        public void onRequestPermissionsResult(String[] permissions) throws RemoteException {
            Intent intent = new Intent(MRequest.ACTION);
            intent.putExtra(PermissionActivity.KEY_INPUT_PERMISSIONS,permissions);
            sendBroadcast(intent);
        }
    };
}
