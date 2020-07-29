package com.example.breakfast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.chinamobile.iot.onenet.OneNetApi;
import com.chinamobile.iot.onenet.OneNetApiCallback;

import java.io.File;

public class LoginActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private EditText userEdit;
    private EditText passwordEdit;

    private Button login;
    private Button wifi_config;
    private TextView resiger_user;
    private  TextView cancle_user;       //注销账户

    private CheckBox remberPass;

    /***
     *
     * 蒲公英更新相关
     */
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        userEdit = (EditText) findViewById(R.id.username);
        passwordEdit = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login);
        remberPass = (CheckBox) findViewById(R.id.rember_pass);
        resiger_user = (TextView) findViewById(R.id.resiger_user);
        cancle_user =findViewById(R.id.cancel_user);


        boolean isRember = preferences.getBoolean("rember_pass", false);
        if (isRember) {
            //将记住的密码写到文本框中
            String account = preferences.getString("account", "");
            String password = preferences.getString("password", "");
            userEdit.setText(account);
            passwordEdit.setText(password);
            remberPass.setChecked(true);  //置复选框选中
        }

        /**
         * 登录按钮保存数据
         */
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = userEdit.getText().toString();   //获取输入框中的信息
                String password = passwordEdit.getText().toString();
                String resiger_userName = preferences.getString("resiger_username", "");
                String resiger_password = preferences.getString("resiger_password", "");
                if (account.equals(resiger_userName) && password.equals(resiger_password)   && !("".equals(account) && "".equals(password) )) {
                    editor = preferences.edit();
                    if (remberPass.isChecked()) {
                        editor.putBoolean("rember_pass", true);  //保存数据
                        editor.putString("account", account);
                        editor.putString("password", password);
                    } else {
                        editor.clear(); //清除数据
                    }
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);//跳转界面
                    startActivity(intent);
                    finish();
                } else if("".equals(account) && "".equals(password) ){
                    Toast.makeText(LoginActivity.this, "请先注册", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(LoginActivity.this, "登录失败！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /**
         * 用户注册事件
         */
        resiger_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        /**
         * 注销账户
         */
        cancle_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferences = PreferenceManager.getDefaultSharedPreferences(SampleApplication.getContext());
                String Subtopic = preferences.getString("resiger_username", "");
                String deviceId =preferences.getString("deviceId", "");
                OneNetApi.deleteTopic(userEdit.getText().toString(), new OneNetApiCallback() {
                    @Override
                    public void onSuccess(String response) {

                      //  Toast.makeText(SampleApplication.getContext(), Subtopic, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(Exception e) {

                    }
                });

                OneNetApi.deleteDevice(deviceId, new OneNetApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        editor = preferences.edit();
                        editor.putString("resiger_username","no_data");
                        editor.putString("resiger_password","no_data");
                        editor.putString("deviceId","no_data");
                        editor.apply();//应用
                        Toast.makeText(SampleApplication.getContext(), "账户注销成功！", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(SampleApplication.getContext(), "账户注销失败！", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });


    }



    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "允许写存储！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "未允许写存储！", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            case REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSIONS: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "允许读存储！", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(this, "未允许读存储！", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
            }
        }
    }

    /**
     * 用于返回注册成功返回传回的数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String returnedName = data.getStringExtra("resiger_user");
                    String returnedPassword = data.getStringExtra("resiger_password");
                    userEdit.setText(returnedName);
                    passwordEdit.setText(returnedPassword);
                    // Toast.makeText(LoginActivity.this,"hah",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
}
