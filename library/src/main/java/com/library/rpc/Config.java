package com.library.rpc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wangyanjie on 18-3-27.
 */

public final class Config {

    public static boolean isLoaded = false;


    public static final int CONTROL_PORT_DEFAULT = 8766;
    public static final String CONTROL_PORT_KEY = "control_port";
    public static int control_port = CONTROL_PORT_DEFAULT;


    public static final int DOWNLOAD_PORT_DEFAULT = 8767;
    public static final String DOWNLOAD_PORT_KEY = "donwload_port";
    public static int donwload_prot = DOWNLOAD_PORT_DEFAULT;

    public static final int PUSH_PORT_DEFAULT = 8765;
    public static final String PUSH_PORT_KEY = "push_port";
    public static int push_prot = PUSH_PORT_DEFAULT;


    public static final String PASSWORD_DEFAULT = "123456";
    public static final String PASSWORD_KEY = "password";

    public static final String PASSWORD_ENC_DEFAULT = null;
    public static final String PASSWORD_ENC_KEY= "password_enc";

    //登录密码
    public static String password = PASSWORD_DEFAULT;//长度必须>6

    //加解密密码
    public static String password_enc = PASSWORD_ENC_DEFAULT;//长度必须>6



    public static Commond.Request conf = null;


    public static synchronized void loadConfig(Context con) { if (con != null && !isLoaded) {
            SharedPreferences sp = getCon(con);
            control_port = sp.getInt(CONTROL_PORT_KEY, CONTROL_PORT_DEFAULT);
            donwload_prot = sp.getInt(DOWNLOAD_PORT_KEY, DOWNLOAD_PORT_DEFAULT);
            password = sp.getString(PASSWORD_KEY, PASSWORD_DEFAULT);
            password_enc = sp.getString(PASSWORD_ENC_KEY, null);
            push_prot = sp.getInt(PUSH_PORT_KEY, PUSH_PORT_DEFAULT);
            isLoaded = true;
        }
    }

    public static synchronized void saveConfig(Context con) {
        if (con != null && isLoaded) {
            getCon(con)
                    .edit()
                    .putInt(CONTROL_PORT_KEY, control_port)
                    .putInt(DOWNLOAD_PORT_KEY, donwload_prot)
                    .putString(PASSWORD_KEY, password)
                    .putString(PASSWORD_ENC_KEY, password_enc)
                    .putInt(PUSH_PORT_KEY, push_prot)
                    .commit();
        } else {
            if (!isLoaded) {
                throw new RuntimeException("loadConfig havent been called");
            }
            if (con == null) {
                throw new RuntimeException("you shouldnt pass in null Context");
            }
        }
    }

    public static SharedPreferences getCon(Context con) {
        return con.getSharedPreferences("video_live_conf", Context.MODE_PRIVATE);
    }

}
