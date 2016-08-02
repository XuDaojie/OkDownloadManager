package io.github.xudaojie.okdownload.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import io.github.xudaojie.okdownload.OkDownloadManager;

/**
 * Created by xdj on 16/8/2.
 */

public class SQLiteUtils {
    private static final String DATABASE_NAME = "download.db";
    private static final String TABLE_NAME = "t_download_manager";

    private static SQLiteDatabase sDatabase;

    public static SQLiteDatabase getDatabase(Context context) {
        if (sDatabase == null) {
            sDatabase = SQLiteDatabase.openOrCreateDatabase(
                    context.getFilesDir() + "/download.db", null);
            return sDatabase;
        } else if (!sDatabase.isOpen()) {
            sDatabase = SQLiteDatabase.openDatabase(context.getFilesDir() + "download.db", null,
                    SQLiteDatabase.OPEN_READWRITE);
        }
        return sDatabase;
    }

    /**
     * 检查表是否已存在
     */
    public static boolean tableExist(SQLiteDatabase db) {
        // 查询表是否存在
        String sql = "select count(*) " +
                "from sqlite_master where type='table' and name = '" + TABLE_NAME +"'";
        Cursor cursor = db.rawQuery(sql, null);

        return cursor.getInt(0) == 0;
    }

    public static void createTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + "(\n" +
                "       allow_write Boolean,\n" +
                "       id integer NOT NULL,\n" +
                "       last_modify_timestamp integer,\n" +
                "       local_filename varchar,\n" +
                "       local_uri varchar,\n" +
                "       media_type varchar,\n" +
                "       media_provider_uri varchar,\n" +
                "       reason varchar,\n" +
                "       status integer,\n" +
                "       title varchar,\n" +
                "       total_size_bytes integer,\n" +
                "       uri varchar,\n" +
                "       PRIMARY KEY(id)\n" +
                ")";

        db.execSQL(sql);
    }

    public static void insert(SQLiteDatabase db,
                              long id, boolean allowWrite, long timeline,
                              String localUri, String mediaType, String imediaProviderUri,
                              String reason, int status, String title,
                              long totalSizeBytes, String uri) {
        db.execSQL("INSERT INTO " + TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public static void insert(SQLiteDatabase db, long id, String localUri, String title) {
        insert(db, id, true, System.currentTimeMillis(),
                localUri, null, null,
                null, OkDownloadManager.STATUS_PENDING, title, 0, null);
    }

    public static void update(SQLiteDatabase db, long id, int status, long totalSizeBytes) {
        String sql = "UPDATE " + TABLE_NAME +"\n" +
                "SET status = ?,\n" +
                " total_size_bytes = ?\n" +
                "WHERE\n" +
                "    id = ?";
        Object[] args = new Object[] {status, totalSizeBytes, id};
        db.execSQL(sql, args);
    }

}
