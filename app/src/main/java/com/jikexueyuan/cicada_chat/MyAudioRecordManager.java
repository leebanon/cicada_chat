package com.jikexueyuan.cicada_chat;


import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.PopupWindow;

import io.rong.common.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.AudioStateMessage;
import io.rong.imkit.manager.IAudioState;
import io.rong.imlib.model.Conversation;

/**
 * Created by Administrator on 2017/9/2.
 */

public class MyAudioRecordManager implements Callback {
    private static final String TAG = "MyAudioRecordManager";
    private int RECORD_INTERVAL;
    private IAudioState mCurAudioState;
    private View mRootView;
    private Context mContext;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private Handler mHandler;
    private AudioManager mAudioManager;
    private MediaRecorder mMediaRecorder;
    private Uri mAudioPath;
    private long smStartRecTime;
    private AudioManager.OnAudioFocusChangeListener mAfChangeListener;
    private PopupWindow mRecordWindow;

    IAudioState idleState;
    IAudioState recordState;
    IAudioState sendingState;
    IAudioState cancelState;
    IAudioState timerState;

    public static MyAudioRecordManager getInstance() {
        return MyAudioRecordManager.SingletonHolder.sInstance;
    }

    @TargetApi(21)
    private MyAudioRecordManager() {
        this.RECORD_INTERVAL = 60;
        this.idleState = new MyAudioRecordManager.IdleState();
        this.recordState = new MyAudioRecordManager.RecordState();
        this.sendingState = new MyAudioRecordManager.SendingState();
        this.cancelState = new MyAudioRecordManager.CancelState();
        this.timerState = new MyAudioRecordManager.TimerState();
        RLog.d("MyAudioRecordManager", "MyAudioRecordManager");
        if(Build.VERSION.SDK_INT < 21) {
            try {
                TelephonyManager e = (TelephonyManager) RongContext.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
                e.listen(new PhoneStateListener() {
                    public void onCallStateChanged(int state, String incomingNumber) {
                        switch(state) {
                            case 1:
                                MyAudioRecordManager.this.sendEmptyMessage(6);
                            case 0:
                            case 2:
                            default:
                                super.onCallStateChanged(state, incomingNumber);
                        }
                    }
                }, 32);
            } catch (SecurityException var2) {
                var2.printStackTrace();
            }
        }

        this.mCurAudioState = this.idleState;
        this.idleState.enter();
    }

    public final boolean handleMessage(Message msg) {

        return false;
    }

    public void setMaxVoiceDuration(int maxVoiceDuration) {
        this.RECORD_INTERVAL = maxVoiceDuration;
    }

    public int getMaxVoiceDuration() {
        return this.RECORD_INTERVAL;
    }

    class TimerState extends IAudioState {
        TimerState() {
        }

        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 3:
                    MyAudioRecordManager.this.setCancelView();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.cancelState;
                case 4:
                default:
                    break;
                case 5:
                    MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            MyAudioRecordManager.this.stopRec();
                            MyAudioRecordManager.this.sendAudioFile();
                            MyAudioRecordManager.this.destroyView();
                        }
                    }, 500L);
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 6:
                    MyAudioRecordManager.this.stopRec();
                    MyAudioRecordManager.this.destroyView();
                    MyAudioRecordManager.this.deleteAudioFile();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 7:
                    int counter = ((Integer)msg.obj).intValue();
                    if(counter >= 0) {
                        Message message = Message.obtain();
                        message.what = 8;
                        message.obj = Integer.valueOf(counter - 1);
                        MyAudioRecordManager.this.mHandler.sendMessageDelayed(message, 1000L);
                        MyAudioRecordManager.this.setTimeoutView(counter);
                    } else {
                        MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MyAudioRecordManager.this.stopRec();
                                MyAudioRecordManager.this.sendAudioFile();
                                MyAudioRecordManager.this.destroyView();
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
            }

        }
    }

    class CancelState extends IAudioState {
        CancelState() {
        }

        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 1:
                case 2:
                case 3:
                default:
                    break;
                case 4:
                    MyAudioRecordManager.this.setRecordingView();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.recordState;
                    MyAudioRecordManager.this.sendEmptyMessage(2);
                    break;
                case 5:
                case 6:
                    MyAudioRecordManager.this.stopRec();
                    MyAudioRecordManager.this.destroyView();
                    MyAudioRecordManager.this.deleteAudioFile();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 7:
                    int counter = ((Integer)msg.obj).intValue();
                    if(counter > 0) {
                        Message message = Message.obtain();
                        message.what = 8;
                        message.obj = Integer.valueOf(counter - 1);
                        MyAudioRecordManager.this.mHandler.sendMessageDelayed(message, 1000L);
                    } else {
                        MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MyAudioRecordManager.this.stopRec();
                                MyAudioRecordManager.this.sendAudioFile();
                                MyAudioRecordManager.this.destroyView();
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                        MyAudioRecordManager.this.idleState.enter();
                    }
            }

        }
    }

    class SendingState extends IAudioState {
        SendingState() {
        }

        void handleMessage(AudioStateMessage message) {
            RLog.d("MyAudioRecordManager", "SendingState handleMessage " + message.what);
            switch(message.what) {
                case 9:
                    MyAudioRecordManager.this.stopRec();
                    if(((Boolean)message.obj).booleanValue()) {
                        MyAudioRecordManager.this.sendAudioFile();
                    }

                    MyAudioRecordManager.this.destroyView();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                default:
            }
        }
    }

    class RecordState extends IAudioState {
        RecordState() {
        }

        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 2:
                    MyAudioRecordManager.this.audioDBChanged();
                    MyAudioRecordManager.this.mHandler.sendEmptyMessageDelayed(2, 150L);
                    break;
                case 3:
                    MyAudioRecordManager.this.setCancelView();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.cancelState;
                case 4:
                default:
                    break;
                case 5:
                    final boolean checked = MyAudioRecordManager.this.checkAudioTimeLength();
                    boolean activityFinished = false;
                    if(msg.obj != null) {
                        activityFinished = ((Boolean)msg.obj).booleanValue();
                    }

                    if(checked && !activityFinished) {
                        MyAudioRecordManager.this.mStateIV.setImageResource(io.rong.imkit.R.drawable.rc_ic_volume_wraning);
                        MyAudioRecordManager.this.mStateTV.setText(io.rong.imkit.R.string.rc_voice_short);
                        MyAudioRecordManager.this.mHandler.removeMessages(2);
                    }

                    if(!activityFinished && MyAudioRecordManager.this.mHandler != null) {
                        MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                AudioStateMessage message = AudioStateMessage.obtain();
                                message.what = 9;
                                message.obj = Boolean.valueOf(!checked);
                                MyAudioRecordManager.this.sendMessage(message);
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.sendingState;
                    } else {
                        MyAudioRecordManager.this.stopRec();
                        if(!checked && activityFinished) {
                            MyAudioRecordManager.this.sendAudioFile();
                        }

                        MyAudioRecordManager.this.destroyView();
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
                    break;
                case 6:
                    MyAudioRecordManager.this.stopRec();
                    MyAudioRecordManager.this.destroyView();
                    MyAudioRecordManager.this.deleteAudioFile();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 7:
                    int counter = ((Integer)msg.obj).intValue();
                    MyAudioRecordManager.this.setTimeoutView(counter);
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.timerState;
                    if(counter >= 0) {
                        Message message = Message.obtain();
                        message.what = 8;
                        message.obj = Integer.valueOf(counter - 1);
                        MyAudioRecordManager.this.mHandler.sendMessageDelayed(message, 1000L);
                    } else {
                        MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MyAudioRecordManager.this.stopRec();
                                MyAudioRecordManager.this.sendAudioFile();
                                MyAudioRecordManager.this.destroyView();
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
            }

        }
    }

    class IdleState extends IAudioState {
        public IdleState() {
            RLog.d("MyAudioRecordManager", "IdleState");
        }

        void enter() {
            super.enter();
            if(MyAudioRecordManager.this.mHandler != null) {
                MyAudioRecordManager.this.mHandler.removeMessages(7);
                MyAudioRecordManager.this.mHandler.removeMessages(8);
                MyAudioRecordManager.this.mHandler.removeMessages(2);
            }

        }

        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", "IdleState handleMessage : " + msg.what);
            switch(msg.what) {
                case 1:
                    MyAudioRecordManager.this.initView(MyAudioRecordManager.this.mRootView);
                    MyAudioRecordManager.this.setRecordingView();
                    MyAudioRecordManager.this.startRec();
                    MyAudioRecordManager.this.smStartRecTime = SystemClock.elapsedRealtime();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.recordState;
                    MyAudioRecordManager.this.sendEmptyMessage(2);
                default:
            }
        }
    }

    static class SingletonHolder {
        static MyAudioRecordManager sInstance = new MyAudioRecordManager(null);

        SingletonHolder() {
        }
    }
}
