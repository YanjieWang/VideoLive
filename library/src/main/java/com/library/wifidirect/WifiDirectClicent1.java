package com.library.wifidirect;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.library.R;
import com.library.util.mLog;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by wangyanjie on 18-4-2.
 */

public class WifiDirectClicent1 extends WifiDirectSuper1 {
    private static final String TAG = "WifiDirectClicent1";
    private ArrayList<String> mListDeviceName = new ArrayList();
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private AlertDialog mDialog;
    private OnConnectedListener mConnectCallBack;

    public WifiDirectClicent1(OnConnectedListener connectCallBack){
        mConnectCallBack = connectCallBack;
    }

    public void init(Context context){
        super.init(context);
        connectServer();
    }
    public void unInit(){
        super.unInit();
    }


    /**
     * 搜索设备
     */
    public void connectServer() {
        Log.e(TAG, "mContext="+mContext);
        mDialog = new AlertDialog.Builder(mContext, R.style.Transparent).create();
        mDialog.setCancelable(false);
        mDialog.setContentView(R.layout.loading_progressba);
        mDialog.show();
        //搜索设备
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "搜索设备成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "搜索设备失败");
            }
        });
    }

    /**
     * 连接设备
     */
    private void connect(final WifiP2pDevice wifiP2pDevice) {
        mLog.log(TAG,"连接设备：" + wifiP2pDevice.deviceName + "----" + wifiP2pDevice.deviceAddress);
        WifiP2pConfig config = new WifiP2pConfig();
        if (wifiP2pDevice != null) {
            config.deviceAddress = wifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "连接成功，mConnectCallBack="+mConnectCallBack);
                    Toast.makeText(mContext, "连接成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    Toast.makeText(mContext, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);

        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (!mListDeviceName.contains(device.deviceName) && !mListDevice.contains(device)) {
                mListDeviceName.add("设备：" + device.deviceName + "----" + device.deviceAddress);
                mListDevice.add(device);
                mLog.log(TAG,"设备：" + device.deviceName + "----" + device.deviceAddress);
                if("T550Z".equals(device.deviceName)) {
                    connect(device);
                }
            }
        }

        //进度条消失
        mDialog.dismiss();
    }

    public interface OnConnectedListener {
        public void onConnected(String ip);
        public void onDisconnected();
    }

    @Override
    public void onDisconnection() {
        super.onDisconnection();
        if(mConnectCallBack!=null){
            mConnectCallBack.onDisconnected();
        }
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        super.onConnection(wifiP2pInfo);
        mLog.log(TAG, "wifiP2pInfo=" + wifiP2pInfo);
        String ip = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        if(mConnectCallBack!=null){
            mLog.log(TAG, "准备进入回调：ip=" + ip);
            mConnectCallBack.onConnected(ip);
            if(mDialog != null&&mDialog.isShowing()){
                mDialog.dismiss();
            }
        }
    }
}
