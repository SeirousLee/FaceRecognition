package com.iot.huateng.facerecognition;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.media.Image;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class MenuActivity extends Activity implements View.OnClickListener{
//    private EditText mEditText;
//   private Button mSample;
    private ImageButton mAuth;
//    private Button mLocalDetect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
//        mEditText = (EditText) findViewById(R.id.name);
//        mSample = (Button) findViewById(R.id.get_sample);
        mAuth = (ImageButton) findViewById(R.id.auth);
//        mLocalDetect = (Button) findViewById(R.id.local_detect);
//        mSample.setOnClickListener(this);
//        mSample.setOnClickListener(this);
        mAuth.setOnClickListener(this);
//        mLocalDetect.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
//            case R.id.get_sample:
               /* String name = mEditText.getText().toString();
                if(null == name || name.equals("")){
                    AlertDialog.Builder builder = new Builder(MenuActivity.this);
                    builder.setMessage("在采集前请先输入要采集人脸的姓名!");
                    builder.setTitle("提示");
                    builder.setPositiveButton("确认", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                } else {
                    Intent mIntent = new Intent(this, CameraActivity.class);
                    mIntent.putExtra("name", name);
                    startActivity(mIntent);
                }*/

//                break;
            case R.id.auth:
                Intent VerIntent = new Intent(this, CameraPreview.class);
                startActivity(VerIntent);
                break;
//            case R.id.local_detect:
//                Intent mIntent = new Intent(this, LocalDetectActivity.class);
//                startActivity(mIntent);
//                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(),"seting",Toast.LENGTH_LONG).show();
                return true;
            case R.id.add:
                MyCustomDialog dialog = new MyCustomDialog(this,"样本采集",new MyCustomDialog.OnCustomDialogListener(){
                    @Override
                    public void back(String name) {
                        if(null == name || name.equals("")){
                            AlertDialog.Builder builder = new Builder(MenuActivity.this);
                            builder.setMessage("在采集前请先输入要采集人脸的姓名!");
                            builder.setTitle("提示");
                            builder.setPositiveButton("确认", new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.create().show();
                        } else {
                            Intent mIntent = new Intent(MenuActivity.this, CameraActivity.class);
                            mIntent.putExtra("name", name);
                            startActivity(mIntent);
                        }
                    }
                });
                dialog.show();
        }
        return false;
    }
}

