package com.dongnao.ricky.util;

import java.io.DataInput;
import java.io.IOException;

public abstract class DataInputDelegate
  implements DataInput
{
  protected final DataInput mDelegate;

  public DataInputDelegate(DataInput delegate)
  {
    this.mDelegate = delegate;
  }

  public int skipBytes(int n) throws IOException {
    return this.mDelegate.skipBytes(n);
  }

  public int readUnsignedShort() throws IOException {
    return this.mDelegate.readUnsignedShort();
  }

  public int readUnsignedByte() throws IOException {
    return this.mDelegate.readUnsignedByte();
  }

  public String readUTF() throws IOException {
    return this.mDelegate.readUTF();
  }

  public short readShort() throws IOException {
    return this.mDelegate.readShort();
  }

  public long readLong() throws IOException {
    return this.mDelegate.readLong();
  }

  public String readLine() throws IOException {
    return this.mDelegate.readLine();
  }

  public int readInt() throws IOException {
    return this.mDelegate.readInt();
  }

  public void readFully(byte[] b, int off, int len) throws IOException {
    this.mDelegate.readFully(b, off, len);
  }

  public void readFully(byte[] b) throws IOException {
    this.mDelegate.readFully(b);
  }

  public float readFloat() throws IOException {
    return this.mDelegate.readFloat();
  }

  public double readDouble() throws IOException {
    return this.mDelegate.readDouble();
  }

  public char readChar() throws IOException {
    return this.mDelegate.readChar();
  }

  public byte readByte() throws IOException {
    return this.mDelegate.readByte();
  }

  public boolean readBoolean() throws IOException {
    return this.mDelegate.readBoolean();
  }
}