package com.jikexueyuan.cicada_chat;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import java.io.FileInputStream;
import java.io.IOException;

import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.IAudioPlayListener;

/**
 * Created by Administrator on 2017/9/2.
 */

public class MyAudioPlayManager implements SensorEventListener {
    private static final String TAG = "MyAudioPlayManager";
    private MediaPlayer _mediaPlayer;
    private IAudioPlayListener _playListener;
    private Uri _playingUri;
    private Sensor _sensor;
    private SensorManager _sensorManager;
    private AudioManager _audioManager;
    private PowerManager _powerManager;
    private PowerManager.WakeLock _wakeLock;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private Context context;


    public MyAudioPlayManager(){

    }

    public static MyAudioPlayManager getInstance() {
        return MyAudioPlayManager.SingletonHolder.sInstance;
    }

    @TargetApi(11)
    public void onSensorChanged(SensorEvent event) {
        float range = event.values[0];
        if(this._sensor != null && this._mediaPlayer != null) {
            if(this._mediaPlayer.isPlaying()) {
                if((double)range > 0.0D) {
                    if(this._audioManager.getMode() == 0) {
                        return;
                    }

                    this._audioManager.setMode(0);
                    this._audioManager.setSpeakerphoneOn(true);
                    final int positions = this._mediaPlayer.getCurrentPosition();

                    try {
                        this._mediaPlayer.reset();
                        this._mediaPlayer.setAudioStreamType(3);
                        this._mediaPlayer.setVolume(1.0F, 1.0F);
                        FileInputStream e = new FileInputStream(this._playingUri.getPath());
                        this._mediaPlayer.setDataSource(e.getFD());
                        this._mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            public void onPrepared(MediaPlayer mp) {
                                mp.seekTo(positions);
                            }
                        });
                        this._mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                            public void onSeekComplete(MediaPlayer mp) {
                                mp.start();
                            }
                        });
                        this._mediaPlayer.prepareAsync();
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }

                    this.setScreenOn();
                } else {
                    this.setScreenOff();
                    if(Build.VERSION.SDK_INT >= 11) {
                        if(this._audioManager.getMode() == 3) {
                            return;
                        }

                        this._audioManager.setMode(3);
                    } else {
                        if(this._audioManager.getMode() == 2) {
                            return;
                        }

                        this._audioManager.setMode(2);
                    }

                    this._audioManager.setSpeakerphoneOn(false);
                    this.replay();
                }
            } else if((double)range > 0.0D) {
                if(this._audioManager.getMode() == 0) {
                    return;
                }

                this._audioManager.setMode(0);
                this._audioManager.setSpeakerphoneOn(true);
                this.setScreenOn();
            }

        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public Uri getPlayingUri() {
        return this._playingUri;
    }

    static class SingletonHolder {
        static MyAudioPlayManager sInstance = new MyAudioPlayManager();

        SingletonHolder() {
        }
    }
}
