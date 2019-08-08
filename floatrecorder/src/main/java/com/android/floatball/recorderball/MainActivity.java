package com.android.floatball.recorderball;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.floatball.libarary.FloatBallManager;
import com.android.floatball.libarary.floatball.FloatBallCfg;
import com.android.floatball.libarary.menu.FloatMenuCfg;
import com.android.floatball.libarary.menu.MenuItem;
import com.android.floatball.libarary.utils.BackGroudSeletor;
import com.android.floatball.libarary.utils.DensityUtil;
import com.android.floatball.recorderball.RecordService.newBinder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.Thread.sleep;

public class MainActivity extends Activity {
    private MediaProjectionManager mMpMngr;
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private Intent mResultIntent = null;
    private int mResultCode = 0;
    public FloatBallManager mFloatballManager;
    RecordService serviceThing;
    Process recorddd, recordall;
    String TAG = "mainactivitytag";

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
            newBinder mBinder = (newBinder) iBinder;
            serviceThing = mBinder.getRSinstance();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
        }
    };

    public void showFloatBall(View v) {
//        mFloatballManager.show();
        setFullScreen(v);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        boolean showMenu = true;
        GlobalVariables.isRSon = false;
        initSinglePageFloatball(showMenu);
        //5 如果没有添加菜单，可以设置悬浮球点击事件
        if (mFloatballManager.getMenuItemSize() == 0) {
            mFloatballManager.setOnFloatBallClickListener(new FloatBallManager.OnFloatBallClickListener() {
                @Override
                public void onFloatBallClick() {
                    toast("点击了悬浮球");
                }
            });
        }
        mFloatballManager.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent bindIntent = new Intent(MainActivity.this, RecordService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
        mMpMngr = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mResultIntent = GlobalVariables.getResultIntent();
        mResultCode = GlobalVariables.getResultCode();
        startIntent();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        //只有activity被添加到windowmanager上以后才可以调用show方法。
        //mFloatballManager.show();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFloatballManager.hide();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
    }

    private void exitFullScreen() {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setAttributes(attrs);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        isfull = false;
    }

    private boolean isfull = false;

    public void setFullScreen(View view) {
        if (isfull) {
            exitFullScreen();
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isfull = true;
        }
    }

    private void initSinglePageFloatball(boolean showMenu) {
        //1 初始化悬浮球配置，定义好悬浮球大小和icon的drawable
        int ballSize = DensityUtil.dip2px(this, 40);
        Drawable ballIcon = BackGroudSeletor.getdrawble("menu_x", this);
        FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.RIGHT_CENTER);
        //设置悬浮球不半隐藏
//        ballCfg.setHideHalfLater(false);
        if (showMenu) {
            //2 需要显示悬浮菜单
            //2.1 初始化悬浮菜单配置，有菜单item的大小和菜单item的个数
            int menuSize = DensityUtil.dip2px(this, 180);
            int menuItemSize = DensityUtil.dip2px(this, 30);
            FloatMenuCfg menuCfg = new FloatMenuCfg(menuSize, menuItemSize);
            //3 生成floatballManager
            //必须传入Activity
            mFloatballManager = new FloatBallManager(this, ballCfg, menuCfg);
            addFloatMenuItem();
        } else {
            //必须传入Activity
            mFloatballManager = new FloatBallManager(this, ballCfg);
        }
    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void addFloatMenuItem() {
        MenuItem playItem = new MenuItem(BackGroudSeletor.getdrawble("play", this)) {
            @SuppressLint("DefaultLocale")
            @Override
            public void action() {
                if (!GlobalVariables.isRSon) {
                    try {
                        int pid = android.os.Process.myPid();
                        Time t = new Time();
                        t.setToNow();
                        GlobalVariables.name = t.year + "年" + (t.month + 1) + "月" + t.monthDay + "日/";
                        GlobalVariables.time = "_" + String.format("%02d", t.hour) + String.format("%02d", t.minute) + String.format("%02d", t.second);
                        Log.e(TAG, "time = " + GlobalVariables.time );
                        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/日志+录屏/" + GlobalVariables.name);
                        if (!file.exists()) {
                            try {
                                file.mkdirs();
                                SharedPreferences.Editor editor = getSharedPreferences("dataForRecorder", MODE_PRIVATE).edit();
                                editor.putInt("num", 1);
                                editor.apply();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        SharedPreferences mpref = getSharedPreferences("dataForRecorder", MODE_PRIVATE);
                        GlobalVariables.number = mpref.getInt("num", 0);
                        file = new File(Environment.getExternalStorageDirectory().getPath() + "/日志+录屏/" +
                                GlobalVariables.name + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time);
                        if (!file.exists()) {
                            try {
                                file.mkdirs();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Runtime.getRuntime().exec("logcat -c");
                        String ddpath = Environment.getExternalStorageDirectory().getPath() + "/日志+录屏/"
                                + GlobalVariables.name + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time + "/" + "打点日志.txt ";
                        String allpath = Environment.getExternalStorageDirectory().getPath() + "/日志+录屏/"
                                + GlobalVariables.name + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time + "/" + "全部日志.txt ";
                        String ddcmd = "logcat -f" + ddpath + "*:s RService:D ";
                        String allcmd = "logcat -f" + allpath + "--pid=" + pid + " *:I";
                        recorddd = Runtime.getRuntime().exec(ddcmd);
                        recordall = Runtime.getRuntime().exec(allcmd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    serviceThing.recordStart();
                    //GlobalVariables.isRSon = true;
                    mFloatballManager.closeMenu();
                } else {
                    toast("请先停止上一个录制");
                    mFloatballManager.closeMenu();
                }
                //Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();

            }
        };
        MenuItem stopItem = new MenuItem(BackGroudSeletor.getdrawble("stop", this)) {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void action() {
                if (GlobalVariables.isRSon) {
                    recordall.destroy();
                    recorddd.destroy();
                    serviceThing.recordStop();
                    toast("录制结束");
//                    Uri uri = Uri.parse(("file://" + GlobalVariables.mVideoPath + GlobalVariables.name + "/" + "video.mp4"));
//                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
//                    sendBroadcast(intent);
                    while (GlobalVariables.isRSon) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String sourcePath = GlobalVariables.mVideoPath + GlobalVariables.name + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time + "/";
                    String location = GlobalVariables.mVideoPath + GlobalVariables.name + "第" + GlobalVariables.number + "次录制" + GlobalVariables.time + ".zip";
                    zipFileAtPath(sourcePath, location);
                    SharedPreferences.Editor editor = getSharedPreferences("dataForRecorder", MODE_PRIVATE).edit();
                    SharedPreferences mpref = getSharedPreferences("dataForRecorder", MODE_PRIVATE);
                    editor.putInt("num", mpref.getInt("num", 0) + 1);
                    editor.apply();
                    mFloatballManager.closeMenu();
                } else {
                    mFloatballManager.closeMenu();
                    toast("请先开始录制");
                }
            }
        };
//        MenuItem scItem = new MenuItem(BackGroudSeletor.getdrawble("screenshot", this)) {
//            @Override
//            public void action() {
//                toast("开始截图");
//                mFloatballManager.closeMenu();
//            }
//        };
        MenuItem adItem = new MenuItem(BackGroudSeletor.getdrawble("android", this)) {
            @Override
            public void action() {
                startActivity(new Intent(MainActivity.this, SettingDemo.class));
                toast("卖个萌");
                mFloatballManager.closeMenu();
            }
        };
        mFloatballManager.addMenuItem(playItem)
                .addMenuItem(stopItem)
                //.addMenuItem(scItem)
                .addMenuItem(adItem)
                .buildMenu();
    }

    private void startIntent() {
        if (mResultIntent != null && mResultCode != 0) {
            startService(new Intent(getApplicationContext(), RecordService.class));
        } else {
            startActivityForResult(mMpMngr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "get capture permission success!");
                mResultCode = resultCode;
                mResultIntent = data;
                GlobalVariables.setResultCode(resultCode);
                GlobalVariables.setResultIntent(data);
                GlobalVariables.setMpmngr(mMpMngr);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    /*
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */

    public void zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;
        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length() + 1);
            } else {
                byte[] data = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                String[] segments = sourcePath.split("/");
                ZipEntry entry = new ZipEntry(segments[segments.length - 1]);
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "zipFileAtPath: failed due to an exception");
            e.printStackTrace();
        }
    }

    private void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {
        final int BUFFER = 2048;
        File[] fileList = folder.listFiles();
        BufferedInputStream origin;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte[] data = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath.substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }
}