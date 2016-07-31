package io.github.xudaojie.okdownload;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by xdj on 16/7/26.
 */

/**
 * http://blog.csdn.net/sbsujjbcy/article/details/48194701
 */
class ProgressResponseBody extends ResponseBody {

    private BufferedSource mBufferedSource;

    private ResponseBody mOriginResponseBody;
    private ProgressListener mProgressListener;

    public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
        this.mOriginResponseBody = responseBody;
        this.mProgressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return mOriginResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mOriginResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (mBufferedSource == null) {
            mBufferedSource = Okio.buffer(source(mOriginResponseBody.source()));
        }
        return mBufferedSource;
    }

    /**
     * 读取，回调进度接口
     *
     * @param source Source
     * @return Source
     */
    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                // 增加当前读取的字节数，如果读取完成了bytesRead会返回-1
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                // 回调，如果contentLength()不知道长度，会返回-1
                mProgressListener.update(totalBytesRead, mOriginResponseBody.contentLength(), bytesRead == -1);
                return bytesRead;
            }
        };
    }

    interface ProgressListener {
        /**
         * @param bytesRead     已下载字节数
         * @param contentLength 总字节数
         * @param done          是否下载完成
         */
        void update(long bytesRead, long contentLength, boolean done);
    }
}


