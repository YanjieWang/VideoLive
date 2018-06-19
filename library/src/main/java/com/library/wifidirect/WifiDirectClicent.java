package com.library.wifidirect;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.library.R;
import com.library.util.mLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by wangyanjie on 18-4-2.
 */

public class WifiDirectClicent extends WifiDirectSuper {
    private static final String TAG = "WifiDirectClicent";
    private ArrayList<String> mListDeviceName = new ArrayList();
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private AlertDialog mDialog;
    private OnConnectedListener mConnectCallBack;
    private WifiP2pServiceRequest mWifiP2pServiceRequest;
    private WifiP2pManager.DnsSdTxtRecordListener txtListener;
    private WifiP2pManager.DnsSdServiceResponseListener servListener;

    public WifiDirectClicent(OnConnectedListener connectCallBack){
        mConnectCallBack = connectCallBack;
    }

    public void init(Context context){
        super.init(context);
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
                    mLog.log(TAG, "DnsSdTxtRecord available -record=" + record);
                    mLog.log(TAG, "DnsSdTxtRecord available -device=" + device);
                    if(record!=null
                            &&"live_service".equals(record.get("service_name"))
                            && "test_pass".equals(record.get("service_pass"))){
                        if(device!=null){
                            mLog.log(TAG, "DnsSdTxtRecord available -connect device=" + device);
                            connect(device);
                        }
                    }
                }
            };
        }
        if(servListener != null) {
            servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
                @Override
                public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                    WifiP2pDevice resourceType) {

                    // Update the device name with the human-friendly version from
                    // the DnsTxtRecord, assuming one arrived.
                    mLog.log(TAG, "onBonjourServiceAvailable " + instanceName);
                }
            };
        }
        connectServerASY();
    }
    public void unInit(){
        super.unInit();
    }


    private void connectServerASY() {
        mDialog = new AlertDialog.Builder(mContext, R.style.Transparent).create();
        mDialog.setCancelable(false);
        mDialog.setContentView(R.layout.loading_progressba);
        mDialog.show();
        //搜索设备
        mWifiP2pManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        mWifiP2pManager.addServiceRequest(mChannel, mWifiP2pServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"添加本地服务请求成功");
                discoverServicesASY();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"添加本地服务请求成功失败 reason="+reason);
                discoverServicesASY();
            }
        });
    }

    private void discoverServicesASY() {
        /**
         * 开始搜索
         * ActionListener，该监听只监听discoverPeers方法是否调用成功
         */

        mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
                Log.d(TAG, "discoverServices:success");
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d(TAG, "discoverServices:fail code="+code);
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, "P2P isn't supported on this device.");
                }
            }
        });
    }

    private void discoverPeersASY(){
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                mLog.log(TAG, "搜索设备成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                mLog.log(TAG, "搜索设备失败 reasonCode=" +reasonCode);
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
                    if(mContext!=null){
                        Toast.makeText(mContext, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                    if (mDialog!=null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");

                    if(mContext != null)Toast.makeText(mContext, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void clearServiceRequestsASY(){
        mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"清除本地服务请求成功");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"清除本地服务请求失败 reason="+reason);
            }
        });
    }




    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);

        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (!mListDeviceName.contains(device.deviceName) && !mListDevice.contains(device)) {
                mListDeviceName.add("设备：" + device.deviceName + "----" + device.deviceAddress);
                mListDevice.add(device);
                mLog.log(TAG,"设备：" + device.deviceName + "----" + device.deviceAddress);
                if(!isMe(device.deviceAddress)) {
                    //connect(device);
                }
            }
        }

        //进度条消失
        //mDialog.dismiss();
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
