package com.kad.kpermissions.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class AppUtils {

    private  Context mContext;

    private static AppUtils appUtils;

    private AppUtils(Context context){
        this.mContext = context;
    }

    public static AppUtils getInstance(Context context){
        if(appUtils == null){
            synchronized (AppUtils.class){
                if(appUtils == null){
                    appUtils = new AppUtils(context);
                }
            }
        }

        return  appUtils;
    }

    /**
     * 判断是否是主进程，如果是则返回true
     * 注意：除了主进程以外的其他进程，必须声明为主应用的私有进程（eg:xxx.yyy.zzz:processName）
     * @return
     */
    public boolean isMainLooper(){
        boolean isMain = true;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = activityManager.getRunningAppProcesses();
        if(processInfoList!=null&&processInfoList.size()>0){
            for(int i=0;i<processInfoList.size();i++){
                ActivityManager.RunningAppProcessInfo processInfo = processInfoList.get(i);
                if(processInfo.processName.contains(":")){
                    isMain =  false;
                    break;
                }
            }
        }
        return  isMain;
    }
}
