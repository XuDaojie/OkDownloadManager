package io.github.xudaojie.okdownload.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import io.github.xudaojie.okdownload.OkDownloadManager;

/**
 * Created by xdj on 16/8/2.
 */

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "SQLiteUtils";

    private static final String DATABASE_NAME = "download.db";
    private static final String TABLE_NAME = "t_download_manager";

    private static SQLiteHelper sInstance;

    public static final SQLiteHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SQLiteHelper(context, DATABASE_NAME, null, 1);
        }
        return sInstance;
    }

    public SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // 数据库初始化
        createTable(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // 数据库版本更新
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // 检查表是否存在、不存在则创建
        if (!tableExist(db)) {
            createTable(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }

//    public static SQLiteDatabase getDatabase(Context context) {
//        if (sDatabase == null) {
//            sDatabase = SQLiteDatabase.openOrCreateDatabase(
//                    context.getFilesDir() + "/" + DATABASE_NAME, null);
//        } else if (!sDatabase.isOpen()) {
//            sDatabase = SQLiteDatabase.openDatabase(context.getFilesDir() + "/databases/" + DATABASE_NAME, null,
//                    SQLiteDatabase.OPEN_READWRITE);
//        }
//
//        if (!tableExist(sDatabase)) {
//            createTable(sDatabase);
//        }
//
//        return sDatabase;
//    }

    /**
     * 检查表是否已存在
     */
    public boolean tableExist(SQLiteDatabase db) {
        // 查询表是否存在
        String sql = "select count(*) " +
                "from sqlite_master where type='table' and name = '" + TABLE_NAME + "'";
        Cursor cursor = db.rawQuery(sql, null);

        // 游标从-1开始
        cursor.moveToFirst();
        return cursor.getInt(0) != 0;
    }

    public void createTable(SQLiteDatabase db) {

        String sql = "CREATE TABLE " + TABLE_NAME + "(\n" +
                "       allow_write Boolean,\n" +
                "       _id integer NOT NULL,\n" +
                "       last_modify_timestamp integer,\n" +
                "       local_filename varchar,\n" +
                "       local_uri varchar,\n" +
                "       media_type varchar,\n" +
                "       media_provider_uri varchar,\n" +
                "       reason varchar,\n" +
                "       status integer,\n" +
                "       title varchar,\n" +
                "       current_size_bytes integer,\n" +
                "       total_size_bytes integer,\n" +
                "       allowed_network_types integer,\n" +
                "       visibility integer,\n" +
                "       uri varchar,\n" +
                "       PRIMARY KEY(_id)\n" +
                ")";

        db.execSQL(sql);
    }

    public void insert(SQLiteDatabase db,
                       long id, boolean allowWrite, long timeline,
                       String localUri, String mediaType, String mediaProviderUri,
                       String reason, int status, String title,
                       long currentSizeBytes, long totalSizeBytes, String uri) {
        try {
            String localFileName = localUri;
            int allowWriteInt = 1;
            if (!allowWrite) allowWriteInt = 0;
            Object[] bindArags = new Object[]{
                    allowWriteInt, id, timeline,
                    localFileName, localUri, mediaType, mediaProviderUri, reason,
                    status, title, currentSizeBytes, totalSizeBytes, uri};
            db.execSQL("INSERT INTO " + TABLE_NAME +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", bindArags);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void insert(SQLiteDatabase db, long id, String url, String localUri, String title) {
        insert(db, id, true, System.currentTimeMillis(),
                localUri, null, null,
                null, OkDownloadManager.STATUS_PENDING, title, 0, 0, url);
    }

    public void insert(long id, String uri, String localUri, String title, int status) {
        SQLiteDatabase db = getWritableDatabase();

        insert(db, id, true, System.currentTimeMillis(),
                localUri, null, null,
                null, status, title, 0, 0, uri);
    }

    public void insert(long id, String url, String localUri, String title) {
        SQLiteDatabase db = getWritableDatabase();

        insert(db, id, url, localUri, title);
    }

    public long insert(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();

        String url = (String) values.get(OkDownloadManager.COLUMN_URI);
        if (url == null) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        long id = System.currentTimeMillis();
        values.put(OkDownloadManager.COLUMN_ID, id);
        db.insert(TABLE_NAME, null, values);

        return id;
    }

    public void update(long id, int status, long totalSizeBytes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Log.v(TAG, "locked:" + db.isDbLockedByCurrentThread());

            String sql = "UPDATE " + TABLE_NAME + "\n" +
                    "SET status = ?,\n" +
                    " total_size_bytes = ?\n" +
                    "WHERE\n" +
                    "    _id = ?";
            Object[] args = new Object[]{status, totalSizeBytes, id};
            db.execSQL(sql, args);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void update(long id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Log.v(TAG, "locked:" + db.isDbLockedByCurrentThread());

            String sql = "UPDATE " + TABLE_NAME + "\n" +
                    "SET status = ?\n" +
                    "WHERE\n" +
                    "    _id = ?";
            Object[] args = new Object[]{status, id};
            db.execSQL(sql, args);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void updateAllRunToPause() {
        Log.d(TAG, "updateAllRunToPause");
        String sql = "update " + TABLE_NAME + "\n" +
                "set status = ?\n" +
                "where status = ?";

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql, new Object[]{OkDownloadManager.STATUS_PAUSED,
                OkDownloadManager.STATUS_RUNNING});
    }

    public int getStatus(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select " + OkDownloadManager.COLUMN_STATUS + " from " + TABLE_NAME + " where _id = ?",
                new String[]{id + ""});
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
        return OkDownloadManager.STATUS_PENDING;
    }

    /**
     * 当前正在下载的任务数
     *
     * @return
     */
    public int getDownloadCount() {
        String sql = "select count(*) from " + TABLE_NAME + "" +
                " where status = " + OkDownloadManager.STATUS_RUNNING;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public Cursor getCursorById(long id) {
        String sql = "select _id, title, uri, local_uri, status from " + TABLE_NAME +
                " where _id = " + id;

        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(sql, null);
    }

    /**
     * 获得所有状态为暂停的任务id
     * @return
     */
    public Cursor getCursorByPause() {
        String sql = "select _id, title, uri, local_uri from " + TABLE_NAME +
                " where status = " + OkDownloadManager.STATUS_PAUSED;

        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(sql, null);
    }

}
