package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.IExtensionClickListener;
import io.rong.imkit.InputMenu;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongExtension;
import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imkit.fragment.IHistoryDataResultCallback;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.mention.RongMentionManager;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imkit.plugin.location.AMapRealTimeActivity;
import io.rong.imkit.plugin.location.IRealTimeLocationStateListener;
import io.rong.imkit.plugin.location.IUserInfoProvider;
import io.rong.imkit.plugin.location.LocationManager;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.PromptPopupDialog;
import io.rong.imkit.widget.AutoRefreshListView;
import io.rong.imkit.widget.CSEvaluateDialog;
import io.rong.imkit.widget.adapter.MessageListAdapter;
import io.rong.imlib.CustomServiceConfig;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.RongIMClient.ResultCallback;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.CSCustomServiceInfo;
import io.rong.imlib.model.CSGroupItem;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.imlib.model.PublicServiceMenuItem;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.CSPullLeaveMessage;
import io.rong.message.ImageMessage;
import io.rong.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.PublicServiceCommandMessage;
import io.rong.message.TextMessage;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyFirstConversationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyFirstConversationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyFirstConversationFragment extends ConversationFragment implements AbsListView.OnScrollListener, IExtensionClickListener, IUserInfoProvider, CSEvaluateDialog.EvaluateClickListener {
    // TODO: Rename parameter arguments, choose names that match
    private static final String TAG = "MyFirstConversationFragment";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private RongExtension mRongExtension;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private SharedPreferences mSharedPreferences;

    private OnFragmentInteractionListener mListener;
    private String recognizerResults;


    private PublicServiceProfile mPublicServiceProfile;
    private boolean mEnableMention;
    private float mLastTouchY;
    private boolean mUpDirection;
    private float mOffsetLimit;
    private CSCustomServiceInfo mCustomUserInfo;
    private CSEvaluateDialog mEvaluateDialg;
    private ConversationInfo mCurrentConversationInfo;
    private String mDraft;
    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private ImageButton mNewMessageBtn;
    private TextView mNewMessageTextView;
    private int mLastMentionMsgId;
    private long indexMessageTime;
    private boolean mCSNeedToQuit = false;
    private boolean mReadRec;
    private boolean mSyncReadStatus;
    private int mNewMessageCount;
    private AutoRefreshListView mList;
    private boolean mHasMoreLocalMessagesUp = true;
    private boolean mHasMoreLocalMessagesDown = true;
    private CustomServiceConfig mCustomServiceConfig;
    private Button mUnreadBtn;
    private MessageListAdapter mListAdapter;
    private View mMsgListView;
    private LinearLayout mNotificationContainer;
    private List<String> mLocationShareParticipants;
    private boolean robotType = true;
    private long csEnterTime;
    private boolean csEvaluate = true;

    int ret = 0; // 函数调用返回值



    public MyFirstConversationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MyFirstConversationFragment.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIat = SpeechRecognizer.createRecognizer(getContext(), mInitListener);
        mSharedPreferences = getContext().getSharedPreferences(IatSettings.PREFER_NAME,
                Activity.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = super.onCreateView(inflater,container,savedInstanceState);
        mRongExtension = (RongExtension)view.findViewById(io.rong.imkit.R.id.rc_extension);
        mRongExtension.setFragment(this);
        this.mOffsetLimit = 70.0F * this.getActivity().getResources().getDisplayMetrics().density;
        this.mMsgListView = this.findViewById(view, io.rong.imkit.R.id.rc_layout_msg_list);
        this.mList = (AutoRefreshListView)this.findViewById(this.mMsgListView, io.rong.imkit.R.id.rc_list);
        this.mList.requestDisallowInterceptTouchEvent(true);
        this.mList.setMode(AutoRefreshListView.Mode.BOTH);
        this.mList.setTranscriptMode(2);
        this.mListAdapter = this.onResolveAdapter(this.getActivity());
        this.mList.setAdapter(this.mListAdapter);
        this.mList.setOnRefreshListener(new AutoRefreshListView.OnRefreshListener() {
            public void onRefreshFromStart() {
                if(MyFirstConversationFragment.this.mHasMoreLocalMessagesUp) {
                    MyFirstConversationFragment.this.getHistoryMessage(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, 30, AutoRefreshListView.Mode.START, 1);
                } else {
                    MyFirstConversationFragment.this.getRemoteHistoryMessages(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, 10);
                }

            }

            public void onRefreshFromEnd() {
                if(MyFirstConversationFragment.this.mHasMoreLocalMessagesDown && MyFirstConversationFragment.this.indexMessageTime > 0L) {
                    MyFirstConversationFragment.this.getHistoryMessage(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, 30, AutoRefreshListView.Mode.END, 1);
                } else {
                    MyFirstConversationFragment.this.mList.onRefreshComplete(0, 0, false);
                }

            }
        });
        this.mList.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == 2 && MyFirstConversationFragment.this.mList.getCount() - MyFirstConversationFragment.this.mList.getHeaderViewsCount() - MyFirstConversationFragment.this.mList.getFooterViewsCount() == 0) {
                    if(MyFirstConversationFragment.this.mHasMoreLocalMessagesUp) {
                        MyFirstConversationFragment.this.getHistoryMessage(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, 30, AutoRefreshListView.Mode.START, 1);
                    } else if(MyFirstConversationFragment.this.mList.getRefreshState() != AutoRefreshListView.State.REFRESHING) {
                        MyFirstConversationFragment.this.getRemoteHistoryMessages(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, 10);
                    }

                    return true;
                } else {
                    if(event.getAction() == 1 && MyFirstConversationFragment.this.mRongExtension != null && MyFirstConversationFragment.this.mRongExtension.isExtensionExpanded()) {
                        MyFirstConversationFragment.this.mRongExtension.collapseExtension();
                    }

                    return false;
                }
            }
        });
        if(RongContext.getInstance().getNewMessageState()) {
            this.mNewMessageTextView = (TextView)this.findViewById(view, io.rong.imkit.R.id.rc_new_message_number);
            this.mNewMessageBtn = (ImageButton)this.findViewById(view, io.rong.imkit.R.id.rc_new_message_count);
            this.mNewMessageBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MyFirstConversationFragment.this.mList.smoothScrollToPosition(MyFirstConversationFragment.this.mListAdapter.getCount());
                    MyFirstConversationFragment.this.mNewMessageBtn.setVisibility(View.GONE);
                    MyFirstConversationFragment.this.mNewMessageTextView.setVisibility(View.GONE);
                    MyFirstConversationFragment.this.mNewMessageCount = 0;
                }
            });
        }

        if(RongContext.getInstance().getUnreadMessageState()) {
            this.mUnreadBtn = (Button)this.findViewById(this.mMsgListView, io.rong.imkit.R.id.rc_unread_message_count);
        }

        this.mList.addOnScrollListener(this);
        this.mListAdapter.setOnItemHandlerListener(new MessageListAdapter.OnItemHandlerListener() {
            public boolean onWarningViewClick(final int position, final Message data, View v) {
                if(!MyFirstConversationFragment.this.onResendItemClick(data)) {
                    RongIMClient.getInstance().deleteMessages(new int[]{data.getMessageId()}, new ResultCallback<Boolean>() {
                        public void onSuccess(Boolean aBoolean) {
                            if(aBoolean.booleanValue()) {
                                MyFirstConversationFragment.this.mListAdapter.remove(position);
                                data.setMessageId(0);
                                if(data.getContent() instanceof ImageMessage) {
                                    RongIM.getInstance().sendImageMessage(data, (String)null, (String)null, (RongIMClient.SendImageMessageCallback)null);
                                } else if(data.getContent() instanceof LocationMessage) {
                                    RongIM.getInstance().sendLocationMessage(data, (String)null, (String)null, (IRongCallback.ISendMessageCallback)null);
                                } else if(data.getContent() instanceof MediaMessageContent) {
                                    RongIM.getInstance().sendMediaMessage(data, (String)null, (String)null, (IRongCallback.ISendMediaMessageCallback)null);
                                } else {
                                    RongIM.getInstance().sendMessage(data, (String)null, (String)null, (IRongCallback.ISendMessageCallback)null);
                                }
                            }

                        }

                        public void onError(RongIMClient.ErrorCode e) {
                        }
                    });
                }

                return true;
            }

            public void onReadReceiptStateClick(Message message) {
                MyFirstConversationFragment.this.onReadReceiptStateClick(message);
            }
        });
        return view;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public MessageListAdapter onResolveAdapter(Context context) {
        return super.onResolveAdapter(context);
    }

    @Override
    public void onEditTextClick(EditText editText) {
        super.onEditTextClick(editText);
    }

    @Override
    public boolean onResendItemClick(Message message) {
        return super.onResendItemClick(message);
    }

    @Override
    public void onReadReceiptStateClick(Message message) {
        super.onReadReceiptStateClick(message);
    }

    @Override
    public void onSelectCustomerServiceGroup(List<CSGroupItem> groupList) {
        super.onSelectCustomerServiceGroup(groupList);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean isLocationSharing() {
        return super.isLocationSharing();
    }

    @Override
    public void showQuitLocationSharingDialog(Activity activity) {
        super.showQuitLocationSharingDialog(activity);
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        return super.handleMessage(msg);
    }

    @Override
    public void onWarningDialog(String msg) {
        super.onWarningDialog(msg);
    }

    @Override
    public void onCustomServiceWarning(String msg, boolean evaluate, boolean robotType) {
        super.onCustomServiceWarning(msg, evaluate, robotType);
    }

    @Override
    public boolean onCustomServiceEvaluation(boolean isPullEva, String dialogId, boolean robotType, boolean evaluate) {
        return super.onCustomServiceEvaluation(isPullEva, dialogId, robotType, evaluate);
    }

    @Override
    public void onSendToggleClick(View v, String text) {
        super.onSendToggleClick(v, text);
    }

    @Override
    public void onImageResult(List<Uri> selectedImages, boolean origin) {
        super.onImageResult(selectedImages, origin);
    }

    @Override
    public void onSwitchToggleClick(View v, ViewGroup inputBoard) {
        super.onSwitchToggleClick(v, inputBoard);
    }

    @Override
    public void onLocationResult(double lat, double lng, String poi, Uri thumb) {
        super.onLocationResult(lat, lng, poi, thumb);
    }

    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();
    }

    @Override
    public void onVoiceInputToggleTouch(View v, MotionEvent event) {
        String[] permissions = new String[]{"android.permission.RECORD_AUDIO"};
        if(!PermissionCheckUtil.checkPermissions(this.getActivity(), permissions)) {
            if(event.getAction() == 0) {
                PermissionCheckUtil.requestPermissions(this, permissions, 100);
            }

        } else {
            if(event.getAction() == 0) {
                AudioPlayManager.getInstance().stopPlay();
                AudioRecordManager.getInstance().startRecord(v.getRootView(), this.mConversationType, this.mTargetId);
                this.mLastTouchY = event.getY();
                this.mUpDirection = false;
                ((Button)v).setText(io.rong.imkit.R.string.rc_audio_input_hover);
            } else if(event.getAction() == 2) {
                if(this.mLastTouchY - event.getY() > this.mOffsetLimit && !this.mUpDirection) {
                    AudioRecordManager.getInstance().willCancelRecord();
                    this.mUpDirection = true;
                    ((Button)v).setText(io.rong.imkit.R.string.rc_audio_input);
                } else if(event.getY() - this.mLastTouchY > -this.mOffsetLimit && this.mUpDirection) {
                    AudioRecordManager.getInstance().continueRecord();
                    this.mUpDirection = false;
                    ((Button)v).setText(io.rong.imkit.R.string.rc_audio_input_hover);
                }
            } else if(event.getAction() == 1 || event.getAction() == 3) {
                AudioRecordManager.getInstance().stopRecord();
                ((Button)v).setText(io.rong.imkit.R.string.rc_audio_input);
            }

            if(this.mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                RongIMClient.getInstance().sendTypingStatus(this.mConversationType, this.mTargetId, "RC:VcMsg");
            }

        }
//         //设置参数
//        setParam();
//        ret = mIat.startListening(mRecognizerListener);
//        if (ret != ErrorCode.SUCCESS) {
//            Log.e("MyFirstConversationFragment", "听写失败，错误码：" + ret);
//        } else {
//            Log.e("MyFirstConversationFragment",  getString(R.string.text_begin));
//        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 100 && grantResults[0] != 0) {
            Toast.makeText(this.getActivity(), this.getResources().getString(io.rong.imkit.R.string.rc_permission_grant_needed), Toast.LENGTH_SHORT).show();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onEmoticonToggleClick(View v, ViewGroup extensionBoard) {
    }

    @Override
    public void onPluginToggleClick(View v, ViewGroup extensionBoard) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        super.onTextChanged(s, start, before, count);
    }

    @Override
    public void afterTextChanged(Editable s) {
        super.afterTextChanged(s);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return super.onKey(v, keyCode, event);
    }

    @Override
    public void onMenuClick(int root, int sub) {
       super.onMenuClick(root, sub);
    }

    @Override
    public void onPluginClicked(IPluginModule pluginModule, int position) {
        super.onPluginClicked(pluginModule, position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onEventMainThread(Event.ConnectEvent event) {
        super.onEventMainThread(event);
    }

    @Override
    public MessageListAdapter getMessageAdapter() {
        return super.getMessageAdapter();
    }

    @Override
    public boolean shouldUpdateMessage(Message message, int left) {
        return true;
    }

    @Override
    protected void initFragment(Uri uri) {
        this.indexMessageTime = this.getActivity().getIntent().getLongExtra("indexMessageTime", 0L);
        RLog.d("ConversationFragment", "initFragment : " + uri + ",this=" + this + ", time = " + this.indexMessageTime);
        if(uri != null) {
            String mode = uri.getLastPathSegment().toUpperCase(Locale.US);
            this.mConversationType = Conversation.ConversationType.valueOf(mode);
            this.mTargetId = uri.getQueryParameter("targetId");
            this.mRongExtension.setConversation(this.mConversationType, this.mTargetId);
            RongIMClient.getInstance().getTextMessageDraft(this.mConversationType, this.mTargetId, new ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    MyFirstConversationFragment.this.mDraft = s;
                    if(MyFirstConversationFragment.this.mRongExtension != null) {
                        EditText editText = MyFirstConversationFragment.this.mRongExtension.getInputEditText();
                        editText.setText(s);
                        editText.setSelection(editText.length());
                        MyFirstConversationFragment.this.mRongExtension.setExtensionClickListener(MyFirstConversationFragment.this);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    if(MyFirstConversationFragment.this.mRongExtension != null) {
                        MyFirstConversationFragment.this.mRongExtension.setExtensionClickListener(MyFirstConversationFragment.this);
                    }
                }
            });
            this.mCurrentConversationInfo = ConversationInfo.obtain(this.mConversationType, this.mTargetId);
            RongContext.getInstance().registerConversationInfo(this.mCurrentConversationInfo);
            this.mNotificationContainer = (LinearLayout)this.mMsgListView.findViewById(io.rong.imkit.R.id.rc_notification_container);
            if(this.mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE) && this.getActivity() != null && this.getActivity().getIntent() != null && this.getActivity().getIntent().getData() != null) {
                this.mCustomUserInfo = (CSCustomServiceInfo)this.getActivity().getIntent().getParcelableExtra("customServiceInfo");
            }


            LocationManager.getInstance().bindConversation(this.getActivity(), this.mConversationType, this.mTargetId);
            LocationManager.getInstance().setUserInfoProvider(this);
            LocationManager.getInstance().setParticipantChangedListener(new IRealTimeLocationStateListener() {
                private View mRealTimeBar;
                private TextView mRealTimeText;

                public void onParticipantChanged(List<String> userIdList) {
                    if(!MyFirstConversationFragment.this.isDetached()) {
                        if(this.mRealTimeBar == null) {
                            this.mRealTimeBar = MyFirstConversationFragment.this.inflateNotificationView(io.rong.imkit.R.layout.rc_notification_realtime_location);
                            this.mRealTimeText = (TextView)this.mRealTimeBar.findViewById(io.rong.imkit.R.id.real_time_location_text);
                            this.mRealTimeBar.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId);
                                    if(status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                                        PromptPopupDialog intent = PromptPopupDialog.newInstance(MyFirstConversationFragment.this.getActivity(), "", MyFirstConversationFragment.this.getResources().getString(io.rong.imkit.R.string.rc_real_time_join_notification));
                                        intent.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                                            public void onPositiveButtonClicked() {
                                                int result = LocationManager.getInstance().joinLocationSharing();
                                                if(result == 0) {
                                                    Intent intent = new Intent(MyFirstConversationFragment.this.getActivity(), AMapRealTimeActivity.class);
                                                    if(MyFirstConversationFragment.this.mLocationShareParticipants != null) {
                                                        intent.putStringArrayListExtra("participants", (ArrayList)MyFirstConversationFragment.this.mLocationShareParticipants);
                                                    }

                                                    MyFirstConversationFragment.this.startActivity(intent);
                                                } else if(result == 1) {
                                                    Toast.makeText(MyFirstConversationFragment.this.getActivity(), io.rong.imkit.R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
                                                } else if(result == 2) {
                                                    Toast.makeText(MyFirstConversationFragment.this.getActivity(), io.rong.imkit.R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
                                                }

                                            }
                                        });
                                        intent.show();
                                    } else {
                                        Intent intent1 = new Intent(MyFirstConversationFragment.this.getActivity(), AMapRealTimeActivity.class);
                                        if(MyFirstConversationFragment.this.mLocationShareParticipants != null) {
                                            intent1.putStringArrayListExtra("participants", (ArrayList)MyFirstConversationFragment.this.mLocationShareParticipants);
                                        }

                                        MyFirstConversationFragment.this.startActivity(intent1);
                                    }

                                }
                            });
                        }

                        MyFirstConversationFragment.this.mLocationShareParticipants = userIdList;
                        if(userIdList != null) {
                            if(userIdList.size() == 0) {
                                MyFirstConversationFragment.this.hideNotificationView(this.mRealTimeBar);
                            } else {
                                if(userIdList.size() == 1 && userIdList.contains(RongIM.getInstance().getCurrentUserId())) {
                                    this.mRealTimeText.setText(MyFirstConversationFragment.this.getResources().getString(io.rong.imkit.R.string.rc_you_are_sharing_location));
                                } else if(userIdList.size() == 1 && !userIdList.contains(RongIM.getInstance().getCurrentUserId())) {
                                    this.mRealTimeText.setText(String.format(MyFirstConversationFragment.this.getResources().getString(io.rong.imkit.R.string.rc_other_is_sharing_location), new Object[]{MyFirstConversationFragment.this.getNameFromCache((String)userIdList.get(0))}));
                                } else {
                                    this.mRealTimeText.setText(String.format(MyFirstConversationFragment.this.getResources().getString(io.rong.imkit.R.string.rc_others_are_sharing_location), new Object[]{Integer.valueOf(userIdList.size())}));
                                }

                                MyFirstConversationFragment.this.showNotificationView(this.mRealTimeBar);
                            }
                        } else {
                            MyFirstConversationFragment.this.hideNotificationView(this.mRealTimeBar);
                        }

                    }
                }

                public void onErrorException() {
                    if(!MyFirstConversationFragment.this.isDetached()) {
                        MyFirstConversationFragment.this.hideNotificationView(this.mRealTimeBar);
                        if(MyFirstConversationFragment.this.mLocationShareParticipants != null) {
                            MyFirstConversationFragment.this.mLocationShareParticipants.clear();
                            MyFirstConversationFragment.this.mLocationShareParticipants = null;
                        }
                    }

                }
            });
            if(this.mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
                boolean scrollMode = this.getActivity() != null && this.getActivity().getIntent().getBooleanExtra("createIfNotExist", true);
                int message = this.getResources().getInteger(io.rong.imkit.R.integer.rc_chatroom_first_pull_message_count);
                if(scrollMode) {
                    RongIMClient.getInstance().joinChatRoom(this.mTargetId, message, new RongIMClient.OperationCallback() {
                        public void onSuccess() {
                            RLog.i("ConversationFragment", "joinChatRoom onSuccess : " + MyFirstConversationFragment.this.mTargetId);
                        }

                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e("ConversationFragment", "joinChatRoom onError : " + errorCode);
                            if(MyFirstConversationFragment.this.getActivity() != null) {
                                if(errorCode != RongIMClient.ErrorCode.RC_NET_UNAVAILABLE && errorCode != RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                                    MyFirstConversationFragment.this.onWarningDialog(MyFirstConversationFragment.this.getString(io.rong.imkit.R.string.rc_join_chatroom_failure));
                                } else {
                                    MyFirstConversationFragment.this.onWarningDialog(MyFirstConversationFragment.this.getString(io.rong.imkit.R.string.rc_notice_network_unavailable));
                                }
                            }

                        }
                    });
                } else {
                    RongIMClient.getInstance().joinExistChatRoom(this.mTargetId, message, new RongIMClient.OperationCallback() {
                        public void onSuccess() {
                            RLog.i("ConversationFragment", "joinExistChatRoom onSuccess : " + MyFirstConversationFragment.this.mTargetId);
                        }

                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e("ConversationFragment", "joinExistChatRoom onError : " + errorCode);
                            if(MyFirstConversationFragment.this.getActivity() != null) {
                                if(errorCode != RongIMClient.ErrorCode.RC_NET_UNAVAILABLE && errorCode != RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                                    MyFirstConversationFragment.this.onWarningDialog(MyFirstConversationFragment.this.getString(io.rong.imkit.R.string.rc_join_chatroom_failure));
                                } else {
                                    MyFirstConversationFragment.this.onWarningDialog(MyFirstConversationFragment.this.getString(io.rong.imkit.R.string.rc_notice_network_unavailable));
                                }
                            }

                        }
                    });
                }
            } else if(this.mConversationType != Conversation.ConversationType.APP_PUBLIC_SERVICE && this.mConversationType != Conversation.ConversationType.PUBLIC_SERVICE) {
                if(this.mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
                    this.onStartCustomService(this.mTargetId);
                } else if(this.mEnableMention && (this.mConversationType.equals(Conversation.ConversationType.DISCUSSION) || this.mConversationType.equals(Conversation.ConversationType.GROUP))) {
                    RongMentionManager.getInstance().createInstance(this.mConversationType, this.mTargetId, this.mRongExtension.getInputEditText());
                }
            } else {
                PublicServiceCommandMessage scrollMode1 = new PublicServiceCommandMessage();
                scrollMode1.setCommand(PublicServiceMenu.PublicServiceMenuItemType.Entry.getMessage());
                Message message1 = Message.obtain(this.mTargetId, this.mConversationType, scrollMode1);
                RongIMClient.getInstance().sendMessage(message1, (String)null, (String)null, new IRongCallback.ISendMessageCallback() {
                    public void onAttached(Message message) {
                    }

                    public void onSuccess(Message message) {
                    }

                    public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                    }
                });
                Conversation.PublicServiceType publicServiceType;
                if(this.mConversationType == Conversation.ConversationType.PUBLIC_SERVICE) {
                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                } else {
                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                }

                RongIM.getInstance().getPublicServiceProfile(publicServiceType, this.mTargetId, new ResultCallback<PublicServiceProfile>() {
                    public void onSuccess(PublicServiceProfile publicServiceProfile) {
                        ArrayList inputMenuList = new ArrayList();
                        PublicServiceMenu menu = publicServiceProfile.getMenu();
                        ArrayList items = menu != null?menu.getMenuItems():null;
                        if(items != null && MyFirstConversationFragment.this.mRongExtension != null) {
                            MyFirstConversationFragment.this.mPublicServiceProfile = publicServiceProfile;
                            Iterator var5 = items.iterator();

                            while(var5.hasNext()) {
                                PublicServiceMenuItem item = (PublicServiceMenuItem)var5.next();
                                InputMenu inputMenu = new InputMenu();
                                inputMenu.title = item.getName();
                                inputMenu.subMenuList = new ArrayList();
                                Iterator var8 = item.getSubMenuItems().iterator();

                                while(var8.hasNext()) {
                                    PublicServiceMenuItem i = (PublicServiceMenuItem)var8.next();
                                    inputMenu.subMenuList.add(i.getName());
                                }

                                inputMenuList.add(inputMenu);
                            }

                            MyFirstConversationFragment.this.mRongExtension.setInputMenu(inputMenuList, true);
                        }

                    }

                    public void onError(RongIMClient.ErrorCode e) {
                    }
                });
            }
        }

        RongIMClient.getInstance().getConversation(this.mConversationType, this.mTargetId, new RongIMClient.ResultCallback<Conversation>() {
            public void onSuccess(Conversation conversation) {
                if(conversation != null && MyFirstConversationFragment.this.getActivity() != null) {
                    final int unreadCount = conversation.getUnreadMessageCount();
                    if(unreadCount > 0) {
                        if(MyFirstConversationFragment.this.mReadRec && MyFirstConversationFragment.this.mConversationType == Conversation.ConversationType.PRIVATE && RongContext.getInstance().isReadReceiptConversationType(Conversation.ConversationType.PRIVATE)) {
                            RongIMClient.getInstance().sendReadReceiptMessage(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, conversation.getSentTime());
                        }

                        if(MyFirstConversationFragment.this.mSyncReadStatus && (MyFirstConversationFragment.this.mConversationType == Conversation.ConversationType.PRIVATE || MyFirstConversationFragment.this.mConversationType == Conversation.ConversationType.GROUP || MyFirstConversationFragment.this.mConversationType == Conversation.ConversationType.DISCUSSION)) {
                            RongIMClient.getInstance().syncConversationReadStatus(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, conversation.getSentTime(), (RongIMClient.OperationCallback)null);
                        }
                    }

                    if(conversation.getMentionedCount() > 0) {
                        MyFirstConversationFragment.this.getLastMentionedMessageId(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId);
                    } else {
                        RongIM.getInstance().clearMessagesUnreadStatus(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, (RongIMClient.ResultCallback)null);
                    }

                    if(unreadCount > 10 && MyFirstConversationFragment.this.mUnreadBtn != null) {
                        if(unreadCount > 150) {
                            MyFirstConversationFragment.this.mUnreadBtn.setText(String.format("%s%s", new Object[]{"150+", MyFirstConversationFragment.this.getActivity().getResources().getString(io.rong.imkit.R.string.rc_new_messages)}));
                        } else {
                            MyFirstConversationFragment.this.mUnreadBtn.setText(String.format("%s%s", new Object[]{Integer.valueOf(unreadCount), MyFirstConversationFragment.this.getActivity().getResources().getString(io.rong.imkit.R.string.rc_new_messages)}));
                        }

                        MyFirstConversationFragment.this.mUnreadBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                MyFirstConversationFragment.this.mUnreadBtn.setClickable(false);
                                TranslateAnimation animation = new TranslateAnimation(0.0F, 500.0F, 0.0F, 0.0F);
                                animation.setDuration(500L);
                                MyFirstConversationFragment.this.mUnreadBtn.startAnimation(animation);
                                animation.setFillAfter(true);
                                animation.setAnimationListener(new Animation.AnimationListener() {
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    public void onAnimationEnd(Animation animation) {
                                        MyFirstConversationFragment.this.mUnreadBtn.setVisibility(View.GONE);
                                        if(unreadCount <= 30) {
                                            if(MyFirstConversationFragment.this.mList.getCount() < 30) {
                                                MyFirstConversationFragment.this.mList.smoothScrollToPosition(MyFirstConversationFragment.this.mListAdapter.getCount() - unreadCount);
                                            } else {
                                                MyFirstConversationFragment.this.mList.smoothScrollToPosition(30 - unreadCount);
                                            }
                                        } else if(unreadCount > 30) {
                                            MyFirstConversationFragment.this.getHistoryMessage(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, unreadCount - 30 - 1, AutoRefreshListView.Mode.START, 2);
                                        }

                                    }

                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                });
                            }
                        });
                        TranslateAnimation translateAnimation = new TranslateAnimation(300.0F, 0.0F, 0.0F, 0.0F);
                        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0F, 1.0F);
                        translateAnimation.setDuration(1000L);
                        alphaAnimation.setDuration(2000L);
                        AnimationSet set = new AnimationSet(true);
                        set.addAnimation(translateAnimation);
                        set.addAnimation(alphaAnimation);
                        MyFirstConversationFragment.this.mUnreadBtn.setVisibility(View.VISIBLE);
                        MyFirstConversationFragment.this.mUnreadBtn.startAnimation(set);
                        set.setAnimationListener(new Animation.AnimationListener() {
                            public void onAnimationStart(Animation animation) {
                            }

                            public void onAnimationEnd(Animation animation) {
                                MyFirstConversationFragment.this.getHandler().postDelayed(new Runnable() {
                                    public void run() {
                                        TranslateAnimation animation = new TranslateAnimation(0.0F, 700.0F, 0.0F, 0.0F);
                                        animation.setDuration(700L);
                                        animation.setFillAfter(true);
                                        MyFirstConversationFragment.this.mUnreadBtn.startAnimation(animation);
                                    }
                                }, 4000L);
                            }

                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                    }
                }

            }

            public void onError(RongIMClient.ErrorCode e) {
            }
        });
        AutoRefreshListView.Mode mode1 = this.indexMessageTime > 0L? AutoRefreshListView.Mode.END: AutoRefreshListView.Mode.START;
        int scrollMode2 = this.indexMessageTime > 0L?1:3;
        this.getHistoryMessage(this.mConversationType, this.mTargetId, 30, mode1, scrollMode2);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String targetId, long dateTime, int reqCount, final IHistoryDataResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, dateTime, reqCount, new ResultCallback<List<Message>>() {
            public void onSuccess(List<Message> messages) {
                if(callback != null) {
                    callback.onResult(messages);
                }

            }

            public void onError(RongIMClient.ErrorCode e) {
                RLog.e("MyFirstConversationFragment", "getRemoteHistoryMessages " + e);
                if(callback != null) {
                    callback.onResult((List<Message>)null);
                }

            }
        });
    }

    private void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String targetId, final int reqCount) {
        this.mList.onRefreshStart(AutoRefreshListView.Mode.START);
        if (this.mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
            this.mList.onRefreshComplete(0, 0, false);
            RLog.w("MyFirstConversationFragment", "Should not get remote message in chatroom");
        } else {
            long dateTime = this.mListAdapter.getCount() == 0 ? 0L : ((UIMessage) this.mListAdapter.getItem(0)).getSentTime();
            this.getRemoteHistoryMessages(conversationType, targetId, dateTime, reqCount, new IHistoryDataResultCallback<List<Message>>() {
                public void onResult(List<Message> messages) {
                    RLog.i("MyFirstConversationFragment", "getRemoteHistoryMessages " + (messages == null ? 0 : messages.size()));
                    Message lastMessage = null;
                    if (messages != null && messages.size() > 0) {
                        if (MyFirstConversationFragment.this.mListAdapter.getCount() == 0) {
                            lastMessage = (Message) messages.get(0);
                        }

                        ArrayList remoteList = new ArrayList();
                        Iterator var4 = messages.iterator();

                        while (var4.hasNext()) {
                            Message uiMessage = (Message) var4.next();
                            if (uiMessage.getMessageId() > 0) {
                                UIMessage uiMessage1 = UIMessage.obtain(uiMessage);
                                if (uiMessage.getContent() instanceof CSPullLeaveMessage) {
                                    uiMessage1.setCsConfig(MyFirstConversationFragment.this.mCustomServiceConfig);
                                }

                                remoteList.add(uiMessage1);
                            }
                        }

                        List remoteList1 = MyFirstConversationFragment.this.filterMessage(remoteList);
                        if (remoteList1 != null && remoteList1.size() > 0) {
                            var4 = remoteList1.iterator();

                            while (var4.hasNext()) {
                                UIMessage uiMessage2 = (UIMessage) var4.next();
                                MyFirstConversationFragment.this.mListAdapter.add(uiMessage2, 0);
                            }

                            MyFirstConversationFragment.this.mList.setTranscriptMode(0);
                            MyFirstConversationFragment.this.mListAdapter.notifyDataSetChanged();
                            MyFirstConversationFragment.this.mList.setSelection(messages.size() + 1);
                            MyFirstConversationFragment.this.sendReadReceiptResponseIfNeeded(messages);
                            MyFirstConversationFragment.this.mList.onRefreshComplete(messages.size(), reqCount, false);
                            if (lastMessage != null) {
                                RongContext.getInstance().getEventBus().post(lastMessage);
                            }
                        }
                    } else {
                        MyFirstConversationFragment.this.mList.onRefreshComplete(0, reqCount, false);
                    }

                }

                public void onError() {
                    MyFirstConversationFragment.this.mList.onRefreshComplete(0, reqCount, false);
                }
            });
        }
    }

    private List<UIMessage> filterMessage(List<UIMessage> srcList) {
        Object destList;
        if(this.mListAdapter.getCount() > 0) {
            destList = new ArrayList();

            for(int i = 0; i < this.mListAdapter.getCount(); ++i) {
                Iterator var4 = srcList.iterator();

                while(var4.hasNext()) {
                    UIMessage msg = (UIMessage)var4.next();
                    if(!((List)destList).contains(msg) && msg.getMessageId() != ((UIMessage)this.mListAdapter.getItem(i)).getMessageId()) {
                        ((List)destList).add(msg);
                    }
                }
            }
        } else {
            destList = srcList;
        }

        return (List)destList;
    }


    private void getLastMentionedMessageId(Conversation.ConversationType conversationType, String targetId) {
        RongIMClient.getInstance().getUnreadMentionedMessages(conversationType, targetId, new ResultCallback<List<Message>>() {
            public void onSuccess(List<Message> messages) {
                if(messages != null && messages.size() > 0) {
                    MyFirstConversationFragment.this.mLastMentionMsgId = ((Message)messages.get(0)).getMessageId();
                    int index = MyFirstConversationFragment.this.mListAdapter.findPosition((long)MyFirstConversationFragment.this.mLastMentionMsgId);
                    RLog.i("MyFirstConversationFragment", "getLastMentionedMessageId " + MyFirstConversationFragment.this.mLastMentionMsgId + " " + index);
                    if(MyFirstConversationFragment.this.mLastMentionMsgId > 0 && index >= 0) {
                        MyFirstConversationFragment.this.mList.smoothScrollToPosition(index);
                        MyFirstConversationFragment.this.mLastMentionMsgId = 0;
                    }
                }

                RongIM.getInstance().clearMessagesUnreadStatus(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, (ResultCallback)null);
            }

            public void onError(RongIMClient.ErrorCode e) {
                RongIM.getInstance().clearMessagesUnreadStatus(MyFirstConversationFragment.this.mConversationType, MyFirstConversationFragment.this.mTargetId, (ResultCallback)null);
            }
        });
    }


    @Override
    public void onExtensionCollapsed() {
    }

    @Override
    public void onExtensionExpanded(int h) {
        super.onExtensionExpanded(h);
    }

    @Override
    public void onStartCustomService(String targetId) {
        super.onStartCustomService(targetId);
    }

    @Override
    public void onStopCustomService(String targetId) {
        RongIMClient.getInstance().stopCustomService(targetId);
    }


    private void startTimer(int event, int interval) {
        this.getHandler().removeMessages(event);
        this.getHandler().sendEmptyMessageDelayed(event, (long)interval);
    }

    private void stopTimer(int event) {
        this.getHandler().removeMessages(event);
    }

    @Override
    public Conversation.ConversationType getConversationType() {
        return this.mConversationType;
    }

    @Override
    public String getTargetId() {
        return this.mTargetId;
    }


    private String getNameFromCache(String targetId) {
        UserInfo info = RongContext.getInstance().getUserInfoFromCache(targetId);
        return info == null?targetId:info.getName();
    }

    public void getHistoryMessage(Conversation.ConversationType conversationType, String targetId, int lastMessageId, int reqCount, MyFirstConversationFragment.LoadMessageDirection direction, final IHistoryDataResultCallback<List<Message>> callback) {
        if(direction == MyFirstConversationFragment.LoadMessageDirection.UP) {
            RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, lastMessageId, reqCount, new ResultCallback<List<Message>>() {
                public void onSuccess(List<Message> messages) {
                    if(callback != null) {
                        callback.onResult(messages);
                    }

                }

                public void onError(RongIMClient.ErrorCode e) {
                    RLog.e("ConversationFragment", "getHistoryMessages " + e);
                    if(callback != null) {
                        callback.onResult((List<Message>) null);
                    }

                }
            });
        } else {
            byte before = 10;
            byte after = 10;
            if(this.mListAdapter.getCount() > 0) {
                after = 30;
                before = 0;
            }

            RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, this.indexMessageTime, before, after, new ResultCallback<List<Message>>() {
                public void onSuccess(List<Message> messages) {
                    if(callback != null) {
                        callback.onResult(messages);
                    }

                    if(messages != null && messages.size() > 0 && MyFirstConversationFragment.this.mHasMoreLocalMessagesDown) {
                        MyFirstConversationFragment.this.indexMessageTime = ((Message)messages.get(0)).getSentTime();
                    } else {
                        MyFirstConversationFragment.this.indexMessageTime = 0L;
                    }

                }

                public void onError(RongIMClient.ErrorCode e) {
                    RLog.e("ConversationFragment", "getHistoryMessages " + e);
                    if(callback != null) {
                        callback.onResult((List<Message>)null);
                    }

                    MyFirstConversationFragment.this.indexMessageTime = 0L;
                }
            });
        }
    }

    public void getHistoryMessage(Conversation.ConversationType conversationType, String targetId, final int reqCount, AutoRefreshListView.Mode mode, final int scrollMode) {
        this.mList.onRefreshStart(mode);
        if(conversationType.equals(Conversation.ConversationType.CHATROOM)) {
            this.mList.onRefreshComplete(0, 0, false);
            RLog.w("MyFirstConversationFragment", "Should not get local message in chatroom");
        } else {
            int last = this.mListAdapter.getCount() == 0?-1:((UIMessage)this.mListAdapter.getItem(0)).getMessageId();
            final MyFirstConversationFragment.LoadMessageDirection direction = mode == AutoRefreshListView.Mode.START? MyFirstConversationFragment.LoadMessageDirection.UP: MyFirstConversationFragment.LoadMessageDirection.DOWN;
            this.getHistoryMessage(conversationType, targetId, last, reqCount, direction, new IHistoryDataResultCallback<List<Message>>() {
                public void onResult(List<Message> messages) {
                    int msgCount = messages == null?0:messages.size();
                    RLog.i("ConversationFragment", "getHistoryMessage " + msgCount);
                    if(direction == MyFirstConversationFragment.LoadMessageDirection.DOWN) {
                        MyFirstConversationFragment.this.mList.onRefreshComplete(msgCount > 1?msgCount:0, msgCount, false);
                        MyFirstConversationFragment.this.mHasMoreLocalMessagesDown = msgCount > 1;
                    } else {
                        MyFirstConversationFragment.this.mList.onRefreshComplete(msgCount, reqCount, false);
                        MyFirstConversationFragment.this.mHasMoreLocalMessagesUp = msgCount == reqCount;
                    }

                    if(messages != null && messages.size() > 0) {
                        int index = 0;
                        if(direction == MyFirstConversationFragment.LoadMessageDirection.DOWN) {
                            index = MyFirstConversationFragment.this.mListAdapter.getCount() == 0?0:MyFirstConversationFragment.this.mListAdapter.getCount();
                        }

                        boolean needRefresh = false;
                        Iterator selected = messages.iterator();

                        while(selected.hasNext()) {
                            Message i = (Message)selected.next();
                            boolean contains = false;

                            for(int uiMessage = 0; uiMessage < MyFirstConversationFragment.this.mListAdapter.getCount(); ++uiMessage) {
                                contains = ((UIMessage)MyFirstConversationFragment.this.mListAdapter.getItem(uiMessage)).getMessageId() == i.getMessageId();
                                if(contains) {
                                    break;
                                }
                            }

                            if(!contains) {
                                UIMessage var11 = UIMessage.obtain(i);
                                if(i.getContent() instanceof CSPullLeaveMessage) {
                                    var11.setCsConfig(MyFirstConversationFragment.this.mCustomServiceConfig);
                                }

                                MyFirstConversationFragment.this.mListAdapter.add(var11, index);
                                needRefresh = true;
                            }
                        }

                        if(needRefresh) {
                            if(scrollMode == 3) {
                                MyFirstConversationFragment.this.mList.setTranscriptMode(2);
                            } else {
                                MyFirstConversationFragment.this.mList.setTranscriptMode(0);
                            }

                            MyFirstConversationFragment.this.mListAdapter.notifyDataSetChanged();
                            if(MyFirstConversationFragment.this.mLastMentionMsgId > 0) {
                                index = MyFirstConversationFragment.this.mListAdapter.findPosition((long)MyFirstConversationFragment.this.mLastMentionMsgId);
                                MyFirstConversationFragment.this.mList.smoothScrollToPosition(index);
                                MyFirstConversationFragment.this.mLastMentionMsgId = 0;
                            } else if(2 == scrollMode) {
                                MyFirstConversationFragment.this.mList.setSelection(0);
                            } else if(scrollMode == 3) {
                                MyFirstConversationFragment.this.mList.setSelection(MyFirstConversationFragment.this.mList.getCount());
                            } else if(direction == MyFirstConversationFragment.LoadMessageDirection.DOWN) {
                                int var9 = MyFirstConversationFragment.this.mList.getSelectedItemPosition();
                                if(var9 <= 0) {
                                    for(int var10 = 0; var10 < MyFirstConversationFragment.this.mListAdapter.getCount(); ++var10) {
                                        if(((UIMessage)MyFirstConversationFragment.this.mListAdapter.getItem(var10)).getSentTime() == MyFirstConversationFragment.this.indexMessageTime) {
                                            MyFirstConversationFragment.this.mList.setSelection(var10);
                                            break;
                                        }
                                    }
                                } else {
                                    MyFirstConversationFragment.this.mList.setSelection(MyFirstConversationFragment.this.mListAdapter.getCount() - messages.size());
                                }
                            } else {
                                MyFirstConversationFragment.this.mList.setSelection(messages.size() + 1);
                            }

                            MyFirstConversationFragment.this.sendReadReceiptResponseIfNeeded(messages);
                        }
                    }

                }

                public void onError() {
                    MyFirstConversationFragment.this.mList.onRefreshComplete(reqCount, reqCount, false);
                }
            });
        }
    }

    public void hideNotificationView(View notificationView) {
        if(notificationView != null) {
            View view = this.mNotificationContainer.findViewById(notificationView.getId());
            if(view != null) {
                this.mNotificationContainer.removeView(view);
                if(this.mNotificationContainer.getChildCount() == 0) {
                    this.mNotificationContainer.setVisibility(View.GONE);
                }
            }

        }
    }

    public void showNotificationView(View notificationView) {
        if(notificationView != null) {
            View view = this.mNotificationContainer.findViewById(notificationView.getId());
            if(view == null) {
                this.mNotificationContainer.addView(notificationView);
                this.mNotificationContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public View inflateNotificationView(@LayoutRes int layout) {
        return LayoutInflater.from(this.getActivity()).inflate(layout, this.mNotificationContainer, false);
    }

    private void sendReadReceiptResponseIfNeeded(List<Message> messages) {
        if(this.mReadRec && (this.mConversationType.equals(Conversation.ConversationType.GROUP) || this.mConversationType.equals(Conversation.ConversationType.DISCUSSION)) && RongContext.getInstance().isReadReceiptConversationType(this.mConversationType)) {
            ArrayList responseMessageList = new ArrayList();
            Iterator var3 = messages.iterator();

            while(var3.hasNext()) {
                Message message = (Message)var3.next();
                ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
                if(readReceiptInfo != null && readReceiptInfo.isReadReceiptMessage() && !readReceiptInfo.hasRespond()) {
                    responseMessageList.add(message);
                }
            }

            if(responseMessageList.size() > 0) {
                RongIMClient.getInstance().sendReadReceiptResponse(this.mConversationType, this.mTargetId, responseMessageList, (RongIMClient.OperationCallback)null);
            }
        }

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // 退出时释放连接
        mIat.cancel();
        mIat.destroy();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
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

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("ConversationActivity", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e("MyFirstConversationFragment", "初始化失败，错误码：" + code);
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
            Log.e("MyFirstConversationFragment", "开始说话");

        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            Log.e("MyFirstConversationFragment",error.getPlainDescription(true));
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.e("MyFirstConversationFragment", "结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            recognizerResults = printResult(results);

            if (isLast) {
                // TODO 最后的结果
                mySendTextMessage(recognizerResults);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "返回音频数据："+data.length);
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
        Message myMessage = Message.obtain(this.getTargetId(), this.getConversationType(), textMessage);

        RongIM.getInstance().sendMessage(myMessage, null, null, new IRongCallback.ISendMessageCallback() {

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

    private static enum LoadMessageDirection {
        DOWN,
        UP;

        private LoadMessageDirection() {
        }
    }

}
