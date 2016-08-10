package io.github.xudaojie.okdownloadmanager.sample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import io.github.xudaojie.okdownload.OkDownloadManager;

public class MainActivity extends AppCompatActivity {

    private static final int EXTERNAL_STORAGE_REQ_CODE = 0;

    private Activity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button downloadBtn = (Button) findViewById(R.id.download_btn);

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 判断是否已获得权限
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 权限申请曾被拒绝,给用户提示
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            mContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE))  {
                        Toast.makeText(mContext, "请在设置中打开权限", Toast.LENGTH_SHORT).show();
                    } else {
                        ActivityCompat.requestPermissions(mContext,
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                EXTERNAL_STORAGE_REQ_CODE);
                    }
                } else {
                    // 5M
//                    String url = "http://pkg3.fir.im/71da3de01a28cff3f9884ada102e22fdbadaab35.apk?attname=app-release.apk_1.0.apk";
                    // 33M
                    String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";

                    OkDownloadManager.download(mContext, "Download", url, "test.apk");
                }

            }
        });
    }
}
