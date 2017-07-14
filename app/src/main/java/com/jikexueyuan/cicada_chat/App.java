package com.jikexueyuan.cicada_chat;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

import io.rong.imkit.RongIM;
import io.rong.imkit.utils.StringUtils;

/**
 * Created by Administrator on 2017/6/25.
 */

public class App extends Application {
    public static String token = "";
    public static String username = "";
    public static boolean isLogin = false;

    @Override
    public void onCreate() {
        super.onCreate();
        SpeechUtility.createUtility(App.this, "appid=" + getString(R.string.xfyun_id));
        RongIM.init(this);
    }
}

