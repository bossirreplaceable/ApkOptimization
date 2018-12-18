package com.dongnao.ricky.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExtDataInput extends DataInputDelegate
{
  public ExtDataInput(InputStream in)
  {
    super(new DataInputStream(in));
  }

  public ExtDataInput(DataInput delegate) {
    super(delegate);
  }

  public int[] readIntArray(int length) throws IOException {
    int[] array = new int[length];
    for (int i = 0; i < length; ++i)
      array[i] = readInt();

    return array;
  }

  public void skipInt() throws IOException {
    skipBytes(4);
  }

  public void skipCheckInt(int expected) throws IOException {
    int got = readInt();
    if (got != expected)
      throw new IOException(String.format(
        "Expected: 0x%08x, got: 0x%08x", new Object[] { Integer.valueOf(expected), Integer.valueOf(got) }));
  }

  public void skipCheckChunkTypeInt(int expected, int possible) throws IOException
  {
    int got = readInt();

    if (got == possible)
      skipCheckChunkTypeInt(expected, -1);
    else if (got != expected)
      throw new IOException(String.format("Expected: 0x%08x, got: 0x%08x", new Object[] { Integer.valueOf(expected), Integer.valueOf(got) }));
  }

  public void skipCheckShort(short expected) throws IOException
  {
    short got = readShort();
    if (got != expected)
      throw new IOException(String.format(
        "Expected: 0x%08x, got: 0x%08x", new Object[] { Short.valueOf(expected), Short.valueOf(got) }));
  }

  public void skipCheckByte(byte expected) throws IOException
  {
    byte got = readByte();
    if (got != expected)
      throw new IOException(String.format(
        "Expected: 0x%08x, got: 0x%08x", new Object[] { Byte.valueOf(expected), Byte.valueOf(got) }));
  }

  public String readNulEndedString(int length, boolean fixed)
    throws IOException
  {
    StringBuilder string = new StringBuilder(16);
    while (length-- != 0) {
      short ch = readShort();
      if (ch == 0)
        break;

      string.append((char)ch);
    }
    if (fixed) {
      skipBytes(length * 2);
    }

    return string.toString();
  }
}