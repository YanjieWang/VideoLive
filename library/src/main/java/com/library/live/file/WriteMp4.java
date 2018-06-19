package com.library.live.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import com.library.common.box.Box;
import com.library.common.box.BoxWriter;
import com.library.rpc.Config;
import com.library.util.ByteUtil;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

/**
 * Created by android1 on 2017/10/20.
 */

public class WriteMp4 {
    private static final String TAG = "WriteMp4";

    public static final int RECODE_STATUS_START = 0;
    public static final int RECODE_STATUS_STOP = 1;
    public static final int RECODE_STATUS_READY = 2;
    public int RECODE_STATUS = RECODE_STATUS_STOP;

    private MediaMuxer mMediaMuxer = null;
    public static final int video = 0;
    public static final int voice = 1;

    private String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive";
    private String path = null;
    private MediaFormat videoFormat = null;
    private MediaFormat voiceFormat = null;

    private int videoTrackIndex;
    private int voiceTrackIndex;
    private long presentationTimeUsVD = 0;
    private long presentationTimeUsVE = 0;

    private boolean isShouldAutoStart = false;
    private boolean canStart = true;

    private int frameNum = 0;
    private final Object lock = new Object();

    private boolean saveStream = false;


    private BoxWriter br = new BoxWriter();



    private boolean hasWriteVoiceInfo = false;
    private boolean hasWriteVideoInfo = false;
    ByteBuffer videoBuffer;
    MediaCodec.BufferInfo videoBufferInfo;
    ByteBuffer voiceBuffer;
    MediaCodec.BufferInfo voiceBufferInfo;



    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    public WriteMp4(String dirpath) {
        if (!TextUtils.isEmpty(dirpath) && !dirpath.equals("")) {
            this.dirpath = dirpath;
        }
        saveStream = true;
    }

    public WriteMp4(String dirpath,boolean saveStream) {
        if (!TextUtils.isEmpty(dirpath) && !dirpath.equals("")) {
            this.dirpath = dirpath;
        }
        this.saveStream = saveStream;
    }

    public void addTrack(MediaFormat mediaFormat, int flag) {
        new Throwable().printStackTrace();
        mLog.log(TAG,"addTrack flag ="+flag);
        mLog.log(TAG,"addTrack mediaFormat ="+mediaFormat);

        if (flag == video) {
            videoFormat = mediaFormat;
        } else if (flag == voice) {
            if(mediaFormat.getByteBuffer("csd-0") != null){
                mLog.log(TAG,"addTrack csd-0 ="+ByteUtil.byte_to_16(mediaFormat.getByteBuffer("csd-0").array()));
                voiceFormat = mediaFormat;
            }

        }
        if (videoFormat != null && voiceFormat != null) {
            if (isShouldAutoStart) {
                start();
            }
        }
    }


    public void start() {
        RECODE_STATUS = RECODE_STATUS_READY;
        synchronized (lock) {
            if (voiceFormat != null && videoFormat != null && mMediaMuxer == null && canStart) {
                isShouldAutoStart = false;
                canStart = false;
                setPath();
                if(!saveStream) {
                    try {
                        mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                        videoTrackIndex = mMediaMuxer.addTrack(videoFormat);
                        voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                        mMediaMuxer.start();
                        presentationTimeUsVE = 0;
                        presentationTimeUsVD = 0;
                        frameNum = 0;
                        RECODE_STATUS = RECODE_STATUS_START;
                        mLog.log(TAG, "文件录制启动");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    presentationTimeUsVE = 0;
                    presentationTimeUsVD = 0;
                    frameNum = 0;
                    RECODE_STATUS = RECODE_STATUS_START;
                    mLog.log(TAG, "码流录制启动");
                }
            } else {
                isShouldAutoStart = true;
            }

            if(saveStream && !br.isStarted()){
                mLog.log(TAG, "初始化缓存视频信息 voiceBuffer="+voiceBuffer);
                mLog.log(TAG, "初始化缓存视频信息 videoBuffer="+videoBuffer);
                mLog.log(TAG, "初始化缓存视频信息 hasWriteVoiceInfo=" + hasWriteVoiceInfo);
                mLog.log(TAG, "初始化缓存视频信息 hasWriteVideoInfo=" + hasWriteVideoInfo);
                setPath();
                br.start(path,Box.CODE_H264);
                if(!hasWriteVoiceInfo && voiceBuffer != null){
                    write(voice,voiceBuffer,voiceBufferInfo);
                }
                if(!hasWriteVideoInfo && videoBuffer != null){
                    write(video,videoBuffer,videoBufferInfo);
                }
            }

        }
    }

    public void write(int flag, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        mLog.log(TAG,"RECODE_STATUS="+RECODE_STATUS+",flag="+flag);
        boolean is_info = bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        if (!is_info) {
            if (RECODE_STATUS == RECODE_STATUS_START) {
                if(!saveStream) {
                    if (flag == video) {
                        if (bufferInfo.presentationTimeUs > presentationTimeUsVD) {//容错
                            presentationTimeUsVD = bufferInfo.presentationTimeUs;
                            if (frameNum == 0) {//视频帧第一帧必须为关键帧
                                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                    mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                                    frameNum++;
                                }
                            } else {
                                mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                                frameNum++;
                            }
                        }
                    } else if (flag == voice) {
                        if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                            presentationTimeUsVE = bufferInfo.presentationTimeUs;
                            mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
                        }
                    }
                } else {
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    outputBuffer.get(outData);
                    if(flag == video){
                        frameNum++;
                        br.writeData(outData, Box.TAG_VIDEO, bufferInfo.presentationTimeUs, bufferInfo.flags);
                        mLog.log(TAG,"write Video frameNum="+frameNum);
                    } else {
                        br.writeData(outData, Box.TAG_AUDIO, bufferInfo.presentationTimeUs, bufferInfo.flags);
                        mLog.log(TAG,"write Audio");
                    }

                }
            }
        } else {
            if(saveStream) {
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                outputBuffer.get(outData);
                if (flag == video) {
                    frameNum++;
                    videoBuffer = ByteBuffer.wrap(outData);
                    videoBufferInfo = new MediaCodec.BufferInfo();
                    videoBufferInfo.set(0,outData.length,bufferInfo.presentationTimeUs,bufferInfo.flags);
                    if(br.isStarted()){
                        hasWriteVideoInfo = true;
                        mLog.log(TAG, "info update hasWriteVideoInfo=" + hasWriteVideoInfo);
                        br.writeData(getshortH264Information(outData), Box.TAG_VIDEO_INFO, bufferInfo.presentationTimeUs, bufferInfo.flags);
                        mLog.log(TAG, "write Video info frameNum=" + frameNum);
                    } else {
                        hasWriteVideoInfo = false;
                        mLog.log(TAG, "cache temp Video info frameNum=" + frameNum);
                    }

                } else {

                    //cache voice info
                    voiceBuffer = ByteBuffer.wrap(outData);
                    voiceBufferInfo = new MediaCodec.BufferInfo();
                    voiceBufferInfo.set(0,outData.length,bufferInfo.presentationTimeUs,bufferInfo.flags);
                    if(br.isStarted()) {
                        br.writeData(outData, Box.TAG_AUDIO_INFO, bufferInfo.presentationTimeUs, bufferInfo.flags);
                        hasWriteVoiceInfo = true;
                        mLog.log(TAG, "write Audio info");
                    } else {
                        hasWriteVoiceInfo = false;
                        mLog.log(TAG, "cache Audio info");
                    }
                }
            }
        }


    }

    private void setPath() {
        OtherUtil.CreateDirFile(dirpath);
        if(!saveStream) {
            path = dirpath + File.separator + sdf.format(System.currentTimeMillis()) + ".mp4";
        } else {
            path = dirpath + File.separator + sdf.format(System.currentTimeMillis()) + ".stream";
        }
    }

    public void stop() {
        mLog.log(TAG, "stop()");
        synchronized (lock) {
            if (RECODE_STATUS == RECODE_STATUS_START) {
                RECODE_STATUS = RECODE_STATUS_STOP;
                try {
                    if(!saveStream) {
                        mMediaMuxer.release();
                    } else {
                        br.stop();

                    }
                    mLog.log(TAG, "文件录制关闭");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mMediaMuxer = null;
                    //文件过短或异常，删除文件
                    File file = new File(path);
                    if (frameNum < 20 && file.exists()) {
                        mLog.log(TAG, "长度过短，删除");
                        file.delete();
                    }
                }
            }else{
                if(saveStream && path != null){
                    //文件过短或异常，删除文件
                    File file = new File(path);
                    if (frameNum < 20 && file.exists()) {
                        mLog.log(TAG, "长度过短，删除");
                        file.delete();
                    }
                }
            }


            isShouldAutoStart = false;
            canStart = true;
            RECODE_STATUS = RECODE_STATUS_STOP;
            hasWriteVoiceInfo = false;
            hasWriteVideoInfo = false;
        }
    }

    public void destroy() {
        stop();
    }

    public int getRecodeStatus() {
        return RECODE_STATUS;
    }


    private byte[] getshortH264Information(byte[] information) {
        for (int i = 5; i < information.length; i++) {
            if (information[i] == (byte) 0x00
                    && information[i + 1] == (byte) 0x00
                    && information[i + 2] == (byte) 0x00
                    && information[i + 3] == (byte) 0x01
                    && information[i + 4] == (byte) 0x65) {
                byte[] bytes = new byte[i];
                System.arraycopy(information, 0, bytes, 0, bytes.length);
                return bytes;
            }
        }
        return information;
    }

}
