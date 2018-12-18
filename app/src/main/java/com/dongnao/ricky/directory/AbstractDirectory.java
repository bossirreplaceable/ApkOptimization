package com.dongnao.ricky.directory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class AbstractDirectory
  implements Directory
{
  protected Set<String> mFiles;
  protected Map<String, AbstractDirectory> mDirs;

  public Set<String> getFiles()
  {
    return getFiles(false);
  }

  public Set<String> getFiles(boolean recursive)
  {
    if (this.mFiles == null)
      loadFiles();

    if (!(recursive)) {
      return this.mFiles;
    }

    Set files = new LinkedHashSet(this.mFiles);
    Iterator localIterator1 = getAbstractDirs().entrySet().iterator(); while (localIterator1.hasNext()) { Entry dir = (Entry)localIterator1.next();
      for (Iterator localIterator2 = ((Directory)dir.getValue()).getFiles(true).iterator(); localIterator2.hasNext(); ) { String path = (String)localIterator2.next();
        files.add(((String)dir.getKey()) + '/' + path);
      }
    }
    return files;
  }

  public boolean containsFile(String path)
  {
    SubPath subpath;
    try {
      subpath = getSubPath(path);
    } catch (PathNotExist e) {
      return false;
    }

    if (subpath.dir != null)
      return subpath.dir.containsFile(subpath.path);

    return getFiles().contains(subpath.path);
  }

  public boolean containsDir(String path)
  {
    SubPath subpath;
    try {
      subpath = getSubPath(path);
    } catch (PathNotExist e) {
      return false;
    }

    if (subpath.dir != null)
      return subpath.dir.containsDir(subpath.path);

    return getAbstractDirs().containsKey(subpath.path);
  }

  public Map<String, Directory> getDirs()
    throws UnsupportedOperationException
  {
    return getDirs(false);
  }

  public Map<String, Directory> getDirs(boolean recursive)
    throws UnsupportedOperationException
  {
    return new LinkedHashMap(getAbstractDirs(recursive));
  }

  public InputStream getFileInput(String path) throws com.dongnao.ricky.directory.DirectoryException
  {
    SubPath subpath = getSubPath(path);
    if (subpath.dir != null)
      return subpath.dir.getFileInput(subpath.path);

    if (!(getFiles().contains(subpath.path)))
      throw new PathNotExist(path);

    return getFileInputLocal(subpath.path);
  }

  public OutputStream getFileOutput(String path) throws com.dongnao.ricky.directory.DirectoryException
  {
    Directory dir;
    ParsedPath parsed = parsePath(path);
    if (parsed.dir == null) {
      getFiles().add(parsed.subpath);
      return getFileOutputLocal(parsed.subpath);
    }

    try
    {
      dir = createDir(parsed.dir);
    } catch (PathAlreadyExists e) {
      dir = (Directory)getAbstractDirs().get(parsed.dir);
    }
    return dir.getFileOutput(parsed.subpath);
  }

  public Directory getDir(String path) throws PathNotExist
  {
    SubPath subpath = getSubPath(path);
    if (subpath.dir != null)
      return subpath.dir.getDir(subpath.path);

    if (!(getAbstractDirs().containsKey(subpath.path)))
      throw new PathNotExist(path);

    return ((Directory)getAbstractDirs().get(subpath.path));
  }

  public Directory createDir(String path) throws com.dongnao.ricky.directory.DirectoryException
  {
    AbstractDirectory dir;
    ParsedPath parsed = parsePath(path);

    if (parsed.dir == null) {
      if (getAbstractDirs().containsKey(parsed.subpath))
        throw new PathAlreadyExists(path);

      dir = createDirLocal(parsed.subpath);
      getAbstractDirs().put(parsed.subpath, dir);
      return dir;
    }

    if (getAbstractDirs().containsKey(parsed.dir)) {
      dir = (AbstractDirectory)getAbstractDirs().get(parsed.dir);
    } else {
      dir = createDirLocal(parsed.dir);
      getAbstractDirs().put(parsed.dir, dir);
    }
    return dir.createDir(parsed.subpath);
  }

  public boolean removeFile(String path)
  {
    SubPath subpath;
    try {
      subpath = getSubPath(path);
    } catch (PathNotExist e) {
      return false;
    }

    if (subpath.dir != null)
      return subpath.dir.removeFile(subpath.path);

    if (!(getFiles().contains(subpath.path)))
      return false;

    removeFileLocal(subpath.path);
    getFiles().remove(subpath.path);
    return true;
  }

  protected Map<String, AbstractDirectory> getAbstractDirs() {
    return getAbstractDirs(false);
  }

  protected Map<String, AbstractDirectory> getAbstractDirs(boolean recursive) {
    if (this.mDirs == null)
      loadDirs();

    if (!(recursive)) {
      return this.mDirs;
    }

    Map dirs = new LinkedHashMap(this.mDirs);
    Iterator localIterator1 = getAbstractDirs().entrySet().iterator(); while (localIterator1.hasNext()) { Entry dir = (Entry)localIterator1.next();

      Iterator localIterator2 = ((AbstractDirectory)dir.getValue()).getAbstractDirs(
        true).entrySet
        ().iterator();

      while (localIterator2.hasNext()) {
        Entry subdir = (Entry)localIterator2.next();
        dirs.put(((String)dir.getKey()) + '/' + ((String)subdir.getKey()), 
          (AbstractDirectory)subdir.getValue());
      }
    }
    return dirs;
  }

  private SubPath getSubPath(String path) throws PathNotExist {
    ParsedPath parsed = parsePath(path);
    if (parsed.dir == null)
      return new SubPath(null, parsed.subpath);

    if (!(getAbstractDirs().containsKey(parsed.dir)))
      throw new PathNotExist(path);

    return new SubPath((AbstractDirectory)getAbstractDirs().get(parsed.dir), parsed.subpath);
  }

  private ParsedPath parsePath(String path) {
    int pos = path.indexOf(47);
    if (pos == -1)
      return new ParsedPath(null, path);

    return new ParsedPath(path.substring(0, pos), path.substring(pos + 1)); }

  protected abstract void loadFiles();

  protected abstract void loadDirs();

  protected abstract InputStream getFileInputLocal(String paramString) throws com.dongnao.ricky.directory.DirectoryException;

  protected abstract OutputStream getFileOutputLocal(String paramString) throws com.dongnao.ricky.directory.DirectoryException;

  protected abstract AbstractDirectory createDirLocal(String paramString) throws com.dongnao.ricky.directory.DirectoryException;

  protected abstract void removeFileLocal(String paramString);

  private class ParsedPath {
    public String dir;
    public String subpath;

    public ParsedPath(String paramString1, String paramString2) { 
      this.dir = paramString1;
      this.subpath = paramString2;
    }
  }

  private class SubPath {
    public AbstractDirectory dir;
    public String path;

    public SubPath(AbstractDirectory paramAbstractDirectory2, String paramString) {
      this.dir = paramAbstractDirectory2;
      this.path = paramString;
    }
  }
}
