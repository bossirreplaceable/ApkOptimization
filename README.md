# 安装包的大小优化：
### 实现功能：
##### 1、将打包的apk中的资源文件的文件名简化成a
        如：
##### 2、将简化后的apk再进行7zip极限压缩
##### 3、将简化后的apk进行7zip极限压缩和align


#### align简介
    Android系统中Application的数据都保存在它的APK文件中，同时可以被多个进程访问，安装的过程包括如下几个步骤：  
         Installer通过每个apk的manifest文件获取与当前应用程序相关联的permissions信息  
         Home application读取当前APK的Name和Icon等信息。  
         System server将读取一些与Application运行相关信息，例如：获取和处理Application的notifications请求等。  
         最后，APK所包含的内容不仅限于当前Application所使用，而且可以被其它的Application调用，提高系统资源的可复用性。  

     zipalign优化的最根本目的是帮助操作系统更高效率的根据请求索引资源，将resource-handling code统一将Data structure alignment（数 据结构对齐标准:DSA）限定为4-byte boundaries。  

     手动执行Align优化：  
        利用tools文件夹下的zipalign工具。首先调出cmd命令行，然后执行:zipalign -v 4 source.apk androidres.apk。这个方法不受API Level的限制，可以对任何版本的APK执行Align优化。  
        同时可以利用zipalign工具检查当前APK是否已经执行过Align优化。命令：zipalign -c -v 4 androidres.apk  
