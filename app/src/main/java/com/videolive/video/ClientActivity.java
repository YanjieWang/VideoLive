package com.videolive.video;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.library.live.Player;
import com.library.live.stream.BaseRecive;
import com.library.live.stream.tcp.TcpRecive;
import com.library.live.stream.upd.UdpRecive;
import com.library.live.vd.VDDecoder;
import com.library.live.view.PlayerView;
import com.library.rpc.Commond;
import com.library.rpc.Config;
import com.library.rpc.RpcClicent;
import com.library.wifidirect.WifiDirectClicent;
import com.videolive.R;

/**
 * Created by wangyanjie on 18-3-30.
 */

public class ClientActivity extends Activity {

    private Button connect;
    private Button send_change_pass;
    private Button start_push;
    private Button stop_push;
    private Button start_recode;
    private Button stop_recode;
    private Button switch_camera;
    private EditText old_pass,new_pass;
    private RpcClicent clicent;
    private Handler han = new Handler();
    private Toast toast = null;
    private PlayerView playerView;
    private Player player;
    private WifiDirectClicent wdc;

    private RpcClicent.ConnectStateChangeListener cscl = new RpcClicent.ConnectStateChangeListener() {
        @Override
        public void onConnected() {
            connect.setText("断开连接");
            connect.setEnabled(true);
            toast("连接成功");
        }

        @Override
        public void onDisConnected() {
            connect.setText("连接服务端");
            connect.setEnabled(true);
            toast("连接已断开");
        }

        @Override
        public void onResault(Commond.Response response) {
            toast(""+response);
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wdc = new WifiDirectClicent();
        wdc.init(getApplicationContext());
        clicent = RpcClicent.getInstance();
        setContentView(R.layout.activity_clicent);
        connect = findViewById(R.id.connect);
        old_pass = findViewById(R.id.old_pass);
        new_pass = findViewById(R.id.new_pass);
        playerView = findViewById(R.id.playerView);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connect.getText().toString().equals("连接服务端")){
                    connect.setText("正在连接");
                    connect.setEnabled(false);
                    clicent.startRpc("192.168.49.1", Config.control_port, han, cscl);
                } else if (connect.getText().toString().equals("断开连接")){
                    connect.setText("正在断开连接");
                    connect.setEnabled(false);
                    clicent.stopRpc();
                }
            }
        });


        send_change_pass=findViewById(R.id.send_change_pass);

        start_push=findViewById(R.id.start_push);
        stop_push=findViewById(R.id.stop_push);
        start_recode=findViewById(R.id.start_recode);
        stop_recode=findViewById(R.id.stop_recode);
        switch_camera=findViewById(R.id.switch_camera);

        send_change_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.ChangePass req = new Commond.Request.ChangePass();
                req.pass=old_pass.getText().toString();
                req.pass_new = new_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });
        start_push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.StartPush req = new Commond.Request.StartPush();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });

        stop_push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.StopPush req = new Commond.Request.StopPush();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });

        stop_recode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.StopRecode req = new Commond.Request.StopRecode();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });

        start_recode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.StartRecode req = new Commond.Request.StartRecode();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });

        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.StartRecode req = new Commond.Request.StartRecode();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });
        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.SwitchCamera req = new Commond.Request.SwitchCamera();
                req.pass=old_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });

        BaseRecive br = new UdpRecive(8765);
        //BaseRecive br = new TcpRecive(getIntent().getExtras().getInt("port"));


        player = new Player.Buider(playerView)
                .setPullMode(br)
                .setVideoCode(VDDecoder.H264)
                .setMultiple(1)
                .setCenterScaleType(true)
                .setVideoPath("/sdcard/VideoLive")
                .build();
        player.start();
    }

    @Override
    protected void onDestroy() {
        clicent.stopRpc();
        super.onDestroy();
        if(toast != null) {
            toast.cancel();
            toast=null;
        }
        player.stopRecode();
        player.stop();
        player.destroy();
        player = null;
        if(wdc!=null){
            wdc.unInit(getApplicationContext());
            wdc=null;
        }
    }

    private void toast(String str){
        if(toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this,str,Toast.LENGTH_LONG);
        toast.show();
    }
}
