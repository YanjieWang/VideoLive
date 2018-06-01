package com.videolive.video;

import android.app.ProgressDialog;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaPlayer;;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.library.common.box.Box;
import com.library.common.box.BoxReader;
import com.library.live.file.WriteMp4;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import com.videolive.R;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DecodeToMp4Activity extends SuperActivity {
    public static final String TAG = "DecodeToMp4Activity";
    private ListView lv;
    ArrayList<String> list;
    ArrayAdapter adapter;
    private final String fixedTopFile="/sdcard";
    private final String decodeFileDirectory="/sdcard/VideoLive/mp4";
    private File parrentDir = new File(fixedTopFile+"/VideoLive");
    private Button toParrentFile;
    private VideoView vv;
    private ProgressDialog pd;
    private boolean isDecodeing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_to_mp4);
        toParrentFile = findViewById(R.id.btn_pre);
        lv = findViewById(R.id.lv);
        vv = findViewById(R.id.vv);
        setupVideo();
        refreshList(parrentDir);
        //设置点击事件mlv
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG,"onItemClick");
                if(list != null){
                    String name = list.get(position);
                    File fileTemp = new File(parrentDir,name);
                    Log.i(TAG,"onItemClick:"+fileTemp.getAbsolutePath());
                    if(fileTemp.exists()){
                        if(fileTemp.canRead()) {
                            if (fileTemp.isDirectory()) {
                                parrentDir = fileTemp;
                                refreshList(parrentDir);
                                toParrentFile.setEnabled(true);
                            } else {
                                if(name.toLowerCase().endsWith(".mp4")){
                                    toast("播放文件：" + fileTemp.getAbsolutePath());
                                    try {
                                        Uri uri = Uri.parse(fileTemp.getAbsolutePath());
                                        vv.setVideoURI(uri);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                } else if(name.toLowerCase().endsWith(".stream")){
                                    if(!isDecodeing) {
                                        toast("解密文件：" + fileTemp.getAbsolutePath());
                                        DecodeToMp4(fileTemp.getAbsolutePath(), decodeFileDirectory, "1234567890123456");
                                    }
                                }

                            }
                        } else{
                            toast("无权限访问：" + fileTemp.getAbsolutePath());
                        }
                    }else{
                        toast("文件不存在：" + fileTemp.getAbsolutePath());
                    }
                }
            }
        });
//实现长按监听
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            /*
             * 点击事件的参数
             * 1、parent指定的是适配器AdqpterView绑定的视图容器,也就是Listview;
             * 2、View:Item的适配器对象的view
             * 3、position:Item在数据数组的对应下标所以
             * id:Item所在的行号
             * */
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG,"onItemLongClick");
                if(list != null){
                    String name = list.get(position);
                    File fileTemp = new File(parrentDir,name);
                    Log.i(TAG,"onItemLongClick:"+fileTemp.getAbsolutePath());
                    if(fileTemp.exists()){
                        if(fileTemp.canRead()&&fileTemp.canWrite()) {
                            if (fileTemp.isFile()) {
                                if(name.toLowerCase().endsWith(".mp4")
                                        ||name.toLowerCase().endsWith(".stream")){
                                    toast("删除：" + fileTemp.getAbsolutePath());
                                    adapter.notifyDataSetInvalidated();
                                    list.remove(position);
                                    fileTemp.delete();
                                    //监听数据源的改变
                                    refreshList(parrentDir);

                                }
                            }
                        } else{
                            toast("无权限访问：" + fileTemp.getAbsolutePath());
                        }
                    }else{
                        toast("文件不存在：" + fileTemp.getAbsolutePath());
                    }
                }
                return true;
            }
        });

        toParrentFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToParrent();
            }
        });
    }

    private void refreshList(File parrentFile){

        if(adapter!=null){
            adapter.notifyDataSetInvalidated();
        }
        if(list==null){
            //构造数据源
            list = new ArrayList<>();
        }else{
            list.clear();
        }
        if(parrentFile.exists()) {
            File[] fs = parrentFile.listFiles();
            for (int i = 0; i < fs.length; i++) {
                String name = fs[i].getName();
                if (fs[i].isDirectory() || name.toLowerCase().endsWith(".stream") || name.toLowerCase().endsWith(".mp4"))
                    list.add(name);
            }
            if (adapter == null) {
                //为适配器添加数据源
                adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
//为listView的容器添加适配器
                lv.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }

    }

    private boolean DecodeToMp4(final String inFile, final String outFile, final String pass){
        isDecodeing = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgressDialog(inFile);
                    }
                });

                WriteMp4 wmp4 = new WriteMp4(outFile,false);
                wmp4.start();
                MediaFormat videoMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 0, 0);
                MediaFormat audioMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, OtherUtil.samplerate,2);
                //byte[] data = new byte[]{(byte) 0x12, (byte) 0x10};
                //audioMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(data));
                //wmp4.addTrack(audioMediaFormat, WriteMp4.voice);
                BoxReader br = new BoxReader();
                final boolean success = br.start(inFile);
                if(success){
                    mLog.log(TAG,"BoxReader start success");
                    Box.Frame fr = br.readData();
                    while(fr != null){
                        if(fr!=null) {
                            ByteBuffer bb = ByteBuffer.allocate(fr.data.length);
                            bb.clear();
                            bb.put(fr.data);
                            MediaCodec.BufferInfo binfo = new MediaCodec.BufferInfo();
                            binfo.set(0, fr.data.length, fr.time, fr.flag);
                            mLog.log(TAG,"fr.tag="+fr.tag);
                            if(fr.tag==Box.TAG_AUDIO||fr.tag==Box.TAG_VIDEO) {
                                wmp4.write(fr.tag, bb, binfo);
                            }else if(fr.tag==Box.TAG_VIDEO_INFO){
                                videoMediaFormat.setByteBuffer("csd-0", Box.getH264SPS(fr.data));
                                videoMediaFormat.setByteBuffer("csd-1", Box.getH264PPS(fr.data));
                                wmp4.addTrack(videoMediaFormat,WriteMp4.video);
                            }else if (fr.tag==Box.TAG_AUDIO_INFO){
                                audioMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(fr.data));
                                wmp4.addTrack(audioMediaFormat,WriteMp4.voice);
                            }
                        }
                        fr = br.readData();
                    }

                } else {
                    mLog.log(TAG,"BoxReader start fail");
                }
                mLog.log("WriteMp4", "stop()");
                wmp4.stop();
                isDecodeing = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(pd!=null&&pd.isShowing()){
                            pd.dismiss();
                        }
                        if(success){
                            toast("解密成功");
                            refreshList(parrentDir);
                        }else{
                            toast("解密失败");
                        }
                    }
                });
            }
        }).start();


        return true;
    }


    private void showProgressDialog(String file){
        if(pd==null){
            pd=new ProgressDialog(this);
            pd.setTitle("正在解密:"+file);
            pd.setCancelable(false);
            pd.setCanceledOnTouchOutside(false);
        }
        pd.show();
    }

    public void exit(View v){
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vv.isPlaying()) {
            vv.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(vv.canPause()){
            vv.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlaybackVideo();
        vv=null;
    }

    @Override
    public void onBackPressed() {
        goToParrent();
    }

    private void goToParrent(){
        File tempParent = parrentDir.getParentFile();
        Log.i("wyj","tempParent="+tempParent.getAbsolutePath());
        if(toParrentFile.isEnabled()) {
            if (fixedTopFile.equals(tempParent.getAbsolutePath())) {
                parrentDir = tempParent;
                refreshList(tempParent);
                toParrentFile.setEnabled(false);
            } else {
                parrentDir = tempParent;
                refreshList(tempParent);
            }
        }
    }

    private void setupVideo() {
        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                vv.start();
            }
        });
        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaybackVideo();
            }
        });
        vv.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                stopPlaybackVideo();
                return true;
            }
        });
        vv.setMediaController(new MediaController(vv.getContext()));
    }

    private void stopPlaybackVideo() {
        try {
            vv.stopPlayback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
