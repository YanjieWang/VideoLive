package com.library.rpc;

import android.content.Context;

import com.library.live.Publish;
import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseSend;
import com.library.util.mLog;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by wangyanjie on 18-3-27.
 */

public class Commond {

    public static class Request implements Serializable {

        public static abstract class RequestBase implements Serializable {
            public String pass;

            public abstract void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue,Publish publish);

            public boolean checkPass(ArrayBlockingQueue<Response> responseQueue) {

                if (pass == null || !pass.equals(Config.password)) {
                    if (responseQueue != null) {
                        Response res = new Response();
                        res.success = false;
                        res.content = "验证失败,密码错误";
                        try {
                            responseQueue.put(res);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return false;
                }
                return true;
            }

        }

        public static class UpdateControlPort extends RequestBase implements Serializable {
            int port;

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

                if (checkPass(responseQueue)) {
                    Commond.Response res = new Commond.Response();
                    Config.control_port = port;
                    Config.saveConfig(con);
                    res.success = true;
                    res.content = "重设配置端口成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class ChangePass extends RequestBase implements Serializable {
            public String pass_new;

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {
                mLog.log("Commond", "pass_new=" + pass_new);
                mLog.log("Commond", "pass=" + pass);
                mLog.log("Commond", "Config.password=" + Config.password);
                mLog.log("Commond", "equals=" + Config.password.equals(pass));
                if (checkPass(responseQueue)) {
                    Commond.Response res = new Commond.Response();
                    if (pass_new == null || pass_new.length() < 6) {
                        res.content = "重设密码失败,新密码无效";
                    } else {
                        Config.password = pass_new;
                        Config.saveConfig(con);
                        res.success = true;
                        res.content = "重设密码成功";
                    }
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        public static class ChangeEncPass extends RequestBase implements Serializable {
            public String enc_pass;

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {
                mLog.log("Commond", "enc_pass=" + enc_pass);
                mLog.log("Commond", "pass=" + pass);
                mLog.log("Commond", "Config.password=" + Config.password);
                mLog.log("Commond", "equals=" + Config.password.equals(pass));

                if (checkPass(responseQueue)) {
                    Commond.Response res = new Commond.Response();
                    if(publish == null) {
                        res.content = "加密密码设置失败,推流器未初始化";
                    } else if(publish.getPublishStatus() != BaseSend.PUBLISH_STATUS_STOP){
                        res.content = "加密密码设置失败,请先停止服务端推流";
                    } else if(publish.getRecodeStatus()!= WriteMp4.RECODE_STATUS_STOP){
                        res.content = "加密密码设置失败,请先停止服务端录制";
                    } else if (enc_pass == null || (enc_pass.length() != 16 && enc_pass.length()!=0)) {
                        res.content = "加密密码设置失败,新密码长度不为16";
                    } else {
                        if(enc_pass==null) enc_pass = "";
                        Config.password_enc = enc_pass;
                        Config.saveConfig(con);
                        res.success = true;
                        res.content = "重设加密密码成功";
                    }
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        public static class UpdatePrograms extends RequestBase implements Serializable {
            public String pushMode;//"UDP/TCP"
            public String pushIp;
            public String framerate;
            public String videoCode;
            public String ispreview;
            public String publishbitrate;
            public String collectionbitrate;
            public int pu_width;
            public int pu_height;
            public int pr_width;
            public int pr_height;
            public boolean rotate;
            public int collectionbitrate_vc;
            public int publishbitrate_vc;
            public String recodeDirPath;
            public String pictureDirPath;
            public int ControlPort;

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

            }
        }

        public static class StartRecode extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue ,Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.startRecode();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "录像开启成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class StopRecode extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue,Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.stopRecode();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "录像停止成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class StartPush extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.start();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "推流开启成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class StopPush extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.stop();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "推流关闭成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class SwitchCamera extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.rotate();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "推流关闭成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static class Rotate extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue, Publish publish) {

                if (checkPass(responseQueue)) {
                    publish.rotate();
                    Commond.Response res = new Commond.Response();
                    res.success = true;
                    res.content = "视频旋转成功";
                    try {
                        responseQueue.put(res);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        public String pushMode;//"UDP/TCP"
        public String pushIp;
        public String framerate;
        public String videoCode;
        public String ispreview;
        public String publishbitrate;
        public String collectionbitrate;
        public int pu_width;
        public int pu_height;
        public int pr_width;
        public int pr_height;
        public boolean rotate;
        public int collectionbitrate_vc;
        public int publishbitrate_vc;
        public String recodeDirPath;
        public String pictureDirPath;
        public int ControlPort;
    }

    public static class Response implements Serializable {
        public boolean success = false;
        public String content;

        @Override
        public String toString() {
            return "Response{" +
                    "success=" + success +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}
