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
                    context.getFilesDir() + "/" + DATABASE_NAME, null);
        } else if (!sDatabase.isOpen()) {
            sDatabase = SQLiteDatabase.openDatabase(context.getFilesDir() + "/" + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READWRITE);
        }

        if (!tableExist(sDatabase)) {
            createTable(sDatabase);
        }

        return sDatabase;
    }

    /**
     * 检查表是否已存在
     */
    public static boolean tableExist(SQLiteDatabase db) {
        // 查询表是否存在
        String sql = "select count(*) " +
                "from sqlite_master where type='table' and name = '" + TABLE_NAME + "'";
        Cursor cursor = db.rawQuery(sql, null);
        boolean exist = false;

        // 游标从-1开始
        cursor.moveToFirst();
        exist = cursor.getInt(0) != 0;

        return exist;
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
                              String localUri, String mediaType, String mediaProviderUri,
                              String reason, int status, String title,
                              long totalSizeBytes, String uri) {

        String localFileName = localUri;
        int allowWriteInt = 1;
        if (!allowWrite) allowWriteInt = 0;
        Object[] bindArags = new Object[]{
                allowWriteInt, id, timeline,
                localFileName, localUri, mediaType, mediaProviderUri, reason,
                status, title, totalSizeBytes, uri};
        db.execSQL("INSERT INTO " + TABLE_NAME +
                " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", bindArags);
    }

    public static void insert(SQLiteDatabase db, long id, String localUri, String title) {
        insert(db, id, true, System.currentTimeMillis(),
                localUri, null, null,
                null, OkDownloadManager.STATUS_PENDING, title, 0, null);
    }

    public static void insert(Context context, long id, String localUri, String title) {
        SQLiteDatabase db = getDatabase(context);

        insert(db, id, localUri, title);
    }

    public static void update(SQLiteDatabase db, long id, int status, long totalSizeBytes) {
        String sql = "UPDATE " + TABLE_NAME + "\n" +
                "SET status = ?,\n" +
                " total_size_bytes = ?\n" +
                "WHERE\n" +
                "    id = ?";
        Object[] args = new Object[]{status, totalSizeBytes, id};
        db.execSQL(sql, args);
    }

    public static void update(Context context, long id, int status, long totalSizeBytes) {
        SQLiteDatabase db = getDatabase(context);
        update(db, id, status, totalSizeBytes);
    }

}
