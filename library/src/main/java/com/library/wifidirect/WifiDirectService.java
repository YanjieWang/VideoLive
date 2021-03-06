package com.library.wifidirect;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import com.library.util.mLog;

import java.io.File;

/**
 * Created by wangyanjie on 18-4-2.
 */

public class WifiDirectService extends WifiDirectSuper implements ReceiveSocket.ProgressReceiveListener{
    private static final String TAG = "WifiDirectService";
    private ProgressDialog mProgressDialog;

    public void init(Context context) {
        super.init(context);
        initASY();

    }

    public void unInit(Context context){
        removeGroup();
        super.unInit();
    }


    /**
     * 创建组群，等待连接
     */
    public void createGroup() {

        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG, "创建群组成功");
                if(mContext!=null)Toast.makeText(mContext, "创建群组成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG, "创建群组失败: " + reason);
                if(mContext!=null)Toast.makeText(mContext, "创建群组失败,请移除已有的组群或者连接同一WIFI重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 移除组群
     */
    public void removeGroup() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG, "移除组群成功");
                if(mContext!=null)Toast.makeText(mContext, "移除组群成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG, "移除组群失败:reason="+reason);
                if(mContext!=null)Toast.makeText(mContext, "移除组群失败,请创建组群重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 移除组群
     */
    private void initASY() {
        //先清除历史数据
        removeLocalServiceASY();
    }


    private void removeLocalServiceASY(){
        mWifiP2pManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"清除本地服务成功");
                removeGroupASY();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"清除本地服务失败 reason="+reason);
                removeGroupASY();
            }
        });
    }

    /**
     * 移除组群
     */
    private void removeGroupASY() {

        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG, "移除组群成功");
                Toast.makeText(mContext, "移除组群成功", Toast.LENGTH_SHORT).show();
                createGroupASY();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG, "移除组群失败:reason="+reason);
                createGroupASY();
            }
        });
    }


    /**
     * 创建组群，等待连接
     */
    private void createGroupASY() {

        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG, "创建群组成功");
                if(mContext!=null)Toast.makeText(mContext, "创建群组成功", Toast.LENGTH_SHORT).show();
                addLocalServiceASY();
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG, "创建群组失败: " + reason);
                if(mContext!=null)Toast.makeText(mContext, "创建群组失败,请移除已有的组群或者连接同一WIFI重试", Toast.LENGTH_SHORT).show();
                addLocalServiceASY();
            }
        });
    }
    /**
     * 创建本地服务，等待连接
     */
    private void addLocalServiceASY() {


        mWifiP2pManager.addLocalService(mChannel, mWifiP2pServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mLog.log(TAG,"添加本地服务成功");
            }

            @Override
            public void onFailure(int reason) {
                mLog.log(TAG,"添加本地服务失败 reason="+reason);
            }
        });
    }




    @Override
    public void onSatrt() {
        mProgressDialog = new ProgressDialog(mContext);
    }

    @Override
    public void onProgressChanged(File file, int progress) {
        mLog.log(TAG, "接收进度：" + progress);
        mProgressDialog.setProgress(progress);
        mProgressDialog.setProgressText(progress + "%");
    }

    @Override
    public void onFinished(File file) {
        mLog.log(TAG, "接收完成");
        mProgressDialog.dismiss();
        Toast.makeText(mContext, file.getName() + "接收完毕！", Toast.LENGTH_SHORT).show();
        //接收完毕后再次启动服务等待下载一次连接，不启动只能接收一次，第二次无效，原因待尚不清楚
    }

    @Override
    public void onFaliure(File file) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        Toast.makeText(mContext, "接收失败，请重试！", Toast.LENGTH_SHORT).show();
    }
}
