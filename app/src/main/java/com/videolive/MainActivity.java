package com.videolive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.library.common.box.Box;
import com.library.common.box.BoxReader;
import com.library.common.box.BoxWriter;
import com.library.live.file.WriteMp4;
import com.library.rpc.Config;
import com.library.util.ByteUtil;
import com.library.util.OtherUtil;
import com.library.util.mLog;
import com.videolive.video.ClientActivity;
import com.videolive.video.DecodeToMp4Activity;
import com.videolive.video.ReciveReady;
import com.videolive.video.SendReady;
import com.videolive.video.ServerActivity;
import com.videolive.video.SuperActivity;
import com.videolive.voice.VoiceReady;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class MainActivity extends SuperActivity {
    private final String TAG = this.getClass().getSimpleName();
    private final int REQUEST_CAMERA = 666;
    EditText publishbitrate_pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        publishbitrate_pass = findViewById(R.id.publishbitrate_pass);
        requestpermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
        publishbitrate_pass.setText(Config.password_enc);
    }

    private void requestpermission() {
        //SD卡读写权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //权限已授权，功能操作
        } else {
            //未授权，提起权限申请
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
            } else {
                //申请权限
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.SYSTEM_ALERT_WINDOW,
                }, REQUEST_CAMERA);
            }

        }
    }


    public void startPush(View v) {
        startActivity(new Intent(MainActivity.this, SendReady.class));
    }

    public void startPull(View v) {
        startActivity(new Intent(MainActivity.this, ReciveReady.class));
    }

    public void startVoice(View v) {
        startActivity(new Intent(MainActivity.this, VoiceReady.class));
    }

    public void startSever(View v) {
        startActivity(new Intent(MainActivity.this, ServerActivity.class));
    }

    public void startClicent(View v) {
        startActivity(new Intent(MainActivity.this, ClientActivity.class));
    }

    public void decodeToMp4(View v) {
        startActivity(new Intent(MainActivity.this, DecodeToMp4Activity.class));
    }

    public void setGloablePass(View v) {
        String pass = publishbitrate_pass.getText().toString().trim();
        if (pass.length() == 16) {
            Config.password_enc = pass;
            Config.saveConfig(getApplicationContext());
            toast("保存成功");
        } else if (pass.length() == 0) {
            Config.password_enc = "";
            Config.saveConfig(getApplicationContext());
            toast("清除密码成功");
        } else {
            toast("密码长度必须为16位");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //判断请求码，确定当前申请的权限
        if (requestCode == REQUEST_CAMERA) {
            //判断权限是否申请通过
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //授权成功
            } else {
                //授权失败
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
