package com.library.live.vd;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseRecive;
import com.library.live.stream.VideoCallback;
import com.library.live.view.PlayerView;
import com.library.util.ByteUtil;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VDDecoder implements SurfaceHolder.Callback, VideoInformationInterface, VideoCallback {
    public static final String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static final String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;
    private String MIME_TYPE = H264;

    //解码分辨率
    private SurfaceHolder holder;
    //解码器
    private MediaCodec mCodec;
    private MediaFormat mediaFormat = null;
    //是否播放
    private boolean isDecoder = false;
    private boolean isMediaCodecInit = false;
    private boolean isSurfaceCreated = false;

    //解码器配置信息
    private byte[] information = null;

    /**
     * 初始化解码器
     */

    private WriteMp4 writeMp4;


    public VDDecoder(PlayerView playerView, String codetype, BaseRecive baseRecive,WriteMp4 writeMp4) {
        this.holder = playerView.getHolder();
        MIME_TYPE = codetype;
        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        baseRecive.setVideoCallback(this);
        baseRecive.setInformaitonInterface(this);
        holder.addCallback(this);
        // 设置该组件让屏幕不会自动关闭
        holder.setKeepScreenOn(true);
        this.writeMp4 = writeMp4;
    }

    /*
    回调包含解码器配置信息的byte，比如h264的sps,pps等（后面还包部分视频数据）
     */
    @Override
    public void Information(byte[] important) {
        information = important;
        if (isSurfaceCreated) {
            beginCodec();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceCreated = true;
        beginCodec();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceCreated = false;
        if (isMediaCodecInit) {
            mCodec.stop();
            isMediaCodecInit = false;
        }
    }

    private synchronized void beginCodec() {
        if (isSurfaceCreated && information != null) {
            isSurfaceCreated = false;
            if (mediaFormat == null) {
                mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 0, 0);//分辨率等信息由sps提供，这里可以随便设置
            }
            if (MIME_TYPE.equals(H264)) {
                mediaFormat.setByteBuffer("csd-0", getH264SPS());
                mediaFormat.setByteBuffer("csd-1", getH264PPS());

            } else if (MIME_TYPE.equals(H265)) {
                mediaFormat.setByteBuffer("csd-0", getH265information());
            }

            mCodec.configure(mediaFormat, holder.getSurface(), null, 0);
            mCodec.start();
            isMediaCodecInit = true;
        }
    }

    private ByteBuffer getH264SPS() {
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x68) {
                byte[] bytes = new byte[i];
                System.arraycopy(information, 0, bytes, 0, i);
                mLog.log("VDDecoder_information", "h264 sps" + ByteUtil.byte_to_16(bytes));
                return ByteBuffer.wrap(bytes);
            }
        }
        return null;
    }

    private ByteBuffer getH264PPS() {
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x68) {
                for (int j = i + 5; j < information.length; j++) {
                    if (information[j] == (byte) 0x00
                            && information[j + 1] == (byte) 0x00
                            && information[j + 2] == (byte) 0x00
                            && information[j + 3] == (byte) 0x01
                            && information[j + 4] == (byte) 0x65) {
                        byte[] bytes = new byte[j - i];
                        System.arraycopy(information, i, bytes, 0, j - i);
                        mLog.log("VDDecoder_information", "h264 pps" + ByteUtil.byte_to_16(bytes));
                        return ByteBuffer.wrap(bytes);
                    }
                }
            }
        }
        return null;
    }

    private ByteBuffer getH265information() {
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x26) {
                byte[] bytes = new byte[i];
                System.arraycopy(information, 0, bytes, 0, i);
                mLog.log("VDDecoder_information", "h265信息" + ByteUtil.byte_to_16(bytes));
                return ByteBuffer.wrap(bytes);
            }
        }
        return null;
    }

    public void start() {
        isDecoder = true;
    }

    /**
     * 停止解码
     */
    public void stop() {
        isDecoder = false;
    }

    public void destroy() {
        isSurfaceCreated = false;
        isDecoder = false;
        if (isMediaCodecInit) {
            isMediaCodecInit = false;
            mCodec.release();
            mCodec = null;
        }
    }

    @Override
    public void videoCallback(byte[] video) {
        if (isDecoder && isMediaCodecInit) {
            decoder(video);
            writeFile(video, video.length);
        }
    }

    private MediaCodec.BufferInfo debufferInfo = new MediaCodec.BufferInfo();

    public void decoder(byte[] video) {
        try {
            int inputBufferIndex = mCodec.dequeueInputBuffer(OtherUtil.waitTime);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(video, 0, video.length);
                mCodec.queueInputBuffer(inputBufferIndex, 0, video.length, 0, 0);
            } else {
                mLog.log("dcoder_failure", "dcoder failure_VD");
                return;
            }
            int outputBufferIndex = mCodec.dequeueOutputBuffer(debufferInfo, OtherUtil.waitTime);

            while (outputBufferIndex >= 0) {
                //true : 将解码的数据显示到surface上
                mCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mCodec.dequeueOutputBuffer(debufferInfo, OtherUtil.waitTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    写入文件
     */
    private ByteBuffer writebuffer = ByteBuffer.allocate(1024 * 80);
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private void writeFile(byte[] output, int length) {
        writebuffer.clear();
        if (MIME_TYPE.equals(H264)) {
//        AVC 00 00 00 01 67 42 80 15 da 05 03 da 52 0a 04 04 0d a1 42 6a 00 00 00 01 68 ce 06 e2后面00 00 00 01 65为帧数据开始，普通帧为41
            if (output[4] == (byte) 0x67) {//KEY
                for (int i = 5; i < length; i++) {
                    if (output[i] == (byte) 0x00
                            && output[i + 1] == (byte) 0x00
                            && output[i + 2] == (byte) 0x00
                            && output[i + 3] == (byte) 0x01
                            && output[i + 4] == (byte) 0x65) {
                        bufferInfo.set(0, length - i, OtherUtil.getFPS(), MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        writebuffer.put(output, i, length - i);
                        break;
                    }
                }
            } else {//NO KEY
                bufferInfo.set(0, length, OtherUtil.getFPS(), MediaCodec.CRYPTO_MODE_UNENCRYPTED);
                writebuffer.put(output);
            }
        } else if (MIME_TYPE.equals(H265)) {
//        HEVC[00 00 00 01 40 01 0c 01 ff ff 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f ac 59 00 00 00 01 42 01 01 01 60 00 00 03 00 b0 00 00 03 00 00 03 00 3f a0 0a 08 07 85 96 bb 93 24 bb 94 82 81 01 01 76 85 09 40 00 00 00 01 44 01 c0 f1 80 04 20]后面00 00 00 01 26为帧数据开始，普通帧为00 00 00 01 02            if (output[4] == (byte) 0x40) {
            if (output[4] == (byte) 0x40) {//KEY
                for (int i = 5; i < length; i++) {
                    if (output[i] == (byte) 0x00
                            && output[i + 1] == (byte) 0x00
                            && output[i + 2] == (byte) 0x00
                            && output[i + 3] == (byte) 0x01
                            && output[i + 4] == (byte) 0x26) {
                        bufferInfo.set(0, length - i, OtherUtil.getFPS(), MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        writebuffer.put(output, i, length - i);
                        break;
                    }
                }
            } else {//NO KEY
                bufferInfo.set(0, length, OtherUtil.getFPS(), MediaCodec.CRYPTO_MODE_UNENCRYPTED);
                writebuffer.put(output);
            }
        }
        writeMp4.write(WriteMp4.video, writebuffer, bufferInfo);
    }
}