package com.videolive.video;

import android.content.Intent;
import android.hardware.Camera;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.library.live.vd.VDEncoder;
import com.videolive.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class SendReady extends AppCompatActivity {
    private EditText url;
    private EditText port;
    private EditText framerate;
    private EditText publishbitrate;
    private EditText collectionbitrate;
    private EditText collectionbitrate_vc;
    private EditText publishbitrate_vc;
    private RadioGroup videoCode;
    private RadioGroup preview;
    private RadioGroup rotate;
    private Button begin;
    private Spinner sb_preview;
    private List<Camera.Size> previewSIzes;
    private ArrayAdapter<String> adapter;
    private Camera.Size sizeNow = null;
    private int cameraNowId = 0;
    private int frontCamId = 1;
    private Spinner sb_wifi_direct;
    private WifiP2pDevice[] mWifiP2pDeviceList;
    private RadioGroup rg_net_protical;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_ready);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if(Camera.getNumberOfCameras()<=0){
            Toast.makeText(this,"设备无摄像头，无法进行推流操作",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        rotate = findViewById(R.id.rotate);
        sb_preview = findViewById(R.id.sb_preview);
        rotate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.front){
                    refreshSp(frontCamId);
                    cameraNowId = frontCamId;
                } else {
                    cameraNowId = (frontCamId==1)?0:1;
                    refreshSp(cameraNowId);
                }
            }
        });

        Camera.CameraInfo cminfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0,cminfo);
        if(cminfo.facing == CAMERA_FACING_FRONT ){
            frontCamId = 0;
        } else {
            frontCamId = 1;
        }
        if(Camera.getNumberOfCameras()==1){
            if(cminfo.facing == CAMERA_FACING_FRONT ){
                findViewById(R.id.front).setVisibility(View.VISIBLE);
                rotate.check(R.id.front);
                findViewById(R.id.back).setVisibility(View.GONE);
            }else{
                findViewById(R.id.back).setVisibility(View.VISIBLE);
                rotate.check(R.id.back);
                findViewById(R.id.front).setVisibility(View.GONE);
            }
        } else {
            rotate.check(R.id.front);
        }

        url = findViewById(R.id.url);
        port = findViewById(R.id.port);
        framerate = findViewById(R.id.framerate);
        publishbitrate = findViewById(R.id.publishbitrate);
        collectionbitrate = findViewById(R.id.collectionbitrate);
        sb_preview = findViewById(R.id.sb_preview);
        collectionbitrate_vc = findViewById(R.id.collectionbitrate_vc);
        publishbitrate_vc = findViewById(R.id.publishbitrate_vc);
        videoCode = findViewById(R.id.svideoCode);
        preview = findViewById(R.id.preview);
        begin = findViewById(R.id.begin);

        sb_preview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sizeNow = previewSIzes.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        sb_wifi_direct = findViewById(R.id.sb_wifi_direct);
        initWifiDirect();
        rg_net_protical = findViewById(R.id.rg_net_protical);
    }


    public void initWifiDirect(){
        sb_wifi_direct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                url.setText(getIPFromMac(mWifiP2pDeviceList[position].deviceAddress));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        WifiP2pManager mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        WifiP2pManager.Channel mChannel = mManager.initialize(this, Looper.myLooper(), null);
        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if(!info.isGroupOwner){
                    sb_wifi_direct.setEnabled(false);
                    if(info.groupOwnerAddress!=null) {
                        url.setText(info.groupOwnerAddress.getHostAddress());
                    }
                } else {
                    sb_wifi_direct.setEnabled(true);
                }
            }
        });

        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group!=null && group.isGroupOwner()){
                    mWifiP2pDeviceList = new WifiP2pDevice[group.getClientList().size()];
                    mWifiP2pDeviceList =group.getClientList().toArray(mWifiP2pDeviceList);
                    String[] list = new String[mWifiP2pDeviceList.length];
                    for (int i=0;i<mWifiP2pDeviceList.length;i++) {
                        list[i]=mWifiP2pDeviceList[i].deviceName+":"+mWifiP2pDeviceList[i].deviceAddress;
                    }
                    ArrayAdapter adapter = new ArrayAdapter<String>(SendReady.this,android.R.layout.simple_spinner_item, list);
                    sb_wifi_direct.setAdapter(adapter);
                    if(list.length>0)sb_wifi_direct.setSelection(0);
                }
            }
        });
    }

    public static String getIPFromMac(String MAC) {
        String mMac = MAC.trim().toLowerCase();
        BufferedReader br = null;
        try {

            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.toLowerCase().trim();
                if (line.contains(mMac)) {
                    return line.split(" ")[0];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void start() {
        if(sizeNow == null) return;
        Intent intent = new Intent(this, Send.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.getText().toString());
        bundle.putInt("port", Integer.parseInt(port.getText().toString()));
        bundle.putInt("framerate", Integer.parseInt(framerate.getText().toString()));
        bundle.putInt("publishbitrate", Integer.parseInt(publishbitrate.getText().toString()) * 1024);
        bundle.putInt("collectionbitrate", Integer.parseInt(collectionbitrate.getText().toString()) * 1024);
        bundle.putInt("collectionbitrate_vc", Integer.parseInt(collectionbitrate_vc.getText().toString()) * 1024);
        bundle.putInt("publishbitrate_vc", Integer.parseInt(publishbitrate_vc.getText().toString()) * 1024);
        bundle.putInt("pu_width", sizeNow.width);
        bundle.putInt("pu_height", sizeNow.height);
        bundle.putInt("pr_width", sizeNow.width);
        bundle.putInt("pr_height", sizeNow.height);
        bundle.putInt("c_height", sizeNow.height);
        bundle.putInt("c_width", sizeNow.width);

        if (videoCode.getCheckedRadioButtonId() == R.id.sh264) {
            bundle.putString("videoCode", VDEncoder.H264);
        } else {
            bundle.putString("videoCode", VDEncoder.H265);
        }
        if (preview.getCheckedRadioButtonId() == R.id.haspreview) {
            bundle.putBoolean("ispreview", true);
        } else {
            bundle.putBoolean("ispreview", false);
        }
        if (rotate.getCheckedRadioButtonId() == R.id.front) {
            bundle.putBoolean("rotate", true);
        } else {
            bundle.putBoolean("rotate", false);
        }
        if (rg_net_protical.getCheckedRadioButtonId()==R.id.rb_udp) {
            bundle.putString("net_protical", "Udp");
        } else {
            bundle.putString("net_protical", "Tcp");
        }
        intent.putExtras(bundle);
        startActivity(intent);
    }

    void refreshSp(int camId){
        Camera camera = Camera.open(camId);
        Camera.Parameters params = camera.getParameters();
        previewSIzes = params.getSupportedPreviewSizes();
        if(adapter != null)adapter.notifyDataSetInvalidated();
        String[] list = new String[previewSIzes.size()];
        for(int i = 0 ;i < previewSIzes.size();i++){
            Camera.Size size = previewSIzes.get(i);
            list[i] = size.width+"*"+size.height;
        }
        camera.release();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, list);
        sb_preview.setAdapter(adapter);
        sb_preview.setSelection(0);
    }
}
