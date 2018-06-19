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
import com.library.live.stream.upd.UdpRecive;
import com.library.live.vd.VDDecoder;
import com.library.live.view.PlayerView;
import com.library.rpc.Commond;
import com.library.rpc.RpcClicent;
import com.videolive.R;

/**
 * Created by wangyanjie on 18-3-30.
 */

public class ClientActivity extends Activity {

    private Button connect;
    private EditText old_pass,new_pass;
    private EditText enc_pass;
    private RpcClicent clicent;
    private Handler han = new Handler();
    private Toast toast = null;
    private PlayerView playerView;
    private Player player;

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
        clicent = RpcClicent.getInstance();
        setContentView(R.layout.activity_clicent);
        connect = findViewById(R.id.connect);
        old_pass = findViewById(R.id.old_pass);
        new_pass = findViewById(R.id.new_pass);
        playerView = findViewById(R.id.playerView);
        enc_pass = findViewById(R.id.enc_pass);

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



    public void connect(View v){
        if(connect.getText().toString().equals("连接服务端")){
            connect.setText("正在连接");
            connect.setEnabled(false);
            clicent.startRpc(han, cscl,ClientActivity.this);
        } else if (connect.getText().toString().equals("断开连接")){
            connect.setText("正在断开连接");
            connect.setEnabled(false);
            clicent.stopRpc();
        }
    }
    public void change_pass(View v){
        Commond.Request.ChangePass req = new Commond.Request.ChangePass();
        req.pass=old_pass.getText().toString();
        req.pass_new = new_pass.getText().toString();
        clicent.sendRequest(req);
    }

    public void change_enc_pass(View v){
        Commond.Request.ChangeEncPass req = new Commond.Request.ChangeEncPass();
        req.pass=old_pass.getText().toString();
        req.enc_pass = enc_pass.getText().toString();
        clicent.sendRequest(req);
    }

    public void start_push(View v){
        Commond.Request.StartPush req = new Commond.Request.StartPush();
        req.pass=old_pass.getText().toString();
        clicent.sendRequest(req);
    }
    public void stop_push(View v){
        Commond.Request.StopPush req = new Commond.Request.StopPush();
        req.pass=old_pass.getText().toString();
        clicent.sendRequest(req);
    }
    public void start_recode(View v){
        Commond.Request.StartRecode req = new Commond.Request.StartRecode();
        req.pass=old_pass.getText().toString();
        clicent.sendRequest(req);
    }
    public void stop_recode(View v){
        Commond.Request.StopRecode req = new Commond.Request.StopRecode();
        req.pass=old_pass.getText().toString();
        clicent.sendRequest(req);
    }
    public void switch_camera(View v){
        Commond.Request.SwitchCamera req = new Commond.Request.SwitchCamera();
        req.pass=old_pass.getText().toString();
        clicent.sendRequest(req);
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
    }

    private void toast(String str){
        if(toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this,str,Toast.LENGTH_LONG);
        toast.show();
    }
}
