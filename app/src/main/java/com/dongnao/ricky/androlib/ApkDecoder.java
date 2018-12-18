package com.dongnao.ricky.androlib;

//import com.dongnao.mm.androlib.res.data.ResPackage;
//import com.dongnao.mm.androlib.res.decoder.ARSCDecoder;
//import com.dongnao.mm.androlib.res.decoder.RawARSCDecoder;
//import com.dongnao.mm.androlib.res.util.ExtFile;
//import com.dongnao.mm.directory.Directory;
//import com.dongnao.mm.directory.DirectoryException;
//import com.dongnao.mm.resourceproguard.Main;
//import com.dongnao.mm.util.FileOperation;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.dongnao.ricky.androlib.res.data.ResPackage;
import com.dongnao.ricky.androlib.res.decoder.ARSCDecoder;
import com.dongnao.ricky.androlib.res.decoder.RawARSCDecoder;
import com.dongnao.ricky.androlib.res.util.ExtFile;
import com.dongnao.ricky.directory.DirectoryException;
import com.dongnao.ricky.resourceproguard.Main;
import com.dongnao.ricky.util.FileOperation;

public class ApkDecoder
{
  private Main mClient;
  private ExtFile mApkFile;
  private File mOutDir;
  private File mOutTempARSCFile;
  private File mOutARSCFile;
  private File mOutResFile;
  private File mRawResFile;
  private File mOutTempDir;
  private File mResMappingFile;
  private HashMap<String, Integer> mCompressData;

  public ApkDecoder(Main m)
  {
    this.mClient = m;
  }
//判断映射文件是否存在
  public boolean hasResources()
    throws com.dongnao.ricky.androlib.AndrolibException
  {
    try
    {
      return this.mApkFile.getDirectory().containsFile("resources.arsc");
    } catch (DirectoryException ex) {
      throw new com.dongnao.ricky.androlib.AndrolibException(ex);
    }
  }

  public void setApkFile(File apkFile) {
    this.mApkFile = new ExtFile(apkFile);
  }

  public void setOutDir(File outDir) throws com.dongnao.ricky.androlib.AndrolibException {
    this.mOutDir = outDir;
  }

  public void ensureFilePath() throws IOException {
    String destDirectory = this.mOutDir.getAbsolutePath();

    if (this.mOutDir.exists()) {
      FileOperation.deleteDir(this.mOutDir);
      this.mOutDir.mkdirs();
    }
    String unZipDest = destDirectory + File.separator + "temp";
    System.out.printf("unziping apk to %s\n", new Object[] { unZipDest });
    //解压apk里的文件到temp目录
    this.mCompressData = FileOperation.unZipAPk(this.mApkFile.getAbsoluteFile().getAbsolutePath(), unZipDest);
    dealWithCompressConfig();
    //混淆完成后资源文件输出目录
    this.mOutResFile = new File(this.mOutDir.getAbsolutePath() + File.separator + "r");
    //raw文件混淆后输出目录
    this.mRawResFile = new File(this.mOutDir.getAbsoluteFile().getAbsolutePath() + File.separator + "temp" + File.separator + "res");
    this.mOutTempDir = new File(this.mOutDir.getAbsoluteFile().getAbsolutePath() + File.separator + "temp");

    if ((!(this.mRawResFile.exists())) || (!(this.mRawResFile.isDirectory()))) {
      throw new IOException("can not found res dir in the apk or it is not a dir");
    }
    //临时映射文件
    this.mOutTempARSCFile = new File(this.mOutDir.getAbsoluteFile().getAbsolutePath() + File.separator + "resources_temp.arsc");
    //映射文件
    this.mOutARSCFile = new File(this.mOutDir.getAbsoluteFile().getAbsolutePath() + File.separator + "resources.arsc");
    //apk文件名
    String basename = this.mApkFile.getName().substring(0, this.mApkFile.getName().indexOf(".apk"));
    //资源文件映射名
    this.mResMappingFile = 
      new File(this.mOutDir.getAbsoluteFile().getAbsolutePath() + File.separator + 
      "resource_mapping_" + basename + ".txt");
  }

  private void dealWithCompressConfig()
  {
    if (this.mClient.isUseCompress()) {
      HashSet patterns = this.mClient.getCompressPatterns();
      if (!(patterns.isEmpty())) {
        Iterator localIterator1 = this.mCompressData.entrySet().iterator(); while (localIterator1.hasNext()) { Map.Entry entry = (Map.Entry)localIterator1.next();

          String name = (String)entry.getKey();

          for (Iterator it = patterns.iterator(); it.hasNext(); ) {
            Pattern p = (Pattern)it.next();
            if (!(p.matcher(name).matches()))
              break;
            this.mCompressData.put(name, Integer.valueOf(8));
          }
        }
      }
    }
  }

  public Main getClient()
  {
    return this.mClient;
  }

  public HashMap<String, Integer> getCompressData() {
    return this.mCompressData;
  }

  public File getOutDir() {
    return this.mOutDir;
  }

  public File getOutResFile() {
    return this.mOutResFile;
  }

  public File getRawResFile() {
    return this.mRawResFile;
  }

  public File getOutTempARSCFile() {
    return this.mOutTempARSCFile;
  }

  public File getOutARSCFile() {
    return this.mOutARSCFile;
  }

  public File getOutTempDir() {
    return this.mOutTempDir;
  }

  public File getResMappingFile() {
    return this.mResMappingFile;
  }

  public void decode()
    throws com.dongnao.ricky.androlib.AndrolibException, IOException, DirectoryException
  {
    if (hasResources()) {
      ensureFilePath();

      System.out.printf("decoding resources.arsc\n", new Object[0]);
      //读取resources.arsc
      RawARSCDecoder.decode(this.mApkFile.getDirectory().getFileInput("resources.arsc"));
      
      ResPackage[] pkgs = ARSCDecoder.decode(this.mApkFile.getDirectory().getFileInput("resources.arsc"), this);
     //写入数据
      ARSCDecoder.write(this.mApkFile.getDirectory().getFileInput("resources.arsc"), this, pkgs);
    }
  }
}