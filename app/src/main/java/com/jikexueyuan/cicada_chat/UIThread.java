package com.jikexueyuan.cicada_chat;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

/**
 * Created by Administrator on 2017/9/6.
 */

class UIThread implements Runnable {
    int mTimeMill = 0;
    boolean vRun = true;
    private final static int CMD_RECORDING_TIME = 2000;
    private final static int CMD_RECORDFAIL = 2001;
    private final static int CMD_STOP = 2002;
    private final static int CMD_PLAYFAIL = 2003;

    public void stopThread() {
        vRun = false;
    }

    @Override
    public void run() {
        while (vRun) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mTimeMill++;
            Log.d("thread", "mThread........" + mTimeMill);
            Message msg = new Message();
            Bundle b = new Bundle();// 存放数据
            b.putInt("cmd", CMD_RECORDING_TIME);
            b.putInt("msg", mTimeMill);
            msg.setData(b);

//            SecondActivity.this.uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
        }

    }
}
