package com.iot.huateng.facerecognition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends Activity implements View.OnClickListener{
    private static final int RESULT_CAPTURE_IMAGE = 1;// 照相的requestCode
    private String strImgPath = "";// 照片文件绝对路径
    private Button mPhoto;
    private Button mStart;

    private TextView mCount;
    private String mPersonName;
    private static int mNumber = 0;

    //训练中的对话框
    private ProgressDialog progressDialog;
    private HttpRequests httpRequests;

    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_photo);
        Intent intent = this.getIntent();
        mPersonName = intent.getStringExtra("name");
        mCount = (TextView) findViewById(R.id.count);
        mCount.setText("" + mNumber);

        //拍照和训练按钮
        mPhoto = (Button) findViewById(R.id.photo);
        mStart = (Button) findViewById(R.id.start);
        mPhoto.setOnClickListener(this);
        mStart.setOnClickListener(this);

        httpRequests = new HttpRequests("f6dc73bd489b555557a225ae2d2d7732", "IISlgUrQvABWzq8SzDgV9pX82wR1lvys",true,true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    /**
     * 照相功能
     */
    private void cameraMethod(int i) {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        strImgPath = Environment.getExternalStorageDirectory().toString() + "/face_detect/";//存放照片的文件夹
        String fileName = "" + i + ".jpg";//照片命名
        File out = new File(strImgPath);
        if (!out.exists()) {
            out.mkdirs();
        }
        out = new File(strImgPath, fileName);
        strImgPath = strImgPath + fileName;//该照片的绝对路径
        Uri uri = Uri.fromFile(out);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(imageCaptureIntent, RESULT_CAPTURE_IMAGE);
    }

    private void lessPhoto(Bitmap bitmap, int i){
        Matrix matrix = new Matrix();
        if(bitmap.getWidth() < 320){
            matrix.postScale(0.25f, 0.25f);
        } else if(bitmap.getWidth() < 600){
            matrix.postScale(0.15f, 0.15f);
        } else {
            matrix.postScale(0.06f, 0.06f);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
        File f = new File("/sdcard/face_detect/" + mNumber + ".jpg");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_CAPTURE_IMAGE://拍照
                mCount.setText("" + mNumber);
                Bitmap bm = BitmapFactory.decodeFile("/sdcard/face_detect/" + mNumber + ".jpg");
                //缩小图片
                lessPhoto(bm, mNumber);
                break;
        }
    }


    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.photo:
                //if(mNumber < 6){
                mNumber++;
                cameraMethod(mNumber);
                //}

                break;
            case R.id.start:
                if(mNumber > 4){
                    //启动线程得到这5张脸的ID。
                    //创建一个名字为mPersonName的人。
                    //将5个人脸ID加入到上面这个人里
                    //显示加载中的进度条
                    progressDialog = ProgressDialog.show(this, "开始训练", "请耐心等待，训练中...", true, true);
                    mThread = new Thread(mRunnable);
                    mThread.start();
                } else {
                    Toast.makeText(CameraActivity.this, "拍摄的样本数太少！", Toast.LENGTH_LONG).show();
                }
                break;
        }

    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                //创建一个人
                httpRequests.personCreate(new PostParameters().setPersonName(mPersonName));
                Log.d("ALEC", "==get======0==");
                //得到5张人脸的face ID
                String[] mFaceID = new String[10];
                for(int i=1; i<=mNumber; i++){
                    File myCaptureFile = new File("/sdcard/face_detect/" + i + ".jpg");
                    JSONObject result = null;
                    result = httpRequests.detectionDetect(new PostParameters().setImg(myCaptureFile));
                    for (int j = 0; j < result.getJSONArray("face").length(); ++j) {
                        String face_id = result.getJSONArray("face").getJSONObject(j).getString("face_id");
                        Log.d("ALEC", "face_id==get========" + face_id);
                        mFaceID[j] = face_id;
                        httpRequests.personAddFace(new PostParameters().setPersonName(mPersonName).setFaceId(
                                face_id));
                    }
                }

                //person/add_face
                //将上面的5个人脸ID添加到mPersonName人身上。
                //for (int i = 1; i < 6; ++i)
                //	httpRequests.personAddFace(new PostParameters().setPersonName(mPersonName).setFaceId(
                //			mFaceID[i]));
                Log.d("ALEC", "==get======1==");
                //将创建的人都加入到group中去
//                httpRequests.groupCreate(new PostParameters().setGroupName("huateng"));
                httpRequests.groupAddPerson(new PostParameters().setGroupName("huateng").setPersonName(mPersonName));
                Log.d("ALEC", "==get======2==");
                //给这个人进行训练
                httpRequests.trainVerify(new PostParameters().setPersonName(mPersonName));
                Log.d("ALEC", "==get======3==");
                Message msg = mHandler.obtainMessage(0);
                mHandler.sendMessage(msg);

            } catch (Exception e) {
                //e.printStackTrace();
                Log.d("ALEC", "error========" + e.getMessage());
                if(e.getMessage().contains("NAME_EXIST")){
                    Message msg = mHandler.obtainMessage(2);
                    mHandler.sendMessage(msg);
                } else {
                    Message msg = mHandler.obtainMessage(1);
                    mHandler.sendMessage(msg);
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(CameraActivity.this, "训练成功！", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(CameraActivity.this, "训练失败！", Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(CameraActivity.this, "该样本姓名已经存在！", Toast.LENGTH_LONG).show();
                    break;
                case 3:

                    break;
                default:
                    //Log.d(TAG, "not get");
            }
        }
    };

}
