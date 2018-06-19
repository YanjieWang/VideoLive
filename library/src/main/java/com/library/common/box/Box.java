package com.library.common.box;


import android.media.MediaFormat;

import com.library.util.Aes;
import com.library.util.ByteUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Box {
    private static final String TAG = "Box";
    public static final int MAX_LENGTH = 10*1024*1024;
    public static final String FIX_HEADER = "basewin_realtime_ip_camera";
    public static final byte byte0 = 0;
    public static final byte byte1 = 1;

    public static final byte CODE_H264=0;
    public static final byte CODE_H265=1;

    public static final byte TAG_VIDEO = 0;
    public static final byte TAG_AUDIO = 1;
    public static final byte TAG_VIDEO_INFO = TAG_VIDEO+10;
    public static final byte TAG_AUDIO_INFO = TAG_AUDIO+10;
    private static final ByteBuffer buffer = ByteBuffer.allocate(8);

    protected boolean encrypted;
    protected byte code;
    protected boolean started;

    protected byte[] intBytes = new byte[4];
    protected byte[] longBytes = new byte[8];

    public boolean isStarted(){
        return started;
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }
    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    //byte 数组与 long 的相互转换
    public static byte[] longToBytes(long x) {
        buffer.clear();
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.clear();
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    public byte getCode(){
        return code;
    }

    public static class Frame {
        public byte tag;//帧类型 0，视频 1 音频 2，视频信息 3.音频信息
        public long time;
        public int flag;
        public byte[] data;

        public Frame(byte tag, long time, int flag, byte[] data) {
            this.tag = tag;
            this.time = time;
            this.flag = flag;
            this.data = data;
        }

        @Override
        public String toString() {
            return "Frame{" +
                    "tag=" + tag +
                    ", time=" + time +
                    ", flag=" + flag +
                    ", data=" + Arrays.toString(data) +
                    '}';
        }
    }

    public static ByteBuffer getH264SPS(byte[] information) {
        mLog.log(TAG, "h264 information" + ByteUtil.byte_to_16(information));
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x68) {
                byte[] bytes = new byte[i];
                System.arraycopy(information, 0, bytes, 0, i);
                mLog.log(TAG, "h264 sps" + ByteUtil.byte_to_16(bytes));
                return ByteBuffer.wrap(bytes);
            }
        }
        return null;
    }

    public static ByteBuffer getH264PPS(byte[] information) {
        mLog.log(TAG, "h264 information" + ByteUtil.byte_to_16(information));
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x68) {
                byte[] bytes = new byte[information.length - i];
                System.arraycopy(information, i, bytes, 0, information.length - i);
                mLog.log(TAG, "h264 pps" + ByteUtil.byte_to_16(bytes));
                return ByteBuffer.wrap(bytes);
            }

        }
        return null;
    }

    public static ByteBuffer getH265information(byte[] information) {
        mLog.log(TAG, "h264 information" + ByteUtil.byte_to_16(information));
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x26) {
                byte[] bytes = new byte[i];
                System.arraycopy(information, 0, bytes, 0, i);
                mLog.log(TAG, "h265信息" + ByteUtil.byte_to_16(bytes));
                return ByteBuffer.wrap(bytes);
            }
        }
        return null;
    }
}
