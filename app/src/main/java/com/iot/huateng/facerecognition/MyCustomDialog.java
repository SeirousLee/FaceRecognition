package com.iot.huateng.facerecognition;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Administrator on 2015/9/8.
 */
public class MyCustomDialog extends Dialog {
    public interface OnCustomDialogListener{
        public void back(String name);
    }
    private String name;
    private OnCustomDialogListener onCustomDialogListener;
    private EditText editText;
    private Button button;

    public MyCustomDialog(Context context,String name,OnCustomDialogListener onCustomDialogListener){
        super(context);
        this.name = name;
        this.onCustomDialogListener = onCustomDialogListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);
        setTitle(name);
        editText = (EditText)findViewById(R.id.edit_name);
        button = (Button)findViewById(R.id.sample);
        button.setOnClickListener(onClickListener);
    }
    private View.OnClickListener onClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            onCustomDialogListener.back(editText.getText().toString());
            MyCustomDialog.this.dismiss();
        }
    };
}
