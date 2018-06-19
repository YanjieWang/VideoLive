package com.library.live.stream.tcp;


import com.library.common.TcpBytes;
import com.library.live.stream.BaseSend;
import com.library.rpc.Config;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by android1 on 2017/9/25.
 */

public class TcpSend extends BaseSend {
    private boolean issend = false;
    private InetSocketAddress address = null;
    private Socket socket = null;
    private int voiceNum = 0;
    private int videoNum = 0;

    private int timeOut = 10000;

    private Thread executorService = null;//单例线程池，用于控制线程结束和执行

    private String pass;

    private ArrayBlockingQueue<TcpBytes> sendQueue = new ArrayBlockingQueue<>(OtherUtil.QueueNum);
    ObjectOutputStream stream = null;

    public TcpSend(String ip, int port) {
        address = new InetSocketAddress(ip, port);
        try {
            socket = new Socket();
            socket.bind(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void startsend() {
        issend = true;
        PUBLISH_STATUS = PUBLISH_STATUS_START;
        starsendThread();
    }

    @Override
    public void stopsend() {
        issend = false;
        if (executorService != null) {
            PUBLISH_STATUS = PUBLISH_STATUS_STOP;
            executorService = null;
        }
    }

    @Override
    public void destroy() {
        stopsend();
        if(stream!=null){
            try {
                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            stream = null;
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

    @Override
    public void addVideo(byte[] video) {
        if (issend) {
            writeVideo(video);
        }
    }
    @Override
    public void addVoice(byte[] voice) {
        if (issend) {
            writeVoice(voice);
        }
    }

    /*
    发送视频
     */
    public void writeVideo(byte[] video) {

        //记录时间值
        int time_vd_vaule = OtherUtil.getTime();
        byte tag = 1;
        TcpBytes tcpBytes = new TcpBytes(tag,videoNum,time_vd_vaule,video);
        //TCD发送
        mLog.log("TcpSend", "writeVideo");
        addbytes(tcpBytes);
        videoNum++;
    }


    /*
    发送音频
     */
    public void writeVoice(byte[] voice) {
        byte tag = 0;
        TcpBytes tcpBytes = new TcpBytes(tag,voiceNum, OtherUtil.getTime(),voice);
        voiceNum++;
        mLog.log("TcpSend", "writeVoice");
        addbytes(tcpBytes);

    }

    private synchronized void addbytes(TcpBytes tcpBytes) {
        //mLog.log("addbytes", "add begin");
        OtherUtil.addQueue(sendQueue, tcpBytes);
        //mLog.log("addbytes", "add end");;
    }

    /*
    真正发送数据
     */
    private void starsendThread() {
        executorService = new Thread(new Runnable() {
            @Override
            public void run() {
                if(socket == null)return;
                try {
                    mLog.log("TcpSend", "连接");

                    if(!socket.isConnected()){
                        socket.connect(address, timeOut);
                        stream = new ObjectOutputStream(socket.getOutputStream());
                        mLog.log("TcpSend", "连接成功");
                    };

                } catch (IOException e) {
                    mLog.log("TcpSend", "连接失败");
                    e.printStackTrace();
                }
                while (issend) {//根据中断标志判断是否执行
                    if ((stream != null) && (sendQueue.size() > 0)) {

                        try {
                            mLog.log("senderror", "发送数据");
                            //stream.writeObject(sendQueue.poll());
                            if(Config.password_enc != null && Config.password_enc.length()!=0) {
                                stream.writeObject(sendQueue.take().encrypt(Config.password_enc));
                            }else{
                                stream.writeObject(sendQueue.take());
                            }
                        } catch (IOException e) {
                            mLog.log("senderror", "发送失败");
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        executorService.start();
    }
}
