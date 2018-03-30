package com.videolive.video;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.library.rpc.Commond;
import com.library.rpc.Config;
import com.library.rpc.RpcClicent;
import com.videolive.R;

/**
 * Created by wangyanjie on 18-3-30.
 */

public class ClientActivity extends Activity {

    private Button connect;
    private Button send_change_pass;
    private EditText old_pass,new_pass;
    private RpcClicent clicent;
    private Handler han = new Handler();
    private Toast toast = null;
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
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connect.getText().toString().equals("连接服务端")){
                    connect.setText("正在连接");
                    connect.setEnabled(false);
                    clicent.startRpc("10.20.10.162", Config.control_port, han, cscl);
                } else if (connect.getText().toString().equals("断开连接")){
                    connect.setText("正在断开连接");
                    connect.setEnabled(false);
                    clicent.stopRpc();
                }
            }
        });


        send_change_pass=findViewById(R.id.send_change_pass);
        send_change_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commond.Request.ChangePass req = new Commond.Request.ChangePass();
                req.pass=old_pass.getText().toString();
                req.pass_new = new_pass.getText().toString();
                clicent.sendRequest(req);
            }
        });
    }

    @Override
    protected void onDestroy() {
        clicent.stopRpc();
        super.onDestroy();
        if(toast != null) {
            toast.cancel();
            toast=null;
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
