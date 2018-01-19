package com.library.common;

import com.library.util.Aes;

import java.io.Serializable;


/**
 * UDP协议内容：
 * <p>
 * UDP头：----
 * 1字节 音视频tag  0音频 1视频
 * 4字节 包序号
 * <p>
 * 视频：----
 * 1字节 帧标记tag  0帧头 1帧中间 2帧尾 3独立帧
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * <p>
 * 音频：----
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * ......
 * 4字节 时间戳
 * 2字节 内容长度（纯数据部分）
 * length 数据内容
 * ......音频5帧一包，所以5个相同数据段
 * <p>
 * 如果修改协议记得修改下面去除的头长度，以及服务器头长度设置
 */
public class TcpBytes implements Serializable{
    private boolean encrypted = false;
    private byte tag;//0音频 1视频
    private int num;//包序号
    private byte[] data;
    private int time;//时间戳


    public TcpBytes(byte tag, int num, int time, byte[] data) {
        this.tag = tag;
        this.num = num;
        if (tag == (byte) 0x01) {//视频
            this.time = time;
            this.data = data;
        } else if (tag == (byte) 0x00) {//音频
            this.time = time;
            this.data = data;
        }
    }

    public TcpBytes encrypt(String pass){
        if(!encrypted) {
            data = Aes.Encrypt(data, 0, data.length, pass);
            encrypted = true;
        }
        return this;
    }

    public TcpBytes decrypt(String pass){
        if(encrypted) {
            data = Aes.Decrypt(data, 0, data.length, pass);
            encrypted = false;
        }
        return this;
    }

    public int getTag() {
        return tag;
    }

    public int getNum() {
        return num;
    }

    public int getTime() {
        return time;
    }

    public byte[] getData() {
        return data;
    }

}
