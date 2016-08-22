OkDownloadManager
===
利用OkHttp实现的更新客户端的工具类

## Using
```xml
OkDownloadManager.download(mContext, "title", url, "test.apk");
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
    compile 'com.github.XuDaojie:OkDownloadManager:v0.2.0'
}
```

## TODO
断点续传<br>
更新检查策略<br>