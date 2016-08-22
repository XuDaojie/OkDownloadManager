package io.github.xudaojie.okdownload.util;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.github.xudaojie.okdownload.NotificationClickReceiver;
import io.github.xudaojie.okdownload.OkDownloadManager;

import static io.github.xudaojie.okdownload.OkDownloadManager.ACTION_NOTIFICATION_CLICKED;
import static io.github.xudaojie.okdownload.OkDownloadManager.COLUMN_ID;

/**
 * Created by xdj on 16/8/8.
 */

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    /**
     * 有网络状态下等待
     * @param context
     * @param title
     * @param id
     */
    public static void showPending(Context context, String title, long id) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications Enabled are false");
            return;
        }

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
        notification.flags |= Notification.FLAG_NO_CLEAR;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) id, notification);
    }

    public static void showRunning(Context context, long id, String title, int percent, Bundle args) {
        /**
         * 返回应用信息中是否允许显示通知,对国内ROM自己的Notification细分出的通知栏、锁屏权限没卵用
         */
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications Enabled are false");
            return;
        }

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText("已下载:" + percent + "%")
                .setProgress(100, percent, false)
                .setSmallIcon(android.R.drawable.stat_sys_download) // 必须设置
//                .setContentIntent(pendingIntent)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) id, notification);
    }

    /**
     * 等待网络连接
     * @param context
     * @param title
     * @param id
     */
    public static void showWaitingForNetwork(Context context, String title, long id) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications Enabled are false");
            return;
        }

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
                .setContentText("已加入队列")
                .setProgress(100, 0, true)
                .setSmallIcon(android.R.drawable.ic_media_pause) // 必须设置
                .setContentIntent(pendingIntent) // 指定点击事件对应的pendingIntent
//                .addAction(android.R.mipmap.sym_def_app_icon, "确定", pendingIntent)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) id, notification);
    }

    public static void showPaused(Context context, long id, String title, Bundle args) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications Enabled are false");
            return;
        }

        // todo 未完成时点击需暂停下载
        Intent i = new Intent(context, NotificationClickReceiver.class);
        i.setAction(ACTION_NOTIFICATION_CLICKED);
        i.putExtra(COLUMN_ID, id);
        i.putExtras(args);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        // 广播必须在Manifest里注册,代码注册无效
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_ONE_SHOT);

        // Notification.Builder.build() 在Api16中才开始支持
        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText("点击继续下载")
                .setSmallIcon(android.R.drawable.ic_media_pause) // 必须设置
                .setContentIntent(pendingIntent) // 指定点击事件对应的pendingIntent
//                .addAction(android.R.mipmap.sym_def_app_icon, "确定", pendingIntent)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) id, notification);
    }

    public static void showCompleted(Context context, long id, String title, Bundle args) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications Enabled are false");
            return;
        }

        Intent receiverIntent = new Intent(context, NotificationClickReceiver.class);
        receiverIntent.setAction(OkDownloadManager.ACTION_NOTIFICATION_CLICKED);
        receiverIntent.putExtras(args);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText("点击进行安装")
                .setSmallIcon(android.R.drawable.stat_sys_download_done) // 必须设置
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) id, notification);
    }

    public static boolean isNotificationEnabled(Context context) {

        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();

        String pkg = context.getApplicationContext().getPackageName();

        int uid = appInfo.uid;

        Class appOpsClass = null; /* Context.APP_OPS_MANAGER */

        try {

            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (int)opPostNotificationValue.get(Integer.class);

            return ((int)checkOpNoThrowMethod.invoke(mAppOps,value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
