package com.library.rpc;

import android.os.Handler;

import com.library.util.mLog;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class RpcClicent {

    private static RpcClicent instance = new RpcClicent();

    private boolean started = false;
    private boolean isConnected = false;


    public static final String TAG = RpcClicent.class.getSimpleName();

    private ArrayBlockingQueue<Commond.Response> responseQueue = new ArrayBlockingQueue<>(100);
    private ArrayBlockingQueue<Commond.Request.RequestBase> resquestQueue = new ArrayBlockingQueue<>(100);
    private CommendReciveThread mCommendReciveThread;
    private CommendSendThread mCommendSendThread;
    private InetSocketAddress address = null;

    private ObjectInputStream mObjectInputStream = null;
    private ObjectOutputStream mObjectOutputStream = null;
    private Socket socket;

    private ConnectStateChangeListener cscl;


    public static RpcClicent getInstance() {
        return instance;
    }

    public void startRpc(String ip, int port, Handler handler, ConnectStateChangeListener cscl) {
        this.cscl = cscl;

        mLog.log(TAG, "start");
        if (address == null || !address.getAddress().getHostAddress().equals(ip) || address.getPort() != port) {
            address = new InetSocketAddress(ip, port);
            mLog.log(TAG, "updata address to：" + address);
            stopRpc();
        }
        started = true;
        if (mCommendReciveThread == null || !mCommendReciveThread.isRun) {
            mCommendSendThread = new CommendSendThread();
            mCommendReciveThread = new CommendReciveThread(handler);
            mCommendSendThread.start();
            mCommendReciveThread.start();
        }
    }

    public boolean sendRequest(Commond.Request.RequestBase requestBase) {
        boolean res = false;
        try {
            resquestQueue.put(requestBase);
            res = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean isConnected() {
        return isConnected;
    }


    public void stopRpc() {
        mLog.log(TAG, "stop");
        started = false;
        if (mCommendReciveThread != null) {
            mCommendReciveThread.stopThread();
            mCommendReciveThread = null;
        }
        if (mCommendSendThread != null) {
            mCommendSendThread.stopThread();
            mCommendSendThread = null;
        }
    }


    class CommendReciveThread extends Thread {
        private Handler callbackHandler;
        public boolean isRun = false;

        CommendReciveThread(Handler callbackHandler) {
            this.callbackHandler = callbackHandler;
        }

        @Override
        public void run() {
            mLog.log(TAG, "接收线程 启动");
            while (isRun) {
                try {
                    while (isRun) {

                        if (isRun && socket == null) {
                            socket = new Socket();
                            mLog.log(TAG, "等待连接 port=" + Config.control_port);
                            socket.connect(address, 1000000);
                            mLog.log(TAG, "连接已经建立");
                        }

                        if (isRun && mObjectOutputStream == null) {
                            mLog.log(TAG, "创建ObjectOutputStream");
                            mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                            mLog.log(TAG, "创建ObjectOutputStream完成");
                            isConnected = true;
                            if (callbackHandler != null) {
                                callbackHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (cscl != null) {
                                            cscl.onConnected();
                                        }
                                    }
                                });
                            }
                        }
                        if (isRun && mObjectInputStream == null) {
                            mLog.log(TAG, "创建ObjectInputStream");
                            mObjectInputStream = new ObjectInputStream(socket.getInputStream());
                            mLog.log(TAG, "创建ObjectInputStream完成");
                        }
                        if (isRun && mObjectInputStream != null) {
                            Commond.Response res = null;
                            try {
                                mLog.log(TAG, "readObject start");
                                Object response = mObjectInputStream.readObject();
                                mLog.log(TAG, "readObject end");
                                if (response != null && response instanceof Commond.Response) {
                                    res = (Commond.Response) response;
                                    mLog.log(TAG, "response.success=" + res.success);
                                    mLog.log(TAG, "response.content=" + res.content);
                                } else {
                                    res = new Commond.Response();
                                    res.success = false;
                                    res.content = "服务端返回参数异常";
                                    mLog.log(TAG, "服务端返回参数异常");
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                                res = new Commond.Response();
                                res.success = false;
                                res.content = "服务端返回参数异常";
                                mLog.log(TAG, "服务端返回参数异常");
                            }
                            final Commond.Response res1 = res;
                            if (callbackHandler != null) {
                                callbackHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (cscl != null) {
                                            cscl.onResault(res1);
                                        }
                                    }
                                });
                            }

                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mLog.log("TcpReceive", "服务初始化失败 端口=" + Config.control_port);
                    myStop();
                    try {
                        if (isRun) Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    if (isConnected && callbackHandler != null) {
                        callbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (cscl != null) {
                                    cscl.onDisConnected();
                                }
                            }
                        });
                    }
                    isConnected = false;

                }
            }
            myStop();
            callbackHandler = null;
            mLog.log(TAG, "接收线程 结束");

        }

        @Override
        public synchronized void start() {
            isRun = true;
            super.start();
        }

        public synchronized void stopThread() {
            mLog.log(TAG, "stopThread");
            isRun = false;
            interrupt();
            myStop();

        }

        private void myStop() {
            if (mObjectInputStream != null) {
                try {
                    mObjectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mObjectInputStream = null;
            }
            if (mObjectOutputStream != null) {
                try {
                    mObjectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mObjectOutputStream = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }

    class CommendSendThread extends Thread {

        public boolean isRun = false;

        @Override
        public void run() {
            mLog.log(TAG, "发送线程 启动");
            while (isRun) {
                try {
                    while (isRun && mObjectOutputStream != null) {
                        Commond.Request.RequestBase reqBase = null;
                        try {
                            mLog.log(TAG, "等待请求");
                            reqBase = resquestQueue.take();
                            mLog.log(TAG, "请求已获取");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (reqBase == null) continue;
                        mLog.log(TAG, "发送请求");
                        mObjectOutputStream.writeObject(reqBase);
                        mLog.log(TAG, "请求已发送");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        if (isRun) Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }


                }
            }
            mLog.log(TAG, "发送线程 结束");

        }

        @Override
        public synchronized void start() {
            isRun = true;
            super.start();
        }

        public synchronized void stopThread() {
            mLog.log(TAG, "stopThread");
            isRun = false;
            interrupt();

        }

    }


    public static interface ConnectStateChangeListener {
        public void onConnected();

        public void onDisConnected();

        public void onResault(Commond.Response response);

    }
}