package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.thirdparty.V;

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



    public MyConversationFragment (){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRongExtension = (RongExtension)view.findViewById(io.rong.imkit.R.id.rc_extension);
        mRongExtension.setFragment(this);
//
//        mExtensionBar = (ViewGroup)LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_extension_bar, (ViewGroup)null);
//
//        mMainBar = (LinearLayout)mExtensionBar.findViewById(io.rong.imkit.R.id.ext_main_bar);
//        mSwitchLayout = (ViewGroup)mExtensionBar.findViewById(io.rong.imkit.R.id.rc_switch_layout);
//        mContainerLayout = (ViewGroup)mExtensionBar.findViewById(io.rong.imkit.R.id.rc_container_layout);
//        mPluginLayout = (ViewGroup)mExtensionBar.findViewById(io.rong.imkit.R.id.rc_plugin_layout);
//
//        mEditTextLayout = LayoutInflater.from(this.getContext()).inflate(io.rong.imkit.R.layout.rc_ext_input_edit_text, (ViewGroup)null);
//        mEditTextLayout.setVisibility(View.VISIBLE);
//        mContainerLayout.addView(this.mEditTextLayout);
//        LayoutInflater.from(getContext()).inflate(io.rong.imkit.R.layout.rc_ext_voice_input, this.mContainerLayout, true);
        mVoiceInputToggle = mContainerLayout.findViewById(io.rong.imkit.R.id.rc_audio_input_toggle);
        mVoiceInputToggle.setVisibility(View.GONE);

        mVoiceInputToggle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
        return view;
    }

    @Override
    public void onVoiceInputToggleTouch(View v, MotionEvent event) {
        String[] permissions = new String[]{"android.permission.RECORD_AUDIO"};
        if (!PermissionCheckUtil.checkPermissions(this.getActivity(), permissions)) {
            if (event.getAction() == 0) {
                PermissionCheckUtil.requestPermissions(this, permissions, 100);
            }

        } else {
            if (event.getAction() == 0) {
                AudioPlayManager.getInstance().stopPlay();
                AudioRecordManager.getInstance().startRecord(v.getRootView(), this.mConversationType, this.mTargetId);
                this.mLastTouchY = event.getY();
                this.mUpDirection = false;
                ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input_hover);
            } else if (event.getAction() == 2) {
                if (this.mLastTouchY - event.getY() > this.mOffsetLimit && !this.mUpDirection) {
                    AudioRecordManager.getInstance().willCancelRecord();
                    this.mUpDirection = true;
                    ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input);
                } else if (event.getY() - this.mLastTouchY > -this.mOffsetLimit && this.mUpDirection) {
                    AudioRecordManager.getInstance().continueRecord();
                    this.mUpDirection = false;
                    ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input_hover);
                }
            } else if (event.getAction() == 1 || event.getAction() == 3) {
                AudioRecordManager.getInstance().stopRecord();
                ((Button) v).setText(io.rong.imkit.R.string.rc_audio_input);
            }

            if (this.mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                RongIMClient.getInstance().sendTypingStatus(this.mConversationType, this.mTargetId, "RC:VcMsg");
            }

        }

    }


}
