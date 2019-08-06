package com.android.floatball.recorderball;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


public class RecordService extends Service {

    private static final String TAG = "RService";
    private MediaProjection mMpj;
    private VirtualDisplay mVirtualDisplay;
    private int windowWidth;
    private int windowHeight;
    private int screenDensity;

    private Surface mSurface;
    private MediaCodec mMediaCodec;
    private MediaMuxer mMuxer;

    private AtomicBoolean mIsQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private boolean mMuxerStarted = false;
    private int mVideoTrackIndex = -1;
    private newBinder mBinder = new newBinder();
    class newBinder extends Binder {
        RecordService getRSinstance() {
            return RecordService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createEnvironment();
        configureMedia();
    }

    private void configureMedia() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", windowWidth, windowHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
    }

    private void createEnvironment() {
        GlobalVariables.mVideoPath = Environment.getExternalStorageDirectory().getPath() + "/" + "日志+录屏/";
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowWidth = wm.getDefaultDisplay().getWidth();
        windowHeight = wm.getDefaultDisplay().getHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;
    }

    protected void recordStop() {
        mIsQuit.set(true);
    }

    protected void recordStart() {
        configureMedia();
        startVirtual();
        new Thread() {
            @Override
            public void run() {
                Log.e(TAG, "start startRecord");
                startRecord();
            }
        }.start();
    }

    protected void startRecord() {
        try {
            mMuxer = new MediaMuxer(GlobalVariables.mVideoPath + GlobalVariables.name
                    + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time + "/"
                    + "video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            recordVirtualDisplay();
        } catch (IOException e) {
            Log.e(TAG, "record failed due to an exception");
            e.printStackTrace();
        } finally {
            release();
        }
    }

    private void startVirtual() {
        if (mMpj != null) {
            virtualDisplay();
        } else {
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    private void setUpMediaProjection() {
        int resultCode = GlobalVariables.getResultCode();
        Intent data = GlobalVariables.getResultIntent();
        mMpj = GlobalVariables.getMpmngr().getMediaProjection(resultCode, data);
    }

    private void virtualDisplay() {
        mVirtualDisplay = mMpj.createVirtualDisplay("record_screen", windowWidth, windowHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, null);
    }

    private void recordVirtualDisplay() {
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), "开始录制", Toast.LENGTH_SHORT).show();
            }
        });
        GlobalVariables.isRSon = true;
        while (!mIsQuit.get()) {
            int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 10000);
            Log.d(TAG, "dequeue output buffer index=" + index);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {//后续输出格式变化
                resetOutputFormat();
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {//请求超时
                Log.d(TAG, "retrieving buffers time out!");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            } else if (index >= 0) {//有效输出
                if (!mMuxerStarted) {
                    throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                }
                encodeToVideoTrack(index);
                mMediaCodec.releaseOutputBuffer(index, false);
            }
        }
    }

    private void encodeToVideoTrack(int index) {
        ByteBuffer encodedData = mMediaCodec.getOutputBuffer(index);
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {//是编码需要的特定数据，不是媒体数据
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodedData != null) {
            encodedData.position(mBufferInfo.offset);
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
            mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);//写入
            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer...");
        }
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once
        if (mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        MediaFormat newFormat = mMediaCodec.getOutputFormat();

        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
        mVideoTrackIndex = mMuxer.addTrack(newFormat);
        mMuxer.start();
        mMuxerStarted = true;
        Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    private void release() {
        mIsQuit.set(false);
        mMuxerStarted = false;
        Log.i(TAG, " release() ");
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        GlobalVariables.isRSon = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
        if (mMpj != null) {
            mMpj.stop();
        }
    }
}
