package com.iot.huateng.facerecognition;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.faceplusplus.api.FaceDetecter;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private Bitmap curBitmap;
    private final static int REQUEST_GET_PHOTO = 1;
    ImageView imageView = null;
    HandlerThread detectThread = null;
    Handler detectHandler = null;
    Button button = null;
    FaceDetecter detecter = null;
    HttpRequests request = null;// 在线api

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        detectThread = new HandlerThread("detect");
        detectThread.start();
        detectHandler = new Handler(detectThread.getLooper());

        imageView = (ImageView) findViewById(R.id.imageview);
        curBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face);
        imageView.setImageBitmap(curBitmap);
        detecter = new FaceDetecter();
        detecter.init(this, "f6dc73bd489b555557a225ae2d2d7732");


        //FIXME 替换成申请的key
        // request = new HttpRequests("YOURAPIKEY",
        //        "YOURAPISECRET");
        request = new HttpRequests("f6dc73bd489b555557a225ae2d2d7732", "IISlgUrQvABWzq8SzDgV9pX82wR1lvys",true,true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detecter.release(this);// 释放引擎
    }

    public static Bitmap getFaceInfoBitmap(FaceDetecter.Face[] faceinfos,
                                           Bitmap oribitmap) {
        Bitmap tmp;
        tmp = oribitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas localCanvas = new Canvas(tmp);
        Paint localPaint = new Paint();
        localPaint.setColor(0xffff0000);
        localPaint.setStyle(Paint.Style.STROKE);
        for (FaceDetecter.Face localFaceInfo : faceinfos) {
            RectF rect = new RectF(oribitmap.getWidth() * localFaceInfo.left, oribitmap.getHeight()
                    * localFaceInfo.top, oribitmap.getWidth() * localFaceInfo.right,
                    oribitmap.getHeight()
                            * localFaceInfo.bottom);
            localCanvas.drawRect(rect, localPaint);
        }
        return tmp;
    }

    public static Bitmap getScaledBitmap(String fileName, int dstWidth)
    {
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, localOptions);
        int originWidth = localOptions.outWidth;
        int originHeight = localOptions.outHeight;

        localOptions.inSampleSize = originWidth > originHeight ? originWidth / dstWidth
                : originHeight / dstWidth;
        localOptions.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(fileName, localOptions);
    }

    private void lessPhoto(Bitmap bitmap, int i){
        Matrix matrix = new Matrix();
        if(bitmap.getWidth() < 320){
            matrix.postScale(0.25f, 0.25f);
        } else if(bitmap.getWidth() < 600){
            matrix.postScale(0.15f, 0.15f);
        } else {
            matrix.postScale(0.04f, 0.04f);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
        File f = new File("/sdcard/" + i + ".jpg");
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.pick:
                startActivityForResult(new Intent("android.intent.action.PICK",
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_GET_PHOTO);
                break;
            case R.id.detect:
                detectHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            JSONObject result = null;
                            for(int j=1; j<17; j++){
                                File mFile = new File("/sdcard/" + j + ".jpg");
                                Bitmap bm = BitmapFactory.decodeFile("/sdcard/" + j + ".jpg");
                                //缩小图片
                                lessPhoto(bm, j);
                                //	File mFile = new File("/sdcard/aa.jpg");
                                result = request.detectionDetect(new PostParameters().setImg(mFile));
                                Log.d("ALEC", "TEST====" + result);
                                for (int i = 0; i < result.getJSONArray("face").length(); ++i) {
                                    String face_id = result.getJSONArray("face").getJSONObject(i).getString("face_id");
                                    Log.d("ALEC", "face_id====" + face_id);

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

                                    showText += "，年龄大概是：" +age.getString("value");
                                    showText += ". 微笑指数为:" + smiling.getString("value");

                                    Toast.makeText(MainActivity.this, showText, Toast.LENGTH_LONG).show();

                                }
                            }

                        } catch(FaceppParseException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                        }


                   /*     Face[] faceinfo = detecter.findFaces(curBitmap);// 进行人脸检测
                        if (faceinfo == null)
                        {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "未发现人脸信息", Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                            return;
                        }

                        //在线api交互
                        try {
                            request.offlineDetect(detecter.getImageByteArray(),detecter.getResultJsonString(), new PostParameters());
                        } catch (FaceppParseException e) {
                            // TODO 自动生成的 catch 块
                            e.printStackTrace();
                        }
                        final Bitmap bit = getFaceInfoBitmap(faceinfo, curBitmap);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                imageView.setImageBitmap(bit);
                                System.gc();
                            }
                        }); ***/
                    }
                } );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GET_PHOTO: {
                    if (data != null) {
                        final String str;
                        Uri localUri = data.getData();
                        String[] arrayOfString = new String[1];
                        arrayOfString[0] = "_data";
                        Cursor localCursor = getContentResolver().query(localUri,
                                arrayOfString, null, null, null);
                        if (localCursor == null)
                            return;
                        localCursor.moveToFirst();
                        str = localCursor.getString(localCursor
                                .getColumnIndex(arrayOfString[0]));
                        localCursor.close();
                        if ((curBitmap != null) && (!curBitmap.isRecycled()))
                            curBitmap.recycle();
                        curBitmap = getScaledBitmap(str, 600);
                        imageView.setImageBitmap(curBitmap);
                    }
                    break;
                }
            }

        }
    }
}