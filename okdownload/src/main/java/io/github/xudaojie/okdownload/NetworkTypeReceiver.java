package io.github.xudaojie.okdownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import io.github.xudaojie.okdownload.util.SQLiteHelper;
import io.github.xudaojie.okdownload.util.SystemUtils;

/**
 * Created by xdj on 16/8/18.
 * 处理网络状态改变
 */

public class NetworkTypeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkTypeReceiver";
    private SQLiteHelper mSQLiteHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        mSQLiteHelper = SQLiteHelper.getInstance(context);

        // 断开网络时,由于正在下载的链接在网络错误中也会进行操作
        // 所以这里无需处理
        if (!SystemUtils.isNetConnected(context)) {
            // TODO: 16/8/18 更新状态
            Log.d(TAG, "network is not connect");

//            Cursor cursor = mSQLiteHelper.getCursorByRunning();
//            while (cursor.moveToNext()) {
//                long id = cursor.getLong(cursor.getColumnIndex(OkDownloadManager.COLUMN_ID));
//                String title = cursor.getString(cursor.getColumnIndex(OkDownloadManager.COLUMN_TITLE));
//                NotificationUtils.showWaitingForNetwork(context, title, id);
//            }
//
//            ContentValues values = new ContentValues();
//            values.put(OkDownloadManager.COLUMN_STATUS, OkDownloadManager.STATUS_WAITING_FOR_NETWORK);
//            String whereClause = OkDownloadManager.COLUMN_STATUS + " = ? and " + OkDownloadManager.COLUMN_STATUS + " = ?";
//            String[] whereArgs = new String[] {
//                    OkDownloadManager.STATUS_RUNNING + ""
//            };
//
//            mSQLiteHelper.update(values, whereClause, whereArgs);
        } else {
            Cursor cursor = mSQLiteHelper.query(
                    new String[]{OkDownloadManager.COLUMN_ID + ""},
                    OkDownloadManager.COLUMN_STATUS + " = ?",
                    new String[]{OkDownloadManager.STATUS_WAITING_FOR_NETWORK + ""});
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(OkDownloadManager.COLUMN_ID));
                OkDownloadManager.download(context, id);
            }
        }
    }
}
