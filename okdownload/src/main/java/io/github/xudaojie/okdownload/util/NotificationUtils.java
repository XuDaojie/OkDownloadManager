package io.github.xudaojie.okdownload.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import io.github.xudaojie.okdownload.NotificationClickReceiver;

import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_NOTIFICATION_CLICKED;
import static io.github.xudaojie.okdownload.OkDownloadManager.COLUMN_ID;

/**
 * Created by xdj on 16/8/8.
 */

public class NotificationUtils {
    public static void showPending(Context context, String title, int id) {
        // todo 未完成时点击需暂停下载
        Intent i = new Intent(context, NotificationClickReceiver.class);
        i.setAction(ACTION_NOTIFICATION_CLICKED);
        i.putExtra(COLUMN_ID, id);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        // 广播必须在Manifest里注册,代码注册无效
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_ONE_SHOT);

        // Notification.Builder.build() 在Api16中才开始支持
        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText("")
                .setProgress(100, 0, true)
                .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
                .setContentIntent(pendingIntent) // 指定点击事件对应的pendingIntent
//                .addAction(android.R.mipmap.sym_def_app_icon, "确定", pendingIntent)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);
    }
}
