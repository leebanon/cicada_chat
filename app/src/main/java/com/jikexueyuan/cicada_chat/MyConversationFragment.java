package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.thirdparty.V;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.rong.imkit.IExtensionClickListener;
import io.rong.imkit.RongExtension;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.plugin.location.IUserInfoProvider;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.widget.CSEvaluateDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 * Created by Administrator on 2017/8/7.
 */

public class MyConversationFragment extends ConversationFragment implements AbsListView.OnScrollListener, IExtensionClickListener, IUserInfoProvider, CSEvaluateDialog.EvaluateClickListener {
    private RongExtension mRongExtension;
    private ViewGroup mExtensionBar;
    private LinearLayout mMainBar;
    private ViewGroup mSwitchLayout;
    private ViewGroup mContainerLayout;
    private ViewGroup mPluginLayout;
    private View mEditTextLayout;
    private View mVoiceInputToggle;
    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private float mLastTouchY;
    private boolean mUpDirection;
    private float mOffsetLimit;
    private IExtensionClickListener mExtensionClickListener;

    private AudioManager mAudioManager;
    private AudioSourceMic mAudioSourceMic;



    public MyConversationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRongExtension = (RongExtension) view.findViewById(io.rong.imkit.R.id.rc_extension);
        mRongExtension.setFragment(this);

//        mExtensionBar = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_extension_bar, (ViewGroup) null);
//        mMainBar = (LinearLayout) mExtensionBar.findViewById(io.rong.imkit.R.id.ext_main_bar);
//        mPluginLayout = (ViewGroup) mExtensionBar.findViewById(io.rong.imkit.R.id.rc_plugin_layout);
//        ImageView iv = (ImageView) mPluginLayout.findViewById(io.rong.imkit.R.id.music);
//
//        iv.setOnTouchListener(new View.OnTouchListener(){
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                String[] permissions = new String[]{"android.permission.RECORD_AUDIO"};
//                if (!PermissionCheckUtil.checkPermissions(getContext(), permissions)) {
//                    if (event.getAction() == 0) {
//                        PermissionCheckUtil.requestPermissions((Activity) getContext(), permissions, 100);
//                    }
//
//                } else if (event.getAction() == 0) {
//                    AudioPlayManager.getInstance().stopPlay();
//                    mConversationType = mRongExtension.getConversationType();
//                    mTargetId = mRongExtension.getTargetId();
//
//                    mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
//                    mAudioSourceMic = new AudioSourceMic();
//                    mAudioSourceMic.Create(16000);
//                    if (mAudioSourceMic != null) {
//                        mAudioSourceMic.Start();
//                    }
//                    openSpeaker();
//                    mLastTouchY = event.getY();
//                    mUpDirection = false;
//                    return false;
//
//                } else if (event.getAction() == 2) {
//                    if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
//                        mUpDirection = true;
//                    } else if (event.getY() - mLastTouchY > -mOffsetLimit && mUpDirection) {
//                        mUpDirection = false;
//                    }
//                } else if (event.getAction() == 1 || event.getAction() == 3) {
//                    closeSpeaker();
//                    if (mAudioSourceMic != null) {
//                        mAudioSourceMic.Close();
//                        mAudioSourceMic = null;
//                    }
//                    Log.e("显示聊天类型", "ConversationType is " + mConversationType + ", TargetId is " + mTargetId);
//                }
//
//                if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
//                    RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:VcMsg");
////                Log.e("显示聊天类型", "ConversationType is "+ mConversationType + ", TargetId is " + mTargetId);
//                }
//
//                return false;
//            }
//
//        });

        return view;
    }

//        mRongExtension.getConversationType();
//        mRongExtension.getTargetId();
//        mRongExtension.setC
//
//        mExtensionBar = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_extension_bar, (ViewGroup) null);
//
//        mMainBar = (LinearLayout) mExtensionBar.findViewById(io.rong.imkit.R.id.ext_main_bar);
//        mSwitchLayout = (ViewGroup) mExtensionBar.findViewById(io.rong.imkit.R.id.rc_switch_layout);
//        mContainerLayout = (ViewGroup) mExtensionBar.findViewById(io.rong.imkit.R.id.rc_container_layout);
//        mPluginLayout = (ViewGroup) mExtensionBar.findViewById(io.rong.imkit.R.id.rc_plugin_layout);
//
//        mEditTextLayout = LayoutInflater.from(this.getContext()).inflate(io.rong.imkit.R.layout.rc_ext_input_edit_text, (ViewGroup) null);
//        mEditTextLayout.setVisibility(View.VISIBLE);
//        mContainerLayout.addView(this.mEditTextLayout);
//        LayoutInflater.from(getContext()).inflate(io.rong.imkit.R.layout.rc_ext_voice_input, this.mContainerLayout, true);
//        mVoiceInputToggle = mContainerLayout.findViewById(io.rong.imkit.R.id.rc_audio_input_toggle);
//        mVoiceInputToggle.setVisibility(View.GONE);
//
//        mVoiceInputToggle.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (mExtensionClickListener != null) {
//                    mExtensionClickListener.onVoiceInputToggleTouch(view, motionEvent);
//                }
//                return false;
//            }
//        });
//        return view;
//    }

    @Override
    public void onVoiceInputToggleTouch(View v, MotionEvent event) {
//        super.onVoiceInputToggleTouch(v, event);
        String[] permissions = new String[]{"android.permission.RECORD_AUDIO"};
        if (!PermissionCheckUtil.checkPermissions(this.getActivity(), permissions)) {
            if (event.getAction() == 0) {
                PermissionCheckUtil.requestPermissions(this, permissions, 100);
            }

        } else {
            if (event.getAction() == 0) {
                MyAudioPlayManager.getInstance().stopPlay();
                mConversationType = mRongExtension.getConversationType();
                mTargetId = mRongExtension.getTargetId();
                MyAudioRecordManager.getInstance().startRecord(v.getRootView(), mConversationType, mTargetId);
                Log.e("啪啪啪","看看显示几条信息");

                mLastTouchY = event.getY();
                mUpDirection = false;
                ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input_hover);
            } else if (event.getAction() == 2) {
                if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
                    MyAudioRecordManager.getInstance().willCancelRecord();
                    mUpDirection = true;
                    ((Button) v).setText(R.string.rc_audio_input_cancel);
                } else if (event.getY() - mLastTouchY > - mOffsetLimit && mUpDirection) {
                    MyAudioRecordManager.getInstance().continueRecord();
                    mUpDirection = false;
                    ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input_hover);}
            } else if (event.getAction() == 1 || event.getAction() == 3) {
                MyAudioRecordManager.getInstance().stopRecord();

                ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input);
                Log.e("显示聊天类型", "ConversationType is "+ mConversationType + ", TargetId is " + mTargetId);
            }

            if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:VcMsg");

            }
        }

    }

    @Override
    public Conversation.ConversationType getConversationType(){
        return mConversationType;
    }

    @Override
    public String getTargetId(){
        return mTargetId;
    }

    @Override
    public void initFragment(Uri uri){
        super.initFragment(uri);
    }

}

