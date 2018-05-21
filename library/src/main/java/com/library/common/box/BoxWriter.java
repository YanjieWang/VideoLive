package com.library.common.box;

import com.library.rpc.Config;
import com.library.util.Aes;
import com.library.util.mLog;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class BoxWriter extends Box {

    private static final String TAG = "BoxWriter";
    private OutputStream os;


    /*以下为头一个包的定义，包含以下内容
    1.       byte[0-3] 长度，假设转成int length
    2.是否加密 byte[4] 0 未加密 1 已加密
    3.视频编码 byte[5] 0 AVC(H.264) 1 HEVC(H.265)
    4.       byte[6-(length+6-1)] 数据部分，用来验证文件是否合法以及AES解密密码是否正确
                    4.1未加密，直接转成utf-8字符串，已加密，使用密码解密后转成utf-8字符串
                    4.2与"basewin_realtime_ip_camera"对比，如果一样，说明格式正确，否则密码错误或格式不正确
    */

    public synchronized boolean start(String file,byte code){

        if(started){
            mLog.log(TAG,"BoxWriter启动失败,已经启动过了");
            stop();
            return false;
        }
        started = true;
        encrypted = Config.password_enc != null;
        try {
            os = new FileOutputStream(file);
            byte[] header = FIX_HEADER.getBytes("UTF-8");
            if(encrypted){
                //需要加密
                header = Aes.Encrypt(header,0,header.length,Config.password_enc);
                if(header == null ){
                    mLog.log(TAG,"BoxWriter启动失败,视频头加密失败");
                    stop();
                    return false;
                }
            }

            //写入长度
            os.write(intToByteArray(header.length));
            mLog.log(TAG,"写入长度 header length="+header.length);

            //写入是否需要加密
            if(encrypted){
                os.write(byte1);
                mLog.log(TAG,"写入是否需要加密 需要");
            } else{
                os.write(byte0);
                mLog.log(TAG,"写入是否需要加密 不需要");
            }


            //写入视频编码
            if(code==CODE_H264){
                mLog.log(TAG,"写入视频编码 CODE_H264");
                os.write(CODE_H264);
            }else if(code==CODE_H265){
                os.write(CODE_H265);
                mLog.log(TAG,"写入视频编码 CODE_H265");
            }else{
                mLog.log(TAG,"BoxWriter启动失败,编码格式不支持");
                stop();
                return false;
            }
            mLog.log(TAG,"写入视频编码 header"+header);
            //写入视频头
            os.write(header);
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mLog.log(TAG,"BoxWriter启动失败 FileNotFoundException");
            stop();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            mLog.log(TAG,"BoxWriter启动失败 UnsupportedEncodingException");
            stop();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mLog.log(TAG,"BoxWriter启动失败 IOException");
            stop();
            return false;
        }
    }



    /*
    从流中读取数据
    格式byte[0-3] 长度，假设转成int length
    byte[4] 帧类型 0，视频 1 音频 2，视频信息 3.音频信息
    byte[5-12] 时间戳 long型 8字节
    byte[13-16] 帧flag
    byte[17-(length+17-1)] 数据部分为明文或null
    */

    public synchronized boolean writeData(byte[] by,byte frameType,long time,int flag){
        if(!started){
            mLog.log(TAG,"写入失败,BoxWriter未启动");
            return false;
        }
        if( by == null ){
            mLog.log(TAG,"data is null");
            return false;
        }
        int length = by.length;
        try {
            byte[] data = by;
            if (encrypted) {
                data = Aes.Encrypt(data,0,data.length,Config.password_enc);
                if(data == null){
                    mLog.log(TAG,"数据加密失败");
                    return false;
                }
                length = data.length;
            }
            os.write(intToByteArray(length));
            os.write(frameType);
            os.write(longToBytes(time));
            os.write(intToByteArray(flag));
            os.write(data, 0, data.length);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void stop() {
        if(os != null){
            try {
                os.flush();
                os.close();
                os = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        started = false;
    }

}
