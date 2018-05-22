package com.library.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.util.Log;
import android.widget.Toast;

import com.library.util.mLog;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wangyanjie on 18-4-2.
 */

public class WifiDirectClicent extends WifiDirectSuper{
    private static final String TAG = WifiDirectClicent.class.getSimpleName();
    private WifiManager mWifiManager;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private WifiP2pBroadcastReceiver mReceiver;
    private WifiP2pInfo mWifiP2pInfo;

    private WifiP2pServiceInfo mWifiP2pServiceInfo;
    private WifiP2pServiceRequest mWifiP2pServiceRequest;

    private WifiP2pManager.DnsSdTxtRecordListener txtListener;
    private WifiP2pManager.DnsSdServiceResponseListener servListener;



    //搜索到设备的监听
    WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener () {
        @Override
        public void onPeersAvailable (WifiP2pDeviceList wifiP2pDeviceList) {
            //获取设备列表
            Collection<WifiP2pDevice> deviceList = wifiP2pDeviceList.getDeviceList ();
            //遍历设备列表，将设备添加到ListView中
            Iterator<WifiP2pDevice> iterator = deviceList.iterator ();
            while(iterator.hasNext ()){
                WifiP2pDevice wifiP2pDevice = iterator.next ();
                //connect(wifiP2pDevice);
                mLog.log(TAG,"发现 wifiP2pDevice:"+wifiP2pDevice);
                mLog.log(TAG,"是否可发现:"+wifiP2pDevice.isServiceDiscoveryCapable());
                mLog.log(TAG,"是否为群主:"+wifiP2pDevice.isGroupOwner());
            }
        }
    };

    WifiP2pManager.DnsSdTxtRecordListener mDnsSdTxtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener(){

        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
            mLog.log(TAG,"mDnsSdTxtRecordListener:"+"onDnsSdTxtRecordAvailable");
        }
    };

    public static String getMacAddr() {
        try {
            NetworkInterface nif = NetworkInterface.getByName("p2p0");
            byte[] macBytes = nif.getHardwareAddress();
            if (macBytes == null) {
                return "";
            }
            StringBuilder res1 = new StringBuilder();
            for (byte b : macBytes) {
                res1.append(String.format("%02X:",b));
            }
            if (res1.length() > 0) {
                res1.deleteCharAt(res1.length() - 1);
            }
            return res1.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    //连接设备的监听
    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener () {
        @Override
        public void onConnectionInfoAvailable (WifiP2pInfo wifiP2pInfo) {
            mWifiP2pInfo = wifiP2pInfo;
            mLog.log (TAG, "onConnectionInfoAvailable: 是否为新群："+mWifiP2pInfo.groupFormed);
            mLog.log (TAG, "onConnectionInfoAvailable: 是否为群主："+mWifiP2pInfo.isGroupOwner);
            mLog.log (TAG, "onConnectionInfoAvailable: 群主ip："+mWifiP2pInfo.groupOwnerAddress);

            if(wifiP2pInfo.isGroupOwner && wifiP2pInfo.groupFormed){//接收端，即群主
                //mLog.log (TAG, "onConnectionInfoAvailable: 接收端"+mWifiP2pInfo);
                //使用异步任务，开启ServerSocket，等待客户端去连接
            }else{//发送端 ,即成员
                //mLog.log (TAG, "onConnectionInfoAvailable: 发送端"+mWifiP2pInfo);
                //显示发送按钮，允许用户点击发送按钮，发送数据。

            }
        }
    };

    WifiP2pManager.ActionListener mActionListener = new WifiP2pManager.ActionListener () {
        @Override
        public void onSuccess () {
            mLog.log(TAG,"开始搜索 wifidirect 搜索");
        }

        @Override
        public void onFailure (int i) {
            mLog.log(TAG,"开始搜索 wifidirect 搜索 失败");
        }
    };

    WifiP2pManager.ActionListener mConnectActionListener = new WifiP2pManager.ActionListener () {
        @Override
        public void onSuccess () {
            mLog.log(TAG,"开始连接 peer");
        }

        @Override
        public void onFailure (int i) {
            mLog.log(TAG,"开始连接peer失败");
        }
    };

    WifiP2pManager.GroupInfoListener mGroupInfoListener = new WifiP2pManager.GroupInfoListener(){

        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            mLog.log(TAG,"mGroupInfoListener:group="+group);
            if(group==null){
                mLog.log(TAG,"未加入任何群");
            }else{
                if(getMacAddr().equals(group.getOwner().deviceAddress)){
                    mLog.log(TAG,"我是群主");
                }else{
                    mLog.log(TAG,"我是成员");
                }
            }

        }
    };



    public void init(Context context){
        initWifi(context);
        initIntentFilter();
        initWifiP2P(context);
        initReceiver(context);
        discoverServices();
    }

    public void unInit(Context context){
        context.unregisterReceiver(mReceiver);
        mReceiver = null;
        mIntentFilter = null;
        mWifiP2pManager.stopPeerDiscovery(mChannel,mActionListener);
        mWifiP2pManager.cancelConnect(mChannel,mConnectActionListener);
        mChannel = null;
        mWifiP2pManager = null;
    }

    private void initWifi(Context context){

        //获取wifi管理类
        mWifiManager = (WifiManager) context.getSystemService (Context.WIFI_SERVICE);
        //打开wifi
        if(mWifiManager!=null){
            if(!mWifiManager.isWifiEnabled()) {
                mWifiManager.setWifiEnabled(true);
            }
        }

    }

    private void initWifiP2P (Context context) {
        //获取wifiP2p管理类
        mWifiP2pManager = (WifiP2pManager) context.getSystemService (Context.WIFI_P2P_SERVICE);
        //初始化通道
        mChannel = mWifiP2pManager.initialize(context, context.getMainLooper (), null);
        mLog.log(TAG,"mChannel="+mChannel);

        //先清除历史数据
        mWifiP2pManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"clearLocalServices onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"clearLocalServices faile reason="+reason);
            }
        });
        mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"clearServiceRequests onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"clearServiceRequests faile reason="+reason);
            }
        });
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"removeGroup onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"removeGroup faile reason="+reason);
            }
        });

        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"createGroup onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"createGroupon faile reason="+reason);
            }
        });
        if(mWifiP2pServiceInfo==null){
            Map<String,String> data = new HashMap<>();
            data.put("service_name","live_service");
            data.put("service_pass","test_pass");
            mWifiP2pServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("WifiDirectService", "main", data);
        }
        if(mWifiP2pServiceRequest==null){
            mWifiP2pServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(SERVICE_NAME, SERVICE_TYPE);
        }
        if(txtListener==null){
            txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
                @Override
                /* Callback includes:
                 * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
                 * record: TXT record dta as a map of key/value pairs.
                 * device: The device running the advertised service.
                 */

                public void onDnsSdTxtRecordAvailable(
                        String fullDomain, Map record, WifiP2pDevice device) {
                    Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                    if(record!=null
                            &&"live_service".equals(record.get("service_name"))
                            && "test_pass".equals(record.get("service_pass"))){
                        if(device!=null){
                            connect(device);
                        }
                    }
                }
            };
        }
        servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
            }
        };


        mWifiP2pManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        mWifiP2pManager.addServiceRequest(mChannel, mWifiP2pServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"addServiceRequest onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"addServiceRequest faile reason="+reason);
            }
        });

        mWifiP2pManager.requestPeers(mChannel,mPeerListListener);
        mWifiP2pManager.requestGroupInfo(mChannel,mGroupInfoListener);
    }

    private void initIntentFilter () {
        mIntentFilter = new IntentFilter ();
        //wifiP2p是否可用状态改变的广播动作
        mIntentFilter.addAction (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //wifiP2p搜索状态改变的广播动作
        mIntentFilter.addAction (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        //wifiP2p设备列表发生改变的广播动作
        mIntentFilter.addAction (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        //wifiP2p设备连接状态发生改变的广播动作
        mIntentFilter.addAction (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //wifiP2p设备本身信息发生改变的广播动作
        mIntentFilter.addAction (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
    private void initReceiver (Context context) {
        mReceiver = new WifiP2pBroadcastReceiver (context,mWifiP2pManager,mChannel,mPeerListListener,mConnectionInfoListener);
        context.registerReceiver (mReceiver,mIntentFilter);
    }


    //搜索设备
    private void discoverServices() {
        /**
         * 开始搜索
         * ActionListener，该监听只监听discoverPeers方法是否调用成功
         */

        mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // Success!
                }

                @Override
                public void onFailure(int code) {
                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                        Log.d(TAG, "P2P isn't supported on this device.");
                    }
                }
        });
    }

    //建立连接
    private void connect (WifiP2pDevice wifiP2pDevice) {
        mLog.log(TAG,wifiP2pDevice.deviceName);
        //创建wifip2p配置对象
        WifiP2pConfig config = new WifiP2pConfig ();
        config.groupOwnerIntent = 15;
        //将设备地址设置到配置对象中
        config.deviceAddress = wifiP2pDevice.deviceAddress;
        //连接
        if(mWifiP2pManager!=null)mWifiP2pManager.connect (mChannel, config, mConnectActionListener);
    }


    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {
        public static final String TAG = "BroadcastReceiver";
        private Context mContext;
        private WifiP2pManager mWifiP2pManager;
        private WifiP2pManager.Channel mChannel;
        private WifiP2pManager.PeerListListener mPeerListListener;
        private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
        public WifiP2pBroadcastReceiver(Context mContext,WifiP2pManager mWifiP2pManager, WifiP2pManager.Channel mChannel,
                                        WifiP2pManager.PeerListListener mPeerListListener,
                                        WifiP2pManager.ConnectionInfoListener mConnectionInfoListener){
            this.mContext = mContext;
            this.mWifiP2pManager = mWifiP2pManager;
            this.mChannel = mChannel;
            this.mPeerListListener = mPeerListListener;
            this.mConnectionInfoListener = mConnectionInfoListener;
        }

        @Override
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction ();
            mLog.log(TAG,"action="+action);
            if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals (action)){
                int wifiState = intent.getIntExtra (WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if(WifiP2pManager.WIFI_P2P_STATE_DISABLED == wifiState){
                    Log.d (TAG, "onReceive: WIFI_P2P_STATE_DISABLED");
                }else if(WifiP2pManager.WIFI_P2P_STATE_ENABLED == wifiState){
                    Log.d (TAG, "onReceive: WIFI_P2P_STATE_ENABLED");
                }
            }else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals (action)){
                int dicoverState = intent.getIntExtra (WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                if(WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED == dicoverState){
                    toast ("开始搜索");
                    Log.d (TAG, "onReceive: WIFI_P2P_DISCOVERY_STARTED");
                }else if(WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED == dicoverState){
                    toast ("停止搜索");
                    Log.d (TAG, "onReceive: WIFI_P2P_DISCOVERY_STOPPED");
                }
            }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals (action)){
                if(mWifiP2pManager!=null){
                    mWifiP2pManager.requestPeers (mChannel,mPeerListListener);
                }
            }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals (action)){

                NetworkInfo mNetworkInfo = intent.getParcelableExtra (WifiP2pManager.EXTRA_NETWORK_INFO);
                if(mNetworkInfo.isConnected ()){
                    toast ("已经连接");
                    if(mWifiP2pManager!=null){
                        mWifiP2pManager.requestConnectionInfo (mChannel,mConnectionInfoListener);
                    }
                }else{
                    toast ("连接断开");
                }
            }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals (action)){
                mLog.log (TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                WifiP2pDevice wifiP2pDevice =(WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mLog.log(TAG,"onReceive:wifiP2pDevice="+wifiP2pDevice);
            }
        }
        private void toast(String text){
            Toast.makeText (mContext,text,Toast.LENGTH_SHORT).show ();
        }
    }

}
