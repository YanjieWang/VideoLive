package com.library.live;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.library.common.UdpControlInterface;
import com.library.live.file.WriteMp4;
import com.library.live.stream.BaseSend;
import com.library.live.vc.VoiceRecord;
import com.library.live.vd.RecordEncoderVD;
import com.library.live.vd.VDEncoder;
import com.library.live.view.PublishView;
import com.library.rpc.Config;
import com.library.util.ImagUtil;
import com.library.util.OtherUtil;
import com.library.util.Rotate3dAnimation;
import com.library.util.mLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class Publish implements TextureView.SurfaceTextureListener {

    public static final String TAG = "Publish";
    private Context context;
    private final int frameMax = 4;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private String picturedirpath;
    private PictureCallBack mPCB;


    //帧率控制队列
    private ArrayBlockingQueue<Image> frameRateControlQueue = new ArrayBlockingQueue<>(frameMax);
    //视频编码
    private VDEncoder vdEncoder = null;
    //视频录制
    private RecordEncoderVD recordEncoderVD = null;
    //音频采集
    private VoiceRecord voiceRecord;
    //UDP发送类
    private BaseSend baseSend;
    //是否翻转，默认后置
    private boolean rotate = false;
    private int rotateAngle = 90;//270 图片需要翻转角度
    private boolean isPreview = true;
    private boolean isCameraBegin = false;
    private boolean useuvPicture = false;

    public static final int TAKEPHOTO = 0;
    public static final int CONVERSION = 1;
    private int ScreenshotsMode = TAKEPHOTO;
    //帧率
    private int frameRate;
    private int publishBitrate;
    private int collectionBitrate;
    private int publishbitrate_vc;
    private int collectionbitrate_vc;
    private String codetype;
    //相机设备
    private CameraDevice cameraDevice;
    //捕获会话
    private CameraCaptureSession session;
    //拍照CaptureRequest
    private CaptureRequest captureRequest;
    //用于获取预览数据相关
    private ImageReader previewImageReader;
    //用于获取拍照数据相关
    private ImageReader pictureImageReader;
    //用于实时显示预览
    private PublishView publishView;
    //预览分辨率
    private Size previewSize;
    //推流分辨率
    private Size publishSize;
    //采集分辨率
    private Size collectionSize;
    //控制前后摄像头
    private int facingFront;
    //拍照回调
    private PictureCallback pictureCallback;

    //异步线程
    private HandlerThread controlFrameRateThread;
    private HandlerThread handlerCamearThread;
    private Handler camearHandler;
    private Handler frameHandler;

    private WriteMp4 writeMp4;

    private CameraManager manager;
    private String cameraId;

    private Publish(Context context, PublishView publishView, boolean isPreview, Size publishSize, Size previewSize, Size collectionSize,
                    int frameRate, int publishBitrate, int collectionBitrate, int collectionbitrate_vc, int publishbitrate_vc, String codetype,
                    boolean rotate, String dirpath, BaseSend baseSend, int ScreenshotsMode) {
        this.ScreenshotsMode = ScreenshotsMode;
        this.context = context;
        this.publishSize = publishSize;
        this.previewSize = previewSize;
        this.collectionSize = collectionSize;
        this.publishBitrate = publishBitrate;
        this.collectionBitrate = collectionBitrate;
        this.frameRate = frameRate;
        this.collectionbitrate_vc = collectionbitrate_vc;
        this.publishbitrate_vc = publishbitrate_vc;
        this.codetype = codetype;
        this.publishView = publishView;
        this.baseSend = baseSend;
        this.rotate = rotate;
        facingFront = rotate ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        this.isPreview = isPreview;
        writeMp4 = new WriteMp4(dirpath);

        handlerCamearThread = new HandlerThread("Camear2");
        handlerCamearThread.start();
        camearHandler = new Handler(handlerCamearThread.getLooper());

        starFrameControl();

        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        initCamera();
        if (isPreview) {//如果需要显示预览
            publishView.setSurfaceTextureListener(this);
        } else {
            openCamera();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /**
     * 初始化相机管理
     */
    private void initCamera() {
        try {
            //遍历所有摄像头,查找符合当前选择的摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == facingFront) {
                    //获取StreamConfigurationMap管理摄像头支持的所有输出格式和尺寸,根据TextureView的尺寸设置预览尺寸
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    //选取最佳分辨率初始化编码器（未必和设置的匹配，由于摄像头不支持设置的分辨率）
                    this.cameraId = cameraId;
                    rotateAngle = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    initCode(map.getOutputSizes(SurfaceTexture.class));
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        if (isCameraBegin) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            //打开相机
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice device) {
                    cameraDevice = device;
                    //开启预览
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {

                }
            }, camearHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initCode(Size[] outputSizes) {
        if (vdEncoder == null) {
            int numw = 10000;
            int numh = 10000;
            int num = 0;
            for (int i = 0; i < outputSizes.length; i++) {
                mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
                if (Math.abs(outputSizes[i].getWidth() - publishSize.getWidth()) <= numw) {
                    numw = Math.abs(outputSizes[i].getWidth() - publishSize.getWidth());
                    if (Math.abs(outputSizes[i].getHeight() - publishSize.getHeight()) <= numh) {
                        numh = Math.abs(outputSizes[i].getHeight() - publishSize.getHeight());
                        num = i;
                    }
                }
            }
            publishSize = outputSizes[num];

            numw = 10000;
            numh = 10000;
            num = 0;
            for (int i = 0; i < outputSizes.length; i++) {
                mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
                if (Math.abs(outputSizes[i].getWidth() - previewSize.getWidth()) <= numw) {
                    numw = Math.abs(outputSizes[i].getWidth() - previewSize.getWidth());
                    if (Math.abs(outputSizes[i].getHeight() - previewSize.getHeight()) <= numh) {
                        numh = Math.abs(outputSizes[i].getHeight() - previewSize.getHeight());
                        num = i;
                    }
                }
            }
            previewSize = outputSizes[num];

            numw = 10000;
            numh = 10000;
            num = 0;
            for (int i = 0; i < outputSizes.length; i++) {
                mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
                if (Math.abs(outputSizes[i].getWidth() - collectionSize.getWidth()) <= numw) {
                    numw = Math.abs(outputSizes[i].getWidth() - collectionSize.getWidth());
                    if (Math.abs(outputSizes[i].getHeight() - collectionSize.getHeight()) <= numh) {
                        numh = Math.abs(outputSizes[i].getHeight() - collectionSize.getHeight());
                        num = i;
                    }
                }
            }
            collectionSize = outputSizes[num];

            mLog.log("pictureSize", "推流分辨率  =  " + publishSize.getWidth() + " * " + publishSize.getHeight());
            mLog.log("pictureSize", "预览分辨率  =  " + previewSize.getWidth() + " * " + previewSize.getHeight());
            mLog.log("pictureSize", "采集分辨率  =  " + collectionSize.getWidth() + " * " + collectionSize.getHeight());


            if (isPreview) {
                publishView.setWeight((double) previewSize.getHeight() / previewSize.getWidth());
            }

            recordEncoderVD = new RecordEncoderVD(collectionSize, frameRate, collectionBitrate, writeMp4, codetype);
            vdEncoder = new VDEncoder(collectionSize, publishSize, frameRate, publishBitrate, codetype, baseSend);
            //初始化音频编码
            voiceRecord = new VoiceRecord(baseSend, collectionbitrate_vc, publishbitrate_vc, writeMp4);
            vdEncoder.start();
            voiceRecord.start();
        }
    }


    private void startPreview() {
        try {
            List<Surface> surfaces = new ArrayList<>();
            //预览数据输出
            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            Surface previewSurface = getPreviewImageReaderSurface();
            previewRequestBuilder.addTarget(previewSurface);
            surfaces.add(previewSurface);

            //预览
            if (isPreview) {
                Surface textureSurface = getTextureSurface();
                previewRequestBuilder.addTarget(textureSurface);
                surfaces.add(textureSurface);
            }

            //拍照数据输出
            if (ScreenshotsMode == TAKEPHOTO) {
                final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotateAngle);
                Surface pictureSurface = getPictureImageReaderSurface();
                captureRequestBuilder.addTarget(pictureSurface);
                surfaces.add(pictureSurface);
                captureRequest = captureRequestBuilder.build();
            }

            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表(同时输出屏幕，输出预览，拍照)，
            // 第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，
            // 第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Publish.this.session = session;
                    //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                    try {
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, camearHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    isCameraBegin = true;
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, camearHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*
    获取textureView的Surface
     */
    private Surface getTextureSurface() {
        SurfaceTexture mSurfaceTexture = publishView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());//设置TextureView的缓冲区大小
        return new Surface(mSurfaceTexture);
    }

    /*
    创建预览ImageReader回调监听（在这里获取每一帧数据）并返回Surface
     */
    private Surface getPreviewImageReaderSurface() {
        //最后一个参数代表每次最多获取几帧数据
        previewImageReader = ImageReader.newInstance(collectionSize.getWidth(), collectionSize.getHeight(), ImageFormat.YUV_420_888, frameMax);
        //监听ImageReader的事件，它的参数就是预览帧数据，可以对这帧数据进行处理,类似于Camera1的PreviewCallback回调的预览帧数据
        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (isCameraBegin) {
                    if (frameRateControlQueue.size() >= (frameMax - 1)) {
                        //超出限制丢弃
                        frameRateControlQueue.poll().close();
                    }
                    frameRateControlQueue.offer(reader.acquireNextImage());
                } else {
                    reader.acquireNextImage().close();
                }
            }
        }, camearHandler);
        return previewImageReader.getSurface();
    }

    private void starFrameControl() {
        controlFrameRateThread = new HandlerThread("FrameRateControl");
        controlFrameRateThread.start();
        frameHandler = new Handler(controlFrameRateThread.getLooper());
        frameHandler.post(new Runnable() {
            @Override
            public void run() {
                frameHandler.postDelayed(this, 1000 / frameRate);//帧率控制时间
                if (!frameRateControlQueue.isEmpty()) {
                    //耗时检测
//                    long time = System.currentTimeMillis();
                    Image image = frameRateControlQueue.poll();
                    //YUV_420_888先转成I420
                    byte[] i420 = ImagUtil.YUV420888toI420(image);
                    image.close();
                    byte[] input = new byte[i420.length];
                    //旋转I420(270需要镜像)然后交给编码器等待编码
                    ImagUtil.rotateI420(i420, collectionSize.getWidth(), collectionSize.getHeight(), input, rotateAngle, rotate);
                    if (useuvPicture && yuvPicture == null) {
                        useuvPicture = false;
                        yuvPicture = Arrays.copyOf(input, input.length);
                        camearHandler.post(pictureRunnable);
                    }
                    //录制编码器
                    recordEncoderVD.addFrame(input);
                    //推流编码器
                    vdEncoder.addFrame(input);
//                    if ((System.currentTimeMillis() - time) > (1000 / frameRate)) {
//                        mLog.log("Frame_slow", "图像处理速度过慢");
//                    }
                } else {
                    mLog.log("Frame_loss", "图像采集速率不够");
                }
            }
        });
    }

    /*
    创建拍照ImageReader回调监听（在这里获取拍照数据）并返回Surface
     */
    private Surface getPictureImageReaderSurface() {
        pictureImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, frameMax);
        pictureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                image.close();
                saveImage(bytes);
            }
        }, camearHandler);
        return pictureImageReader.getSurface();
    }

    private void saveImage(byte[] bytes) {
        if (mPCB != null) {
            mPCB.onPicture(ScreenshotsMode, bytes);
            mPCB = null;
        }

        mLog.log(TAG, "保存图片，path=" + picturedirpath);
        if (picturedirpath == null) {
            return;
        }


        OtherUtil.CreateDirFile(picturedirpath);
        if (!new File(picturedirpath).exists()) {
            mLog.log(TAG, "保存图片失败，无权限创建 path=" + picturedirpath);
            return;
        }

        FileOutputStream output;
        try {
            output = new FileOutputStream(picturedirpath + File.separator + sdf.format(System.currentTimeMillis()) + ".jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if (ScreenshotsMode == CONVERSION) {
            byte[] picture = new byte[bytes.length];
            ImagUtil.yuvI420ToNV21(bytes, picture, collectionSize.getHeight(), collectionSize.getWidth());
            YuvImage yuvImage = new YuvImage(picture, ImageFormat.NV21, collectionSize.getHeight(), collectionSize.getWidth(), null);
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, output);

        } else if (ScreenshotsMode == TAKEPHOTO) {
            if (rotate) {
                Bitmap newBitmap = mirror(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                newBitmap.recycle();
            } else {
                try {
                    output.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            output.flush();
            if (pictureCallback != null) {
                pictureCallback.Success(picturedirpath + File.separator + sdf.format(System.currentTimeMillis()) + ".jpg");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            OtherUtil.close(output);
        }
    }

    private Bitmap mirror(Bitmap temp) {
        Matrix m = new Matrix();
        m.postScale(-1, 1);   //镜像水平翻转
//        m.postScale(1, -1);   //镜像垂直翻转
//        m.postRotate(-90);  //旋转-90度
        Bitmap newBitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), m, true);
        temp.recycle();
        return newBitmap;
    }

    private void releaseCamera() {
        isCameraBegin = false;
        while (!frameRateControlQueue.isEmpty()) {
            frameRateControlQueue.poll().close();
        }
        //释放相机各种资源
        if (session != null) {
            session.close();
            cameraDevice.close();
            previewImageReader.close();
            session = null;
            cameraDevice = null;
            previewImageReader = null;

        }
        if (pictureImageReader != null) {
            pictureImageReader.close();
            pictureImageReader = null;
        }
    }

    public void startRecode() {
        voiceRecord.startRecode();
        recordEncoderVD.start();
        writeMp4.start();
    }

    public void stopRecode() {
        voiceRecord.stopRecode();
        recordEncoderVD.stop();
        writeMp4.stop();
    }

    //旋转
    public void rotate() {
        if (isCameraBegin) {
            if (publishView != null)
                Rotate3dAnimation.rotate3dDegrees180(publishView, 700, 500, Rotate3dAnimation.ROTATE_Y_AXIS);
            facingFront = !rotate ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
            releaseCamera();
            initCamera();
            openCamera();
            //最后来设置标志，防止最后几帧数据旋转错误
            rotate = !rotate;
        }
    }

    //不需要保存path传null，不需要回调 pcb传null
    public void takePicture(String path, PictureCallBack pcb) {
        mPCB = pcb;
        picturedirpath = path;
        if (ScreenshotsMode == CONVERSION) {
            useuvPicture = true;

        } else if (ScreenshotsMode == TAKEPHOTO) {
            if (session != null) {
                try {
                    session.capture(captureRequest, null, camearHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private byte[] yuvPicture;

    private Runnable pictureRunnable = new Runnable() {
        @Override
        public void run() {
            saveImage(yuvPicture);
            yuvPicture = null;
        }
    };

    public void start() {
        baseSend.startsend();
    }

    public void stop() {
        baseSend.stopsend();
    }

    public void destroy() {
        releaseCamera();
        recordEncoderVD.destroy();
        vdEncoder.destroy();
        voiceRecord.destroy();
        baseSend.destroy();
        frameHandler.removeCallbacksAndMessages(null);
        controlFrameRateThread.quitSafely();
        camearHandler.removeCallbacksAndMessages(null);
        handlerCamearThread.quitSafely();
        writeMp4.destroy();
        pictureCallback = null;
    }

    public int getPublishStatus() {
        return baseSend.getPublishStatus();
    }

    public int getRecodeStatus() {
        return writeMp4.getRecodeStatus();
    }

    public void setPictureCallback(PictureCallback pictureCallback) {
        this.pictureCallback = pictureCallback;
    }

    public static class Buider {
        private PublishView publishView;
        private Context context;

        @IntDef({CONVERSION, TAKEPHOTO})
        private @interface ScreenshotsMode {
        }

        private int screenshotsMode = TAKEPHOTO;

        //编码参数
        private int frameRate = 15;
        private int publishBitrate = 600 * 1024;
        private int collectionBitrate = 600 * 1024;
        private int collectionbitrate_vc = 64 * 1024;
        private int publishbitrate_vc = 24 * 1024;
        //推流分辨率,仅控制推流编码
        private Size publishSize = new Size(480, 320);
        //预览分辨率，仅控制预览
        private Size previewSize = new Size(480, 320);
        //采集分辨率，录制编码分辨率，图片处理分辨率（开销最大）
        private Size collectionSize = new Size(480, 320);
        //是否翻转，默认后置
        private boolean rotate = false;
        //设置是否需要显示预览,默认显示
        private boolean isPreview = true;
        private String codetype = VDEncoder.H264;
        //录制地址
        private String dirpath = null;
        //拍照地址
        private String picturedirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoPicture";

        private BaseSend baseSend;
        private UdpControlInterface udpControl = null;

        public Buider(Context context, PublishView publishView) {
            this.context = context;
            this.publishView = publishView;
        }

        public Buider(Context context) {
            this.context = context;
        }

        //编码分辨率
        public Buider setPublishSize(int publishWidth, int publishHeight) {
            publishSize = new Size(publishWidth, publishHeight);
            return this;
        }

        public Buider setPreviewSize(int previewWidth, int previewHeight) {
            previewSize = new Size(previewWidth, previewHeight);
            return this;
        }

        public Buider setCollectionSize(int collectionWidth, int collectionHeight) {
            collectionSize = new Size(collectionWidth, collectionHeight);
            return this;
        }

        public Buider setFrameRate(int frameRate) {
            this.frameRate = Math.max(8, frameRate);//限制最小8帧
            return this;
        }

        public Buider setIsPreview(boolean isPreview) {
            this.isPreview = isPreview;
            return this;
        }

        public Buider setPublishBitrate(int publishBitrate) {
            this.publishBitrate = publishBitrate;
            return this;
        }

        public Buider setCollectionBitrate(int collectionBitrate) {
            this.collectionBitrate = collectionBitrate;
            return this;
        }

        public Buider setPublishBitrateVC(int publishbitrate_vc) {
            this.publishbitrate_vc = Math.min(48 * 1024, publishbitrate_vc);//限制最大48，因为发送会合并5个包，过大会导致溢出
            return this;
        }

        public Buider setCollectionBitrateVC(int collectionbitrate_vc) {
            this.collectionbitrate_vc = collectionbitrate_vc;
            return this;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }

        public Buider setScreenshotsMode(@ScreenshotsMode int screenshotsMode) {
            this.screenshotsMode = screenshotsMode;
            return this;
        }


        public Buider setVideoDirPath(String dirpath) {
            this.dirpath = dirpath;
            return this;
        }


        public Buider setRotate(boolean rotate) {
            this.rotate = rotate;
            return this;
        }

        public Buider setPushMode(BaseSend baseSend) {
            this.baseSend = baseSend;
            return this;
        }

        public Buider setCenterScaleType(boolean isCenterScaleType) {
            if (publishView != null) publishView.setCenterScaleType(isCenterScaleType);
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setPassWd(String passWd) {
            Config.password_enc = passWd;
            return this;
        }

        public Publish build() {
            baseSend.setUdpControl(udpControl);
            Publish pb = new Publish(context, publishView, isPreview, publishSize, previewSize, collectionSize, frameRate,
                    publishBitrate, collectionBitrate, collectionbitrate_vc, publishbitrate_vc, codetype, rotate, dirpath,
                    baseSend, screenshotsMode);
            mLog.log("siudf", "" + pb);
            return pb;
        }
    }

    @Override
    public String toString() {
        return "Publish{" +
                "context=" + context +
                ", frameMax=" + frameMax +
                ", frameRateControlQueue=" + frameRateControlQueue +
                ", vdEncoder=" + vdEncoder +
                ", recordEncoderVD=" + recordEncoderVD +
                ", voiceRecord=" + voiceRecord +
                ", baseSend=" + baseSend +
                ", rotate=" + rotate +
                ", rotateAngle=" + rotateAngle +
                ", isPreview=" + isPreview +
                ", isCameraBegin=" + isCameraBegin +
                ", useuvPicture=" + useuvPicture +
                ", ScreenshotsMode=" + ScreenshotsMode +
                ", frameRate=" + frameRate +
                ", publishBitrate=" + publishBitrate +
                ", collectionBitrate=" + collectionBitrate +
                ", publishbitrate_vc=" + publishbitrate_vc +
                ", collectionbitrate_vc=" + collectionbitrate_vc +
                ", codetype='" + codetype + '\'' +
                ", cameraDevice=" + cameraDevice +
                ", session=" + session +
                ", captureRequest=" + captureRequest +
                ", previewImageReader=" + previewImageReader +
                ", pictureImageReader=" + pictureImageReader +
                ", publishView=" + publishView +
                ", previewSize=" + previewSize +
                ", publishSize=" + publishSize +
                ", collectionSize=" + collectionSize +
                ", facingFront=" + facingFront +
                ", picturedirpath='" + picturedirpath + '\'' +
                ", pictureCallback=" + pictureCallback +
                ", controlFrameRateThread=" + controlFrameRateThread +
                ", handlerCamearThread=" + handlerCamearThread +
                ", camearHandler=" + camearHandler +
                ", frameHandler=" + frameHandler +
                ", writeMp4=" + writeMp4 +
                ", manager=" + manager +
                ", cameraId='" + cameraId + '\'' +
                ", yuvPicture=" + Arrays.toString(yuvPicture) +
                ", pictureRunnable=" + pictureRunnable +
                '}';
    }

    public interface PictureCallBack {
        public void onPicture(int shutMode, byte[] pic);

    }

}