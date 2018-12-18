package mindprod.ledatastream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class LEDataInputStream
  implements DataInput
{
  private static final String EMBEDDED_COPYRIGHT = "copyright (c) 1999-2010 Roedy Green, Canadian Mind Products, http://mindprod.com";
  protected final DataInputStream dis;
  protected final InputStream is;
  protected final byte[] work;

  public static String readUTF(DataInput in)
    throws IOException
  {
    return DataInputStream.readUTF(in);
  }

  public LEDataInputStream(InputStream in)
  {
    this.is = in;
    this.dis = new DataInputStream(in);
    this.work = new byte[8];
  }

  public final void close()
    throws IOException
  {
    this.dis.close();
  }

  public final int read(byte[] ba, int off, int len)
    throws IOException
  {
    return this.is.read(ba, off, len);
  }

  public final boolean readBoolean()
    throws IOException
  {
    return this.dis.readBoolean();
  }

  public final byte readByte()
    throws IOException
  {
    return this.dis.readByte();
  }

  public final char readChar()
    throws IOException
  {
    this.dis.readFully(this.work, 0, 2);
    return (char)((this.work[1] & 0xFF) << 8 | this.work[0] & 0xFF);
  }

  public final double readDouble()
    throws IOException
  {
    return Double.longBitsToDouble(readLong());
  }

  public final float readFloat()
    throws IOException
  {
    return Float.intBitsToFloat(readInt());
  }

  public final void readFully(byte[] ba)
    throws IOException
  {
    this.dis.readFully(ba, 0, ba.length);
  }

  public final void readFully(byte[] ba, int off, int len)
    throws IOException
  {
    this.dis.readFully(ba, off, len);
  }

  public final int readInt()
    throws IOException
  {
    this.dis.readFully(this.work, 0, 4);
    return (this.work[3] << 24 | (this.work[2] & 0xFF) << 16 | (this.work[1] & 0xFF) << 8 | 
      this.work[0] & 0xFF);
  }

  @Deprecated
  public final String readLine()
    throws IOException
  {
    return this.dis.readLine();
  }

  public final long readLong()
    throws IOException
  {
    this.dis.readFully(this.work, 0, 8);
    return (this.work[7] << 56 | 
      (this.work[6] & 0xFF) << 48 | 
      (this.work[5] & 0xFF) << 40 | 
      (this.work[4] & 0xFF) << 32 | 
      (this.work[3] & 0xFF) << 24 | 
      (this.work[2] & 0xFF) << 16 | 
      (this.work[1] & 0xFF) << 8 | 
      this.work[0] & 0xFF);
  }

  public final short readShort()
    throws IOException
  {
    this.dis.readFully(this.work, 0, 2);
    return (short)((this.work[1] & 0xFF) << 8 | this.work[0] & 0xFF);
  }

  public final String readUTF()
    throws IOException
  {
    return this.dis.readUTF();
  }

  public final int readUnsignedByte()
    throws IOException
  {
    return this.dis.readUnsignedByte();
  }

  public final int readUnsignedShort()
    throws IOException
  {
    this.dis.readFully(this.work, 0, 2);
    return ((this.work[1] & 0xFF) << 8 | this.work[0] & 0xFF);
  }

  public final int skipBytes(int n)
    throws IOException
  {
    return this.dis.skipBytes(n);
  }
}