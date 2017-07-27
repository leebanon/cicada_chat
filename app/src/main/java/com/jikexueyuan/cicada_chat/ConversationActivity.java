package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.rong.imkit.RongIM;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.ImageMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

public class ConversationActivity extends FragmentActivity  {
    private SharedPreferences mSharedPreferences;
    //引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private EditText et_message;
    private String mTargetID;
    private Conversation.ConversationType mConversationType;
    String recognizerResults;

    int ret = 0; // 函数调用返回值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        setTitle("Chating");
        et_message = (EditText) findViewById(R.id.et_message);

        if (SpeechUtility.getUtility() == null) {
            SpeechUtility.createUtility(ConversationActivity.this, SpeechConstant.APPID + "=575816bd"); //初始化
        }

        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(ConversationActivity.this, mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME,
                Activity.MODE_PRIVATE);

        RongIM.getInstance().setSendMessageListener(new RongIM.OnSendMessageListener() {
            final String TAG = "OnSendMessageListener";

            /**
             * 消息发送前监听器处理接口（是否发送成功可以从 SentStatus 属性获取）。
             * 设置自己发出消息的监听器，在 init() 之后即可设置。
             * 注意：如果在 Activity 里设置，需要在 Activity 销毁时，将监听设置为 null，防止内存泄露。
             *
             * @param message 发送的消息实例。
             * @return 处理后的消息实例。
             */
            @Override
            public Message onSend(Message message) {
                //开发者根据自己需求自行处理逻辑
                MessageContent messageContent = message.getContent();
                if(messageContent instanceof TextMessage){
                    TextMessage textMessage = (TextMessage) messageContent;
                    String sendMessage = textMessage.getContent();
                    Log.e(TAG, "sendMessage " + sendMessage);
                }
                if(messageContent instanceof VoiceMessage){
                    VoiceMessage voiceMessage = (VoiceMessage) messageContent;
                    int sendMessageDuration = voiceMessage.getDuration();
                    Uri uri = voiceMessage.getUri();
                    String uri_string = uri.toString();
                    Log.e(TAG, "voiceMessageDuration " + sendMessageDuration);

                }
                return message;
            }


            /**
             * 消息在 UI 展示后执行/自己的消息发出后执行,无论成功或失败。
             *
             * @param message              消息实例。
             * @param sentMessageErrorCode 发送消息失败的状态码，消息发送成功 SentMessageErrorCode 为 null。
             * @return true 表示走自己的处理方式，false 走融云默认处理方式。
             */
            @Override
            public boolean onSent(Message message, RongIM.SentMessageErrorCode sentMessageErrorCode) {
//                et_message.setText(null);
                mIatResults.clear();
                if (message.getSentStatus() == Message.SentStatus.FAILED) {
                    if (sentMessageErrorCode == RongIM.SentMessageErrorCode.NOT_IN_CHATROOM) {
                        //不在聊天室
                    } else if (sentMessageErrorCode == RongIM.SentMessageErrorCode.NOT_IN_DISCUSSION) {
                        //不在讨论组
                    } else if (sentMessageErrorCode == RongIM.SentMessageErrorCode.NOT_IN_GROUP) {
                        //不在群组
                    } else if (sentMessageErrorCode == RongIM.SentMessageErrorCode.REJECTED_BY_BLACKLIST) {
                        //你在他的黑名单中
                    }
                }

                MessageContent messageContent = message.getContent();

                if (messageContent instanceof TextMessage) {//文本消息
                    TextMessage textMessage = (TextMessage) messageContent;
                    Log.d(TAG, "onSent-TextMessage:" + textMessage.getContent());
                } else if (messageContent instanceof ImageMessage) {//图片消息
                    ImageMessage imageMessage = (ImageMessage) messageContent;
                    Log.d(TAG, "onSent-ImageMessage:" + imageMessage.getRemoteUri());
                } else if (messageContent instanceof VoiceMessage) {//语音消息
                    VoiceMessage voiceMessage = (VoiceMessage) messageContent;
                    String vm_Uri = voiceMessage.getUri().toString();
                    Log.d(TAG, "onSent-voiceMessage:" + vm_Uri);
                    et_message.setText("voiceMessage uri is "+ vm_Uri);
                    mTargetID = message.getTargetId();
                    mConversationType = message.getConversationType();


                    // 移动数据分析，收集开始听写事件
                    FlowerCollector.onEvent(ConversationActivity.this, "iat_recognize");

                    // 设置参数
                    setParam();
                    // 设置音频来源为外部文件
                    mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
                    ret = mIat.startListening(mRecognizerListener);


                    if (ret != ErrorCode.SUCCESS) {
                        Toast.makeText(ConversationActivity.this, "识别失败,错误码：" + ret, Toast.LENGTH_SHORT).show();
                    } else {
                        byte[] audioData = FucUtil.readAudioFile(ConversationActivity.this,"iattest.wav");

                        if (null != audioData) {
                            Toast.makeText(ConversationActivity.this, getString(R.string.text_begin_recognizer),Toast.LENGTH_SHORT);
                            // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），位长16bit，单声道的wav或者pcm
                            // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                            // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                            // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                            mIat.writeAudio(audioData, 0, audioData.length);
                            mIat.stopListening();
                        } else {
                            mIat.cancel();
                            Toast.makeText(ConversationActivity.this, "读取音频流失败",Toast.LENGTH_SHORT).show();
                        }
                    }

                } else if (messageContent instanceof RichContentMessage) {//图文消息
                    RichContentMessage richContentMessage = (RichContentMessage) messageContent;
                    Log.d(TAG, "onSent-RichContentMessage:" + richContentMessage.getContent());
                } else {
                    Log.d(TAG, "onSent-其他消息，自己来判断处理");
                }

                return false;

            }

        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RongIM.getInstance().setSendMessageListener(null);
        // 退出时释放连接
        mIat.cancel();
        mIat.destroy();
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(ConversationActivity.this);
        FlowerCollector.onPageStart("ConversationActivity");
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(ConversationActivity.this);
        FlowerCollector.onPageStart("ConversationActivity");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("ConversationActivity", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(ConversationActivity.this, "初始化失败，错误码：" + code, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Toast.makeText(ConversationActivity.this, "开始转化成文字",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            Toast.makeText(ConversationActivity.this, error.getPlainDescription(true),Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Toast.makeText(ConversationActivity.this, "结束说话", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            recognizerResults = printResult(results);
            if (isLast) {
                // TODO 最后的结果

                mySendTextMessage(recognizerResults);

            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Toast.makeText(ConversationActivity.this, "当前正在说话，音量大小：", Toast.LENGTH_SHORT).show();
            Log.d("ConversationActivity", "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private String printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        return resultBuffer.toString();
    }

    private void mySendTextMessage(String rr){
        TextMessage textMessage = TextMessage.obtain(rr);
        Message myMessage = Message.obtain(mTargetID, mConversationType, textMessage);

        RongIM.getInstance().sendMessage(myMessage, null, null, new IRongCallback.ISendMediaMessageCallback() {
            @Override
            public void onProgress(Message message, int i) {

            }

            @Override
            public void onCanceled(Message message) {

            }

            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onSuccess(Message message) {
                Log.e("Send tM success", message.getContent().toString());

            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                Log.e("Send textMessage error", errorCode.toString());
            }
        });
    }


        /**
         * 参数设置
         *
         * @param param
         * @return
         */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        //设置语法 ID 和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，参考科大讯飞MSC集成指南。
//        mIat.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
//        mIat.setParameter(SpeechConstant.SUBJECT, null);

        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }
}

