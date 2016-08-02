OkDownloadManager
===
利用OkHttp实现的更新客户端的工具类

## Using
```xml
Intent i = new Intent(mContext, OkDownloadManager.class);
i.putExtra("id", (int) System.currentTimeMillis());
i.putExtra("url", url);
i.putExtra("title", "TestDownload");
i.putExtra("fileName", "test.apk");
startService(i);
```

## Including in your project
要将**OkDownloadManager**引入你的项目，需要修改你的**build.gradle**

### Add repository 
```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
### Add dependency
```groovy
dependencies {
    compile 'com.github.XuDaojie:OkDownloadManager:v0.1.0'
}
```

## TODO
断点续传