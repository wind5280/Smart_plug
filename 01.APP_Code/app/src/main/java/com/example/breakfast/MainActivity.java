package com.example.breakfast;
/****************

 app:itemBackground="@null"
 **********************/

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.chinamobile.iot.onenet.mqtt.MqttCallBack;
import com.chinamobile.iot.onenet.mqtt.MqttClient;
import com.chinamobile.iot.onenet.mqtt.MqttConnectOptions;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttConnAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttMessage;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttPubAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttPublish;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttSubAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttSubscribe;
import com.chinamobile.iot.onenet.mqtt.protocol.imp.QoS;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DatePicker.OnDateChangedListener, TimePicker.OnTimeChangedListener {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private ScheduledExecutorService scheduler;
    private String getprefrences;

    /**
     * onenet 服务器IP
     */
    private String host = "183.230.40.39";

    /**
     * onenet 服务器端口
     */
    private int port = 6002;

    /**
     * 设备ID
     */
    private String Device_id = "606784852";

    /**
     * 产品ID,在产品概况里面
     */
    private String Product_id = "354343";

    /**
     * Master-APIkey，在产品概况里面
     */
    private String API_key = "5RJbtFc8PaR2gz=7ALw30a80NlM=";


    private static String Subtopic = "app";  //订阅的主题
    private static String Pubtopic = "hardware"; //发布主题  你硬件设置订阅的主题

    /**
     * 时间日期相关
     */
    private DatePicker datePicker;
    private Context context;
    private LinearLayout llDate, llTime;
    private TextView tvDate, tvTime;
    private int year, month, day, hour, minute;

    //在TextView上显示的字符
    private StringBuffer date, time;

    private Button set_btn;   //设定按钮
    private EditText set_timer; //设定时间

    private static TextView text;
    private static TextView check;

    /**
     * 持有弱引用MainActivity,GC回收时会被回收掉.
     */
    private static class myHandler extends Handler {

        //持有弱引用MainActivity,GC回收时会被回收掉.
        private final WeakReference<MainActivity> mActivty;

        public myHandler(MainActivity activity) {      //构造器
            mActivty = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivty.get();
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:  //MQTT 收到消息回传

                    break;
                case 30:  //连接失败
                    break;
                case 0:   //连接成功
                    Toast.makeText(SampleApplication.getContext(), "连接成功", Toast.LENGTH_SHORT).show();
                    MqttSubscribe mqttSubscribe1 = new MqttSubscribe(Subtopic, QoS.AT_LEAST_ONCE);
                    //  Toast.makeText(BaseApplication.getContext(), Subtopic, Toast.LENGTH_SHORT).show();
                    MqttClient.getInstance().subscribe(mqttSubscribe1);
                    break;
                default:
                    break;
            }

        }

    }

    private final myHandler mHandler = new myHandler(MainActivity.this);

    private final Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                 Mqtt_init();
            } catch (Exception e) {
                e.printStackTrace();

            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        date = new StringBuffer();
        time = new StringBuffer();


        llDate = (LinearLayout) findViewById(R.id.ll_date);
        tvDate = (TextView) findViewById(R.id.tv_date);
        llTime = (LinearLayout) findViewById(R.id.ll_time);
        tvTime = (TextView) findViewById(R.id.tv_time);
        llDate.setOnClickListener((View.OnClickListener) this);
        llTime.setOnClickListener((View.OnClickListener) this);
        set_btn = (Button) findViewById(R.id.set_time);
        set_btn.setOnClickListener(this);
        set_timer = findViewById(R.id.set_timer);   //设定定时时间文本框
        text = (TextView) findViewById(R.id.text_in);
        check = (TextView) findViewById(R.id.check);

        /***
         *  初始化时间及日期
         */
        initDateTime();

        /**
         * 参数初始化
         */
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Subtopic = preferences.getString("resiger_username", "");
        Pubtopic = preferences.getString("mqtt_pub_toplic","");
        Device_id = preferences.getString("deviceId","");

        tvDate.setText(year + "年" + month + "月" + day);
        tvTime.setText(hour + "时" + minute + "分");


        /********************************************
         */
        Mqtt_init();


//        startReconnect();
//        myHandler handler = new myHandler(this);
//        handler.postDelayed(myRunnable,1000);//20s 检测一次连接状态




    }


    /**
     * 初始化控件
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_date:
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (date.length() > 0) { //清除上次记录的日期
                            date.delete(0, date.length());
                        }
                        tvDate.setText(date.append(String.valueOf(year)).append("年").append(String.valueOf(month)).append("月").append(day).append("日"));
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(context, R.layout.dialog_date, null);
                final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.datePicker);

                dialog.setTitle("设置日期");
                dialog.setView(dialogView);
                dialog.show();
                //初始化日期监听事件
                datePicker.init(year, month-1 , day, this);

                break;
            case R.id.ll_time:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                builder2.setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (time.length() > 0) { //清除上次记录的日期
                            time.delete(0, time.length());
                        }
                        tvTime.setText(time.append(String.valueOf(hour)).append("时").append(String.valueOf(minute)).append("分"));
                        dialog.dismiss();
                    }
                });
                builder2.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog2 = builder2.create();
                View dialogView2 = View.inflate(context, R.layout.dialog_time, null);
                TimePicker timePicker = (TimePicker) dialogView2.findViewById(R.id.timePicker);
                timePicker.setIs24HourView(true); //设置24小时制
                timePicker.setCurrentHour(hour);
                timePicker.setCurrentMinute(minute);
                timePicker.setOnTimeChangedListener(this);
                dialog2.setTitle("设置时间");
                dialog2.setView(dialogView2);
                dialog2.show();
                break;
            case R.id.set_time:   //时间配置按键

                sendJson(Pubtopic);
                //addDevice(Subtopic);
                showWaitDialog();

                break;
        }
    }

    /**
     * 获取当前的日期和时间
     */
    private void initDateTime() {
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH)+1;
        day = calendar.get(Calendar.DAY_OF_MONTH);

        hour = calendar.get(Calendar.HOUR_OF_DAY);  //24小时方法
        minute = calendar.get(Calendar.MINUTE);

    }

    /******************mqtt 初始化*****************************/
    private void Mqtt_init() {
        //初始化sdk
        MqttClient.initialize(this, host, port, Device_id, Product_id, API_key);
        //设置接受响应回调
        MqttClient.getInstance().setCallBack(callBack);
        //设置连接属性
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setKeepAlive(180);
        connectOptions.setWill(false);
        connectOptions.setWillQoS(QoS.AT_MOST_ONCE);
        connectOptions.setWillRetain(false);
        //建立TCP连接
        MqttClient.getInstance().connect(connectOptions);
    }

    /**
     * MQTT回调函数
     */
    private MqttCallBack callBack = new MqttCallBack() {
        @Override
        public void messageArrived(MqttMessage mqttMessage) {
            switch (mqttMessage.getMqttHeader().getType()) {
                case CONNACK:
                    MqttConnAck mqttConnAck = (MqttConnAck) mqttMessage;
                    Message message = new Message();
                    message.what = mqttConnAck.getConnectionAck();
                    mHandler.sendMessage(message);
                    break;
                case PUBLISH:
                    MqttPublish mqttPublish = (MqttPublish) mqttMessage;  //MqttPublish 继承自MqttMessage，应该不会报异常呀，这个bug 待解决
                    byte[] data = mqttPublish.getData();
                    // 以下时数据解析
                    String data_string = "";  //存储解析的数据
                    data[0] = 0;
                    for (int i = 0; i < data.length; i++) {
                        String hex = Integer.toHexString(data[i] & 0xff);//byte 是8位  int 是32位 不与0xff 会自动转换成32位
                        if (hex.length() == 1) {
                            hex = '0' + hex;
                            System.out.println(hex);
                        }
                        if (i == 0) {
                            data_string = hex.toUpperCase();
                        } else {
                            data_string += hex.toUpperCase(); //字母转换成大写  其实小写都没问题的
                        }
                    }
                    break;
                case SUBSCRIBE:
                    MqttSubscribe mqttSubscribe = (MqttSubscribe) mqttMessage;
                    try {
                        text.setText(new String(mqttSubscribe.getPacket()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case SUBACK:
                    MqttSubAck mqttSubAck = (MqttSubAck) mqttMessage;
                    break;
                case PINGRESP:
                    break;
                case PUBACK:
                    MqttPubAck mqttPubAck = (MqttPubAck) mqttMessage;

                    break;
                case PUBCOMP:
                    break;
            }
        }

        @Override
        public void connectionLost(Exception e) {

        }

        @Override
        public void disconnect() {
            Mqtt_init();
        }
    };


    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * @param topic
     * @param message
     */
    private void Send_messsages(String topic, String message) {

        MqttPublish mqttPublish = new MqttPublish(topic, message.getBytes(), QoS.AT_LEAST_ONCE);
        MqttClient.getInstance().sendMsg(mqttPublish);

    }

    /***
     * 发送Json 格式数据
     * @param
     *
     */
    private  void  sendJson(String pubTopic)  {

        JSONObject jsonObject =new JSONObject();

        try {
            jsonObject.put("topic", pubTopic);   //消息主题
            jsonObject.put("user", Subtopic);   //客户端 id
            jsonObject.put("month", month);      //月份
            jsonObject.put("day", day);          //日期
            jsonObject.put("hour", hour);        //小时
            jsonObject.put("minute", minute);    //分钟

            int setTime =10;
            if (set_timer.getText().toString().isEmpty()){
                set_timer.setHintTextColor(Color.parseColor("#E90B0B"));
                set_timer.clearFocus(); //取消焦点
            }else {
                 setTime = Integer.parseInt(set_timer.getText().toString());
                System.out.println(setTime+"*********************");
                set_timer.clearFocus();
            }

           jsonObject.put("times", setTime);//设定时长

            jsonObject.put("version", "1.0");//版本号

        } catch (JSONException e) {
            e.printStackTrace();
        }


        Send_messsages(pubTopic,jsonObject.toString());

    }


    /**
     * 菜单相关
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //设备配网
        if (item.getItemId() == R.id.config_menu) {
            Intent intent = new Intent(MainActivity.this, WifiConfigActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else if (item.getItemId() == R.id.config_device) {  //绑定设备
            showInputDialog();//弹出输入框
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @param view
     * @param year
     * @param monthOfYear
     * @param dayOfMonth
     */
    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        this.year = year;
        this.month = monthOfYear+1;
        this.day = dayOfMonth;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        this.hour = hourOfDay;
        this.minute = minute;
    }

    /**
     * 用于返回配网成功返回的消息
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
                    String returnedData = data.getStringExtra("data_return");
                    // Log.d("MainActivity",returnedData);
//                    if (!client.isConnected()) {
//                        try {
//                            if (!(client.isConnected()))  //如果还未连接
//                            {
//                                client.connect(options);
//                                Message msg = new Message();
//                                msg.what = 31;
//                                // 没有用到obj字段
//                                mHandler.sendMessage(msg);
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            Message msg = new Message();
//                            msg.what = 30;
//                            // 没有用到obj字段
//                            mHandler.sendMessage(msg);
//                        }
//                    }
                }
                break;
            default:
        }
    }


    /**
     * 设置设备id 输入对话框
     */

    private void showInputDialog() {
        final EditText editText = new EditText(MainActivity.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("请输入设备ID").setView(editText);
        inputDialog.setCancelable(false);
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {  // 解决闪退问题
                        if (editText.getText().toString().equals("")) {
                            Toast.makeText(MainActivity.this, "请输入设备ID", Toast.LENGTH_SHORT).show();
                        } else {
                            Pubtopic = editText.getText().toString();
                            sendJson(Pubtopic);
                            editor = preferences.edit();
                            editor.putString("mqtt_pub_toplic", Pubtopic);
                            editor.apply();
                            Toast.makeText(MainActivity.this, Pubtopic, Toast.LENGTH_SHORT).show();
                        }

                    }
                }).show();

    }

    /**
     * 设置时间过程
     */
    private void showWaitDialog() {
//        ProgressDialog waitDialog = new ProgressDialog(MainActivity.this);
//        waitDialog.setTitle("设置定时时间");
//        waitDialog.setMessage("设置中...");
//        waitDialog.setCancelable(true);
//        waitDialog.setIndeterminate(true);
//        waitDialog.show();
//        text.setText(this.hour + ":" + this.minute);

        Toast.makeText(SampleApplication.getContext(), "请稍等！", Toast.LENGTH_SHORT).show();
        if (Pubtopic.equals("")) {
            Toast.makeText(MainActivity.this, "请绑定设备", Toast.LENGTH_SHORT).show();
        } else {
//            publishmessageplus(mqtt_pub_toplic, "{demo:2<" + year +"D"+ this.month+ "a" +this.day+ "h"
//                    +this.hour+"i"+this.minute+ "r" + set_timer.getText().toString() + "}");
            //Toast.makeText(MainActivity.this,this.hour+":"+this.minute,Toast.LENGTH_SHORT).show();

//            Send_messsages

        }

    }



}


