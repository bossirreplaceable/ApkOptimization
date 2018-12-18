package com.dongnao.ricky.androlib.res.decoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.dongnao.ricky.androlib.AndrolibException;
import com.dongnao.ricky.androlib.res.data.ResPackage;
import com.dongnao.ricky.androlib.res.data.ResType;
import com.dongnao.ricky.util.ExtDataInput;

import mindprod.ledatastream.LEDataInputStream;

public class RawARSCDecoder
{
  private ExtDataInput mIn;
  private Header mHeader;
  private StringBlock mTypeNames;
  private StringBlock mSpecNames;
  private ResPackage mPkg;
  private ResType mType;
  private int mCurTypeID = -1;
  private ResPackage[] mPkgs;
  private static HashMap<Integer, Set<String>> mExistTypeNames;
  private int mResId;
  private static final short ENTRY_FLAG_COMPLEX = 1;
  private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName
    ());
  private static final int KNOWN_CONFIG_BYTES = 38;

  public static ResPackage[] decode(InputStream arscStream)
    throws AndrolibException
  {
    RawARSCDecoder decoder;
    try
    {
      decoder = new RawARSCDecoder(arscStream);
      System.out.printf("parse to get the exist names in the resouces.arsc first\n", new Object[0]);

      ResPackage[] pkgs = decoder.readTable();

      return pkgs;
    } catch (IOException ex) {
      throw new AndrolibException("Could not decode arsc file", ex);
    }
  }

  private RawARSCDecoder(InputStream arscStream)
    throws AndrolibException, IOException
  {
    this.mIn = new ExtDataInput(new LEDataInputStream(arscStream));
    mExistTypeNames = new HashMap();
  }

  private ResPackage[] readTable() throws IOException, AndrolibException
  {
    nextChunkCheckType(2);
    int packageCount = this.mIn.readInt();

    StringBlock.read(this.mIn);

    ResPackage[] packages = new ResPackage[packageCount];

    nextChunk();
    for (int i = 0; i < packageCount; ++i) {
      packages[i] = readPackage();
    }

    return packages;
  }

  private ResPackage readPackage()
    throws IOException, AndrolibException
  {
    checkChunkType(512);
    int id = (byte)this.mIn.readInt();
    String name = this.mIn.readNulEndedString(128, true);

    this.mIn.skipInt();
    this.mIn.skipInt();
    this.mIn.skipInt();
    this.mIn.skipInt();

    this.mTypeNames = StringBlock.read(this.mIn);

    this.mSpecNames = StringBlock.read(this.mIn);

    this.mResId = (id << 24);

    this.mPkg = new ResPackage(id, name);

    nextChunk();
    while (this.mHeader.type == 514) {
      readType();
    }

    return this.mPkg;
  }

  private void readType() throws AndrolibException, IOException
  {
    checkChunkType(514);
    byte id = this.mIn.readByte();
    this.mIn.skipBytes(3);
    int entryCount = this.mIn.readInt();

    this.mCurTypeID = id;

    this.mIn.skipBytes(entryCount * 4);

    this.mResId = (0xFF000000 & this.mResId | id << 16);

    this.mType = new ResType(this.mTypeNames.getString(id - 1), this.mPkg);

    while (nextChunk().type == 513)
      readConfig();
  }

  private void readConfig()
    throws IOException, AndrolibException
  {
    checkChunkType(513);
    this.mIn.skipInt();
    int entryCount = this.mIn.readInt();
    int entriesStart = this.mIn.readInt();

    readConfigFlags();
    int[] entryOffsets = this.mIn.readIntArray(entryCount);

    for (int i = 0; i < entryOffsets.length; ++i)
    {
      if (entryOffsets[i] != -1) {
        this.mResId = (this.mResId & 0xFFFF0000 | i);

        readEntry();
      }
    }
  }

  private void readEntry()
    throws IOException, AndrolibException
  {
    this.mIn.skipBytes(2);
    short flags = this.mIn.readShort();
    int specNamesId = this.mIn.readInt();

    putTypeSpecNameStrings(this.mCurTypeID, this.mSpecNames.getString(specNamesId));
    boolean readDirect = false;
    if ((flags & 0x1) == 0) {
      readDirect = true;
      readValue(readDirect, specNamesId);
    } else {
      readDirect = false;
      readComplexEntry(readDirect, specNamesId);
    }
  }

  private void readComplexEntry(boolean flags, int specNamesId)
    throws IOException, AndrolibException
  {
    int parent = this.mIn.readInt();
    int count = this.mIn.readInt();

    for (int i = 0; i < count; ++i) {
      this.mIn.readInt();
      readValue(flags, specNamesId);
    }
  }

  private void readValue(boolean flags, int specNamesId)
    throws IOException, AndrolibException
  {
    this.mIn.skipCheckShort((short) 8);
    this.mIn.skipCheckByte((byte) 0);
    byte type = this.mIn.readByte();
    int data = this.mIn.readInt();
  }

  private void readConfigFlags()
    throws IOException, AndrolibException
  {
    int size = this.mIn.readInt();
    if (size < 28) {
      throw new AndrolibException("Config size < 28");
    }

    boolean isInvalid = false;

    short mcc = this.mIn.readShort();
    short mnc = this.mIn.readShort();

    char[] language = { (char)this.mIn.readByte(), (char)this.mIn.readByte() };
    char[] country = { (char)this.mIn.readByte(), (char)this.mIn.readByte() };

    byte orientation = this.mIn.readByte();
    byte touchscreen = this.mIn.readByte();

    int density = this.mIn.readUnsignedShort();

    byte keyboard = this.mIn.readByte();
    byte navigation = this.mIn.readByte();
    byte inputFlags = this.mIn.readByte();
    this.mIn.skipBytes(1);

    short screenWidth = this.mIn.readShort();
    short screenHeight = this.mIn.readShort();

    short sdkVersion = this.mIn.readShort();
    this.mIn.skipBytes(2);

    byte screenLayout = 0;
    byte uiMode = 0;
    short smallestScreenWidthDp = 0;
    if (size >= 32) {
      screenLayout = this.mIn.readByte();
      uiMode = this.mIn.readByte();
      smallestScreenWidthDp = this.mIn.readShort();
    }

    short screenWidthDp = 0;
    short screenHeightDp = 0;
    if (size >= 36) {
      screenWidthDp = this.mIn.readShort();
      screenHeightDp = this.mIn.readShort();
    }

    short layoutDirection = 0;
    if (size >= 38) {
      layoutDirection = this.mIn.readShort();
    }

    int exceedingSize = size - 38;
    if (exceedingSize > 0) {
      byte[] buf = new byte[exceedingSize];
      this.mIn.readFully(buf);
      BigInteger exceedingBI = new BigInteger(1, buf);

      if (exceedingBI.equals(BigInteger.ZERO)) {
        LOGGER.fine(
          String.format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.", new Object[] { 
          Integer.valueOf(38) }));
      } else {
        LOGGER.warning(String.format("Config flags size > %d. Exceeding bytes: 0x%X.", new Object[] { 
          Integer.valueOf(38), exceedingBI }));
        isInvalid = true;
      }
    }
  }

  private Header nextChunk()
    throws IOException
  {
    return (this.mHeader = Header.read(this.mIn));
  }

  private void checkChunkType(int expectedType)
    throws AndrolibException
  {
    if (this.mHeader.type != expectedType)
      throw new AndrolibException(String.format(
        "Invalid chunk type: expected=0x%08x, got=0x%08x", new Object[] { 
        Integer.valueOf(expectedType), Short.valueOf(this.mHeader.type) }));
  }

  private void nextChunkCheckType(int expectedType)
    throws IOException, AndrolibException
  {
    nextChunk();
    checkChunkType(expectedType);
  }

  private void putTypeSpecNameStrings(int type, String name)
  {
    Set names = (Set)mExistTypeNames.get(Integer.valueOf(type));
    if (names == null)
      names = new HashSet();

    names.add(name);
    mExistTypeNames.put(Integer.valueOf(type), names);
  }

  public static Set<String> getExistTypeSpecNameStrings(int type) {
    return ((Set<String>)mExistTypeNames.get(Integer.valueOf(type)));
  }

  public static class FlagsOffset
  {
    public final int offset;
    public final int count;

    public FlagsOffset(int offset, int count)
    {
      this.offset = offset;
      this.count = count;
    }
  }

  public static class Header
  {
    public final short type;
    public final int chunkSize;
    public static final short TYPE_NONE = -1;
    public static final short TYPE_TABLE = 2;
    public static final short TYPE_PACKAGE = 512;
    public static final short TYPE_TYPE = 514;
    public static final short TYPE_CONFIG = 513;

    public Header(short type, int size)
    {
      this.type = type;
      this.chunkSize = size;
    }

    public static Header read(ExtDataInput in) throws IOException {
      short type;
      try {
        type = in.readShort();
      } catch (EOFException ex) {
        return new Header((short) -1, 0);
      }
      in.skipBytes(2);

      return new Header(type, in.readInt());
    }
  }
}