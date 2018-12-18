package com.dongnao.ricky.directory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
//zip文件专用封装类，包含zip文件本身和文件路径
public class ZipRODirectory extends AbstractDirectory
{
  private ZipFile mZipFile;
  private String mPath;

  public ZipRODirectory(String zipFileName)
    throws DirectoryException
  {
    this(zipFileName, "");
  }

  public ZipRODirectory(File zipFile) throws DirectoryException {
    this(zipFile, "");
  }

  public ZipRODirectory(ZipFile zipFile) {
    this(zipFile, "");
  }

  public ZipRODirectory(String zipFileName, String path) throws DirectoryException
  {
    this(new File(zipFileName), path);
  }

  public ZipRODirectory(File zipFile, String path) throws DirectoryException
  {
    try {
      this.mZipFile = new ZipFile(zipFile);
    } catch (IOException e) {
      throw new DirectoryException(e);
    }
    this.mPath = path;
  }

  public ZipRODirectory(ZipFile zipFile, String path)
  {
    this.mZipFile = zipFile;
    this.mPath = path;
  }

  protected AbstractDirectory createDirLocal(String name)
    throws DirectoryException
  {
    throw new UnsupportedOperationException();
  }

  protected InputStream getFileInputLocal(String name) throws DirectoryException
  {
    try
    {
      return getZipFile().getInputStream(new ZipEntry(getPath() + name));
    } catch (IOException e) {
      throw new PathNotExist(name, e);
    }
  }

  protected OutputStream getFileOutputLocal(String name)
    throws DirectoryException
  {
    throw new UnsupportedOperationException();
  }

  protected void loadDirs()
  {
    loadAll();
  }

  protected void loadFiles()
  {
    loadAll();
  }

  protected void removeFileLocal(String name)
  {
    throw new UnsupportedOperationException();
  }

  private void loadAll() {
    this.mFiles = new LinkedHashSet();
    this.mDirs = new LinkedHashMap();

    int prefixLen = getPath().length();
    Enumeration entries = getZipFile().entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry)entries.nextElement();
      String name = entry.getName();

      if (!(name.equals(getPath()))) { if (!(name.startsWith(getPath()))) {
          continue;
        }

        String subname = name.substring(prefixLen);

        int pos = subname.indexOf(47);
        if (pos == -1) {
          if (!(entry.isDirectory()))
            this.mFiles.add(subname);
        }
        else
        {
          subname = subname.substring(0, pos);

          if (!(this.mDirs.containsKey(subname))) {
            AbstractDirectory dir = new ZipRODirectory(getZipFile(), getPath() + subname + '/');
            this.mDirs.put(subname, dir); } }
      }
    }
  }

  private String getPath() {
    return this.mPath;
  }

  private ZipFile getZipFile() {
    return this.mZipFile;
  }
}