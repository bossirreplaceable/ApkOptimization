package com.dongnao.ricky.androlib;

import com.dongnao.ricky.resourceproguard.Main;
import com.dongnao.ricky.util.FileOperation;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ResourceRepackage
{
  private Main mClient;
  private File mSignedApk;
  private File mSignedWith7ZipApk;
  private File mAlignedWith7ZipApk;
  private File m7zipOutPutDir;
  private File mStoredOutPutDir;
  private String mApkName;
  private File mOutDir;

  public ResourceRepackage(Main m, File signedFile)
  {
    this.mClient = m;
    this.mSignedApk = signedFile;
  }

  public void setOutDir(File outDir) {
    this.mOutDir = outDir;
  }

  public void repackageApk() throws IOException, InterruptedException {
    insureFileName();

    repackageWith7z();
    alignApk();
    deleteUnusedFiles();
  }

  private void deleteUnusedFiles()
  {
    FileOperation.deleteDir(this.m7zipOutPutDir);
    FileOperation.deleteDir(this.mStoredOutPutDir);
    if (this.mSignedWith7ZipApk.exists())
      this.mSignedWith7ZipApk.delete();
  }

  private void insureFileName()
    throws IOException
  {
    if (!(this.mSignedApk.exists())) {
      throw new IOException(String.format(
        "can not found the signed apk file to repackage, path=%s", new Object[] { 
        this.mSignedApk.getAbsolutePath() }));
    }

    String apkBasename = this.mSignedApk.getName();
    this.mApkName = apkBasename.substring(0, apkBasename.indexOf(".apk"));

    if (this.mOutDir == null) {
      this.mOutDir = new File(this.mSignedApk.getAbsoluteFile().getParent() + File.separator + this.mApkName);
    }

    this.mSignedWith7ZipApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_channel_7zip.apk");
    this.mAlignedWith7ZipApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_channel_7zip_aligned.apk");

    this.m7zipOutPutDir = new File(this.mOutDir.getAbsolutePath() + File.separator + "out_7zip");
    this.mStoredOutPutDir = new File(this.mOutDir.getAbsolutePath() + File.separator + "storefiles");

    FileOperation.deleteDir(this.m7zipOutPutDir);
    FileOperation.deleteDir(this.mStoredOutPutDir);
    FileOperation.deleteDir(this.mSignedWith7ZipApk);
    FileOperation.deleteDir(this.mAlignedWith7ZipApk);
  }

  private void repackageWith7z()
    throws IOException, InterruptedException
  {
    System.out.printf("use 7zip to repackage: %s, will cost much more time\n", new Object[] { this.mSignedWith7ZipApk.getName() });
    HashMap compressData = FileOperation.unZipAPk(this.mSignedApk.getAbsolutePath(), this.m7zipOutPutDir.getAbsolutePath());

    generalRaw7zip();

    ArrayList storedFiles = new ArrayList();

    for (Iterator localIterator = compressData.keySet().iterator(); localIterator.hasNext(); ) { String name = (String)localIterator.next();
      File file = new File(this.m7zipOutPutDir.getAbsolutePath() + File.separator + name);
      if (!(file.exists())) {
        break;
      }

      int method = ((Integer)compressData.get(name)).intValue();

      if (method != 0) break;
      storedFiles.add(name);
    }

    addStoredFileIn7Zip(storedFiles);

    if (!(this.mSignedWith7ZipApk.exists())) {
      throw new IOException(String.format(
        "7z repackage signed apk fail,you must install 7z command line version first, linux: p7zip, window: 7za, path=%s", new Object[] { 
        this.mSignedWith7ZipApk.getAbsolutePath() }));
    }

    System.out.printf("use 7zip to repackage %s done, time cost from begin: %fs\n", new Object[] { this.mSignedWith7ZipApk.getName(), 
      Double.valueOf(this.mClient.diffTimeFromBegin()) });
  }

  private void generalRaw7zip() throws IOException, InterruptedException
  {
    String line;
    System.out.printf("general the raw 7zip file\n", new Object[0]);
    Process pro = null;
    String outPath = this.m7zipOutPutDir.getAbsoluteFile().getAbsolutePath();

    String path = outPath + File.separator + "*";
    String cmd;
    if (this.mClient.get7zipPath() == null)
    	cmd = "7za";
    else
    	cmd = this.mClient.get7zipPath();

     cmd = cmd + " a -tzip " + this.mSignedWith7ZipApk.getAbsolutePath() + " " + path + " -mx9";

    pro = Runtime.getRuntime().exec(cmd);

    InputStreamReader ir = null;
    LineNumberReader input = null;

    ir = new InputStreamReader(pro.getInputStream());

    input = new LineNumberReader(ir);
    do;while ((line = input.readLine()) != null);

    if (pro != null) {
      pro.waitFor();
      pro.destroy();
    }
  }

  private void addStoredFileIn7Zip(ArrayList<String> storedFiles) throws IOException, InterruptedException {
    String line;
    System.out.printf("rewrite the stored file into the 7zip, file count:%d\n", new Object[] { Integer.valueOf(storedFiles.size()) });
    String storedParentName = this.mStoredOutPutDir.getAbsolutePath() + File.separator;
    String outputName = this.m7zipOutPutDir.getAbsolutePath() + File.separator;
    for (Iterator localIterator = storedFiles.iterator(); localIterator.hasNext(); ) { String name = (String)localIterator.next();
      FileOperation.copyFileUsingStream(new File(outputName + name), new File(storedParentName + name));
    }

    Process pro = null;

    storedParentName = storedParentName + File.separator + "*";
    String cmd;
    if (this.mClient.get7zipPath() == null)
    	cmd = "7za";
    else
    	cmd = this.mClient.get7zipPath();

     cmd = cmd + " a -tzip " + this.mSignedWith7ZipApk.getAbsolutePath() + " " + storedParentName + " -mx0";

    pro = Runtime.getRuntime().exec(cmd);

    InputStreamReader ir = null;
    LineNumberReader input = null;

    ir = new InputStreamReader(pro.getInputStream());

    input = new LineNumberReader(ir);
    do;while ((line = input.readLine()) != null);

    if (pro != null) {
      pro.waitFor();
      pro.destroy();
    }
  }

  private void alignApk() throws IOException, InterruptedException
  {
    if (this.mSignedWith7ZipApk.exists())
    {
      alignApk(this.mSignedWith7ZipApk, this.mAlignedWith7ZipApk);
    }
  }

  private void alignApk(File before, File after) throws IOException, InterruptedException {
    System.out.printf("zipaligning apk: %s\n", new Object[] { before.getName() });
    if (!(before.exists())) {
      throw new IOException(String.format(
        "can not found the raw apk file to zipalign, path=%s", new Object[] { 
        before.getAbsolutePath() }));
    }
    String cmd;
    if (this.mClient.getZipalignPath() == null)
      cmd = "zipalign";
    else
      cmd = this.mClient.getZipalignPath();

     cmd = cmd + " 4 " + before.getAbsolutePath() + " " + after.getAbsolutePath();

    Process pro = Runtime.getRuntime().exec(cmd);

    pro.waitFor();
    pro.destroy();

    System.out.printf("zipaligning apk %s done, time cost from begin: %fs\n", new Object[] { after.getName(), 
      Double.valueOf(this.mClient.diffTimeFromBegin()) });
  }
}