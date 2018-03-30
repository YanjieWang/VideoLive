package com.library.rpc;

import android.content.Context;

import com.library.util.mLog;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by wangyanjie on 18-3-27.
 */

public class Commond {

    public static class Request implements Serializable {
        public static class Type {
            public static final int DEFAULT = 0;
            public static final int CHANGE_PASS = 2;
            public static final int UPDATE_PRAGRAMS = 3;
            public static final int START_RECODE = 4;
            public static final int STOP_RECODE = 5;
            public static final int START_PUSH = 6;
            public static final int STOP_PUSH = 7;
            public static final int DOWNLOAD = 8;
            public static final int DELETE = 9;
        }

        public static abstract class RequestBase implements Serializable {
            public String pass;

            public abstract void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue);

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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {
                mLog.log("Commond", "pass_new=" + pass_new);
                mLog.log("Commond", "pass=" + pass);
                mLog.log("Commond", "Config.password=" + Config.password);
                mLog.log("Commond", "equals=" + Config.password.equals(pass));
                if (checkPass(responseQueue)) {
                    Commond.Response res = new Commond.Response();
                    if (pass_new == null || pass_new.length() < 6) {
                        res.success = false;
                        res.content = "重设密码失败,新密码无效";
                        try {
                            responseQueue.put(res);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Config.password = pass_new;
                        Config.saveConfig(con);
                        res.success = true;
                        res.content = "重设密码成功";
                        try {
                            responseQueue.put(res);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

            }
        }

        public static class StartRecode extends RequestBase implements Serializable {

            @Override
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

                if (checkPass(responseQueue)) {
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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

                if (checkPass(responseQueue)) {
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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

                if (checkPass(responseQueue)) {
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
            public void doRequest(Context con, ArrayBlockingQueue<Response> responseQueue) {

                if (checkPass(responseQueue)) {
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
        public boolean success;
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
