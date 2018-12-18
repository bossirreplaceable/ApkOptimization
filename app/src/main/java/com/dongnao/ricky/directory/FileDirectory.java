package com.dongnao.ricky.directory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FileDirectory extends AbstractDirectory
{
  private File mDir;

  public FileDirectory(String dir)
    throws com.dongnao.ricky.directory.DirectoryException
  {
    this(new File(dir));
  }

  public FileDirectory(File dir) throws com.dongnao.ricky.directory.DirectoryException
  {
    if (!(dir.isDirectory()))
      throw new com.dongnao.ricky.directory.DirectoryException("file must be a directory: " + dir);

    this.mDir = dir;
  }

  protected AbstractDirectory createDirLocal(String name)
  {
    File dir = new File(generatePath(name));
    dir.mkdir();
    try {
		return new FileDirectory(dir);
	} catch (DirectoryException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    return null;
  }

  protected InputStream getFileInputLocal(String name)
  {
    try {
      return new FileInputStream(generatePath(name));
    } catch (FileNotFoundException e) {
    }
    return null;
  }

  protected OutputStream getFileOutputLocal(String name)
  {
      try {
		return new FileOutputStream(generatePath(name));
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      return null;
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
    new File(generatePath(name)).delete();
  }

  private String generatePath(String name) {
    return getDir().getPath() + '/' + name;
  }

  private void loadAll() {
    this.mFiles = new LinkedHashSet();
    this.mDirs = new LinkedHashMap();

    File[] files = getDir().listFiles();
    for (int i = 0; i < files.length; ++i) {
      File file = files[i];
      if (file.isFile())
        this.mFiles.add(file.getName());
      else
        try
        {
          this.mDirs.put(file.getName(), new FileDirectory(file));
        } catch (DirectoryException localDirectoryException) {
        }
    }
  }

  private File getDir() {
    return this.mDir;
  }
}