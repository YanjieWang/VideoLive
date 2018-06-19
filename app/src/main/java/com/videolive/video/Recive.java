package com.videolive.video;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.live.Player;
import com.library.live.stream.BaseRecive;
import com.library.live.stream.tcp.TcpRecive;
import com.library.live.stream.upd.UdpRecive;
import com.library.live.view.PlayerView;
import com.library.rpc.Commond;
import com.library.util.mLog;
import com.videolive.R;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

public class Recive extends AppCompatActivity {
    private Player player;
    private Button jiestar;
    private Button recode;
    private ArrayBlockingQueue<Commond.Response> responseQueue = new ArrayBlockingQueue<>(100);
    private ArrayBlockingQueue<Commond.Request.RequestBase> resquestQueue = new ArrayBlockingQueue<>(100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recive);

        //需要让服务器知道自己是接收方并且知道自己的IP，这个自行完成

        jiestar = findViewById(R.id.jiestar);
        recode = findViewById(R.id.recode);

        BaseRecive br = null;
        if(getIntent().getExtras().getString("net_protical").equals("Udp")){
            br = new UdpRecive(getIntent().getExtras().getInt("port"));
        }else{
            br = new TcpRecive(getIntent().getExtras().getInt("port"));
        }

        player = new Player.Buider((PlayerView) findViewById(R.id.playerView))
                .setPullMode(br)
                .setVideoCode(getIntent().getExtras().getString("videoCode"))
                .setMultiple(getIntent().getExtras().getInt("multiple"))
                .setCenterScaleType(true)
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive"+ File.separator+"clicent")
                .build();

        jiestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (jiestar.getText().toString().equals("开始播放")) {
                    player.start();
                    jiestar.setText("停止播放");
                } else {
                    jiestar.setText("开始播放");
                    player.stop();
                }
            }
        });
        recode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recode.getText().toString().equals("开始录制")) {
                    player.starRecode();
                    recode.setText("停止录制");
                } else {
                    recode.setText("开始录制");
                    player.stopRecode();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        player.destroy();
        super.onDestroy();
        mLog.log("onDestory", "onDestory");

    }




}
