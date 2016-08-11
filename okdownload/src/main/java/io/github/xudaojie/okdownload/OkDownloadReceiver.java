package io.github.xudaojie.okdownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

import io.github.xudaojie.okdownload.util.FileUtils;
import io.github.xudaojie.okdownload.util.SQLiteHelper;

/**
 * Created by xdj on 16/7/26.
 */

public class OkDownloadReceiver extends BroadcastReceiver {

    private static final String TAG = "OkDownloadReceiver";

    /**
     * 消息被点击 todo 下载完成后点击、下载中点击
     */
//    public static final String NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";

    @Override
    public void onReceive(Context context, Intent intent) {
        SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(context);
        String action = intent.getAction();
        Notification notification = null;

        int id = intent.getIntExtra(OkDownloadManager.COLUMN_ID, 0);
        String title = intent.getStringExtra(OkDownloadManager.COLUMN_TITLE);
        String filePath = intent.getStringExtra(OkDownloadManager.COLUMN_LOCAL_URI);

        if (TextUtils.equals(OkDownloadManager.ACTION_DOWNLOAD, action)) {
            int percent = intent.getIntExtra(OkDownloadManager.DOWNLOAD_PERCENT, 0);
            long totalSizeBytes = intent.getLongExtra(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, 0);
            Log.d(TAG, percent + "%");

            if (percent != 100) {
                notification = new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setContentText("已下载:" + percent + "%")
                        .setProgress(100, percent, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
//                .setContentIntent(pendingIntent)
                        .build();

                if (sqLiteHelper.getStatus(id) != OkDownloadManager.STATUS_RUNNING) {
                    sqLiteHelper.update(id, OkDownloadManager.STATUS_RUNNING, totalSizeBytes);
                }
            } else {
                Intent receiverIntent = new Intent(context, NotificationClickReceiver.class);
                receiverIntent.setAction(OkDownloadManager.ACTION_NOTIFICATION_CLICKED);
                receiverIntent.putExtras(intent.getExtras());
                PendingIntent pendingIntent = PendingIntent
                        .getBroadcast(context, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                notification = new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setContentText("点击进行安装")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done) // 必须设置
                        .setContentIntent(pendingIntent)
                        .build();

                // 修改文件名为正确文件名
                File currentFile = new File(filePath + OkDownloadManager.TEMP_SUFFIX);
                currentFile.renameTo(new File(filePath));

                sqLiteHelper.update(id, OkDownloadManager.STATUS_SUCCESSFUL, totalSizeBytes);
                // TODO: 16/8/8 继续下一个下载
                FileUtils.installApk(context, filePath);
            }
//        notification.flags = Notification.FLAG_AUTO_CANCEL;

        } else if (TextUtils.equals(OkDownloadManager.ACTION_DOWNLOAD_FAIL, action)) {
            Intent receiverIntent = new Intent(context, NotificationClickReceiver.class);
            receiverIntent.setAction(OkDownloadManager.ACTION_NOTIFICATION_CLICKED);
            receiverIntent.putExtras(intent.getExtras());
            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(context, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText("点击继续下载")
                    .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
                    .setContentIntent(pendingIntent)
                    .build();

            if (sqLiteHelper.getStatus(id) != OkDownloadManager.STATUS_PAUSED) {
                sqLiteHelper.update(id, OkDownloadManager.STATUS_PAUSED);
            }
        }

        if (notification != null) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(id, notification);
        }
    }
}
