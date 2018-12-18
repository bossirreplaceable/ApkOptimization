package com.dongnao.ricky.androlib.res.decoder;

import com.dongnao.ricky.androlib.AndrolibException;
import com.dongnao.ricky.util.ExtDataInput;
import com.dongnao.ricky.util.ExtDataOutput;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StringBlock {
	private int[] m_stringOffsets;
	private byte[] m_strings;
	private int[] m_styleOffsets;
	private int[] m_styles;
	private boolean m_isUTF8;
	private int[] m_stringOwns;
	private static final CharsetDecoder UTF16LE_DECODER = Charset.forName("UTF-16LE").newDecoder();
	private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8").newDecoder();
	private static final Logger LOGGER = Logger.getLogger(StringBlock.class.getName());
	private static final int CHUNK_STRINGPOOL_TYPE = 1835009;
	private static final int UTF8_FLAG = 256;
	private static final int CHUNK_NULL_TYPE = 0;
	private static final byte NULL = 0;

	public static StringBlock read(ExtDataInput reader) throws IOException {
		reader.skipCheckChunkTypeInt(1835009, 0);
		int chunkSize = reader.readInt();
		int stringCount = reader.readInt();
		int styleCount = reader.readInt();

		int flags = reader.readInt();
		int stringsOffset = reader.readInt();
		int stylesOffset = reader.readInt();

		StringBlock block = new StringBlock();
		block.m_isUTF8 = ((flags & 0x100) != 0);

		block.m_stringOffsets = reader.readIntArray(stringCount);
		block.m_stringOwns = new int[stringCount];
		Arrays.fill(block.m_stringOwns, -1);

		if (styleCount != 0) {
			block.m_styleOffsets = reader.readIntArray(styleCount);
		}

		int size = ((stylesOffset == 0) ? chunkSize : stylesOffset) - stringsOffset;

		if (size % 4 != 0)
			throw new IOException("String data size is not multiple of 4 (" + size + ").");

		block.m_strings = new byte[size];

		reader.readFully(block.m_strings);

		if (stylesOffset != 0) {
			size = chunkSize - stylesOffset;
			if (size % 4 != 0)
				throw new IOException("Style data size is not multiple of 4 (" + size + ").");

			block.m_styles = reader.readIntArray(size / 4);
		}

		return block;
	}

	public static int writeSpecNameStringBlock(ExtDataInput reader, ExtDataOutput out, HashSet<String> specNames,
			Map<String, Integer> curSpecNameToPos) throws IOException, AndrolibException {
		int type = reader.readInt();
		int chunkSize = reader.readInt();
		int stringCount = reader.readInt();
		int styleOffsetCount = reader.readInt();

		if (styleOffsetCount != 0) {
			throw new AndrolibException(
					String.format("writeSpecNameStringBlock styleOffsetCount != 0  styleOffsetCount %d",
							new Object[] { Integer.valueOf(styleOffsetCount) }));
		}

		int flags = reader.readInt();
		boolean isUTF8 = (flags & 0x100) != 0;
		int stringsOffset = reader.readInt();
		int stylesOffset = reader.readInt();

		reader.readIntArray(stringCount);

		int size = ((stylesOffset == 0) ? chunkSize : stylesOffset) - stringsOffset;

		if (size % 4 != 0)
			throw new IOException("String data size is not multiple of 4 (" + size + ").");

		byte[] temp_strings = new byte[size];
		reader.readFully(temp_strings);

		int totalSize = 0;

		out.writeCheckInt(type, 1835009);

		totalSize += 4;

		stringCount = specNames.size();

		totalSize += 24 + 4 * stringCount;
		stringsOffset = totalSize;

		int[] stringOffsets = new int[stringCount];
		byte[] strings = new byte[size];
		int offset = 0;
		int i = 0;
		curSpecNameToPos.clear();

		for (Iterator it = specNames.iterator(); it.hasNext();) {
			byte[] tempByte;
			stringOffsets[i] = offset;
			String name = (String) it.next();

			curSpecNameToPos.put(name, Integer.valueOf(i));
			if (isUTF8) {
				strings[(offset++)] = (byte) name.length();
				strings[(offset++)] = (byte) name.length();
				totalSize += 2;
				tempByte = name.getBytes(Charset.forName("UTF-8"));
				if (name.length() != tempByte.length) {
					throw new AndrolibException(
							String.format("writeSpecNameStringBlock lenght is different  name %d, tempByte %d\n",
									new Object[] { Integer.valueOf(name.length()), Integer.valueOf(tempByte.length) }));
				}

				System.arraycopy(tempByte, 0, strings, offset, tempByte.length);

				offset += name.length();
				strings[(offset++)] = 0;
				totalSize += name.length() + 1;
			} else {
				writeShort(strings, offset, (short) name.length());
				offset += 2;
				totalSize += 2;
				tempByte = name.getBytes(Charset.forName("UTF-16LE"));
				if (name.length() * 2 != tempByte.length)
					throw new AndrolibException(
							String.format("writeSpecNameStringBlock lenght is different  name %d, tempByte %d\n",
									new Object[] { Integer.valueOf(name.length()), Integer.valueOf(tempByte.length) }));

				System.arraycopy(tempByte, 0, strings, offset, tempByte.length);
				offset += tempByte.length;
				strings[(offset++)] = 0;
				strings[(offset++)] = 0;
				totalSize += tempByte.length + 2;
			}

			++i;
		}

		size = totalSize - stringsOffset;
		if (size % 4 != 0) {
			int add = 4 - size % 4;
			for (i = 0; i < add; ++i) {
				strings[(offset++)] = 0;
				++totalSize;
			}
		}

		out.writeInt(totalSize);
		out.writeInt(stringCount);
		out.writeInt(styleOffsetCount);
		out.writeInt(flags);
		out.writeInt(stringsOffset);
		out.writeInt(stylesOffset);
		out.writeIntArray(stringOffsets);
		out.write(strings, 0, offset);

		return (chunkSize - totalSize);
	}

	public static int writeTableNameStringBlock(ExtDataInput reader, ExtDataOutput out,
			Map<Integer, String> tableProguardMap) throws IOException, AndrolibException {
		int type = reader.readInt();
		int chunkSize = reader.readInt();
		int stringCount = reader.readInt();
		int styleOffsetCount = reader.readInt();

		int flags = reader.readInt();
		int stringsOffset = reader.readInt();
		int stylesOffset = reader.readInt();

		StringBlock block = new StringBlock();
		block.m_isUTF8 = ((flags & 0x100) != 0);
		if (block.m_isUTF8)
			System.out.printf("resources.arsc Character Encoding: utf-8\n", new Object[0]);
		else {
			System.out.printf("resources.arsc Character Encoding: utf-16\n", new Object[0]);
		}

		block.m_stringOffsets = reader.readIntArray(stringCount);
		block.m_stringOwns = new int[stringCount];
		for (int i = 0; i < stringCount; ++i) {
			block.m_stringOwns[i] = -1;
		}

		if (styleOffsetCount != 0) {
			block.m_styleOffsets = reader.readIntArray(styleOffsetCount);
		}

		int size = ((stylesOffset == 0) ? chunkSize : stylesOffset) - stringsOffset;

		if (size % 4 != 0)
			throw new IOException("String data size is not multiple of 4 (" + size + ").");

		block.m_strings = new byte[size];
		reader.readFully(block.m_strings);

		if (stylesOffset != 0) {
			size = chunkSize - stylesOffset;
			if (size % 4 != 0)
				throw new IOException("Style data size is not multiple of 4 (" + size + ").");

			block.m_styles = reader.readIntArray(size / 4);
		}

		int totalSize = 0;

		out.writeCheckInt(type, 1835009);

		totalSize += 4;

		totalSize += 24 + 4 * stringCount + 4 * styleOffsetCount;
		stringsOffset = totalSize;

		byte[] strings = new byte[block.m_strings.length];
		int[] stringOffsets = new int[stringCount];
		System.arraycopy(block.m_stringOffsets, 0, stringOffsets, 0, stringOffsets.length);

		int offset = 0;
		int i = 0;

		for (i = 0; i < stringCount; ++i) {
			stringOffsets[i] = offset;

			if (tableProguardMap.get(Integer.valueOf(i)) == null) {
				int copyLen = (i == stringCount - 1) ? block.m_strings.length - block.m_stringOffsets[i]
						: block.m_stringOffsets[(i + 1)] - block.m_stringOffsets[i];
				System.arraycopy(block.m_strings, block.m_stringOffsets[i], strings, offset, copyLen);
				offset += copyLen;
				totalSize += copyLen;
			} else {
				byte[] tempByte;
				String name = (String) tableProguardMap.get(Integer.valueOf(i));

				if (block.m_isUTF8) {
					strings[(offset++)] = (byte) name.length();
					strings[(offset++)] = (byte) name.length();
					totalSize += 2;
					tempByte = name.getBytes(Charset.forName("UTF-8"));
					if (name.length() != tempByte.length)
						throw new AndrolibException(String.format(
								"writeTableNameStringBlock lenght is different  name %d, tempByte %d\n",
								new Object[] { Integer.valueOf(name.length()), Integer.valueOf(tempByte.length) }));

					System.arraycopy(tempByte, 0, strings, offset, tempByte.length);
					offset += name.length();
					strings[(offset++)] = 0;
					totalSize += name.length() + 1;
				} else {
					writeShort(strings, offset, (short) name.length());
					offset += 2;
					totalSize += 2;
					tempByte = name.getBytes(Charset.forName("UTF-16LE"));
					if (name.length() * 2 != tempByte.length)
						throw new AndrolibException(String.format(
								"writeTableNameStringBlock lenght is different  name %d, tempByte %d\n",
								new Object[] { Integer.valueOf(name.length()), Integer.valueOf(tempByte.length) }));

					System.arraycopy(tempByte, 0, strings, offset, tempByte.length);
					offset += tempByte.length;
					strings[(offset++)] = 0;
					strings[(offset++)] = 0;
					totalSize += tempByte.length + 2;
				}
			}

		}

		size = totalSize - stringsOffset;
		if (size % 4 != 0) {
			int add = 4 - size % 4;
			for (i = 0; i < add; ++i) {
				strings[(offset++)] = 0;
				++totalSize;
			}

		}

		if (stylesOffset != 0) {
			stylesOffset = totalSize;
			totalSize += block.m_styles.length * 4;
		}

		out.writeInt(totalSize);
		out.writeInt(stringCount);
		out.writeInt(styleOffsetCount);
		out.writeInt(flags);
		out.writeInt(stringsOffset);
		out.writeInt(stylesOffset);
		out.writeIntArray(stringOffsets);
		if (stylesOffset != 0)
			out.writeIntArray(block.m_styleOffsets);

		out.write(strings, 0, offset);

		if (stylesOffset != 0) {
			out.writeIntArray(block.m_styles);
		}

		return (chunkSize - totalSize);
	}

	public static void writeAll(ExtDataInput reader, ExtDataOutput out) throws IOException {
		out.writeCheckChunkTypeInt(reader, 1835009, 0);
		int chunkSize = reader.readInt();
		out.writeInt(chunkSize);
		out.writeBytes(reader, chunkSize - 8);
	}

	public int getCount() {
		return ((this.m_stringOffsets != null) ? this.m_stringOffsets.length : 0);
	}

	public String getString(int index) {
		int length;
		int[] val;
		if ((index < 0) || (this.m_stringOffsets == null) || (index >= this.m_stringOffsets.length))
			return null;

		int offset = this.m_stringOffsets[index];

		if (this.m_isUTF8) {
			val = getUtf8(this.m_strings, offset);
			offset = val[0];
			length = val[1];
		} else {
			val = getUtf16(this.m_strings, offset);
			offset += val[0];
			length = val[1];
		}
		return decodeString(offset, length);
	}

	private static final int[] getUtf8(byte[] array, int offset) {
    int val = array[offset];

    if ((val & 0x80) != 0)
      offset += 2;
    else
      ++offset;

    val = array[offset];
    if ((val & 0x80) != 0)
      offset += 2;
    else
      ++offset;

    int length = 0;
    while (array[(offset + length)] != 0)
      ++length;

    return new int[]{ offset, length };
  }

	private static final int[] getUtf16(byte[] array, int offset) {
    int val = (array[(offset + 1)] & 0xFF) << 8 | array[offset] & 0xFF;

    if (val == 32768) {
      int high = (array[(offset + 3)] & 0xFF) << 8;
      int low = array[(offset + 2)] & 0xFF;
      return new int[]{ 4, (high + low) * 2 };
    }
    return new int[]{ 2, val * 2 };
  }

	public CharSequence get(int index) {
		return getString(index);
	}

	public int find(String string) {
		if (string == null)
			return -1;

		for (int i = 0; i != this.m_stringOffsets.length; ++i) {
			int offset = this.m_stringOffsets[i];
			int length = getShort(this.m_strings, offset);
			if (length != string.length())
				continue;

			int j = 0;
			for (; j != length; ++j) {
				offset += 2;
				if (string.charAt(j) != getShort(this.m_strings, offset))
					break;
			}

			if (j == length)
				return i;
		}

		return -1;
	}

	private String decodeString(int offset, int length) {
		try {
			return ((this.m_isUTF8) ? UTF8_DECODER : UTF16LE_DECODER)
					.decode(ByteBuffer.wrap(this.m_strings, offset, length)).toString();
		} catch (CharacterCodingException ex) {
			LOGGER.log(Level.WARNING, null, ex);
		}
		return null;
	}

	private static final int getShort(byte[] array, int offset) {
		return ((array[(offset + 1)] & 0xFF) << 8 | array[offset] & 0xFF);
	}

	private static final void writeShort(byte[] array, int offset, short value) {
		array[offset] = (byte) (0xFF & value);
		array[(offset + 1)] = (byte) (0xFF & value >> 8);
	}

	private static final int getShort(int[] array, int offset) {
		int value = array[(offset / 4)];
		if (offset % 4 / 2 == 0)
			return (value & 0xFFFF);

		return (value >>> 16);
	}
}