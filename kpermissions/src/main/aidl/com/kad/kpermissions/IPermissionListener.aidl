// IPermissionListener.aidl
package com.kad.kpermissions;

// Declare any non-default types here with import statements

interface IPermissionListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onRequestPermissionsResult(in String[] permissions);
}
