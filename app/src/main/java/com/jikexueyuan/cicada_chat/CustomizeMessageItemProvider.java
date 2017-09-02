package com.jikexueyuan.cicada_chat;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.RongContext;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.IAudioPlayListener;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;

import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imkit.widget.provider.VoiceMessageItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.VoiceMessage;

/**
 * Created by Administrator on 2017/7/27.
 */
@ProviderTag(messageContent = CustomizeMessage.class)
public class CustomizeMessageItemProvider extends IContainerItemProvider.MessageProvider<CustomizeMessage> {
    private static final String TAG = "CustomizeMessageItemProvider";

    private static class ViewHolder {
        ImageView img;
        TextView left;
        TextView right;
        ImageView unread;
        private ViewHolder() {
        }
    }

    public CustomizeMessageItemProvider(Context context) {
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_customize_message, (ViewGroup)null);
        ViewHolder holder = new ViewHolder();
        holder.left = (TextView)view.findViewById(R.id.rc_customize_left);
        holder.right = (TextView)view.findViewById(R.id.rc_customize_right);
        holder.img = (ImageView)view.findViewById(R.id.rc_customize_img);
        holder.unread = (ImageView)view.findViewById(R.id.rc_customize_voice_unread);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int position, CustomizeMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();
        Uri playingUri;
        boolean listened;
        if(message.continuePlayAudio) {
            playingUri = MyAudioPlayManager.getInstance().getPlayingUri();
            if(playingUri == null || !playingUri.equals(content.getUri())) {
                listened = message.getMessage().getReceivedStatus().isListened();
                MyAudioPlayManager.getInstance().startPlay(v.getContext(), content.getUri(), new CustomizeMessageItemProvider.CustomizeMessagePlayListener(v.getContext(), message, holder, listened));
            }
        } else {
            playingUri = MyAudioPlayManager.getInstance().getPlayingUri();
            if(playingUri != null && playingUri.equals(content.getUri())) {
                this.setLayout(v.getContext(), holder, message, true);
                listened = message.getMessage().getReceivedStatus().isListened();
                MyAudioPlayManager.getInstance().setPlayListener(new CustomizeMessageItemProvider.CustomizeMessagePlayListener(v.getContext(), message, holder, listened));
            } else {
                this.setLayout(v.getContext(), holder, message, false);
            }
        }
//        if (message.getMessageDirection() == Message.MessageDirection.SEND) {//消息方向，自己发送的
//            holder.message.setBackgroundResource(io.rong.imkit.R.drawable.rc_ic_bubble_right);
//        } else {
//            holder.message.setBackgroundResource(io.rong.imkit.R.drawable.rc_ic_bubble_left);
//        }
//        holder.message.setText(content.getBase64());
//        AndroidEmoji.ensure((Spannable) holder.message.getText());//显示消息中的 Emoji 表情。
    }

    @Override
    public void onItemClick(View view, int position, CustomizeMessage content, UIMessage message) {
        RLog.d("VoiceMessageItemProvider", "Item index:" + position);
        CustomizeMessageItemProvider.ViewHolder holder = (CustomizeMessageItemProvider.ViewHolder)view.getTag();
        holder.unread.setVisibility(8);
        Uri playingUri = MyAudioPlayManager.getInstance().getPlayingUri();
        if(playingUri != null && playingUri.equals(content.getUri())) {
            MyAudioPlayManager.getInstance().stopPlay();
        } else {
            boolean listened = message.getMessage().getReceivedStatus().isListened();
            MyAudioPlayManager.getInstance().startPlay(view.getContext(), content.getUri(), new CustomizeMessageItemProvider.CustomizeMessagePlayListener(view.getContext(), message, holder, listened));
        }

    }

    @Override
    public void onItemLongClick(View view, int position, CustomizeMessage content, UIMessage message) {

    }

    private void setLayout(Context context, VoiceMessageItemProvider.ViewHolder holder, UIMessage message, boolean playing) {
        VoiceMessage content = (VoiceMessage)message.getContent();
        byte minLength = 57;
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        holder.img.getLayoutParams().width = (int)((float)(content.getDuration() * (180 / duration) + minLength) * context.getResources().getDisplayMetrics().density);
        AnimationDrawable animationDrawable;
        if(message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.left.setText(String.format("%s\"", new Object[]{Integer.valueOf(content.getDuration())}));
            holder.left.setVisibility(0);
            holder.right.setVisibility(8);
            holder.unread.setVisibility(8);
            holder.img.setScaleType(ImageView.ScaleType.FIT_END);
            holder.img.setBackgroundResource(io.rong.imkit.R.drawable.rc_ic_bubble_right);
            animationDrawable = (AnimationDrawable)context.getResources().getDrawable(io.rong.imkit.R.drawable.rc_an_voice_sent);
            if(playing) {
                holder.img.setImageDrawable(animationDrawable);
                if(animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(io.rong.imkit.R.drawable.rc_ic_voice_sent));
                if(animationDrawable != null) {
                    animationDrawable.stop();
                }
            }
        } else {
            holder.right.setText(String.format("%s\"", new Object[]{Integer.valueOf(content.getDuration())}));
            holder.right.setVisibility(0);
            holder.left.setVisibility(8);
            if(!message.getReceivedStatus().isListened()) {
                holder.unread.setVisibility(0);
            } else {
                holder.unread.setVisibility(8);
            }

            holder.img.setBackgroundResource(io.rong.imkit.R.drawable.rc_ic_bubble_left);
            animationDrawable = (AnimationDrawable)context.getResources().getDrawable(io.rong.imkit.R.drawable.rc_an_voice_receive);
            if(playing) {
                holder.img.setImageDrawable(animationDrawable);
                if(animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(io.rong.imkit.R.drawable.rc_ic_voice_receive));
                if(animationDrawable != null) {
                    animationDrawable.stop();
                }
            }

            holder.img.setScaleType(ImageView.ScaleType.FIT_START);
        }

    }


    @Override
    public Spannable getContentSummary(CustomizeMessage data) {
        return new SpannableString("这是一条自定义消息CustomizeMessage");
    }

    private class CustomizeMessagePlayListener implements IAudioPlayListener {
        private Context context;
        private UIMessage message;
        private CustomizeMessageItemProvider.ViewHolder holder;
        private boolean listened;

        public CustomizeMessagePlayListener(Context context, UIMessage message, CustomizeMessageItemProvider.ViewHolder holder, boolean listened) {
            this.context = context;
            this.message = message;
            this.holder = holder;
            this.listened = listened;
        }

        public void onStart(Uri uri) {
            this.message.continuePlayAudio = false;
            this.message.setListening(true);
            this.message.getReceivedStatus().setListened();
            RongIMClient.getInstance().setMessageReceivedStatus(this.message.getMessageId(), this.message.getReceivedStatus(), (RongIMClient.ResultCallback)null);
            CustomizeMessageItemProvider.this.setLayout(this.context, this.holder, this.message, true);
            EventBus.getDefault().post(new Event.AudioListenedEvent(this.message.getMessage()));
        }

        public void onStop(Uri uri) {
            this.message.setListening(false);
            CustomizeMessageItemProvider.this.setLayout(this.context, this.holder, this.message, false);
        }

        public void onComplete(Uri uri) {
            Event.PlayAudioEvent event = Event.PlayAudioEvent.obtain();
            event.messageId = this.message.getMessageId();
            if(this.message.isListening() && this.message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                try {
                    event.continuously = RongContext.getInstance().getResources().getBoolean(io.rong.imkit.R.bool.rc_play_audio_continuous);
                } catch (Resources.NotFoundException var4) {
                    var4.printStackTrace();
                }
            }

            if(event.continuously) {
                EventBus.getDefault().post(event);
            }

            this.message.setListening(false);
            CustomizeMessageItemProvider.this.setLayout(this.context, this.holder, this.message, false);
        }
    }


}
