package com.dongnao.ricky.directory;

public class PathNotExist extends DirectoryException
{
  private static final long serialVersionUID = -6949242015506342032L;

  public PathNotExist()
  {
  }

  public PathNotExist(String detailMessage, Throwable throwable)
  {
    super(detailMessage, throwable);
  }

  public PathNotExist(String detailMessage) {
    super(detailMessage);
  }

  public PathNotExist(Throwable throwable) {
    super(throwable);
  }
}