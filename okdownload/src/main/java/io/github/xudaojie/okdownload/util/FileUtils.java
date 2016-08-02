package io.github.xudaojie.okdownload.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by xdj on 16/8/1.
 */

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static final void installApk(Context context, String apkPath) {
        String filePath = apkPath;
        //如果文件名不为空，说明已经存在了，然后获取uri，进行安装
        File path = new File(filePath);
        if(!path.exists()){
            Log.e(TAG, "APK is not found");
            return;
        }
        Uri uri = Uri.fromFile(path);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri,"application/vnd.android.package-archive");
        // 执行意图进行安装
        context.startActivity(install);
    }

    /**
     * 将文件保存到本地
     * @param outPath
     * @param stream
     * @throws IOException
     */
    public static void save(String outPath, InputStream stream) throws IOException {
        File file = new File(outPath);
        int len;
        byte[] buf = new byte[2048];

//        InputStream stream = response.body().byteStream();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            while ((len = stream.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } finally {
            fos.flush();
            fos.close();
            stream.close();
        }
    }

    /**
     * 检查文件名是否已存在 在则重命名(1)(2)
     * @param index 如果为0则不显示 xxx(1).apk xxx(2).apk
     */
    public static String checkOrCreateFileName(String filePath, int index) {
        String filePathNoType = filePath.substring(0, filePath.lastIndexOf('.'));
        String fileType = filePath.substring(filePath.lastIndexOf('.'), filePath.length());

        String finalFilePath = filePath;
        if (index != 0) {
            finalFilePath = filePathNoType + "(" + index + ")" + fileType;
        }

        File file = new File(finalFilePath);
        if(!file.exists()) {
            return finalFilePath;
        }

        return checkOrCreateFileName(filePath, ++index);
    }
}
