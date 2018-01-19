package com.library.live;


import com.library.common.UdpControlInterface;
import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseRecive;
import com.library.live.vc.VoiceTrack;
import com.library.live.vd.VDDecoder;
import com.library.live.view.PlayerView;

/**
 * Created by android1 on 2017/10/13.
 */

public class Player {
    private VDDecoder vdDecoder;
    private VoiceTrack voiceTrack;
    private BaseRecive baseRecive;
    private PlayerView playerView;
    private WriteMp4 writeMp4;

    private Player(PlayerView playerView, String codetype, BaseRecive baseRecive, UdpControlInterface udpControl, int multiple,String path) {
        this.baseRecive = baseRecive;
        this.playerView = playerView;
        this.baseRecive.setUdpControl(udpControl);

        if(path != null)writeMp4 = new WriteMp4(path);
        vdDecoder = new VDDecoder(playerView, codetype, baseRecive,writeMp4);
        voiceTrack = new VoiceTrack(baseRecive,writeMp4);
        voiceTrack.setIncreaseMultiple(multiple);
        //文件录入类



    }

    public void setVoiceIncreaseMultiple(int multiple) {
        voiceTrack.setIncreaseMultiple(multiple);
    }


    public void start() {
        voiceTrack.start();
        vdDecoder.start();
        baseRecive.startRevice();
    }

    public void stop() {
        baseRecive.stopRevice();
        vdDecoder.stop();
        voiceTrack.stop();
        playerView.stop();
    }

    public void destroy() {
        baseRecive.destroy();
        vdDecoder.destroy();
        voiceTrack.destroy();
        if(writeMp4!=null)writeMp4.stop();
    }

    public void starRecode() {
        if(writeMp4!=null)writeMp4.start();
    }

    public void stopRecode() {
        if(writeMp4!=null)writeMp4.stop();
    }


    public int getReciveStatus() {
        return baseRecive.getReciveStatus();
    }

    public static class Buider {
        private PlayerView playerView;
        private BaseRecive baseRecive;
        private String codetype = VDDecoder.H264;
        private UdpControlInterface udpControl = null;
        private int multiple = 1;

        private int udpPacketCacheMin = 3;//udp包最小缓存数量，用于udp包排序
        private int videoFrameCacheMin = 6;//视频帧达到播放标准的数量

        private IsOutBuffer isOutBuffer = null;//缓冲接口回调

        String VideoPath = null;

        public Buider(PlayerView playerView) {
            this.playerView = playerView;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }


        public Buider setPullMode(BaseRecive baseRecive) {
            this.baseRecive = baseRecive;
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setMultiple(int multiple) {
            this.multiple = multiple;
            return this;
        }

        public Buider setUdpPacketCacheMin(int udpPacketCacheMin) {
            this.udpPacketCacheMin = udpPacketCacheMin;
            return this;
        }

        public Buider setVideoFrameCacheMin(int videoFrameCacheMin) {
            this.videoFrameCacheMin = videoFrameCacheMin;
            return this;
        }


        public Buider setIsOutBuffer(IsOutBuffer isOutBuffer) {
            this.isOutBuffer = isOutBuffer;
            return this;
        }

        public Buider setBufferAnimator(boolean bufferAnimator) {
            playerView.setBufferAnimator(bufferAnimator);
            return this;
        }

        public Buider setCenterScaleType(boolean isCenterScaleType) {
            playerView.setCenterScaleType(isCenterScaleType);
            return this;
        }

        public Buider setVideoPath(String path) {
            VideoPath = path;
            return this;
        }

        public Player build() {
            baseRecive.setPacketCacheMin(udpPacketCacheMin);
            baseRecive.setWeightCallback(playerView);//将playerView接口设置给baseRecive用以回调图像比例
            baseRecive.setIsInBuffer(playerView);//将playerView接口设置给baseRecive用以回调缓冲状态
            playerView.setIsOutBuffer(isOutBuffer);//给playerView设置isOutBuffer接口用以将缓冲状态回调给客户端
            baseRecive.setOther(videoFrameCacheMin);
            return new Player(playerView, codetype, baseRecive, udpControl, multiple,VideoPath);
        }
    }
}
