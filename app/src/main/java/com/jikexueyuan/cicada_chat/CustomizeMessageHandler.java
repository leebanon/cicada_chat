package com.jikexueyuan.cicada_chat;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imlib.NativeClient;
import io.rong.imlib.model.Message;
import io.rong.message.MessageHandler;


/**
 * Created by Administrator on 2017/9/10.
 */

public class CustomizeMessageHandler extends MessageHandler<CustomizeMessage> {
    private static final String TAG = "CustomizeMessageHandler";
    private static final String VOICE_PATH = "/customize/";

    public CustomizeMessageHandler(Context context){
        super(context);
    }

    public void decodeMessage(Message message, CustomizeMessage model) {
        Uri uri = obtainVoiceUri(this.getContext());
        String name = message.getMessageId() + ".wav";
        if(message.getMessageId() == 0) {
            name = message.getSentTime() + ".wav";
        }

        File file = new File(uri.toString() + "/customize/" + name);
        if(!TextUtils.isEmpty(model.getBase64()) && !file.exists()) {
            try {
                byte[] e = Base64.decode(model.getBase64(), 2);
                file = saveFile(e, uri.toString() + "/customize/", name);
            } catch (IllegalArgumentException var7) {
                RLog.e("CustomizeMessageHandler", "afterDecodeMessage Not Base64 Content!");
                var7.printStackTrace();
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }

        model.setUri(Uri.fromFile(file));
        model.setBase64((String)null);
    }

    public void encodeMessage(Message message) {
        CustomizeMessage model = (CustomizeMessage)message.getContent();
        Uri uri = obtainVoiceUri(this.getContext());
        byte[] voiceData = FileUtils.getByteFromUri(model.getUri());
        File file = null;

        try {
            String e = Base64.encodeToString(voiceData, 2);
            model.setBase64(e);
            String name = message.getMessageId() + ".wav";
            file = saveFile(voiceData, uri.toString() + "/customize/", name);
        } catch (IllegalArgumentException var8) {
            RLog.e("CustomizeMessageHandler", "beforeEncodeMessage Not Base64 Content!");
            var8.printStackTrace();
        } catch (IOException var9) {
            var9.printStackTrace();
        }

        if(file != null && file.exists()) {
            model.setUri(Uri.fromFile(file));
        }

    }

    private static File saveFile(byte[] data, String path, String fileName) throws IOException {
        File file = new File(path);
        if(!file.exists()) {
            file.mkdirs();
        }

        file = new File(path + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write(data);
        bos.flush();
        bos.close();
        return file;
    }

    private static Uri obtainVoiceUri(Context context) {
        File file = context.getFilesDir();
        String path = file.getAbsolutePath();
        String userId = NativeClient.getInstance().getCurrentUserId();
        return Uri.parse(path + File.separator + userId);
    }
}
