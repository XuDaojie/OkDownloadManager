package io.github.xudaojie.okdownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.github.xudaojie.okdownload.util.FileUtils;
import io.github.xudaojie.okdownload.util.SQLiteHelper;

/**
 * Created by xdj on 16/7/25.
 */

public class NotificationClickReceiver extends BroadcastReceiver {

    public NotificationClickReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(context);

        long id = intent.getLongExtra(OkDownloadManager.COLUMN_ID, 0);
        String filePath = intent.getStringExtra(OkDownloadManager.COLUMN_LOCAL_URI);

        if (sqLiteHelper.getStatus(id) == OkDownloadManager.STATUS_PAUSED) {
            OkDownloadManager.download(context, id);
        } else if (sqLiteHelper.getStatus(id) == OkDownloadManager.STATUS_SUCCESSFUL){
            FileUtils.installApk(context, filePath);
        }

    }
}
