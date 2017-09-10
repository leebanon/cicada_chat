package com.jikexueyuan.cicada_chat;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import io.rong.common.ParcelUtils;
import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.VoiceMessage;


/**
 * Created by Administrator on 2017/7/27.
 */

@MessageTag(value = "app:custom", flag = MessageTag.ISCOUNTED | MessageTag.ISPERSISTED,messageHandler = CustomizeMessageHandler.class)
public class CustomizeMessage extends MessageContent{
//    private String content;
    private Uri mUri;
    private int mDuration;
    private String mBase64;
    protected String extra;

    private CustomizeMessage(Uri uri, int duration) {
        this.mUri = uri;
        this.mDuration = duration;
    }

    public static CustomizeMessage obtain(Uri uri, int duration) {
        return new CustomizeMessage(uri, duration);
    }

    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("content",this.mBase64);
            jsonObj.put("duration",this.mDuration);
            if(!TextUtils.isEmpty(this.getExtra())){
                jsonObj.put("extra",this.extra);
            }
            if(this.getJSONUserInfo() != null) {
                jsonObj.putOpt("user", this.getJSONUserInfo());
            }
        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        }

        this.mBase64 = null;

        try {
            return jsonObj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }


    public CustomizeMessage(byte[] data) {
        String jsonStr = null;

        try {
            jsonStr = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e1) {

        }

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);

            if (jsonObj.has("duration")) {
                this.setDuration(jsonObj.optInt("duration"));
            }
            if (jsonObj.has("content")) {
                this.setBase64(jsonObj.optString("content"));
            }
            if (jsonObj.has("extra")){
                this.setExtra(jsonObj.optString("extra"));
            }
            if(jsonObj.has("user")){
                this.setUserInfo(this.parseJsonToUserInfo(jsonObj.getJSONObject("user")));
            }

        } catch (JSONException e) {
            RLog.e("JSONException",  e.getMessage());
        }

    }

    //给消息赋值。
    public CustomizeMessage(Parcel in) {
        //这里可继续增加你消息的属性
        this.setExtra(ParcelUtils.readFromParcel(in));
        this.mUri = (Uri)ParcelUtils.readFromParcel(in, Uri.class);
        this.mDuration = ParcelUtils.readIntFromParcel(in).intValue();
        this.setUserInfo((UserInfo)ParcelUtils.readFromParcel(in, UserInfo.class));
    }

    /**
     * 读取接口，目的是要从Parcel中构造一个实现了Parcelable的类的实例处理。
     */
    public static final Creator<CustomizeMessage> CREATOR = new Creator<CustomizeMessage>() {

        @Override
        public CustomizeMessage createFromParcel(Parcel source) {
            return new CustomizeMessage(source);
        }

        @Override
        public CustomizeMessage[] newArray(int size) {
            return new CustomizeMessage[size];
        }
    };

    /**
     * 描述了包含在 Parcelable 对象排列信息中的特殊对象的类型。
     *
     * @return 一个标志位，表明Parcelable对象特殊对象类型集合的排列。
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 将类的数据写入外部提供的 Parcel 中。
     *
     * @param dest  对象被写入的 Parcel。
     * @param flags 对象如何被写入的附加标志。
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //该类为工具类，对消息中属性进行序列化
        //这里可继续增加你消息的属性
        ParcelUtils.writeToParcel(dest, this.extra);
        ParcelUtils.writeToParcel(dest, this.mUri);
        ParcelUtils.writeToParcel(dest, Integer.valueOf(this.mDuration));
        ParcelUtils.writeToParcel(dest, this.getUserInfo());
    }



    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public String getBase64() {
        return mBase64;
    }

    public void setBase64(String base64) {
        mBase64 = base64;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

}
