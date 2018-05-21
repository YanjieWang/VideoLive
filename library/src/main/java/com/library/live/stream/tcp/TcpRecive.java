package com.library.live.stream.tcp;

import com.library.common.TcpBytes;
import com.library.live.stream.BaseRecive;
import com.library.live.stream.IsInBuffer;
import com.library.rpc.Config;
import com.library.util.mLog;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by android1 on 2017/9/23.
 */

public class TcpRecive extends BaseRecive implements CachingStrategyCallback {
    private ServerSocket socket = null;

    private LinkedList<TcpBytes> videoList = new LinkedList<>();
    private LinkedList<TcpBytes> voiceList = new LinkedList<>();

    private Strategy strategy;

    private Thread handlerTcpThread;

    private boolean mRun = false;

    private ObjectInputStream ois;

    public TcpRecive(int port) {
        try {
            socket = new ServerSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        strategy = new Strategy();
        strategy.setCachingStrategyCallback(this);
    }


    @Override
    public void startRevice() {
        mLog.log("TcpReceive", "starRevice");
        videoList.clear();
        voiceList.clear();
        frameBuffer.clear();
        strategy.star();
        startReciveTcp();
    }

    /*
     接收UDP包
     */
    private void startReciveTcp() {
        //如果socket为空，则需要手动调用write方法送入数据
        mLog.log("TcpReceive", "starReciveTcp");
        if(mRun)return;

        if (socket != null ) {
            mRun = true;
            if(handlerTcpThread != null){
                return ;
            }
            handlerTcpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mLog.log("TcpReceive", "run");
                    Socket client = null;

                    while (mRun) {
                        if(ois == null){
                            try {
                                mLog.log("TcpReceive", "等待连接");
                                client = socket.accept();
                                ois = new ObjectInputStream(client.getInputStream());
                                mLog.log("TcpReceive", "连接成功");
                            } catch (IOException e) {
                                e.printStackTrace();
                                mLog.log("TcpReceive", "连接失败");
                            }
                        }

                        try {
                            TcpBytes tcpBytes = (TcpBytes)ois.readObject();
                            if(Config.password_enc != null) {
                                write(tcpBytes.decrypt(Config.password_enc));
                            }else{
                                write(tcpBytes);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            if(ois!=null){
                                try {
                                    ois.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                ois=null;
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            if(ois!=null){
                                try {
                                    ois.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                ois=null;
                            }
                        }
                    }
                    mLog.log("interrupt_Thread", "中断线程");
                    mRun = false;
                    handlerTcpThread = null;

                }
            });
            handlerTcpThread.start();
        }
    }


    //丢包率计算
//    int vdnum = 0;
//    int vcnum = 0;

    //添加解码数据
    public void write(TcpBytes tcpBytes) {

        if (tcpBytes.getTag() == (byte) 0x01) {
            //按序号有序插入
            addtcp(videoList, tcpBytes);

            //计算丢包率--------------------------------
//            if ((vdnum % 500) == 0) {//每500个包输出一次
//                mLog.log("UdpLoss", "视频丢包率 :  " +
//                        ((float) udpBytes.getNum() - (float) vdnum) * (float) 100 / (float) udpBytes.getNum() + "%");
//            }
//            vdnum++;

            //从排好序的队列中取出数据
            if (videoList.size() > (packetMin * 5 * 4)) {//视频帧包数量本来就多，并且音频5帧一包，这里可以多存一点，确保策略处理时音频帧比视频帧多
                //由于与读取的并发操作存在问题,这里单线程执行
                mosaicVideoFrame(videoList.removeFirst());
            }
        } else if (tcpBytes.getTag() == (byte) 0x00) {
            //按序号有序插入
            addtcp(voiceList, tcpBytes);

            //计算丢包率--------------------------------
//            if ((vcnum % 50) == 0) {//每50个包输出一次
//                mLog.log("UdpLoss", "音频丢包率 :  " +
//                        ((float) udpBytes.getNum() - (float) vcnum) * (float) 100 / (float) udpBytes.getNum() + "%");
//            }
//            vcnum++;

            //从排好序的队列中取出数据
            if (voiceList.size() > packetMin) {
                //由于与读取的并发操作存在问题,这里单线程执行
                mosaicVoiceFrame(voiceList.removeFirst());
            }
        }
    }

    private int oldudptime_vd = 0;//记录上一个包的时间
    private int oneFrame = 0;//同帧标识符(使用时间戳当同帧标识)
    private ByteBuffer frameBuffer = ByteBuffer.allocate(1024 * 80);

    /*
     将链表数据拼接成帧
     */
    private void mosaicVideoFrame(TcpBytes tcpBytes) {
        //获取并移除数据
        CheckInformation(tcpBytes.getData());
        strategy.addVideo(tcpBytes.getTime() - oldudptime_vd, tcpBytes.getTime(), tcpBytes.getData());
        oldudptime_vd = tcpBytes.getTime();
    }


    /*
    检测关键帧，回调配置信息
     */
    private void CheckInformation(byte[] frame) {
//        HEVC 00 00 00 01 40 01 0c 01 ff ff 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f ac 59 00 00 00 01 42 01 01 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f a0 0a 08 07 85 96 bb 93 24 bb 94 82 81 01 01 76 85 09 40 00 00 00 01 44 01 c0 f1 80 04 20 后面 00 00 00 01 26 为帧数据开始，普通帧为 00 00 00 01 02
//        AVC 00 00 00 01 67 42 80 15 da 05 03 da 52 0a 04 04 0d a1 42 6a 00 00 00 01 68 ce 06 e2 后面 00 00 00 01 65 为帧数据开始，普通帧为 41
        if (frame[4] == (byte) 0x67 || frame[4] == (byte) 0x40) {
            getInformation(frame);
        }
    }

    private int oldtcptime_vc = 0;//记录上一个包的时间

    /*
     将链表数据拼接成帧
     */
    private void mosaicVoiceFrame(TcpBytes tcpBytes) {
        //从一个包中取出5帧数据，交个策略处理
        strategy.addVoice(tcpBytes.getTime() - oldtcptime_vc, tcpBytes.getTime(), tcpBytes.getData());
        oldtcptime_vc = tcpBytes.getTime();
    }

    /*
    有序插入数据
     */
    private void addtcp(LinkedList<TcpBytes> list, TcpBytes udpbyte) {
        if (list.size() == 0) {
            list.add(udpbyte);
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (udpbyte.getNum() > list.get(i).getNum()) {
                    list.add(i + 1, udpbyte);
                    return;
                }
            }
            //序号最小，插在头部
            list.addFirst(udpbyte);
        }
    }

    @Override
    public void stopRevice() {
        strategy.stop();
        mRun = false;

    }

    @Override
    public void destroy() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
        mRun = false;
        stopRevice();
    }

    /*
    可以通过这个方法获得一些策略参数，根据需要决定是否需要,
    3个参数分别为 视频帧达到播放条件的缓存帧数，视频帧缓冲时间，音频帧缓冲时间
     */
    @Override
    public void setOther(int videoFrameCacheMin) {
        strategy.setVideoFrameCacheMin(videoFrameCacheMin);
    }

    @Override
    public void setIsInBuffer(IsInBuffer isInBuffer) {
        strategy.setIsInBuffer(isInBuffer);
    }


    @Override
    public void videoStrategy(byte[] video) {
        if (videoCallback != null) {
            //回调给解码器
            videoCallback.videoCallback(video);
        }
    }

    @Override
    public void voiceStrategy(byte[] voice) {
        if (voiceCallback != null) {
            //回调给解码器
            voiceCallback.voiceCallback(voice);
        }
    }
}
