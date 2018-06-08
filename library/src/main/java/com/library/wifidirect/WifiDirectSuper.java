package com.library.wifidirect;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.util.Log;

import com.library.util.mLog;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiDirectSuper implements Wifip2pActionListener {
    private static final String TAG = "WifiDirectSuper";

    public WifiP2pManager mWifiP2pManager;
    public WifiP2pManager.Channel mChannel;
    public Wifip2pReceiver mWifip2pReceiver;
    public WifiP2pInfo mWifiP2pInfo;
    public Context mContext;
    public WifiP2pDnsSdServiceInfo mWifiP2pServiceInfo;
    public static final String SERVICE_NAME = "real_time_live";
    public static final String SERVICE_TYPE = "sender";



    public void init(Context context){
        //注册WifiP2pManager
        mContext = context;
        mWifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(context, context.getMainLooper(), this);
        if(mWifiP2pServiceInfo == null){
            Map<String,String> data = new HashMap<>();
            data.put("service_name","live_service");
            data.put("service_pass","test_pass");
            mWifiP2pServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, SERVICE_TYPE, data);
        }
        //注册广播
        mWifip2pReceiver = new Wifip2pReceiver(mWifiP2pManager, mChannel, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(mWifip2pReceiver, intentFilter);
    }

    public void unInit(){
        //注销广播
        mContext.unregisterReceiver(mWifip2pReceiver);
        mWifip2pReceiver = null;
        mContext = null;
    }



    @Override
    public void wifiP2pEnabled(boolean enabled) {
        Log.e(TAG, "传输通道是否可用：" + enabled);
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            mWifiP2pInfo = wifiP2pInfo;
            mLog.log(TAG, "WifiP2pInfo:" + wifiP2pInfo);
        }
    }

    @Override
    public void onDisconnection() {
        Log.e(TAG, "连接断开");
    }

    @Override
    public void onDeviceInfo(WifiP2pDevice wifiP2pDevice) {
        mLog.log(TAG, "当前的的设备名称" + wifiP2pDevice.deviceName);
    }
    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        for (WifiP2pDevice device : wifiP2pDeviceList) {
            mLog.log(TAG, "连接的设备信息：" + device.deviceName + "--------" + device.deviceAddress);
        }
    }

    @Override
    public void onChannelDisconnected() {

    }


    public boolean isMe(String mac){
        String myMac = getNewMac();
        mLog.log(TAG,"isMe：myMac=" + myMac + ",mac=" + mac);
        if(myMac != null && mac != null){
            myMac = myMac.trim().toUpperCase();
            String targetMac = mac.trim().toUpperCase();
            return myMac.equals(targetMac);
        }
        return true;
    }

    /**
     * 通过网络接口取
     * @return
     */
    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
