package io.github.xudaojie.okdownload;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

import io.github.xudaojie.okdownload.util.FileUtils;
import io.github.xudaojie.okdownload.util.NotificationUtils;
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

        long id = intent.getLongExtra(OkDownloadManager.COLUMN_ID, 0);
        String title = intent.getStringExtra(OkDownloadManager.COLUMN_TITLE);
        String filePath = intent.getStringExtra(OkDownloadManager.COLUMN_LOCAL_URI);
        long totalSizeBytes = intent.getLongExtra(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, 0);
        long currentSizeBytes = intent.getLongExtra(OkDownloadManager.COLUMN_CURRENT_SIZE_BYTES, 0);
        int percent = intent.getIntExtra(OkDownloadManager.DOWNLOAD_PERCENT, 0);
        Log.d(TAG, percent + "%");

        if (TextUtils.equals(OkDownloadManager.ACTION_DOWNLOAD, action)) {

            if (percent != 100) {
                NotificationUtils.showRunning(context, id, title, percent, intent.getExtras());

                if (sqLiteHelper.getStatus(id) != OkDownloadManager.STATUS_RUNNING) {
                    ContentValues values = new ContentValues();
                    values.put(OkDownloadManager.COLUMN_STATUS, OkDownloadManager.STATUS_RUNNING);
                    values.put(OkDownloadManager.COLUMN_TOTAL_SIZE_BYTES, totalSizeBytes);
                    values.put(OkDownloadManager.COLUMN_CURRENT_SIZE_BYTES, currentSizeBytes);
                    sqLiteHelper.update(values, OkDownloadManager.COLUMN_ID + "= ?",
                            new String[] {id + ""});
                }
            } else {
                NotificationUtils.showCompleted(context, id, title, intent.getExtras());

                // 修改文件名为正确文件名
                File currentFile = new File(filePath + OkDownloadManager.TEMP_SUFFIX);
                currentFile.renameTo(new File(filePath));

                sqLiteHelper.update(id, OkDownloadManager.STATUS_SUCCESSFUL, totalSizeBytes);
                // TODO: 16/8/8 继续下一个下载
                FileUtils.installApk(context, filePath);
            }
//        notification.flags = Notification.FLAG_AUTO_CANCEL;

        } else if (TextUtils.equals(OkDownloadManager.ACTION_DOWNLOAD_FAIL, action)) {
            NotificationUtils.showPaused(context, id, title, intent.getExtras());

            if (sqLiteHelper.getStatus(id) != OkDownloadManager.STATUS_PAUSED) {
                sqLiteHelper.update(id, OkDownloadManager.STATUS_PAUSED);
            }
        }
    }
}
