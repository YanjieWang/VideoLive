package com.videolive.video;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class SuperActivity extends Activity{
    private Toast mToast = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mToast != null){
            mToast.cancel();
            mToast = null;
        }
    }

    public void toast(String toast){
        if(mToast!=null){
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), toast+"", Toast.LENGTH_SHORT);
        mToast.show();
    }
}
