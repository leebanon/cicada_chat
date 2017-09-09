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

import io.rong.common.RLog;
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

    @TargetApi(21)
    private void setScreenOff() {
        if(this._wakeLock == null) {
            this._wakeLock = this._powerManager.newWakeLock(32, "MyAudioPlayManager");
        }

        if(this._wakeLock != null) {
            this._wakeLock.acquire();
        }

    }

    private void setScreenOn() {
        if(this._wakeLock != null) {
            this._wakeLock.setReferenceCounted(false);
            this._wakeLock.release();
            this._wakeLock = null;
        }

    }
    
    private void replay() {
        try {
            this._mediaPlayer.reset();
            this._mediaPlayer.setAudioStreamType(3);
            this._mediaPlayer.setVolume(1.0F, 1.0F);
            FileInputStream e = new FileInputStream(this._playingUri.getPath());
            this._mediaPlayer.setDataSource(e.getFD());
            this._mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException var3) {
                        var3.printStackTrace();
                    }

                    mp.start();
                }
            });
            this._mediaPlayer.prepareAsync();
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }

    public void startPlay(Context context, Uri audioUri, IAudioPlayListener playListener) {
        if(context != null && audioUri != null) {
            this.context = context;
            if(this._playListener != null && this._playingUri != null) {
                this._playListener.onStop(this._playingUri);
            }

            this.resetMediaPlayer();
            this.afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    RLog.d("MyAudioPlayManager", "OnAudioFocusChangeListener " + focusChange);
                    if(MyAudioPlayManager.this._audioManager != null && focusChange == -1) {
                        MyAudioPlayManager.this._audioManager.abandonAudioFocus(MyAudioPlayManager.this.afChangeListener);
                        MyAudioPlayManager.this.afChangeListener = null;
                        MyAudioPlayManager.this.resetMediaPlayer();
                    }

                }
            };

            try {
                this._powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                this._audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                if(!this._audioManager.isWiredHeadsetOn()) {
                    this._sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
                    this._sensor = this._sensorManager.getDefaultSensor(8);
                    this._sensorManager.registerListener(this, this._sensor, 3);
                }

                this.muteAudioFocus(this._audioManager, true);
                this._playListener = playListener;
                this._playingUri = audioUri;
                this._mediaPlayer = new MediaPlayer();
                this._mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        if(MyAudioPlayManager.this._playListener != null) {
                            MyAudioPlayManager.this._playListener.onComplete(MyAudioPlayManager.this._playingUri);
                            MyAudioPlayManager.this._playListener = null;
                            MyAudioPlayManager.this.context = null;
                        }

                        MyAudioPlayManager.this.reset();
                    }
                });
                this._mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        MyAudioPlayManager.this.reset();
                        return true;
                    }
                });
                FileInputStream e = new FileInputStream(audioUri.getPath());
                this._mediaPlayer.setDataSource(e.getFD());
                this._mediaPlayer.setAudioStreamType(3);
                this._mediaPlayer.prepare();
                this._mediaPlayer.start();
                if(this._playListener != null) {
                    this._playListener.onStart(this._playingUri);
                }
            } catch (Exception var5) {
                var5.printStackTrace();
                if(this._playListener != null) {
                    this._playListener.onStop(audioUri);
                    this._playListener = null;
                }

                this.reset();
            }

        } else {
            RLog.e("MyAudioPlayManager", "startPlay context or audioUri is null.");
        }
    }

    public void setPlayListener(IAudioPlayListener listener) {
        this._playListener = listener;
    }

    public void stopPlay() {
        if(this._playListener != null && this._playingUri != null) {
            this._playListener.onStop(this._playingUri);
        }

        this.reset();
    }

    private void reset() {
        this.resetMediaPlayer();
        this.resetMyAudioPlayManager();
    }

    private void resetMyAudioPlayManager() {
        if(this._audioManager != null) {
            this.muteAudioFocus(this._audioManager, false);
        }

        if(this._sensorManager != null) {
            this._sensorManager.unregisterListener(this);
        }

        this._sensorManager = null;
        this._sensor = null;
        this._powerManager = null;
        this._audioManager = null;
        this._wakeLock = null;
        this._playListener = null;
        this._playingUri = null;
    }

    private void resetMediaPlayer() {
        if(this._mediaPlayer != null) {
            try {
                this._mediaPlayer.stop();
                this._mediaPlayer.reset();
                this._mediaPlayer.release();
                this._mediaPlayer = null;
            } catch (IllegalStateException var2) {
                var2.printStackTrace();
            }
        }

    }
    
    @TargetApi(8)
    private void muteAudioFocus(AudioManager audioManager, boolean bMute) {
        if(Build.VERSION.SDK_INT < 8) {
            RLog.d("MyAudioPlayManager", "muteAudioFocus Android 2.1 and below can not stop music");
        } else {
            if(bMute) {
                audioManager.requestAudioFocus(this.afChangeListener, 3, 2);
            } else {
                audioManager.abandonAudioFocus(this.afChangeListener);
                this.afChangeListener = null;
            }

        }
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
