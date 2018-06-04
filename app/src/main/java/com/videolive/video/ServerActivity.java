package com.videolive.video;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.library.rpc.RpcSeverService;
import com.videolive.R;

import java.util.ArrayList;

/**
 * Created by wangyanjie on 18-3-30.
 */

public class ServerActivity extends Activity {

    private Button start;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        start = findViewById(R.id.start);
        start.setText(isWorked(RpcSeverService.class.getName())?"停止服务端":"启动服务端");
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start.getText().toString().equals("启动服务端")){
                    start.setText("停止服务端");
                    startService(new Intent(ServerActivity.this, RpcSeverService.class));
                }else{
                    start.setText("启动服务端");
                    stopService(new Intent(ServerActivity.this, RpcSeverService.class));
                }
            }
        });

    }
    private boolean isWorked(String className) {
        ActivityManager myManager = (ActivityManager) ServerActivity.this
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(className)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
