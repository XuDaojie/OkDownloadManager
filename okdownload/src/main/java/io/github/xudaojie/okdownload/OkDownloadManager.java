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

    public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
    public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS";
    public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    /** @deprecated */
    @Deprecated
    public static final String COLUMN_LOCAL_FILENAME = "local_filename";
    public static final String COLUMN_LOCAL_URI = "local_uri";
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

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d("Service", "onCreate");

        if (mHttpClient == null) {
//            mHttpClient = new OkHttpClient();
//            mHttpClient.networkInterceptors()

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

                                                if (done) {
                                                    mUrl = null;
                                                    mPercent = -1;
                                                }

                                                mTimeline = currentTimeline;
                                                mPercent = percent;
                                                int id = 0;
                                                String title = "";
                                                String filePath = "";
                                                if (tagMap != null && tagMap.get("id") != null) {
                                                    id = (int) tagMap.get("id");
                                                    title = (String) tagMap.get("title");
                                                    filePath = (String) tagMap.get("filePath");
                                                }

                                                Intent i = new Intent();
                                                i.setAction("ok_http_download");
                                                i.putExtra("percent", percent);
                                                i.putExtra("url", url);
                                                i.putExtra("id", id);
                                                i.putExtra("title", title);
                                                i.putExtra("filePath", filePath);

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
            filter.addAction("ok_http_download");
            mBroadcastManager.registerReceiver(okHttpReceiver, filter);
        }

//        download();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand");
        if (intent != null && intent.hasExtra("id") && intent.hasExtra("url")) {
            int id = intent.getIntExtra("id", 0);
            String url = intent.getStringExtra("url");
            String title = intent.getStringExtra("title");
            String fileName = intent.getStringExtra("fileName");
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
    }

    public void download(int id, String url, String title, final String filePath) {
        if (mUrl != null) {
            Log.d(TAG, "同时只能进行一个下载");
            return;
        }

        mUrl = url;

        Intent i = new Intent(mContext, NotificationClickReceiver.class);
        i.setAction("notification_clicked");
        i.putExtra("type", id);
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



        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Map<String, Object> tags = new HashMap<>();
        tags.put("origin_tag", request.tag());
        tags.put("id", id);
        tags.put("title", title);
        tags.put("filePath", filePath);

        request = request.newBuilder()
                .tag(tags)
                .build();

        mHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        FileUtils.save(filePath + TEMP_SUFFIX, response.body().byteStream());
                    }
                });
    }



}