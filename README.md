# Agora-Dynamic-Loading-Sample-App-Android
示例项目演示动态加载SO，包含以下功能：
1. 下载、解压Zip文件；
2. 加载libagora-rtc-sdk-jni.so；
3. 测试是否加载成功；

运行该项目需要如下准备：
从Agora官网上下载所需数据
  ```
  [Agora官网](https://www.agora.io/cn/)
  ```
在服务器上创建zip包：
xxx.zip包结构如下：
  ```
  -xxx.zip
    -armeabi-v7a
      -libagora-rtc-sdk-jni.so
    -x86
      -libagora-rtc-sdk-jni.so
    -arm64-v8a
      -libagora-rtc-sdk-jni.so
  ```
在app/libs中加入测试需要的：
  ```
  agora-rtc-sdk.jar  
 ```
在agora.io.dynamicload.MainActivity中修改：
  ```
  // 修改下载文件链接
  // 例如：http://172.16.0.225:8000/AndroidStudioProjects/libs.zip
  mDownloadUrl = http://xxx/zipFIleName.zip;
  
  // 修改下载文件保存地址
  // 例如：Environment.getExternalStorageDirectory().getAbsolutePath() + "/123";
  mSaveUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourPathName";
  
  // 修改解压文件地址 
  // 例如：Environment.getExternalStorageDirectory().getAbsolutePath() + "/123/libs";
  mDepressUrl = mSaveUrl + File.separator + zipfileName;
  ```
  
  
###WARNING
该开源项目未做下载文件完整性校验，未适配阿里dalvik.system.LexClassLoader。

Agora提供全球领先的音视频解决方案
Agora 视频 SDK 支持 iOS / Android / Windows / macOS 等多个平台，你可以在https://github.com/AgoraIO/查看对应各平台的示例项目：
Agora-Android-Tutorial-1to1
Agora-iOS-Tutorial-Swift-1to1
Agora-Windows-Tutorial-1to1
Agora-macOS-Tutorial-Swift-1to1

运行环境
Android Studio 2.0 +
真实 Android 设备
部分模拟器会存在问题，所以推荐使用真机

###联系我们
如果在集成中遇到问题, 你可以到 开发者社区 提问
如果有售前咨询问题, 可以拨打 400 632 6626，或加入官方Q群 12742516 提问
如果需要售后技术支持, 你可以在 Agora Dashboard 提交工单
如果发现了示例代码的 bug, 欢迎提交 issue
代码许可
The MIT License (MIT).
