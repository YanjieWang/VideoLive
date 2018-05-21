package com.library.common.box;

import com.library.rpc.Config;
import com.library.util.Aes;
import com.library.util.mLog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.BadPaddingException;

public class BoxReader extends Box{
    private static final String TAG = "BoxReader";

    private InputStream is;


    /*以下为头一个包的定义，包含以下内容
    1.       byte[0-3] 长度，假设转成int length
    2.是否加密 byte[4] 0 未加密 1 已加密
    3.视频编码 byte[5] 0 AVC(H.264) 1 HEVC(H.265)
    4.       byte[6-(length+6-1)] 数据部分，用来验证文件是否合法以及AES解密密码是否正确
                    4.1未加密，直接转成utf-8字符串，已加密，使用密码解密后转成utf-8字符串
                    4.2与"basewin_realtime_ip_camera"对比，如果一样，说明格式正确，否则密码错误或格式不正确
    */

    public synchronized boolean start(String file){

        if(started){
            mLog.log(TAG,"BoxReader启动失败,已经启动过了");
            stop();
            return false;
        }
        started = true;
        try {
            is = new FileInputStream(file);
            //获取内容长度
            int count = is.read(intBytes);
            while(count < 4 && count >= 0){
                int countTemp = is.read(intBytes,count,4-count);
                if(countTemp < 0){
                    mLog.log(TAG,"BoxReader启动失败,数据流已结束");
                    stop();
                    return false;
                }
                count = count + countTemp;
            }
            int dataLength = byteArrayToInt(intBytes);
            mLog.log(TAG,"获取 dataLength="+dataLength);
            if(dataLength<=0||dataLength>MAX_LENGTH){
                mLog.log(TAG,"BoxReader启动失败，数据长度非法,长度="+dataLength);
                stop();
                return false;
            }

            //获取加密标志
            int encryptedFlag = is.read();
            mLog.log(TAG,"获取 encryptedFlag="+encryptedFlag);
            if(encryptedFlag<0){
                mLog.log(TAG,"BoxReader启动失败,获取加密标志失败,数据流已结束");
                stop();
                return false;
            } else {
                encrypted = encryptedFlag == byte1;
            }


            //获取编码类型
            int codeFlag = is.read();
            mLog.log(TAG,"获取 codeFlag="+codeFlag);
            if(codeFlag < 0){
                mLog.log(TAG,"BoxReader启动失败,获取编码类型失败,数据流已结束");
                stop();
                return false;
            } else {
                if(codeFlag == CODE_H264){
                    code = CODE_H264;
                } else if(codeFlag == CODE_H265){
                    code = CODE_H265;
                } else {
                    mLog.log(TAG,"BoxReader启动失败 获取编码类型失败,未知编码");
                    stop();
                    return false;
                }
            }



            //获取数据
            count = 0 ;
            byte[] data = new byte[dataLength];
            while(count < dataLength && count >= 0){
                int countTemp = is.read(data,count,dataLength - count);
                if(countTemp < 0){
                    mLog.log(TAG,"BoxReader启动失败，数据获取失败,数据流已结束");
                    stop();
                    return false;
                }
                count = count + countTemp;
            }

            if (encrypted) {
                if(Config.password_enc==null){
                    mLog.log(TAG,"BoxReader启动失败，数据已加密，Box密码为null");
                    stop();
                    return false;
                }
                data = Aes.Decrypt(data,0,dataLength,Config.password_enc);
                if(data == null){
                    mLog.log(TAG,"BoxReader启动失败，数据已加密，数据解密失败");
                    stop();
                    return false;
                }
            }

            String compare = new String(data, "UTF-8");
            mLog.log(TAG,"获取 compare="+compare);
            if(FIX_HEADER.equals(compare)){
                return true;
            }else{
                mLog.log(TAG,"BoxReader启动失败，文件格式错误");
                stop();
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mLog.log(TAG,"BoxReader启动失败");
            stop();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mLog.log(TAG,"BoxReader启动失败");
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
    public synchronized Frame readData(){
        if(!started){
            mLog.log(TAG,"读取失败,BoxReader未启动");
            return null;
        }
        if(is == null) {
            mLog.log(TAG,"读取失败，InputStream is null");
            return null;
        }

        try {
            //获取内容长度
            int count = is.read(intBytes);
            while(count < 4 && count >= 0){
                int countTemp = is.read(intBytes,count,4-count);
                if(countTemp < 0){
                    mLog.log(TAG,"获取数据长度失败,数据流已结束");
                    return null;
                }
                count = count + countTemp;
            }
            int dataLength = byteArrayToInt(intBytes);
            if(dataLength<=0||dataLength>MAX_LENGTH){
                mLog.log(TAG,"数据长度非法,长度="+dataLength);
                return null;
            }


            //获取数据帧类型
            int tag = is.read();
            if(tag < 0){
                mLog.log(TAG,"BoxReader 获取数据帧类型失败,数据流已结束");
                return null;
            } else {
                if((tag == TAG_VIDEO) || (tag==TAG_AUDIO)||(tag==TAG_VIDEO_INFO)||(tag==TAG_VIDEO_INFO)){
                    code = (byte) tag;
                } else {
                    mLog.log(TAG,"BoxReader 获取数据帧类型失败 未知 tag=" +tag);
                    return null;
                }
            }

            //获取时间戳
            count = is.read(longBytes);
            while(count < 8 && count >= 0){
                int countTemp = is.read(longBytes,count,8-count);
                if(countTemp < 0){
                    mLog.log(TAG,"获取数据时间戳失败,数据流已结束");
                    return null;
                }
                count = count + countTemp;
            }
            long time = bytesToLong(longBytes);


            //获取帧flag
            count = is.read(intBytes);
            while(count < 4 && count >= 0){
                int countTemp = is.read(intBytes,count,4-count);
                if(countTemp < 0){
                    mLog.log(TAG,"获取数据长度失败,数据流已结束");
                    return null;
                }
                count = count + countTemp;
            }
            int flag = byteArrayToInt(intBytes);


            //获取数据
            count = 0 ;
            byte[] data = new byte[dataLength];
            while(count < dataLength && count >= 0){
                int countTemp = is.read(data,count,dataLength - count);
                if(countTemp < 0){
                    mLog.log(TAG,"获取数据失败,数据流已结束");
                    return null;
                }
                count = count + countTemp;
            }



            if (encrypted) {
                if(Config.password_enc==null){
                    mLog.log(TAG,"解密失败，数据已加密，Box密码为null");
                    return null;
                }
                data = Aes.Decrypt(data,0,dataLength,Config.password_enc);
                if(data == null){
                    mLog.log(TAG,"解密失败，解密揭秘失败，可能密码错误");
                    return null;
                }
            }
            return new Frame((byte)tag,time,flag,data);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void stop() {
        if(is != null){
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        started = false;
    }
}
