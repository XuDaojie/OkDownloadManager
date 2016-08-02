package io.github.xudaojie.okdownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import io.github.xudaojie.okdownload.util.FileUtils;

/**
 * Created by xdj on 16/7/26.
 */

public class OkDownloadReceiver extends BroadcastReceiver {

    /**
     * 消息被点击 todo 下载完成后点击、下载中点击
     */
    public static final String NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";

    @Override
    public void onReceive(Context context, Intent intent) {
        int percent = intent.getIntExtra("percent", 0);
        int id = intent.getIntExtra("id", 0);
        String title = intent.getStringExtra("title");
        String filePath = intent.getStringExtra("filePath");
        Log.d("OkHttpReceiver", percent + "%");

        Notification notification;
        if (percent != 100) {
             notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText("已下载:" + percent + "%")
                    .setProgress(100, percent, false)
                    .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
//                .setContentIntent(pendingIntent)
                    .build();
        } else {
            Intent receiverIntent = new Intent(context, NotificationClickReceiver.class);
            receiverIntent.setAction(NOTIFICATION_CLICKED);
            receiverIntent.putExtras(intent);
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

            FileUtils.installApk(context, filePath);
        }
//        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);
    }
}
