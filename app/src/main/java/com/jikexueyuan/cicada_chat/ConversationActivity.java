package com.jikexueyuan.cicada_chat;

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.sunflower.FlowerCollector;

import io.rong.imkit.RongExtension;
import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.ImageMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

public class ConversationActivity extends FragmentActivity implements MyFirstConversationFragment.OnFragmentInteractionListener {

    private EditText et_message;
    private String mTargetID;
    private Conversation.ConversationType mConversationType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        setTitle("Chating");
        et_message = (EditText) findViewById(R.id.et_message);

        if (SpeechUtility.getUtility() == null) {
            SpeechUtility.createUtility(ConversationActivity.this, SpeechConstant.APPID + "=575816bd"); //初始化
        }

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
                et_message.setText(null);
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
                    Log.e(TAG, "mTagetID is "+ mTargetID);
                    Log.e(TAG, "mConversationType is " + mConversationType);


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

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}

