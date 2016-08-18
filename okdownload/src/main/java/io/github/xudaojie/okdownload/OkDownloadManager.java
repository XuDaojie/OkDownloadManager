package io.github.xudaojie.okdownload;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.xudaojie.okdownload.util.SQLiteHelper;

/**
 * Created by xdj on 16/7/25.
 */
public class OkDownloadManager {

    private static OkDownloadManager sInstance;

    public static final String TEMP_SUFFIX = ".t"; // 中间文件后缀
    public static final String FILENAME = "filename"; // 文件名
    public static final String ORIGIN_TAG = "origin_tag";
    public static final String DOWNLOAD_PERCENT = "download_percent";
    public static final String DOWNLOAD_TYPE = "download_type"; // 继续下载还是重新下载

    public static final int DOWNLOAD_MODE_NEW_TASK = 1;
    public static final int DOWNLOAD_MODE_CONTINUE = 2; // 继续下载(断点续传)

    public static final String ACTION_DOWNLOAD_FAIL = "android.intent.action.DOWNLOAD_FAIL"; // 下载过程中出现异常
    public static final String ACTION_DOWNLOAD = "android.intent.action.DOWNLOAD"; // 正在下载
    public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
    //    public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS"; // 获取下载历史
    public static final String COLUMN_ALLOWED_NETWORK_TYPES = "allowed_network_types";
    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";

    public static final String COLUMN_LOCAL_URI = "local_uri"; // 本地文件路径
    public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";
    public static final String COLUMN_MEDIA_TYPE = "media_type";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_SIZE_BYTES = "total_bytes";
    public static final String COLUMN_CURRENT_SIZE_BYTES = "current_bytes";
    public static final String COLUMN_URI = "uri";
    public static final int ERROR_CANNOT_RESUME = 1008;
    public static final int ERROR_DEVICE_NOT_FOUND = 1007;
    public static final int ERROR_FILE_ALREADY_EXISTS = 1009;
    public static final int ERROR_FILE_ERROR = 1001;
    public static final int ERROR_HTTP_DATA_ERROR = 1004;
    public static final int ERROR_INSUFFICIENT_SPACE = 1006;
    public static final int ERROR_TOO_MANY_REDIRECTS = 1005;
    public static final int ERROR_UNHANDLED_HTTP_CODE = 1002;
    public static final int ERROR_UNKNOWN = 1000;
    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
    public static final String EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS = "extra_click_download_ids";
    public static final String INTENT_EXTRAS_SORT_BY_SIZE = "android.app.DownloadManager.extra_sortBySize";
    public static final int PAUSED_QUEUED_FOR_WIFI = 3;
    public static final int PAUSED_UNKNOWN = 4;
    public static final int PAUSED_WAITING_FOR_NETWORK = 2;
    public static final int PAUSED_WAITING_TO_RETRY = 1;
    public static final int STATUS_FAILED = 16;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_SUCCESSFUL = 8;

    /**
     * This download is waiting for network connectivity to proceed.
     */
    public static final int STATUS_WAITING_FOR_NETWORK = 195;

    private static final String TAG = "OkDownloadManager";

    private Context mContext;
    private SQLiteHelper mSQLiteHelper;

    public static OkDownloadManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new OkDownloadManager(context);
        }
        return sInstance;
    }

    public static void download(Context context, String title, String url, String fileName) {
        Intent i = new Intent(context, OkDownloadManager.class);
        i.putExtra(COLUMN_ID, (int) System.currentTimeMillis());
        i.putExtra(COLUMN_URI, url);
        i.putExtra(COLUMN_TITLE, title);
        i.putExtra(FILENAME, fileName);
        context.startService(i);
    }

    public static void download(Context context, long id) {
        OkDownloadManager downloadManager = OkDownloadManager.getInstance(context);
        OkDownloadManager.Request request = new OkDownloadManager.Request(id);
        downloadManager.enqueue(request);
    }

    public OkDownloadManager(Context context) {
        mSQLiteHelper = SQLiteHelper.getInstance(context);
        mContext = context;
    }

    /**
     * Enqueue a new download.  The download will start automatically once the download manager is
     * ready to execute it and connectivity is available.
     *
     * @param request the parameters specifying this download
     * @return an ID for the download, unique across the system.  This ID is used to make future
     * calls related to this download.
     */
    public long enqueue(Request request) {
        long id = request.getDwonloadId();

        if (request.getDwonloadId() == 0) {
            ContentValues values = request.toContentValues();
            id = mSQLiteHelper.insert(values);
        }

        Intent i = new Intent(mContext, DownloadService.class);
        i.putExtra(COLUMN_ID, id);
        mContext.startService(i);

        return id;
    }

    public static class Request {
        public static final int NETWORK_MOBILE = 1;
        public static final int NETWORK_WIFI = 2;

        /**
         * This download is visible but only shows in the notifications
         * while it's in progress.
         */
        public static final int VISIBILITY_VISIBLE = 0;

        /**
         * This download is visible and shows in the notifications while
         * in progress and after completion.
         */
        public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;

        /**
         * This download doesn't show in the UI or in the notifications.
         */
        public static final int VISIBILITY_HIDDEN = 2;

        /**
         * This download shows in the notifications after completion ONLY.
         * It is usuable only with
         * {@link DownloadManager#addCompletedDownload(String, String,
         * boolean, String, String, long, boolean)}.
         */
        public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;

        /** can take any of the following values: {@link #VISIBILITY_HIDDEN}
         * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}, {@link #VISIBILITY_VISIBLE},
         * {@link #VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION}
         */
        private int mNotificationVisibility = VISIBILITY_VISIBLE;

        private Uri mUri;
        private Uri mDestinationUri;
        private List<Pair<String, String>> mRequestHeaders = new ArrayList<>();
        private CharSequence mTitle;
        private CharSequence mDescription;
        private String mMimeType; // TODO: 16/8/11
        // ~ 位反 ~00110011 11001100
        private int mAllowedNetworkTypes = ~0; // default to all network types allowed

        private boolean mMeteredAllowed = true; // TODO: 16/8/11

        private long mDownloadId;

        /**
         * @param uri the HTTP or HTTPS URI to download.
         */
        public Request(Uri uri) {
            if (uri == null) {
                throw new NullPointerException();
            }
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
            }
            mUri = uri;
        }

        /**
         * 继续之前的下载
         * @param downloadId
         */
        public Request(long downloadId) {
            mDownloadId = downloadId;
        }

        Request(String uriString) {
            mUri = Uri.parse(uriString);
        }

        /**
         * Set the local destination for the downloaded file. Must be a file URI to a path on
         * external storage, and the calling application must have the WRITE_EXTERNAL_STORAGE
         * permission.
         * By default, downloads are saved to a generated filename in the shared download cache and
         * may be deleted by the system at any time to reclaim space.
         *
         * @return this object
         */
        public Request setDestinationUri(Uri uri) {
            mDestinationUri = uri;
            return this;
        }

        /**
         * Set the local destination for the downloaded file to a path within
         * the application's external files directory (as returned by
         * {@link Context#getExternalFilesDir(String)}.
         * <p>
         * @param context the {@link Context} to use in determining the external
         *            files directory
         * @param dirType the directory type to pass to
         *            {@link Context#getExternalFilesDir(String)}
         * @param subPath the path within the external directory, including the
         *            destination filename
         * @return this object
         * @throws IllegalStateException If the external storage directory
         *             cannot be found or created.
         */
        public Request setDestinationInExternalFilesDir(Context context, String dirType,
                                                                        String subPath) {
            final File file = context.getExternalFilesDir(dirType);
            if (file == null) {
                throw new IllegalStateException("Failed to get external storage files directory");
            } else if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new IllegalStateException(file.getAbsolutePath() +
                            " already exists and is not a directory");
                }
            } else {
                if (!file.mkdirs()) {
                    throw new IllegalStateException("Unable to create directory: "+
                            file.getAbsolutePath());
                }
            }
            setDestinationFromBase(file, subPath);
           return this;
        }

        private void setDestinationFromBase(File base, String subPath) {
            if (subPath == null) {
                throw new NullPointerException("subPath cannot be null");
            }
            mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
        }

        /**
         * Add an HTTP header to be included with the download request.  The header will be added to
         * the end of the list.
         * @param header HTTP header name
         * @param value header value
         * @return this object
         * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">HTTP/1.1
         *      Message Headers</a>
         */
        public Request addRequestHeader(String header, String value) {
            if (header == null) {
                throw new NullPointerException("header cannot be null");
            }
            if (header.contains(":")) {
                throw new IllegalArgumentException("header may not contain ':'");
            }
            if (value == null) {
                value = "";
            }
            mRequestHeaders.add(Pair.create(header, value));
            return this;
        }

        /**
         * Set the title of this download, to be displayed in notifications (if enabled).  If no
         * title is given, a default one will be assigned based on the download filename, once the
         * download starts.
         * @return this object
         */
        public Request setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Set a description of this download, to be displayed in notifications (if enabled)
         * @return this object
         * todo
         */
        public Request setDescription(CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Control whether a system notification is posted by the download manager while this
         * download is running or when it is completed.
         * If enabled, the download manager posts notifications about downloads
         * through the system {@link android.app.NotificationManager}.
         * By default, a notification is shown only when the download is in progress.
         *<p>
         * It can take the following values: {@link #VISIBILITY_HIDDEN},
         * {@link #VISIBILITY_VISIBLE},
         * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}.
         *<p>
         * If set to {@link #VISIBILITY_HIDDEN}, this requires the permission
         * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
         *
         * @param visibility the visibility setting value
         * @return this object
         */
        public Request setNotificationVisibility(int visibility) {
            mNotificationVisibility = visibility;
            return this;
        }

        /**
         * Restrict the types of networks over which this download may proceed.
         *
         * @param flags any combination of the NETWORK_* bit flags.
         * @return this object
         */
        public Request setAllowedNetworkTypes(int flags) {
            mAllowedNetworkTypes = flags;
            return this;
        }

        /**
         * Set whether this download may proceed over a metered network
         * connection. By default, metered networks are allowed.
         *
         * @see ConnectivityManager#isActiveNetworkMetered()
         * todo 流量统计?
         */
        public Request setAllowedOverMetered(boolean allow) {
            mMeteredAllowed = allow;
            return this;
        }

        public long getDwonloadId() {
            return mDownloadId;
        }

        /**
         * @return ContentValues to be passed to DownloadProvider.insert()
         */
        ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            assert mUri != null;
            values.put(COLUMN_URI, mUri.toString());

            if (mDestinationUri != null) {
//                values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_FILE_URI);
//                values.put(Downloads.Impl.COLUMN_FILE_NAME_HINT, mDestinationUri.toString());
                values.put(COLUMN_LOCAL_URI, mDestinationUri.toString());
            } else {
                values.put(COLUMN_LOCAL_URI, Environment.getExternalStorageDirectory()
                        + "/Download/" + mUri.getLastPathSegment());
            }
            // is the file supposed to be media-scannable?
//            values.put(Downloads.Impl.COLUMN_MEDIA_SCANNED, (mScannable) ? SCANNABLE_VALUE_YES :
//                    SCANNABLE_VALUE_NO);

            if (!mRequestHeaders.isEmpty()) {
//                encodeHttpHeaders(values);
            }

            if (mTitle != null) {
                values.put(COLUMN_TITLE, mTitle.toString());
            } else {
                values.put(COLUMN_TITLE, mUri.getLastPathSegment());
         }

            values.put(COLUMN_VISIBILITY, mNotificationVisibility);
            values.put(COLUMN_ALLOWED_NETWORK_TYPES, mAllowedNetworkTypes);

            values.put(COLUMN_STATUS, STATUS_PENDING);

//            putIfNonNull(values, Downloads.Impl.COLUMN_TITLE, mTitle);
//            putIfNonNull(values, Downloads.Impl.COLUMN_DESCRIPTION, mDescription);
//            putIfNonNull(values, Downloads.Impl.COLUMN_MIME_TYPE, mMimeType);

//            values.put(Downloads.Impl.COLUMN_VISIBILITY, mNotificationVisibility);
//            values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, mAllowedNetworkTypes);
//            values.put(Downloads.Impl.COLUMN_ALLOW_ROAMING, mRoamingAllowed);
//            values.put(Downloads.Impl.COLUMN_ALLOW_METERED, mMeteredAllowed);
//            values.put(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, mIsVisibleInDownloadsUi);

            return values;

        }
    }
}