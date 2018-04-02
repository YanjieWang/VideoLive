package com.library.rpc;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.library.R;
import com.library.live.Publish;
import com.library.live.stream.BaseSend;
import com.library.live.stream.upd.UdpSend;
import com.library.live.vd.VDEncoder;
import com.library.live.view.PublishView;
import com.library.util.mLog;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by wangyanjie on 18-3-27.
 */

public class RpcSeverService extends Service {
    private static final String TAG = RpcSeverService.class.getSimpleName();
    private CommendReciveThread mCommendReciveThread;
    private CommendSendThread mCommendSendThread;
    private ArrayBlockingQueue<Commond.Response> responseQueue = new ArrayBlockingQueue<>(100);
    private ObjectInputStream mObjectInputStream = null;
    private ObjectOutputStream mObjectOutputStream = null;
    private ServerSocket serverSocket;
    private Socket socket = null;
    private PublishView pv;
    private Publish publish;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        if(!Config.isLoaded) {
            Config.loadConfig(this);
        }
        super.onCreate();
        View mView = LayoutInflater.from(this).inflate(R.layout.sevice_publish_view,
                null);
        //pv = mView.findViewById(R.id.publish_view);

        BaseSend bs = new UdpSend("192.168.49.1", 8765);

        publish = new Publish.Buider(this, pv)
                .setPushMode(bs)
                .setFrameRate(15)
                .setVideoCode(VDEncoder.H265)
                .setIsPreview(false)
                .setPublishBitrate(2000*1024)
                .setCollectionBitrate(2000*1024)
                .setCollectionBitrateVC(64*1024)
                .setPublishBitrateVC(24*1024)
                .setPublishSize(1920,1080)
                .setPreviewSize(1920,1080)
                .setCollectionSize(1920,1080)
                .setRotate(false)
                .setVideoDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive")
                .setPictureDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoPicture")
                .setCenterScaleType(true)
                .setScreenshotsMode(Publish.TAKEPHOTO)
                .build();
        publish.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCommendReciveThread == null) {
            mCommendReciveThread = new CommendReciveThread();
            mCommendReciveThread.start();
            mCommendSendThread = new CommendSendThread();
            mCommendSendThread.start();
            mLog.log(TAG, "CommondService 启动");
        }
        return START_NOT_STICKY;
    }

    class CommendReciveThread extends Thread {
        public boolean isRun = false;

        @Override
        public void run() {
            mLog.log(TAG, "接收线程 启动");
            while (isRun) {
                try {
                    if (serverSocket == null) {
                        mLog.log(TAG, "创建SeverSocket");
                        serverSocket = new ServerSocket(Config.control_port);
                        mLog.log(TAG, "创建SeverSocket 完成");
                    }
                    while (isRun && serverSocket != null) {
                        if (isRun && mObjectInputStream == null && socket == null) {
                            try {
                                mLog.log(TAG, "等待客户端连接 port=" + Config.control_port);
                                socket = serverSocket.accept();
                                mObjectInputStream = new ObjectInputStream(socket.getInputStream());
                                mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                                mLog.log(TAG, "连接成功");
                            } catch (IOException e) {
                                e.printStackTrace();
                                mObjectInputStream = null;
                                mObjectOutputStream = null;
                                try {
                                    Thread.currentThread().sleep(1000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                mLog.log(TAG, "连接失败");
                            }
                        }

                        Object request = null;
                        while (isRun && mObjectInputStream != null) {
                            try {
                                request = null;
                                request = mObjectInputStream.readObject();
                                if (request != null && request instanceof Commond.Request.RequestBase) {
                                    Commond.Request.RequestBase requestBase = (Commond.Request.RequestBase) request;
                                    requestBase.doRequest(RpcSeverService.this, responseQueue,publish);
                                    mLog.log(TAG, "收到请求" + requestBase);
                                } else {
                                    Commond.Response res = new Commond.Response();
                                    res.success = false;
                                    res.content = "参数错误";
                                    try {
                                        responseQueue.put(res);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                } catch (IOException e) {
                    myStop();
                    e.printStackTrace();
                    mLog.log(TAG, "服务初始化失败 端口=" + Config.control_port);
                }
            }
            mLog.log(TAG, "接收线程 已停止");


        }

        @Override
        public synchronized void start() {
            isRun = true;
            super.start();
        }

        public synchronized void stopThread() {
            isRun = false;
            interrupt();
            myStop();
        }

        private void myStop() {
            if (mObjectInputStream != null) {
                try {
                    mObjectInputStream.close();
                    mObjectInputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mObjectOutputStream != null) {
                try {
                    mObjectOutputStream.close();
                    mObjectOutputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommendSendThread extends Thread {
        public boolean isRun = false;

        @Override
        public void run() {
            Commond.Response response;
            mLog.log(TAG, "发送线程 启动");
            while (isRun) {
                try {
                    response = responseQueue.take();
                    if (response != null && mObjectOutputStream != null) {
                        mLog.log(TAG, "response send start");
                        mObjectOutputStream.writeObject(response);
                        mLog.log(TAG, "responseQueue.size()=" + responseQueue.size());
                        mLog.log(TAG, "response.success=" + response.success);
                        mLog.log(TAG, "response.content=" + response.content);
                        response = null;
                        mLog.log(TAG, "response send end");
                    }
                } catch (InterruptedException e) {
                    response = null;
                    e.printStackTrace();
                } catch (IOException e1) {
                    response = null;
                    e1.printStackTrace();
                }
            }
            mLog.log(TAG, "发送线程 已停止");

        }

        @Override
        public synchronized void start() {
            isRun = true;
            super.start();
        }

        public synchronized void stopThread() {
            isRun = false;
            interrupt();
        }
    }

    @Override
    public void onDestroy() {
        mLog.log(TAG, "onDestroy");
        super.onDestroy();
        if (mCommendReciveThread != null) {
            mCommendReciveThread.stopThread();
            mCommendReciveThread = null;
        }
        if (mCommendSendThread != null) {
            mCommendSendThread.stopThread();
            mCommendSendThread = null;
        }
        publish.stop();
        publish.destroy();
    }

}
