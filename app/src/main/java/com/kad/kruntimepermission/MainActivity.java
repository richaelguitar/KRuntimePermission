package com.kad.kruntimepermission;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kad.kpermissions.Action;
import com.kad.kpermissions.KPermission;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private PermissionSetting setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setting = new PermissionSetting(this);
        button = (Button) findViewById(R.id.btn_request_write);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KPermission.with(MainActivity.this)
                        .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onGranted(new Action() {
                            @Override
                            public void onAction(List<String> permissions) {
                                Toast.makeText(MainActivity.this,"授权成功",Toast.LENGTH_SHORT).show();
                            }
                        }).onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        if(KPermission.hasAlwaysDeniedPermission(MainActivity.this,permissions)){
                            setting.showSetting(permissions);
                        }
                    }
                }).start();
            }
        });

    }
}
