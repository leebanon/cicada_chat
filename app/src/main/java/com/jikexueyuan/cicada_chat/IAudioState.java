package com.jikexueyuan.cicada_chat;

import io.rong.imkit.manager.AudioStateMessage;

/**
 * Created by Administrator on 2017/9/3.
 */

public abstract class IAudioState {
    public IAudioState() {
    }

    void enter() {
    }

    abstract void handleMessage(AudioStateMessage var1);
}

