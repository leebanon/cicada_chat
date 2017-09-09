package com.jikexueyuan.cicada_chat;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.rong.common.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.AudioStateMessage;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.TypingMessage.TypingMessageManager;
import io.rong.imlib.model.Conversation;
import io.rong.message.VoiceMessage;

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
    private AudioRecord mAudioRecord;
    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 16000;
    private int mChannelConfig =  AudioFormat.CHANNEL_IN_MONO;
    private int mAudioEncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufferSize;
    private Uri mAudioPath;
    private boolean mIsRecStarted;
    private long mStartRecTime;
    private AudioManager.OnAudioFocusChangeListener mAfChangeListener;
    private PopupWindow mRecordWindow;

    IAudioState idleState;
    IAudioState recordState;
    IAudioState sendingState;
    IAudioState cancelState;
    IAudioState timerState;

    private UIThread uiThread;
    //AudioName裸音频数据文件 ，麦克风
    private String AudioName = "";
    //NewAudioName可播放的音频文件
    private String NewAudioName = "";
    private boolean isRecord = false;// 设置正在录制的状态

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
        RLog.i("AudioRecordManager", "handleMessage " + msg.what);
        AudioStateMessage m;
        switch(msg.what) {
            case 2:
                this.sendEmptyMessage(2);
                break;
            case 7:
                m = AudioStateMessage.obtain();
                m.what = msg.what;
                m.obj = msg.obj;
                this.sendMessage(m);
                break;
            case 8:
                m = AudioStateMessage.obtain();
                m.what = 7;
                m.obj = msg.obj;
                this.sendMessage(m);
        }

        return false;
    }

    private void initView(View root) {
        this.mHandler = new Handler(root.getHandler().getLooper(), this);
//        LayoutInflater inflater = LayoutInflater.from(root.getContext());
//        View view = inflater.inflate(io.rong.imkit.R.layout.rc_wi_vo_popup, (ViewGroup)null);
//        this.mStateIV = (ImageView)view.findViewById(io.rong.imkit.R.id.rc_audio_state_image);
//        this.mStateTV = (TextView)view.findViewById(io.rong.imkit.R.id.rc_audio_state_text);
//        this.mTimerTV = (TextView)view.findViewById(io.rong.imkit.R.id.rc_audio_timer);
//        this.mRecordWindow = new PopupWindow(view, -1, -1);
//        this.mRecordWindow.showAtLocation(root, 17, 0, 0);
//        this.mRecordWindow.setFocusable(true);
//        this.mRecordWindow.setOutsideTouchable(false);
//        this.mRecordWindow.setTouchable(false);
    }

    public void setMaxVoiceDuration(int maxVoiceDuration) {
        this.RECORD_INTERVAL = maxVoiceDuration;
    }

    public int getMaxVoiceDuration() {
        return this.RECORD_INTERVAL;
    }

    public void startRecord(View rootView, Conversation.ConversationType conversationType, String targetId) {
        this.mRootView = rootView;
        this.mContext = rootView.getContext().getApplicationContext();
        this.mConversationType = conversationType;
        this.mTargetId = targetId;
        this.mAudioManager = (AudioManager)this.mContext.getSystemService(Context.AUDIO_SERVICE);
        if(this.mAfChangeListener != null) {
            this.mAudioManager.abandonAudioFocus(this.mAfChangeListener);
            this.mAfChangeListener = null;
        }

        this.mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                RLog.d("AudioRecordManager", "OnAudioFocusChangeListener " + focusChange);
                if(focusChange == -1) {
                    MyAudioRecordManager.this.mAudioManager.abandonAudioFocus(MyAudioRecordManager.this.mAfChangeListener);
                    MyAudioRecordManager.this.mAfChangeListener = null;
                    MyAudioRecordManager.this.sendEmptyMessage(6);
                }

            }
        };
        this.sendEmptyMessage(1);
        if(TypingMessageManager.getInstance().isShowMessageTyping()) {
            RongIMClient.getInstance().sendTypingStatus(conversationType, targetId, "RC:VcMsg");
        }

    }

    public void willCancelRecord() {
        this.sendEmptyMessage(3);
    }

    public void continueRecord() {
        this.sendEmptyMessage(4);
    }

    public void stopRecord() {
        this.sendEmptyMessage(5);
    }

    public void destroyRecord() {
        AudioStateMessage msg = new AudioStateMessage();
        msg.obj = Boolean.valueOf(true);
        msg.what = 5;
        this.sendMessage(msg);
    }

    void sendMessage(AudioStateMessage message) {
        this.mCurAudioState.handleMessage(message);
    }

    void sendEmptyMessage(int event) {
        AudioStateMessage message = AudioStateMessage.obtain();
        message.what = event;
        this.mCurAudioState.handleMessage(message);
    }
    

    private void startRec() {

        RLog.d("MyAudioRecordManager", "startRec");

        try {
            this.muteAudioFocus(this.mAudioManager, true);
            this.mAudioManager.setMode(0);
            AudioName = AudioFileFunc.getRawFilePath();
//            NewAudioName = AudioFileFunc.getWavFilePath();

            minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioEncodingBitRate);
            if(minBufferSize == AudioRecord.ERROR_BAD_VALUE){
                Log.e(TAG, "Invalid parameter !");
            }
            Log.i(TAG,"minBufferSize: " + minBufferSize);

            this.mAudioRecord = new AudioRecord(mAudioSource,mSampleRate,mChannelConfig,mAudioEncodingBitRate,minBufferSize*4);

//            RLog.d("MyAudioRecordManager","mAudioRecord is "+(boolean)(mAudioRecord == null));
            // 让录制状态为true
            isRecord = true;
            this.mAudioRecord.startRecording();

            new Thread(new AudioRecordThread()).start();


            this.mAudioPath = Uri.fromFile(new File(this.mContext.getCacheDir(), System.currentTimeMillis() + "temp.wav"));
            //this.mAudioRecord.setOutputFile(this.mAudioPath.getPath());
            Message e1 = Message.obtain();
            e1.what = 7;
            e1.obj = Integer.valueOf(10);
            this.mHandler.sendMessageDelayed(e1, (long)(this.RECORD_INTERVAL * 1000 - 10000));
        } catch (Exception var4) {
            var4.printStackTrace();
        }

    }

    private boolean checkAudioTimeLength() {
        long delta = SystemClock.elapsedRealtime() - this.mStartRecTime;
        return delta < 1000L;
    }

    private void stopRec() {
        RLog.d("AudioRecordManager", "stopRec");

        try {
            this.muteAudioFocus(this.mAudioManager, false);
            if(this.mAudioRecord != null) {
                isRecord = false;//停止文件写入
                this.mAudioRecord.stop();
                this.mAudioRecord.release();
                this.mAudioRecord = null;
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    private void deleteAudioFile() {
        RLog.d("AudioRecordManager", "deleteAudioFile");
        if(this.mAudioPath != null) {
            File file = new File(this.mAudioPath.getPath());
            if(file.exists()) {
                file.delete();
            }
        }
    }

    private void sendAudioFile() {
        RLog.d("AudioRecordManager", "sendAudioFile path = " + this.mAudioPath);
        if(this.mAudioPath != null) {
            File file = new File(this.mAudioPath.getPath());
            if(!file.exists() || file.length() == 0L) {
                RLog.e("AudioRecordManager", "sendAudioFile fail cause of file length 0 or audio permission denied");
                return;
            }

            int duration = (int)(SystemClock.elapsedRealtime() - this.mStartRecTime) / 1000;
            CustomizeMessage customizeMessage = CustomizeMessage.obtain(this.mAudioPath, duration > this.RECORD_INTERVAL?this.RECORD_INTERVAL:duration);
            RongIM.getInstance().sendMessage(io.rong.imlib.model.Message.obtain(this.mTargetId, this.mConversationType, customizeMessage), (String)null, (String)null, new IRongCallback.ISendMessageCallback() {
                public void onAttached(io.rong.imlib.model.Message message) {
                }

                public void onSuccess(io.rong.imlib.model.Message message) {
                }

                public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {
                }
            });
        }

    }

    private void muteAudioFocus(AudioManager audioManager, boolean bMute) {
        if(Build.VERSION.SDK_INT < 8) {
            RLog.d("AudioRecordManager", "muteAudioFocus Android 2.1 and below can not stop music");
        } else {
            if(bMute) {
                audioManager.requestAudioFocus(this.mAfChangeListener, 3, 2);
            } else {
                audioManager.abandonAudioFocus(this.mAfChangeListener);
                this.mAfChangeListener = null;
            }

        }
    }

    class TimerState extends IAudioState{
        TimerState() {
        }

        @Override
        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 3:
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.cancelState;
                case 4:
                default:
                    break;
                case 5:
                    MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            MyAudioRecordManager.this.stopRec();
                            MyAudioRecordManager.this.sendAudioFile();
                        }
                    }, 500L);
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 6:
                    MyAudioRecordManager.this.stopRec();
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
                    } else {
                        MyAudioRecordManager.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MyAudioRecordManager.this.stopRec();
                                MyAudioRecordManager.this.sendAudioFile();
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
            }

        }
    }

    class CancelState extends IAudioState{
        CancelState() {
        }

        @Override
        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 1:
                case 2:
                case 3:
                default:
                    break;
                case 4:
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.recordState;
                    MyAudioRecordManager.this.sendEmptyMessage(2);
                    break;
                case 5:
                case 6:
                    MyAudioRecordManager.this.stopRec();
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

        @Override
        void handleMessage(AudioStateMessage message) {
            RLog.d("MyAudioRecordManager", "SendingState handleMessage " + message.what);
            switch(message.what) {
                case 9:
                    MyAudioRecordManager.this.stopRec();
                    if(((Boolean)message.obj).booleanValue()) {
                        MyAudioRecordManager.this.sendAudioFile();
                    }

                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                default:
            }
        }
    }

    class RecordState extends IAudioState{
        RecordState() {
        }

        @Override
        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", this.getClass().getSimpleName() + " handleMessage : " + msg.what);
            switch(msg.what) {
                case 2:
                    MyAudioRecordManager.this.mHandler.sendEmptyMessageDelayed(2, 150L);
                    break;
                case 3:
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

                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
                    break;
                case 6:
                    MyAudioRecordManager.this.stopRec();
                    MyAudioRecordManager.this.deleteAudioFile();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    MyAudioRecordManager.this.idleState.enter();
                    break;
                case 7:
                    int counter = ((Integer)msg.obj).intValue();
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
                            }
                        }, 500L);
                        MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.idleState;
                    }
            }

        }
    }

    class IdleState extends IAudioState{
        public IdleState() {
            RLog.d("MyAudioRecordManager", "IdleState");
        }

        void enter() {
            if(MyAudioRecordManager.this.mHandler != null) {
                MyAudioRecordManager.this.mHandler.removeMessages(7);
                MyAudioRecordManager.this.mHandler.removeMessages(8);
                MyAudioRecordManager.this.mHandler.removeMessages(2);
            }

        }

        @Override
        void handleMessage(AudioStateMessage msg) {
            RLog.d("MyAudioRecordManager", "IdleState handleMessage : " + msg.what);
            switch(msg.what) {
                case 1:
                    MyAudioRecordManager.this.initView(MyAudioRecordManager.this.mRootView);
                    MyAudioRecordManager.this.startRec();
                    MyAudioRecordManager.this.mStartRecTime = SystemClock.elapsedRealtime();
                    MyAudioRecordManager.this.mCurAudioState = MyAudioRecordManager.this.recordState;
                    MyAudioRecordManager.this.sendEmptyMessage(2);
                default:
            }
        }
    }

    class AudioRecordThread implements Runnable {
        @Override
        public void run() {
            writeDateTOFile();//往文件中写入裸数据
            copyWaveFile(AudioName, mAudioPath.getPath());//给裸数据加上头文件
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[minBufferSize*4];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File file = new File(AudioName);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (isRecord == true) {
            readsize = mAudioRecord.read(audiodata, 0, minBufferSize*4);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos!=null) {
                try {
                    fos.write(audiodata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if(fos != null)
                fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = AudioFileFunc.AUDIO_SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * AudioFileFunc.AUDIO_SAMPLE_RATE * channels / 8;
        byte[] data = new byte[minBufferSize*4];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    static class SingletonHolder {
        static MyAudioRecordManager sInstance = new MyAudioRecordManager();

        SingletonHolder() {
        }
    }
}
