package com.dongnao.ricky.androlib.res.util;

import java.io.File;
import java.net.URI;

import com.dongnao.ricky.directory.Directory;
import com.dongnao.ricky.directory.DirectoryException;
import com.dongnao.ricky.directory.FileDirectory;
import com.dongnao.ricky.directory.ZipRODirectory;

public class ExtFile extends File
{
  private Directory mDirectory;

  public ExtFile(File file)
  {
    super(file.getPath());
  }

  public ExtFile(URI uri) {
    super(uri);
  }

  public ExtFile(File parent, String child) {
    super(parent, child);
  }

  public ExtFile(String parent, String child) {
    super(parent, child);
  }

  public ExtFile(String pathname) {
    super(pathname);
  }

  public Directory getDirectory() throws DirectoryException {
    if (this.mDirectory == null)
      if (isDirectory())
        this.mDirectory = new FileDirectory(this);
      else
        this.mDirectory = new ZipRODirectory(this);


    return this.mDirectory;
  }
}