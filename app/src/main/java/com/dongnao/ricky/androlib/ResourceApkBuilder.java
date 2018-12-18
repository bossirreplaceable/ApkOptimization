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
import java.util.List;
import java.util.Set;

public class ResourceApkBuilder
{
  private Main mClient;
  private File mOutDir;
  private File m7zipOutPutDir;
  private File mUnSignedApk;
  private File mSignedApk;
  private File mSignedWith7ZipApk;
  private File mAlignedApk;
  private File mAlignedWith7ZipApk;
  private String mApkName;

  public ResourceApkBuilder(Main m)
  {
    this.mClient = m;
  }

  public void setOutDir(File outDir, String apkname)
    throws AndrolibException
  {
    this.mOutDir = outDir;
    this.mApkName = apkname;
  }

  public void buildApk(HashMap<String, Integer> compressData) throws IOException, InterruptedException {
    System.out.println("building apk");
	insureFileName();
    generalUnsignApk(compressData);
    signApk();
    use7zApk(compressData);
    alignApk();
  }

  private void insureFileName()
  {
    this.mUnSignedApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_unsigned.apk");

    this.mSignedWith7ZipApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_signed_7zip.apk");
    this.mSignedApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_signed.apk");
    this.mAlignedApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_signed_aligned.apk");
    this.mAlignedWith7ZipApk = new File(this.mOutDir.getAbsolutePath() + File.separator + this.mApkName + "_signed_7zip_aligned.apk");

    this.m7zipOutPutDir = new File(this.mOutDir.getAbsolutePath() + File.separator + "out_7zip");
  }

  private void use7zApk(HashMap<String, Integer> compressData) throws IOException, InterruptedException
  {
    if (!(this.mClient.isUse7zip())) {
      return;
    }

    if (!(this.mClient.isUseSignAPk())) {
      throw new IOException(
        "if you want to use 7z, you must set the sign issue to active in the config file first");
    }

    if (!(this.mSignedApk.exists())) {
      throw new IOException(String.format(
        "can not found the signed apk file to 7z, if you want to use 7z, you must fill the sign data in the config file, path=%s", new Object[] { 
        this.mSignedApk.getAbsolutePath() }));
    }

    System.out.printf("use 7zip to repackage: %s, will cost much more time\n", new Object[] { this.mSignedWith7ZipApk.getName() });
    FileOperation.unZipAPk(this.mSignedApk.getAbsolutePath(), this.m7zipOutPutDir.getAbsolutePath());

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

    System.out.printf("use 7zip to repackage %s done, file reduce: %fkb, time cost from begin: %fs\n", new Object[] { this.mSignedWith7ZipApk.getName(), 
      Double.valueOf(this.mClient.diffApkSizeFromRaw(FileOperation.getFileSizes(this.mSignedWith7ZipApk))), Double.valueOf(this.mClient.diffTimeFromBegin()) });
  }

  private void signApk()
    throws IOException, InterruptedException
  {
    if (this.mClient.isUseSignAPk()) {
      System.out.printf("signing apk: %s\n", new Object[] { this.mSignedApk.getName() });

      if (this.mSignedApk.exists()) {
        this.mSignedApk.delete();
      }

      String cmd = "jarsigner -sigalg MD5withRSA -digestalg SHA1 -keystore " + this.mClient.getSignatureFile() + " -storepass " + this.mClient.getStorePass() + " -keypass " + this.mClient.getKeyPass() + 
        " -signedjar " + this.mSignedApk.getAbsolutePath() + " " + this.mUnSignedApk.getAbsolutePath() + " " + this.mClient.getStoreAlias();

      Process pro = Runtime.getRuntime().exec(cmd);

      pro.waitFor();
      pro.destroy();

      if (!(this.mSignedApk.exists())) {
        throw new IOException(String.format(
          "can not found the signed apk file, is the input sign data correct? path=%s", new Object[] { 
          this.mSignedApk.getAbsolutePath() }));
      }

      System.out.printf("sign apk %s done, file reduce: %fkb, time cost from begin: %fs\n", new Object[] { this.mSignedApk.getName(), 
        Double.valueOf(this.mClient.diffApkSizeFromRaw(FileOperation.getFileSizes(this.mSignedApk))), Double.valueOf(this.mClient.diffTimeFromBegin()) });
    }
  }

  private void alignApk()
    throws IOException, InterruptedException
  {
    if (!(this.mClient.isUseSignAPk()))
      return;

    if (this.mSignedWith7ZipApk.exists()) {
      if (this.mSignedApk.exists())
        alignApk(this.mSignedApk, this.mAlignedApk);

      alignApk(this.mSignedWith7ZipApk, this.mAlignedWith7ZipApk);
    } else if (this.mSignedApk.exists()) {
      alignApk(this.mSignedApk, this.mAlignedApk);
    } else {
      throw new IOException(
        "can not found any signed apk file");
    }
  }

  private void alignApk(File before, File after) throws IOException, InterruptedException
  {
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
System.out.println("~~~~~cmd:"+cmd);
    Process pro = Runtime.getRuntime().exec(cmd);

    pro.waitFor();
    pro.destroy();

    if (!(after.exists())) {
      throw new IOException(String.format(
        "can not found the aligned apk file, the ZipAlign path is correct? path=%s", new Object[] { 
        this.mAlignedApk.getAbsolutePath() }));
    }

    System.out.printf("zipaligning apk %s done, file reduce: %fkb, time cost from begin: %fs\n", new Object[] { after.getName(), 
      Double.valueOf(this.mClient.diffApkSizeFromRaw(FileOperation.getFileSizes(after))), Double.valueOf(this.mClient.diffTimeFromBegin()) });
  }

  private void generalUnsignApk(HashMap<String, Integer> compressData) throws IOException, InterruptedException
  {
    File[] arrayOfFile1;
    System.out.printf("general unsigned apk: %s\n", new Object[] { this.mUnSignedApk.getName() });

    File tempOutDir = new File(this.mOutDir.getAbsolutePath() + File.separator + "temp");

    if (!(tempOutDir.exists())) {
      System.err.printf("Missing apk unzip files, path=%s\n", new Object[] { tempOutDir.getAbsolutePath() });
      System.exit(-1);
    }

    File[] unzipFiles = tempOutDir.listFiles();
    List collectFiles = new ArrayList();
    int j = (arrayOfFile1 = unzipFiles).length; for (int i = 0; i < j; ++i) { File f = arrayOfFile1[i];
      String name = f.getName();
      if ((!(name.equals("res"))) && (!(name.equals(this.mClient.getMetaName())))) { if (name.equals("resources.arsc"))
          continue;

        collectFiles.add(f);
      }
    }

    File destResDir = new File(this.mOutDir.getAbsolutePath() + File.separator + "r");

    File rawResDir = new File(tempOutDir.getAbsolutePath() + File.separator + "res");

    if (FileOperation.getlist(destResDir) != FileOperation.getlist(rawResDir)) {
      throw new IOException(String.format(
        "the file count of %s, and the file count of %s is not equal, there must be some problem, please contact shwenzhang for detail\n", new Object[] { 
        rawResDir.getAbsolutePath(), destResDir.getAbsolutePath() }));
    }

    if (!(destResDir.exists())) {
      System.err.printf("Missing res files, path=%s\n", new Object[] { destResDir.getAbsolutePath() });
      System.exit(-1);
    }

    collectFiles.add(destResDir);

    File rawARSCFile = new File(this.mOutDir.getAbsolutePath() + File.separator + "resources.arsc");
    if (!(rawARSCFile.exists())) {
      System.err.printf("Missing resources.arsc files, path=%s\n", new Object[] { rawARSCFile.getAbsolutePath() });
      System.exit(-1);
    }
    collectFiles.add(rawARSCFile);

    FileOperation.zipFiles(collectFiles, this.mUnSignedApk, compressData);

    if (!(this.mUnSignedApk.exists())) {
      throw new IOException(String.format(
        "can not found the unsign apk file path=%s", new Object[] { 
        this.mUnSignedApk.getAbsolutePath() }));
    }

    System.out.printf("general unsigned apk %s done, file reduce: %fkb, time cost from begin: %fs\n", new Object[] { this.mUnSignedApk.getName(), 
      Double.valueOf(this.mClient.diffApkSizeFromRaw(FileOperation.getFileSizes(this.mUnSignedApk))), Double.valueOf(this.mClient.diffTimeFromBegin()) });
  }

  private void addStoredFileIn7Zip(ArrayList<String> storedFiles) throws IOException, InterruptedException
  {
    String line;
    System.out.printf("rewrite the stored file into the 7zip, file count:%d\n", new Object[] { Integer.valueOf(storedFiles.size()) });

    String storedParentName = this.mOutDir.getAbsolutePath() + File.separator + "storefiles" + File.separator;
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

  private void generalRaw7zip() throws IOException, InterruptedException {
    String line;
    System.out.printf("general the raw 7zip file\n", new Object[0]);
    Process pro = null;
    String outPath = this.m7zipOutPutDir.getAbsoluteFile().getAbsolutePath();
    System.out.println("5--------7zip---outPath=" + outPath);
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
}