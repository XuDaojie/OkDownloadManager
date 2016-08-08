package io.github.xudaojie.okdownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.xudaojie.okdownload.util.FileUtils;
import io.github.xudaojie.okdownload.util.SQLiteHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xdj on 16/7/25.
 */
public class OkDownloadManager extends Service {

    public static final String TEMP_SUFFIX = ".t"; // 中间文件后缀
    public static final String FILENAME = "filename"; // 文件名
    public static final String ORIGIN_TAG = "origin_tag";
    public static final String DOWNLOAD_PERCENT = "download_percent";

    public static final String ACTION_DOWNLOAD = "android.intent.action.DOWNLOAD"; // 正在下载
    public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
//    public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS"; // 获取下载历史
    public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    /** @deprecated */
    @Deprecated
    public static final String COLUMN_LOCAL_FILENAME = "local_filename"; // 本地文件路径
    public static final String COLUMN_LOCAL_URI = "local_uri"; // 本地文件路径
    public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";
    public static final String COLUMN_MEDIA_TYPE = "media_type";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_SIZE_BYTES = "total_size";
    public static final String COLUMN_URI = "uri";
    public static final int ERROR_CANNOT_RESUME = 1008;
    public static final int ERROR_DEVICE_NOT_FOUND = 1007;
    public static final int ERROR_FILE_ALREADY_EXISTS = 1009;
    public static final int ERROR_FILE_ERROR = 1001;
    public static final int ERROR_HTTP_DATA_ERROR = 1004;
    public static final int ERROR_INSUFFICIENT_SPACE = 1006;
    public static final int ERROR_TOO_MANY_REDIRECTS = 1005;
    public static final int ERROR_UNHANDLED_HTTP_CODE = 1002;
    public static final int ERROR_UNKNOWN = 1000;
    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
    public static final String EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS = "extra_click_download_ids";
    public static final String INTENT_EXTRAS_SORT_BY_SIZE = "android.app.DownloadManager.extra_sortBySize";
    public static final int PAUSED_QUEUED_FOR_WIFI = 3;
    public static final int PAUSED_UNKNOWN = 4;
    public static final int PAUSED_WAITING_FOR_NETWORK = 2;
    public static final int PAUSED_WAITING_TO_RETRY = 1;
    public static final int STATUS_FAILED = 16;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_SUCCESSFUL = 8;

    private static final String TAG = "OkDownloadManager";

    private static OkHttpClient mHttpClient;

    private Context mContext;
    private LocalBroadcastManager mBroadcastManager;

    private long mTimeline; // 上次发送广播的时间
    private String mUrl; // 正在进行下载的链接
    private int mPercent; // 正在进行下载的进度

    private SQLiteHelper mSQLiteHelper;

    public static void download(Context context, String title, String url, String fileName) {
        Intent i = new Intent(context, OkDownloadManager.class);
        i.putExtra(COLUMN_ID, (int) System.currentTimeMillis());
        i.putExtra(COLUMN_URI, url);
        i.putExtra(COLUMN_TITLE, title);
        i.putExtra(FILENAME, fileName);
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d("Service", "onCreate");

        if (mHttpClient == null) {
            mHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(final Interceptor.Chain chain) throws IOException {
                            final Request request = chain.request();
                            final String url = chain.request().url().toString();
                            final Map<String, Object> tagMap = (Map<String, Object>)request.tag();

                            Response response = chain.proceed(request);
                            return response.newBuilder()
                                    .body(new ProgressResponseBody(response.body(), new ProgressResponseBody.ProgressListener() {
                                        @Override
                                        public void update(long bytesRead, long contentLength, boolean done) {
                                            int percent = (int) ((float)bytesRead / contentLength * 100);
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

                                                if (tagMap != null && tagMap.get(COLUMN_ID) != null) {
                                                    id = (int) tagMap.get(COLUMN_ID);
                                                    title = (String) tagMap.get(COLUMN_TITLE);
                                                    filePath = (String) tagMap.get(OkDownloadManager.COLUMN_LOCAL_URI);
                                                }

                                                Intent i = new Intent();
                                                i.setAction(ACTION_DOWNLOAD);
                                                i.putExtra(DOWNLOAD_PERCENT, percent);
                                                i.putExtra(COLUMN_URI, url);
                                                i.putExtra(COLUMN_ID, id);
                                                i.putExtra(COLUMN_TITLE, title);
                                                i.putExtra(COLUMN_LOCAL_URI, filePath);
                                                i.putExtra(COLUMN_TOTAL_SIZE_BYTES, contentLength);

                                                // 必须也使用LocalBroadcastReceiver进行注册才能接收
                                                mBroadcastManager.sendBroadcast(i);
                                            }

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
            mBroadcastManager.registerReceiver(okHttpReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand");
        if (intent != null && intent.hasExtra(COLUMN_ID) && intent.hasExtra(COLUMN_URI)) {
            String fileName = intent.getStringExtra(FILENAME);
            int id = intent.getIntExtra(COLUMN_ID, 0);
            String url = intent.getStringExtra(COLUMN_URI);
            String title = intent.getStringExtra(COLUMN_TITLE);
            String filePath = Environment.getExternalStorageDirectory() + "/Download/" + fileName;

            filePath = FileUtils.checkOrCreateFileName(filePath, 0);

            download(id, url, title, filePath);
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
        if (mSQLiteHelper != null) {
            mSQLiteHelper.close();
        }
    }

    public void download(final int id, String url, final String title, final String filePath) {
        if (mUrl != null) {
            mSQLiteHelper.insert(id, url, title, OkDownloadManager.STATUS_PENDING);
            Log.d(TAG, "同时只能进行一个下载");
            return;
        }

        mUrl = url;

        // todo 未完成时点击需暂停下载
        Intent i = new Intent(mContext, NotificationClickReceiver.class);
        i.setAction(ACTION_NOTIFICATION_CLICKED);
        i.putExtra(COLUMN_ID, id);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        // 广播必须在Manifest里注册,代码注册无效
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_ONE_SHOT);

        // Notification.Builder.build() 在Api16中才开始支持
        Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(title)
                .setContentText("")
                .setProgress(100, 0, true)
                .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
                .setContentIntent(pendingIntent) // 指定点击事件对应的pendingIntent
//                .addAction(android.R.mipmap.sym_def_app_icon, "确定", pendingIntent)
                .build();

        NotificationManager manager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);

        mSQLiteHelper = SQLiteHelper.getInstance(mContext);
        mSQLiteHelper.insert(id, filePath, title);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Map<String, Object> tags = new HashMap<>();
        tags.put(ORIGIN_TAG, request.tag());
        tags.put(COLUMN_ID, id);
        tags.put(COLUMN_TITLE, title);
        tags.put(COLUMN_LOCAL_URI, filePath);
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
                        FileUtils.save(filePath + TEMP_SUFFIX, response.body().byteStream());
                    }
                });
    }

}