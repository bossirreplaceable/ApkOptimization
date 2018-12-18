package com.dongnao.ricky.util;

import java.io.DataOutput;
import java.io.IOException;

public class DataOutputDelegate
  implements DataOutput
{
  protected final DataOutput mDelegate;

  public DataOutputDelegate(DataOutput delegate)
  {
    this.mDelegate = delegate;
  }

  public void write(int b)
    throws IOException
  {
    this.mDelegate.write(b);
  }

  public void write(byte[] b)
    throws IOException
  {
    this.mDelegate.write(b);
  }

  public void write(byte[] b, int off, int len)
    throws IOException
  {
    this.mDelegate.write(b, off, len);
  }

  public void writeBoolean(boolean v)
    throws IOException
  {
    this.mDelegate.writeBoolean(v);
  }

  public void writeByte(int v)
    throws IOException
  {
    this.mDelegate.writeByte(v);
  }

  public void writeShort(int v)
    throws IOException
  {
    this.mDelegate.writeShort(v);
  }

  public void writeChar(int v)
    throws IOException
  {
    this.mDelegate.writeChar(v);
  }

  public void writeInt(int v)
    throws IOException
  {
    this.mDelegate.writeInt(v);
  }

  public void writeLong(long v)
    throws IOException
  {
    this.mDelegate.writeLong(v);
  }

  public void writeFloat(float v)
    throws IOException
  {
    this.mDelegate.writeFloat(v);
  }

  public void writeDouble(double v)
    throws IOException
  {
    this.mDelegate.writeDouble(v);
  }

  public void writeBytes(String s)
    throws IOException
  {
    this.mDelegate.writeBytes(s);
  }

  public void writeChars(String s)
    throws IOException
  {
    this.mDelegate.writeChars(s);
  }

  public void writeUTF(String s)
    throws IOException
  {
    this.mDelegate.writeUTF(s);
  }
}