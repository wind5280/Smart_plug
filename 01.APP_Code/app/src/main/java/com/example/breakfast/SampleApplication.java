package com.example.breakfast;

import android.app.Application;
import android.content.Context;

import com.chinamobile.iot.onenet.OneNetApi;
import com.chinamobile.iot.onenet.http.Config;

import java.util.concurrent.TimeUnit;

public class SampleApplication extends Application {

    private  static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // 初始化SDK（必须）
        Config config = Config.newBuilder()
                .connectTimeout(60000, TimeUnit.MILLISECONDS)
                .readTimeout(60000, TimeUnit.MILLISECONDS)
                .writeTimeout(60000, TimeUnit.MILLISECONDS)
                .retryCount(2)
                .build();
        OneNetApi.init(this, true, config);

        OneNetApi.setAppKey("5RJbtFc8PaR2gz=7ALw30a80NlM=");

    }

    /**
     *
     * @return
     */
    public  static   Context getContext(){
        return  context ;
    }

}
