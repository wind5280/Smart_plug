package com.example.breakfast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chinamobile.iot.onenet.OneNetApi;
import com.chinamobile.iot.onenet.OneNetApiCallback;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor ;


    //注册按钮
    private Button  resiger_btn ;
    //用户名输入框
    private EditText userName;
    private EditText password1;
    private EditText password2;

    /**
     *  设备ID
     */
    public static   String Device_id;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        userName = findViewById(R.id.username);
        password1 =findViewById(R.id.password1);
        password2 = findViewById(R.id.password2);
        //注册并保存注册信息
        resiger_btn = (Button)findViewById(R.id.resiger_btn);
        resiger_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断两次输入密码是否一致
                String data1 = password1.getText().toString();
                String data2 = password2.getText().toString();
                if(data1.equals(data2) && !("".equals(userName.getText().toString()) || "".equals(password1.getText().toString())) ){
                    editor = preferences.edit();
                    editor.putString("resiger_username",userName.getText().toString());
                    editor.putString("resiger_password",password1.getText().toString());
                    editor.apply();//应用

                    addDevice(userName.getText().toString()); //添加设备

//                    Toast.makeText(RegisterActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("resiger_user",userName.getText().toString());
                    intent.putExtra("resiger_password",password1.getText().toString());
                    setResult(RESULT_OK,intent);
                    finish();
                }else if("".equals(userName.getText().toString()) || "".equals(password1.getText().toString())){
                    Toast.makeText(RegisterActivity.this,"用户名及密码不能为空",Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.d(String.valueOf(RegisterActivity.this),password1.getText().toString() + "///" +password2.getText().toString()  );
                    Toast.makeText(RegisterActivity.this,"两次密码不一致",Toast.LENGTH_SHORT).show();
                }

            }
        });


    }


    /**
     *
     * @param deviceName
     * @return  返回设备id
     */
    private  void  addDevice(String deviceName){
        JSONObject requestContent = new JSONObject();
        try {
            /**
             *   设备名称
             */
            requestContent.putOpt("title", deviceName);

            /**
             *  协议选择
             */
            requestContent.putOpt("protocol", "MQTT");

            /**
             *  数据私有
             */
            requestContent.putOpt("private", true);

            /**
             * *  鉴权信息
             */
            requestContent.putOpt("auth_info",userName.getText().toString());


            /**
             *  调用 ONenet api
             */
            OneNetApi.addDevice(requestContent.toString(), new OneNetApiCallback() {
                @Override
                public void onSuccess(String response) {
                    JsonObject resp = new JsonParser().parse(response).getAsJsonObject();
                    int errno = resp.get("errno").getAsInt();

                    /**
                     *  解析 对象中的对象  嵌套解析
                     */
                    JsonObject data = resp.get("data").getAsJsonObject();
                    String deviceId = data.get("device_id").getAsString();

                    /**
                     *  添加成功
                     */
                    if (0 == errno) {
                        Toast.makeText(getApplicationContext(),"注册成功", Toast.LENGTH_SHORT).show();
                        LocalBroadcastManager.getInstance(RegisterActivity.this).sendBroadcast(new Intent("com.example.breakfast.intent.ACTION_UPDATE_DEVICE_LIST"));
                        editor = preferences.edit();
                        editor.putString("deviceId",deviceId);
                        editor.apply();//应用
                        /**
                         * 添加失败
                         */

                    } else {
                        String error = resp.get("error").getAsString();
                        Toast.makeText(getApplicationContext(), "请重新注册", Toast.LENGTH_SHORT).show();
                        /**
                         * 清空数据
                         */
                        userName.setText("");
                        password1.setText("");
                        password2.setText("");
                    }
                }

                @Override
                public void onFailed(Exception e) {

                    Toast.makeText(getApplicationContext(), "请重新注册", Toast.LENGTH_SHORT).show();
                    /**
                     * 清空数据
                     */
                    userName.setText("");
                    password1.setText("");
                    password2.setText("");
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
