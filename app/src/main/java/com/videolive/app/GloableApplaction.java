package com.videolive.app;

import android.app.Application;

import com.library.rpc.Config;

public class GloableApplaction extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Config.loadConfig(getApplicationContext());
    }
}
