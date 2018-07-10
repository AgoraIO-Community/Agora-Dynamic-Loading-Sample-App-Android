package agora.io.dynamicload;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.agora.rtc.RtcEngine;

public class MainActivity extends Activity implements View.OnClickListener,
        LoadUtils.OnSoLoadBeReady, DownloadUtils.OnDownloadListener {
    private final static int MSG_DOWNLOAD = 0x1001;
    private final static int MSG_CREATE_LOADER_PATH = 0x1002;
    private final static int MSG_TEST = 0x1003;

    private final static String mDownloadUrl = YOUR PATH;//"http://172.16.0.225:8000/AndroidStudioProjects/libs.zip";
    private final static String mSaveUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/123";
    private final static String mDepressUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/123/libs";
    private final static String mSOName = "libagora-rtc-sdk-jni.so";

    private WorkDispatcher mWorkDispatcher;

    private TextView mTvDisplay;
    private Status mStatus = Status.NON;
    private Status mInjectStatus = Status.NON;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (shouldAskPermission())
            askPermission();

        initWidgets();

        mTvDisplay.append("The demo will show how to dynamic-load so from a url or exist sdcard path.\n");
        mTvDisplay.append("Click DOWNLOAD to download zip-file and depress it.\n");
        mTvDisplay.append("Click COPY-AND-INJECT-PATH to copy .so to data/data and add data/data path to findLibrary-path.\n");
        mTvDisplay.append("If you sure .so path had been injected, Click TEST-AGORA-INIT to check the .so weather load success.\n");
        mTvDisplay.append("If you're not sure about it, please download zip-file and injected-path for it.\n");
    }

    private void initWidgets() {
        Button mBtnDownload = findViewById(R.id.download);
        mBtnDownload.setOnClickListener(this);

        Button mBtnCreatePath = findViewById(R.id.create);
        mBtnCreatePath.setOnClickListener(this);

        Button mBtnTest = findViewById(R.id.test);
        mBtnTest.setOnClickListener(this);

        mTvDisplay = findViewById(R.id.tv_display);

        mWorkDispatcher = new WorkDispatcher(this, mTvDisplay);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download:
                if (mStatus == Status.NON) {
                    mStatus = Status.DOWNLOADING;
                    mTvDisplay.append("begin download...\n");
                    sendMsg(MSG_DOWNLOAD, new String[]{mDownloadUrl, mSaveUrl});
                } else {
                    mTvDisplay.append("please wait for other task finish...\n");
                }
                break;
            case R.id.create:
                if (mStatus == Status.NON && mInjectStatus != Status.INJECT) {
                    mTvDisplay.append("begin load...\n");
                    mStatus = Status.INJECT;
                    sendMsg(MSG_CREATE_LOADER_PATH, new String[]{mDepressUrl, mSOName});
                } else {
                    mTvDisplay.append("you had inject once or please wait for other task finish...\n");
                }
                break;
            case R.id.test:
                if (mStatus == Status.NON && mInjectStatus == Status.INJECT) {
                    mTvDisplay.append("testing agora so loaded...\n");
                    sendMsg(MSG_TEST, "");
                } else {
                    mTvDisplay.append("please wait for other task finish or inject first...\n");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            //do something you like
        }
    }

    private boolean shouldAskPermission() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    @TargetApi(23)
    private void askPermission() {
        requestPermissions(new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
        }, 200);
    }

    static class WorkDispatcher extends Handler {
        private DownloadUtils mDownloadUtils;
        private WeakReference<Activity> mWActivity;
        private WeakReference<TextView> mDisplayView;

        WorkDispatcher(Activity activity, TextView tv) {
            if (activity instanceof DownloadUtils.OnDownloadListener)
                mDownloadUtils = new DownloadUtils((DownloadUtils.OnDownloadListener) activity);

            mWActivity = new WeakReference<>(activity);
            mDisplayView = new WeakReference<>(tv);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DOWNLOAD:
                    String[] strs = (String[]) msg.obj;
                    if (mDownloadUtils != null)
                        mDownloadUtils.execute(strs[0], strs[1]);
                    break;
                case MSG_CREATE_LOADER_PATH:
                    String[] str = (String[]) msg.obj;
                    LoadUtils mLoadUtils = new LoadUtils(new WeakReference<Context>(mWActivity.get()), (LoadUtils.OnSoLoadBeReady) (mWActivity.get()));
                    mLoadUtils.execute(str[0], str[1]);
                    break;
                case MSG_TEST:
                    doTest(mDisplayView);
                    break;
                default:
                    break;
            }
        }
    }

    private void sendMsg(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mWorkDispatcher.sendMessage(msg);
    }

    @Override
    public void onLoadReady(final Boolean loadSuccess) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!loadSuccess)
                    mTvDisplay.append("load so error\n");
                else {
                    mTvDisplay.append("load so success\n");
                    mTvDisplay.append("<----Now you can click TEST-AGORA-INIT---->\n");
                    mTvDisplay.append("<----If app not crash, Congratulations!!!---->\n");
                }
                mStatus = Status.NON;
                mInjectStatus = Status.INJECT;
            }
        });
    }

    @Override
    public void onDownloadSuccess(String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus = Status.NON;
                mTvDisplay.append("download and depress success.\n");
                mTvDisplay.append("download and depress end...\n");
                mTvDisplay.append("<----Now you can click COPY-AND-INJECT-PATH---->\n");
            }
        });
    }

    @Override
    public void onDownloadProgress(final int percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvDisplay.append("downloading--->" + percentage + "\n");
            }
        });
    }

    @Override
    public void onDownloadFailed(final String errorMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus = Status.NON;
                mTvDisplay.append("download failed with error...\n");
                mTvDisplay.append(errorMsg + "\n");
            }
        });
    }

    static void doTest(WeakReference<TextView> tv) {
        RtcEngine.getSdkVersion();

        if (tv.get() != null)
            tv.get().append("test success, Congratulations!!!\n");
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
    }

    enum Status {
        NON,
        DOWNLOADING,
        INJECT
    }
}
