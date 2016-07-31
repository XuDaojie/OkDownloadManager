package io.github.xudaojie.okdownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Created by xdj on 16/7/25.
 */

public class NotificationClickReceiver extends BroadcastReceiver {

    public NotificationClickReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String filePath = intent.getStringExtra("filePath");
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

//        Intent i = new Intent(context, Notification.class);
////        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        Notification notification = new NotificationCompat.Builder(context)
//                .setContentTitle("Title")
//                .setContentText("已暂停,点击继续")
////                    .setProgress(100, 10, false)
//                .setSmallIcon(android.R.drawable.presence_online) // 必须设置
//                .setContentIntent(pendingIntent)
////                .addAction(android.R.mipmap.sym_def_app_icon, "确定", pendingIntent)
//                .build();
//
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.notify(10, notification);
    }
}
