package com.iot.huateng.facerecognition;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.iot.huateng.facerecognition.R;

import javax.net.ssl.HttpsURLConnection;

public class CameraPreview extends Activity implements Callback, PreviewCallback {
    SurfaceView camerasurface = null;
    FaceMask mask = null;
    Camera mCamera = null;
    HandlerThread handleThread = null;
    Handler detectHandler = null;
    Runnable detectRunnalbe = null;
    private int width = 640;
    private int height = 480;
    // private int width = 60;
    // private int height = 80;
    FaceDetecter facedetecter = null;
    HttpRequests httpRequests;
    HttpURLConnection connection;

    // 播放音乐参数
    private Handler handler;
    private int musicNum;
    private SoundPool sp;
    private MediaPlayer mp,mp2,mp3;

    private SharedPreferences mPerferences;
    private int mFaceCount = 0;
    private boolean isDetecting = false;
    private byte[] mOri;
    private SurfaceHolder mSurfaceHolder;

    private TextView mTextView;
    private int time;

    // 该对象主要用来说话，可调用speak方法。
    private SpeakCallBack mTextToSpeech;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapreview);
        mp = MediaPlayer.create(this,R.raw.happy);
        mp2=MediaPlayer.create(this,R.raw.backgroundmusic);
        mp3=MediaPlayer.create(this,R.raw.home);
        mTextView = (TextView) findViewById(R.id.result);
        mPerferences = getSharedPreferences("faceCheck", 0);

        camerasurface = (SurfaceView) findViewById(R.id.camera_preview);
        mask = (FaceMask) findViewById(R.id.mask);
        LayoutParams para = new LayoutParams(720, 960);
        handleThread = new HandlerThread("dt");
        handleThread.start();
        detectHandler = new Handler(handleThread.getLooper());
        para.addRule(RelativeLayout.CENTER_IN_PARENT);
        camerasurface.setLayoutParams(para);
        mask.setLayoutParams(para);
        /// camerasurface.getHolder().addCallback(this);
        mSurfaceHolder = camerasurface.getHolder();

        mCamera = Camera.open(1);
        Camera.Parameters para2 = mCamera.getParameters();
        List<Camera.Size> sizeList  = para2.getSupportedPictureSizes();
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();
                Log.i("surpo","size==" + cur.width + " " + cur.height);}}
        para2.setPreviewSize(width, height);
        para2.setPreviewFrameRate(5);// 每秒5帧
        para2.setPictureFormat(PixelFormat.JPEG);// 设置照片的输出格式
        para2.set("jpeg-quality", 100);// 照片质量100最大
        mCamera.setParameters(para2);

        mSurfaceHolder.addCallback(this);
        camerasurface.setKeepScreenOn(true);



        facedetecter = new FaceDetecter();
        if (!facedetecter.init(this, "f6dc73bd489b555557a225ae2d2d7732")) {
            Log.e("diff", "有错误 ");
        }
        facedetecter.setTrackingMode(true);

        mTextToSpeech = new SpeakCallBack(this);

        httpRequests = new HttpRequests("f6dc73bd489b555557a225ae2d2d7732", "IISlgUrQvABWzq8SzDgV9pX82wR1lvys",true,true);
        //加载图片库

    }

    private static File getPhoto(Context context, String pkg) {
        AssetManager asset = context.getAssets();
        String apkPath = context.getFilesDir().getAbsolutePath() + File.separator + pkg;
        try {
            InputStream is = asset.open(pkg);
            FileOutputStream fos = context.openFileOutput(pkg, Context.MODE_WORLD_READABLE);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            is.close();
            fos.close();
            return new File(apkPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        facedetecter.release(this);
        handleThread.quit();
        if (mTextToSpeech != null) {
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        camera.setPreviewCallback(null);
        Log.i("length", data.length + "");
        detectHandler.post(new Runnable() {

            @Override
            public void run() {
//                byte[] ori = new byte[width * height];
//                int is = 0;
//                for (int x = width - 1; x >= 0; x--) {
//                    for (int y = height - 1; y >= 0; y--) {
//                        ori[is] = data[y * width + x];
//                        is++;
//                    }
//                }
                //判断视频中是否有人脸出现
                mFaceCount = mPerferences.getInt("face_count", 0);
                if (mFaceCount > 0) {

                    if (!isDetecting) {
                        Log.d("ALEC", "detect face==========" + mFaceCount);
                        mFaceCount = 0;
                        SharedPreferences.Editor editor = mPerferences.edit();
                        editor.putInt("face_count", 0);
                        editor.commit();

                        mOri = data;
                        isDetecting = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Bitmap bitmap = getYUV420SPBitmap(mOri, 640, 480);
                                    // Write to SD Card
                                    File myCaptureFile = new File("/sdcard/2.jpg");
                                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                                    bos.flush();
                                    bos.close();
                                    Thread.sleep(2000);
                                    time++;
                                    Log.d("ALEC", "detect face=====222=====");
                                    new Thread(new httpThread()).start();
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }


                Bitmap bitmap = getYUV420SPBitmap(data, 640, 480);

                final Face[] faceinfo = facedetecter.findFaces(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mask.setFaceInfo(faceinfo);
                    }
                });
                CameraPreview.this.mCamera.setPreviewCallback(CameraPreview.this);
            }
        });
    }

    class httpThread implements Runnable {

        public httpThread() {
        }

        public void run() {
            try {
                File myCaptureFile = new File("/sdcard/2.jpg");
                JSONObject result = null;
//                time++;
                result = httpRequests.detectionDetect(new PostParameters().setImg(myCaptureFile));
                //result = httpRequests.detectionDetect(new PostParameters().setImg(mOri));
                Log.d("ALEC", "detect face=====333=====");
                Log.d("ALEC", "TEST====" + result);
                for (int i = 0; i < result.getJSONArray("face").length(); ++i) {
                    String face_id = result.getJSONArray("face").getJSONObject(i).getString("face_id");
                    Log.d("ALEC", "face_id====" + face_id);
                    JSONObject mPersons = httpRequests.infoGetPersonList();
                    Log.d("ALEC", "TEST===2222=" + mPersons);
                    String mYourNmae = "";
                    double mConfidence = 0.0f;
                    String bakName ="";
                    for(int j=0; j<mPersons.getJSONArray("person").length(); j++){
                        JSONObject mPerson = mPersons.getJSONArray("person").getJSONObject(j);
                        String personID = mPerson.getString("person_id");
                        String personName = mPerson.getString("person_name");
                        Log.d("ALEC", "TEST===33==" + personName);
                        JSONObject mVerify = httpRequests.recognitionVerify(new PostParameters().setFaceId(face_id).setPersonId(personID));
                        Log.d("ALEC", "TEST===44==" + mVerify);
                        if(mVerify.getDouble("confidence") > mConfidence){
                            mConfidence = mVerify.getDouble("confidence");
                            bakName = personName;
                        }
                        if(mVerify.getBoolean("is_same_person")){
                            mYourNmae = personName;
                            break;
                        }

                    }

                    String img_id = result.getString("img_id");
                    Log.d("ALEC", "img_id====" + img_id);

                    JSONObject attribute = result.getJSONArray("face").getJSONObject(i).getJSONObject("attribute");
                    JSONObject race = attribute.getJSONObject("race");
                    JSONObject gender = attribute.getJSONObject("gender");
                    JSONObject age = attribute.getJSONObject("age");
                    JSONObject smiling = attribute.getJSONObject("smiling");
                    Log.d("ALEC", "race====" + race.getString("value"));
                    Log.d("ALEC", "gender====" + gender.getString("value"));
                    Log.d("ALEC", "age====" + age.getString("value"));
                    Log.d("ALEC", "smiling====" + smiling.getString("value"));

                    String showText = "识别到一位";

                    if(race.getString("value").equals("Asian")){
                        showText += "黄种";
                    } else if(race.getString("value").equals("White")){
                        showText += "白种";
                    } else {
                        showText += "黑种";
                    }

                    if(gender.getString("value").equals("Female")){
                        showText += "女人";
                    } else {
                        showText += "男人";
                    }

                    showText += "，\n年龄大概是：" +age.getString("value");
                    showText += ". \n微笑指数为:" + smiling.getString("value");
                    if(mYourNmae.equals("")) {
                        //if(mConfidence > 78){
                        //	showText += "\n 你最可能是：" + bakName;
                        //}
                        showText += "\n 不认识你！";
                        if (time<2){
                            mTextToSpeech.speak("让我再看你一眼。");
//                            time++;
                        }else{
                            mTextToSpeech.speak("对不起，我还不认识你");
                        }

                    } else {
                        showText += "\n 你是：" + mYourNmae;
                        Log.i("@@", "@@" + gender.getString("value"));
                        if (mYourNmae.equals("Xin")){
                            liandong("http://192.168.1.200:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=321");
//                            liandong("http://121.40.210.148:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=321");
                        }
                        else if (gender.getString("value").equals("Female")){
//                            mTextToSpeech.speak("节日快乐");
//                            soundRturn();
                            playMusic(1);
                            Thread.sleep(2500);
                            playMusic(2);

                            liandong("http://192.168.1.200:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
//                            liandong("http://121.40.210.148:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
                            Thread.sleep(2000);
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
//                            liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
//                            liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");
//                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");

                            finish();

                        }else{
                            Log.i("@@", gender.getString("value"));
                            Log.i("@@", "@@zhixingdaoz");
                            if (mYourNmae.equals("Lvdan")){
                                playMusic(1);
                                Thread.sleep(2500);
                                playMusic(2);

                            liandong("http://192.168.1.200:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
//                                liandong("http://121.40.210.148:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
                                Thread.sleep(2000);
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
//                                liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
//                                liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");
//                                liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
                                liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");

                                finish();
                            }else{
//                            mTextToSpeech.speak("欢迎回家，一路辛苦");
                            playMusic(3);

                            Thread.sleep(2000);
//                            playMusic(2);
                            liandong("http://192.168.1.200:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
//                            liandong("http://121.40.210.148:8030/api/webapi.ashx?api=execompositeoperate&u=tiyanguan2&p=123456&id=322");
                            Thread.sleep(2000);
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=exe&deviceid=6388&id=55");
//                            liandong("http://121.40.210.148/Gallery/api.ashx?api=exe&deviceid=6388&id=55");
//                            liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
//                            liandong("http://121.40.210.148/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=1&controlId=2");
                            liandong("http://192.168.1.200/Gallery/api.ashx?api=ir&deviceid=6075&keynum=11&controlId=1");

                            finish();
                            }
                        }


                    }
                    Log.d("ALEC", "AAAA====" + showText);
                    Message msg = mHandler.obtainMessage(1, showText);
                    mHandler.sendMessage(msg);
//                    Toast.makeText(CameraPreview.this, showText, Toast.LENGTH_LONG).show();
                }
                SharedPreferences.Editor editor = mPerferences.edit();
                editor.putInt("face_count", 0);
                editor.commit();
                isDetecting = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void liandong(String url){
        try {
            Log.i("@@","wangluoyichang1");
            URL url1 = new URL(url);
            connection = (HttpURLConnection) url1.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.connect();
            DataInputStream dis = new DataInputStream(connection.getInputStream());
            if (connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                Log.i("@@","wangluoyichang2");
            }

        }catch (Exception e){
            Log.i("@@","wangluoyichang3" + e.getMessage());
            e.printStackTrace();
        }finally {
            if (connection!=null ){
                connection.disconnect();
            }
        }

    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String showText = (String)msg.obj;
                    //mTextView.setText(showText);
//                    Toast.makeText(CameraPreview.this, showText, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };



    public final Bitmap getYUV420SPBitmap(byte[] yuv420sp, int width, int height)
    {
        final int decodeWidth = 640;
        final int decodeHeight = 480;

        Bitmap bitmap = Bitmap.createBitmap(
                decodeYUV420SP(yuv420sp, width, height),
                width,
                height,
                Bitmap.Config.ARGB_8888
        );
        if (width > decodeWidth || height > decodeHeight) {
            bitmap = scaleBitmap(bitmap, decodeWidth, decodeHeight);
        }
        //wzc
        Matrix matrix = new Matrix();
        // 缩放原图
        matrix.postScale(0.25f, 0.25f);
        //  matrix.postScale(0.3f, 0.3f);
        //bmp.getWidth(), bmp.getHeight()分别表示缩放后的位图宽高
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
//        bitmap = adjustPhotoRotation(bitmap, 90);
//        bitmap = adjustPhotoRotation(bitmap, 90);
//        bitmap = adjustPhotoRotation(bitmap, 90);
        return bitmap;
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        if (scaleWidth >= 1 && scaleHeight >= 1) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        float scaleop = scaleWidth > scaleHeight ? scaleHeight : scaleWidth;
        matrix.postScale(scaleop, scaleop);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private static int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) throws NullPointerException, IllegalArgumentException {
        final int frameSize = width * height;
        if (yuv420sp == null) {
            throw new NullPointerException("buffer yuv420sp is null");
        }
//        Log.d("--width---height---yuv420sp.length", width+"---"+height+"---"+yuv420sp.length);

        if (yuv420sp.length < frameSize) {
            throw new IllegalArgumentException("buffer yuv420sp is illegal");
        }
        int[] rgb = new int[frameSize];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    //旋转图片
    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);
        // m.postScale(0.5f, 0.5f);
        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        return bm1;
    }

    private final class SpeakCallBack extends MyTextToSpeech {
        public SpeakCallBack(Context context) {
            super(context);
        }

        @Override
        public void speakCompletedCallBack(String callback) {
        }
    };
    public void soundRturn(int flag) {
        handler = new Handler();
        sp = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
            if (flag==1)
            musicNum = sp.load(this, R.raw.backgroundmusic, 1);

        handler.post(r);
    }

    Runnable r = new Runnable() {
        public void run() {
            try {
                Thread.sleep(200);
                sp.play(musicNum, 1, 1, 0, 0, 1);
//                Log.i(KEY.TAG, "soundSuccess!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    public void playMusic(int flag) {
        if (flag == 1) {
            if (mp != null) {
                mp.stop();
            }

            try {
                mp.prepare();
                mp.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if (flag==2){
            if (mp2 != null) {
                mp2.stop();
            }

            try {
                mp2.prepare();
                mp2.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
   }else if (flag==3){
            if (mp3 != null) {
                mp3.stop();
            }

            try {
                mp3.prepare();
                mp3.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

