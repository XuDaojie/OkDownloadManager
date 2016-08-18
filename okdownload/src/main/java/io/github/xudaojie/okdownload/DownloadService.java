package io.github.xudaojie.okdownload;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import io.github.xudaojie.okdownload.util.SystemUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_DOWNLOAD;
import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_DOWNLOAD_FAIL;
import static io.github.xudaojie.okdownload.OkDownloadManager.COLUMN_ID;
import static io.github.xudaojie.okdownload.OkDownloadManager.COLUMN_STATUS;
import static io.github.xudaojie.okdownload.OkDownloadManager.COLUMN_TITLE;
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
                                            Log.d(TAG, bytesRead + "/" + contentLength);

                                            long id = 0;
                                            String title = "";
                                            String filePath = "";
                                            long totalSizeBytes = 0;
                                            long currentSizeBytes = 0;

                                            if (tagMap != null && tagMap.get(OkDownloadManager.COLUMN_ID) != null) {
                                                id = (long) tagMap.get(OkDownloadManager.COLUMN_ID);
                                                title = (String) tagMap.get(OkDownloadManager.COLUMN_TITLE);
                                                filePath = (String) tagMap.get(OkDownloadManager.COLUMN_LOCAL_URI);
                                                totalSizeBytes = (long) tagMap.get(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                                                currentSizeBytes = (long) tagMap.get(OkDownloadManager.COLUMN_CURRENT_SIZE_BYTES);
                                            }

                                            currentSizeBytes += bytesRead;
                                            if (totalSizeBytes == 0) {
                                                totalSizeBytes = contentLength;
                                            }

                                            int percent = (int) ((float) currentSizeBytes / totalSizeBytes * 100);
                                            // 广播接收也是在主界面执行的,全部发送的话会造成系统卡顿
                                            long currentTimeline = System.currentTimeMillis();
                                            if ((currentTimeline - mTimeline > 600 && mPercent != percent)
                                                    || done) {
                                                mTimeline = currentTimeline;
                                                mPercent = percent;
                                                Log.d(TAG, percent + "%");
                                                if (done) {
                                                    mPercent = -1;
                                                    Log.d(TAG, "download done; url: " + url);
                                                }

                                                Intent i = new Intent();
                                                i.setAction(OkDownloadManager.ACTION_DOWNLOAD);
                                                i.putExtra(OkDownloadManager.DOWNLOAD_PERCENT, percent);
                                                i.putExtra(OkDownloadManager.COLUMN_URI, url);
                                                i.putExtra(OkDownloadManager.COLUMN_ID, id);
                                                i.putExtra(OkDownloadManager.COLUMN_TITLE, title);
                                                i.putExtra(OkDownloadManager.COLUMN_LOCAL_URI, filePath);
                                                i.putExtra(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, totalSizeBytes);
                                                i.putExtra(OkDownloadManager.COLUMN_CURRENT_SIZE_BYTES, currentSizeBytes);

                                                // 必须也使用LocalBroadcastReceiver进行注册才能接收
                                                mBroadcastManager.sendBroadcast(i);
                                            }

                                        }

                                        @Override
                                        public void onFailure(IOException e) {
                                            long id = 0;
                                            String title = "";
                                            String filePath = "";

                                            Log.d(TAG, "onFailure");

                                            if (tagMap != null && tagMap.get(OkDownloadManager.COLUMN_ID) != null) {
                                                id = (long) tagMap.get(OkDownloadManager.COLUMN_ID);
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

            // TODO: 16/8/18 注销广播 Android N无效 只能静态注册?
//            NetworkTypeReceiver networkTypeReceiver = new NetworkTypeReceiver();
//            IntentFilter networkFilter = new IntentFilter();
//            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//            mBroadcastManager.registerReceiver(networkTypeReceiver, networkFilter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand");

        if (intent != null && intent.hasExtra(OkDownloadManager.COLUMN_ID)) {
            long id = intent.getLongExtra(OkDownloadManager.COLUMN_ID, 0);

            Cursor taskCursor = mSQLiteHelper.getCursorById(id);
            if (taskCursor.getCount() == 0) {
                // TODO: 16/8/16 任务不存在
                Log.d(TAG, "not found id");
                return super.onStartCommand(intent, flags, startId);
            }
            taskCursor.moveToNext();
            String url = taskCursor.getString(taskCursor.getColumnIndex(OkDownloadManager.COLUMN_URI));
            String title = taskCursor.getString(taskCursor.getColumnIndex(OkDownloadManager.COLUMN_TITLE));
            String localUri = taskCursor.getString(taskCursor.getColumnIndex(OkDownloadManager.COLUMN_LOCAL_URI));
            long totalSizeBytes = taskCursor.getLong(taskCursor.getColumnIndex(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            File file = new File(localUri + TEMP_SUFFIX);
            long pos = 0;
            if (file.exists()) {
                pos = file.length();
            }
            download(id, url, title, localUri, totalSizeBytes, pos);
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
     * @param pos 从文件流的指定位置开始下载
     */
    public void download(final long id, String url, final String title, final String filePath,
                         long totalSizeBytes, final long pos) {
        NotificationUtils.showPending(mContext, title, id);

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
        tags.put(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, totalSizeBytes);
        tags.put(OkDownloadManager.COLUMN_CURRENT_SIZE_BYTES, pos);
        request = request.newBuilder()
                .tag(tags)
                .build();

        if (!SystemUtils.isNetConnected(mContext)) {
            Intent i = new Intent();
            i.setAction(OkDownloadManager.ACTION_DOWNLOAD);
            i.putExtra(COLUMN_ID, id);
            i.putExtra(COLUMN_STATUS, OkDownloadManager.STATUS_WAITING_FOR_NETWORK);
            i.putExtra(COLUMN_TITLE, title);
            // TODO: 16/8/18 将其他参数也传入
            mBroadcastManager.sendBroadcast(i);
        } else {
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

}
