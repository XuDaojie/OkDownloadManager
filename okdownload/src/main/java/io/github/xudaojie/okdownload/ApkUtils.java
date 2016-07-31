package io.github.xudaojie.okdownload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Created by xdj on 16/8/1.
 */

public class ApkUtils {
    public static final void install(Context context, String apkPath) {
        String filePath = apkPath;
        //如果文件名不为空，说明已经存在了，然后获取uri，进行安装
        File path = new File(filePath);
        if(!path.exists()){
            return;
        }
        Uri uri = Uri.fromFile(path);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri,"application/vnd.android.package-archive");
        // 执行意图进行安装
        context.startActivity(install);
    }
}
