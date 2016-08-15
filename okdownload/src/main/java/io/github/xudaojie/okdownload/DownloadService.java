package io.github.xudaojie.okdownload;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.xudaojie.okdownload.util.FileUtils;
import io.github.xudaojie.okdownload.util.NotificationUtils;
import io.github.xudaojie.okdownload.util.SQLiteHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_DOWNLOAD;
import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_DOWNLOAD_FAIL;
import static io.github.xudaojie.okdownload.OkDownloadManager.DOWNLOAD_TYPE;
import static io.github.xudaojie.okdownload.OkDownloadManager.DOWNLOAD_TYPE_CONTINUE;
import static io.github.xudaojie.okdownload.OkDownloadManager.DOWNLOAD_TYPE_DEFAULT;
import static io.github.xudaojie.okdownload.OkDownloadManager.TEMP_SUFFIX;

/**
 * Created by xdj on 16/8/14.
 */

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    private static OkHttpClient mHttpClient;

    private Context mContext;
    private LocalBroadcastManager mBroadcastManager;

    private long mTimeline; // 上次发送广播的时间
    private String mUrl; // 正在进行下载的链接
    private int mPercent; // 正在进行下载的进度

    private SQLiteHelper mSQLiteHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d("Service", "onCreate");
        mSQLiteHelper = SQLiteHelper.getInstance(mContext);

        if (mHttpClient == null) {
            mHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(final Interceptor.Chain chain) throws IOException {
                            final okhttp3.Request request = chain.request();
                            final String url = chain.request().url().toString();
                            final Map<String, Object> tagMap = (Map<String, Object>) request.tag();

                            Response response = chain.proceed(request);
                            return response.newBuilder()
                                    .body(new ProgressResponseBody(response.body(), new ProgressResponseBody.ProgressListener() {
                                        @Override
                                        public void update(long bytesRead, long contentLength, boolean done) {
                                            int percent = (int) ((float) bytesRead / contentLength * 100);
                                            // 广播接收也是在主界面执行的,全部发送的话会造成系统卡顿
                                            long currentTimeline = System.currentTimeMillis();
                                            if ((currentTimeline - mTimeline > 600 && mPercent != percent)
                                                    || done) {
                                                int id = 0;
                                                String title = "";
                                                String filePath = "";

                                                mTimeline = currentTimeline;
                                                mPercent = percent;
                                                Log.d(TAG, percent + "%");
                                                if (done) {
                                                    mUrl = null;
                                                    mPercent = -1;
                                                    Log.d(TAG, "download done; url: " + mUrl);
                                                }

                                                if (tagMap != null && tagMap.get(OkDownloadManager.COLUMN_ID) != null) {
                                                    id = (int) tagMap.get(OkDownloadManager.COLUMN_ID);
                                                    title = (String) tagMap.get(OkDownloadManager.COLUMN_TITLE);
                                                    filePath = (String) tagMap.get(OkDownloadManager.COLUMN_LOCAL_URI);
                                                }

                                                Intent i = new Intent();
                                                i.setAction(OkDownloadManager.ACTION_DOWNLOAD);
                                                i.putExtra(OkDownloadManager.DOWNLOAD_PERCENT, percent);
                                                i.putExtra(OkDownloadManager.COLUMN_URI, url);
                                                i.putExtra(OkDownloadManager.COLUMN_ID, id);
                                                i.putExtra(OkDownloadManager.COLUMN_TITLE, title);
                                                i.putExtra(OkDownloadManager.COLUMN_LOCAL_URI, filePath);
                                                i.putExtra(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, contentLength);

                                                // 必须也使用LocalBroadcastReceiver进行注册才能接收
                                                mBroadcastManager.sendBroadcast(i);
                                            }

                                        }

                                        @Override
                                        public void onFailure(IOException e) {
                                            int id = 0;
                                            String title = "";
                                            String filePath = "";

                                            Log.d(TAG, "onFailure");

                                            if (tagMap != null && tagMap.get(OkDownloadManager.COLUMN_ID) != null) {
                                                id = (int) tagMap.get(OkDownloadManager.COLUMN_ID);
                                                title = (String) tagMap.get(OkDownloadManager.COLUMN_TITLE);
                                                filePath = (String) tagMap.get(OkDownloadManager.COLUMN_LOCAL_URI);
                                            }

                                            Intent i = new Intent();
                                            i.setAction(ACTION_DOWNLOAD_FAIL);
                                            i.putExtra(OkDownloadManager.COLUMN_URI, url);
                                            i.putExtra(OkDownloadManager.COLUMN_ID, id);
                                            i.putExtra(OkDownloadManager.COLUMN_TITLE, title);
                                            i.putExtra(OkDownloadManager.COLUMN_LOCAL_URI, filePath);

                                            // 必须也使用LocalBroadcastReceiver进行注册才能接收
                                            mBroadcastManager.sendBroadcast(i);
                                        }
                                    }))
                                    .build();
                        }
                    })
                    .build();
            mBroadcastManager = LocalBroadcastManager.getInstance(mContext);

            OkDownloadReceiver okHttpReceiver = new OkDownloadReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_DOWNLOAD);
            filter.addAction(ACTION_DOWNLOAD_FAIL);
            mBroadcastManager.registerReceiver(okHttpReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand");

        if (intent != null && intent.hasExtra(OkDownloadManager.COLUMN_ID)
                && intent.hasExtra(OkDownloadManager.COLUMN_URI)) {
            int downloadType = intent.getIntExtra(DOWNLOAD_TYPE, DOWNLOAD_TYPE_DEFAULT);

            if (downloadType != DOWNLOAD_TYPE_CONTINUE) {
                String fileName = intent.getStringExtra(OkDownloadManager.FILENAME);
                int id = intent.getIntExtra(OkDownloadManager.COLUMN_ID, 0);
                String url = intent.getStringExtra(OkDownloadManager.COLUMN_URI);
                String title = intent.getStringExtra(OkDownloadManager.COLUMN_TITLE);
                String filePath = Environment.getExternalStorageDirectory() + "/Download/" + fileName;

                filePath = FileUtils.checkOrCreateFileName(filePath, 0);

                download(id, url, title, filePath, 0);
            } else {
                int id = intent.getIntExtra(OkDownloadManager.COLUMN_ID, 0);
                String url = intent.getStringExtra(OkDownloadManager.COLUMN_URI);
                String title = intent.getStringExtra(OkDownloadManager.COLUMN_TITLE);
                String filePath = intent.getStringExtra(OkDownloadManager.COLUMN_LOCAL_URI);

                File file = new File(filePath + TEMP_SUFFIX);

                download(id, url, title, filePath, file.length());
            }
        } else {
            // 服务尚未停止Apk被回收,会再度启动Service
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Service", "onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Service", "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("Service", "onDestroy");
        super.onDestroy();
        mSQLiteHelper = SQLiteHelper.getInstance(this);
        if (mSQLiteHelper != null) {
            // 将所有在下载的状态改为暂停
            mSQLiteHelper.updateAllRunToPause();
            mSQLiteHelper.close();
        }
    }

    /**
     * @param id
     * @param url
     * @param title
     * @param filePath
     * @param pos      从文件流的指定位置开始下载
     */
    public void download(final int id, String url, final String title, final String filePath,
                         final long pos) {
        if (mSQLiteHelper.getDownloadCount() >= 1) {
            NotificationUtils.showPending(mContext, title, id);
            mSQLiteHelper.insert(id, url, filePath, title, OkDownloadManager.STATUS_PENDING);
            Log.d(TAG, "同时只能进行一个下载");
            return;
        }

        mUrl = url;

        NotificationUtils.showPending(mContext, title, id);
        if (pos == 0) {
            mSQLiteHelper.insert(id, url, filePath, title);
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader("Range", "bytes=" + pos + "-")
                .url(url)
                .get()
                .build();
        Map<String, Object> tags = new HashMap<>();
        tags.put(OkDownloadManager.ORIGIN_TAG, request.tag());
        tags.put(OkDownloadManager.COLUMN_ID, id);
        tags.put(OkDownloadManager.COLUMN_TITLE, title);
        tags.put(OkDownloadManager.COLUMN_LOCAL_URI, filePath);
        request = request.newBuilder()
                .tag(tags)
                .build();

        mHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mSQLiteHelper.update(id, OkDownloadManager.STATUS_FAILED, 0);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "onResponse");
                        FileUtils.save(filePath + TEMP_SUFFIX, response.body().byteStream(), pos);
                    }
                });
    }

}
