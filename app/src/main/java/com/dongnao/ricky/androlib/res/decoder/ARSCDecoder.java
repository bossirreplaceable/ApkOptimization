package com.dongnao.ricky.androlib.res.decoder;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.dongnao.ricky.androlib.AndrolibException;
import com.dongnao.ricky.androlib.ApkDecoder;
import com.dongnao.ricky.androlib.res.data.ResPackage;
import com.dongnao.ricky.androlib.res.data.ResType;
import com.dongnao.ricky.resourceproguard.Main;
import com.dongnao.ricky.util.ExtDataInput;
import com.dongnao.ricky.util.ExtDataOutput;
import com.dongnao.ricky.util.FileOperation;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

public class ARSCDecoder {
	private ExtDataInput mIn;
	private ExtDataOutput mOut;
	private Header mHeader;
	private StringBlock mTableStrings;
	private StringBlock mTypeNames;
	private StringBlock mSpecNames;
	private ResPackage mPkg;
	private ResType mType;
	private ResPackage[] mPkgs;
	private int[] mPkgsLenghtChange;
	private int mTableLenghtChange = 0;
	private int mResId;
	private static final short ENTRY_FLAG_COMPLEX = 1;
	private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());
	private static final int KNOWN_CONFIG_BYTES = 38;
	private int mCurTypeID = -1;
	private int mCurEntryID = -1;
	private int mCurPackageID = -1;
	private ProguardStringBuilder mProguardBuilder;
	private static Map<Integer, String> mTableStringsProguard = new LinkedHashMap();
	private boolean mShouldProguardForType = false;
	private Writer mMappingWriter;
	private Map<String, String> mOldFileName = new LinkedHashMap();
	private Map<String, Integer> mCurSpecNameToPos = new LinkedHashMap();
	private HashSet<String> mShouldProguardTypeSet = new HashSet();
	private ApkDecoder mApkDecoder;

	public static ResPackage[] decode(InputStream arscStream, ApkDecoder apkDecoder) throws AndrolibException {
		ARSCDecoder decoder;
		try {
			decoder = new ARSCDecoder(arscStream, apkDecoder);
			ResPackage[] pkgs = decoder.readTable();

			return pkgs;
		} catch (IOException ex) {
			throw new AndrolibException("Could not decode arsc file", ex);
		}
	}

	public static void write(InputStream arscStream, ApkDecoder decoder, ResPackage[] pkgs) throws AndrolibException {
		ARSCDecoder writer;
		try {
			writer = new ARSCDecoder(arscStream, decoder, pkgs);
			writer.writeTable();
		} catch (IOException ex) {
			throw new AndrolibException("Could not decode arsc file", ex);
		}
	}

	private ARSCDecoder(InputStream arscStream, ApkDecoder decoder) throws AndrolibException, IOException {
		this.mIn = new ExtDataInput(new LEDataInputStream(arscStream));
		this.mApkDecoder = decoder;
		proguardFileName();
	}

	private void proguardFileName() throws IOException, AndrolibException {
		File[] arrayOfFile1;
		this.mMappingWriter = new BufferedWriter(new FileWriter(this.mApkDecoder.getResMappingFile(), false));

		this.mProguardBuilder = new ProguardStringBuilder();
		this.mProguardBuilder.reset();

		File rawResFile = this.mApkDecoder.getRawResFile();

		File[] resFiles = rawResFile.listFiles();

		int k = (arrayOfFile1 = resFiles).length;
		for (int i = 0; i < k; ++i) {
			File resFile = arrayOfFile1[i];
			String raw = resFile.getName();

			if (raw.contains("-")) {
				raw = raw.substring(0, raw.indexOf("-"));
			}

			this.mShouldProguardTypeSet.add(raw);
		}

		Main client = this.mApkDecoder.getClient();

		if (client.isUseKeepMapping()) {
			File[] arrayOfFile2;
			HashMap fileMapping = client.getOldFileMapping();

			Object keepFileNames = new ArrayList();

			String resRoot = null;
			for (Iterator localIterator = fileMapping.values().iterator(); localIterator.hasNext();) {
				String name = (String) localIterator.next();
				int dot = name.indexOf("/");
				if (dot == -1)
					throw new IOException(String.format("the old mapping res file path should be like r/a, yours %s\n",
							new Object[] { name }));

				resRoot = name.substring(0, dot);
				((List) keepFileNames).add(name.substring(dot + 1));
			}

			this.mProguardBuilder.removeStrings((Collection) keepFileNames);

			int dot = (arrayOfFile2 = resFiles).length;
			for (int l = 0; l < dot; ++l) {
				File resFile = arrayOfFile2[l];
				String raw = "res/" + resFile.getName();
				if (fileMapping.containsKey(raw)) {
					this.mOldFileName.put(raw, (String) fileMapping.get(raw));
				} else {
					System.out.printf("can not find the file mapping %s\n", new Object[] { raw });
					this.mOldFileName.put(raw, resRoot + "/" + this.mProguardBuilder.getReplaceString());
				}
			}
		} else {
			for (int j = 0; j < resFiles.length; ++j) {
				this.mOldFileName.put("res/" + resFiles[j].getName(), "r/" + this.mProguardBuilder.getReplaceString());
			}
		}

		generalFileResMapping();

		File destResDir = this.mApkDecoder.getOutResFile();
		FileOperation.deleteDir(destResDir);
		destResDir.mkdir();
	}

	private ARSCDecoder(InputStream arscStream, ApkDecoder decoder, ResPackage[] pkgs) throws FileNotFoundException {
		this.mApkDecoder = decoder;

		this.mIn = new ExtDataInput(new LEDataInputStream(arscStream));

		this.mOut = new ExtDataOutput(
				new LEDataOutputStream(new FileOutputStream(this.mApkDecoder.getOutTempARSCFile(), false)));
		this.mPkgs = pkgs;

		this.mPkgsLenghtChange = new int[pkgs.length];
	}

	private ResPackage[] readTable() throws IOException, AndrolibException {
		nextChunkCheckType(2);
		int packageCount = this.mIn.readInt();

		this.mTableStrings = StringBlock.read(this.mIn);

		ResPackage[] packages = new ResPackage[packageCount];

		nextChunk();
		for (int i = 0; i < packageCount; ++i) {
			packages[i] = readPackage();
		}

		System.out.printf("resources mapping file %s done\n",
				new Object[] { this.mApkDecoder.getResMappingFile().getAbsolutePath() });

		this.mMappingWriter.close();
		return packages;
	}

	private void writeTable() throws IOException, AndrolibException {
		System.out.printf("writing new resources.arsc \n", new Object[0]);

		this.mTableLenghtChange = 0;

		writeNextChunkCheck(2, 0);
		int packageCount = this.mIn.readInt();
		this.mOut.writeInt(packageCount);

		this.mTableLenghtChange += StringBlock.writeTableNameStringBlock(this.mIn, this.mOut, mTableStringsProguard);
		writeNextChunk(0);
		if (packageCount != this.mPkgs.length)
			throw new AndrolibException(String.format("writeTable package count is different before %d, now %d",
					new Object[] { Integer.valueOf(this.mPkgs.length), Integer.valueOf(packageCount) }));

		for (int i = 0; i < packageCount; ++i) {
			this.mCurPackageID = i;
			writePackage();
		}

		reWriteTable();
	}

	private void generalFileResMapping() throws IOException {
		this.mMappingWriter.write("res path mapping:\n");
		for (Iterator localIterator = this.mOldFileName.keySet().iterator(); localIterator.hasNext();) {
			String raw = (String) localIterator.next();
			this.mMappingWriter.write("    " + raw + " -> " + ((String) this.mOldFileName.get(raw)));
			this.mMappingWriter.write("\n");
		}

		this.mMappingWriter.write("\n\n");
		this.mMappingWriter.write("res id mapping:\n");
		this.mMappingWriter.flush();
	}

	private void generalResIDMapping(String packagename, String typename, String specname, String replace)
			throws IOException {
		this.mMappingWriter.write("    " + packagename + ".R." + typename + "." + specname + " -> " + packagename
				+ ".R." + typename + "." + replace);
		this.mMappingWriter.write("\n");

		this.mMappingWriter.flush();
	}

	private void reWriteTable() throws AndrolibException, IOException {
		this.mIn = new ExtDataInput(new LEDataInputStream(new FileInputStream(this.mApkDecoder.getOutTempARSCFile())));
		this.mOut = new ExtDataOutput(
				new LEDataOutputStream(new FileOutputStream(this.mApkDecoder.getOutARSCFile(), false)));

		writeNextChunkCheck(2, this.mTableLenghtChange);
		System.out.printf("resources.arsc reduece: %fkb, time cost from begin: %fs\n",
				new Object[] { Double.valueOf(this.mTableLenghtChange / 1024.0D),
						Double.valueOf(this.mApkDecoder.getClient().diffTimeFromBegin()) });

		int packageCount = this.mIn.readInt();
		this.mOut.writeInt(packageCount);

		StringBlock.writeAll(this.mIn, this.mOut);

		for (int i = 0; i < packageCount; ++i) {
			this.mCurPackageID = i;
			writeNextChunk(this.mPkgsLenghtChange[this.mCurPackageID]);

			this.mOut.writeBytes(this.mIn, this.mHeader.chunkSize - 8);
		}
		this.mApkDecoder.getOutTempARSCFile().delete();
	}

	private ResPackage readPackage() throws IOException, AndrolibException {
		checkChunkType(512);
		int id = (byte) this.mIn.readInt();
		String name = this.mIn.readNulEndedString(128, true);

		System.out.printf("reading packagename %s\n", new Object[] { name });

		this.mIn.skipInt();
		this.mIn.skipInt();
		this.mIn.skipInt();
		this.mIn.skipInt();

		this.mCurTypeID = -1;

		this.mTypeNames = StringBlock.read(this.mIn);

		this.mSpecNames = StringBlock.read(this.mIn);

		this.mResId = (id << 24);

		this.mPkg = new ResPackage(id, name);

		if (this.mPkg.getName().equals("android"))
			this.mPkg.setCanProguard(false);
		else {
			this.mPkg.setCanProguard(true);
		}

		nextChunk();
		while (this.mHeader.type == 514) {
			readType();
		}

		return this.mPkg;
	}

	private void writePackage() throws IOException, AndrolibException {
		checkChunkType(512);
		int id = (byte) this.mIn.readInt();
		this.mOut.writeInt(id);

		this.mResId = (id << 24);

		this.mOut.writeBytes(this.mIn, 256);

		this.mOut.writeInt(this.mIn.readInt());

		this.mOut.writeInt(this.mIn.readInt());

		this.mOut.writeInt(this.mIn.readInt());

		this.mOut.writeInt(this.mIn.readInt());

		StringBlock.writeAll(this.mIn, this.mOut);

		if (this.mPkgs[this.mCurPackageID].isCanProguard()) {
			int specSizeChange = StringBlock.writeSpecNameStringBlock(this.mIn, this.mOut,
					this.mPkgs[this.mCurPackageID].getSpecNamesBlock(), this.mCurSpecNameToPos);
			this.mPkgsLenghtChange[this.mCurPackageID] += specSizeChange;

			this.mTableLenghtChange += specSizeChange;
		} else {
			StringBlock.writeAll(this.mIn, this.mOut);
		}

		writeNextChunk(0);

		while (this.mHeader.type == 514)
			writeType();
	}

	private void reduceFromOldMappingFile() {
		if (this.mPkg.isCanProguard()) {
			Main client = this.mApkDecoder.getClient();
			if (client.isUseKeepMapping()) {
				HashMap resMapping = client.getOldResMapping();
				String packName = this.mPkg.getName();
				if (resMapping.containsKey(packName)) {
					HashMap typeMaps = (HashMap) resMapping.get(packName);
					String typeName = this.mType.getName();

					if (typeMaps.containsKey(typeName)) {
						HashMap proguard = (HashMap) typeMaps.get(typeName);

						this.mProguardBuilder.removeStrings(proguard.values());
					}
				}
			}
		}
	}

	private void readType() throws AndrolibException, IOException {
		checkChunkType(514);
		byte id = this.mIn.readByte();
		this.mIn.skipBytes(3);
		int entryCount = this.mIn.readInt();

		if (this.mCurTypeID != id) {
			this.mProguardBuilder.reset();
			this.mCurTypeID = id;

			Set existNames = RawARSCDecoder.getExistTypeSpecNameStrings(this.mCurTypeID);
			if(existNames!=null){
				this.mProguardBuilder.removeStrings(existNames);
			}
		}

		this.mShouldProguardForType = isToProguardFile(this.mTypeNames.getString(id - 1));

		this.mIn.skipBytes(entryCount * 4);

		this.mResId = (0xFF000000 & this.mResId | id << 16);

		this.mType = new ResType(this.mTypeNames.getString(id - 1), this.mPkg);

		reduceFromOldMappingFile();

		while (nextChunk().type == 513)
			readConfig();
	}

	private void writeType() throws AndrolibException, IOException {
		checkChunkType(514);

		byte id = this.mIn.readByte();
		this.mOut.writeByte(id);

		this.mResId = (0xFF000000 & this.mResId | id << 16);

		this.mOut.writeBytes(this.mIn, 3);

		int entryCount = this.mIn.readInt();

		this.mOut.writeInt(entryCount);

		int[] entryOffsets = this.mIn.readIntArray(entryCount);
		this.mOut.writeIntArray(entryOffsets);

		while (writeNextChunk(0).type == 513)
			writeConfig();
	}

	private void readConfig() throws IOException, AndrolibException {
		checkChunkType(513);
		this.mIn.skipInt();
		int entryCount = this.mIn.readInt();
		int entriesStart = this.mIn.readInt();

		readConfigFlags();
		int[] entryOffsets = this.mIn.readIntArray(entryCount);

		for (int i = 0; i < entryOffsets.length; ++i) {
			this.mCurEntryID = i;
			if (entryOffsets[i] != -1) {
				this.mResId = (this.mResId & 0xFFFF0000 | i);

				readEntry();
			}
		}
	}

	private void writeConfig() throws IOException, AndrolibException {
		checkChunkType(513);

		this.mOut.writeInt(this.mIn.readInt());

		int entryCount = this.mIn.readInt();
		this.mOut.writeInt(entryCount);

		this.mOut.writeInt(this.mIn.readInt());

		writeConfigFlags();
		int[] entryOffsets = this.mIn.readIntArray(entryCount);
		this.mOut.writeIntArray(entryOffsets);

		for (int i = 0; i < entryOffsets.length; ++i)
			if (entryOffsets[i] != -1) {
				this.mResId = (this.mResId & 0xFFFF0000 | i);

				writeEntry();
			}
	}

	private void readEntry() throws IOException, AndrolibException {
		this.mIn.skipBytes(2);
		short flags = this.mIn.readShort();
		int specNamesId = this.mIn.readInt();

		if ((this.mPkg.isCanProguard()) && (!(this.mProguardBuilder.isReplaced(this.mCurEntryID)))
				&& (!(this.mProguardBuilder.isInWhiteList(this.mCurEntryID)))) {
			Main client = this.mApkDecoder.getClient();
			boolean isWhiteList = false;
			if (client.isUseWhiteList()) {
				HashMap whiteList = client.getWhiteList();
				String packName = this.mPkg.getName();
				if (whiteList.containsKey(packName)) {
					HashMap typeMaps = (HashMap) whiteList.get(packName);
					String typeName = this.mType.getName();

					if (typeMaps.containsKey(typeName)) {
						String specName = this.mSpecNames.get(specNamesId).toString();
						HashSet patterns = (HashSet) typeMaps.get(typeName);
						for (Iterator it = patterns.iterator(); it.hasNext();) {
							Pattern p = (Pattern) it.next();
							if (!(p.matcher(specName).matches()))
								break;
							this.mPkg.putSpecNamesReplace(this.mResId, specName);
							this.mPkg.putSpecNamesblock(specName);
							this.mProguardBuilder.setInWhiteList(this.mCurEntryID, true);

							this.mType.putSpecProguardName(specName);
							isWhiteList = true;

							break;
						}

					}

				}

			}

			String replaceString = null;

			if (!(isWhiteList)) {
				boolean keepMapping = false;
				if (client.isUseKeepMapping()) {
					HashMap resMapping = client.getOldResMapping();
					String packName = this.mPkg.getName();
					if (resMapping.containsKey(packName)) {
						HashMap typeMaps = (HashMap) resMapping.get(packName);
						String typeName = this.mType.getName();

						if (typeMaps.containsKey(typeName)) {
							HashMap proguard = (HashMap) typeMaps.get(typeName);
							String specName = this.mSpecNames.get(specNamesId).toString();
							if (proguard.containsKey(specName)) {
								keepMapping = true;
								replaceString = (String) proguard.get(specName);
							}

						}

					}

				}

				if (!(keepMapping)) {
					replaceString = this.mProguardBuilder.getReplaceString();
				}

				this.mProguardBuilder.setInReplaceList(this.mCurEntryID, true);
				if (replaceString == null)
					throw new AndrolibException("readEntry replaceString == null");

				generalResIDMapping(this.mPkg.getName(), this.mType.getName(),
						this.mSpecNames.get(specNamesId).toString(), replaceString);
				this.mPkg.putSpecNamesReplace(this.mResId, replaceString);
				this.mPkg.putSpecNamesblock(replaceString);
				this.mType.putSpecProguardName(replaceString);
			}

		}

		boolean readDirect = false;
		if ((flags & 0x1) == 0) {
			readDirect = true;
			readValue(readDirect, specNamesId);
		} else {
			readDirect = false;
			readComplexEntry(readDirect, specNamesId);
		}
	}

	private void writeEntry() throws IOException, AndrolibException {
		this.mOut.writeBytes(this.mIn, 2);
		short flags = this.mIn.readShort();
		this.mOut.writeShort(flags);
		int specNamesId = this.mIn.readInt();
		ResPackage pkg = this.mPkgs[this.mCurPackageID];

		if (pkg.isCanProguard()) {
			specNamesId = ((Integer) this.mCurSpecNameToPos.get(pkg.getSpecRepplace(this.mResId))).intValue();

			if (specNamesId < 0) {
				throw new AndrolibException(String.format("writeEntry new specNamesId < 0 %d",
						new Object[] { Integer.valueOf(specNamesId) }));
			}

		}

		this.mOut.writeInt(specNamesId);

		if ((flags & 0x1) == 0)
			writeValue();
		else
			writeComplexEntry();
	}

	private void readComplexEntry(boolean flags, int specNamesId) throws IOException, AndrolibException {
		int parent = this.mIn.readInt();
		int count = this.mIn.readInt();

		for (int i = 0; i < count; ++i) {
			this.mIn.readInt();
			readValue(flags, specNamesId);
		}
	}

	private void writeComplexEntry() throws IOException, AndrolibException {
		this.mOut.writeInt(this.mIn.readInt());
		int count = this.mIn.readInt();
		this.mOut.writeInt(count);

		for (int i = 0; i < count; ++i) {
			this.mOut.writeInt(this.mIn.readInt());
			writeValue();
		}
	}

	private void readValue(boolean flags, int specNamesId) throws IOException, AndrolibException {
		this.mIn.skipCheckShort((short) 8);
		this.mIn.skipCheckByte((byte) 0);
		byte type = this.mIn.readByte();
		int data = this.mIn.readInt();

		if ((this.mPkg.isCanProguard()) && (flags) && (type == 3) && (this.mShouldProguardForType)
				&& (this.mShouldProguardTypeSet.contains(this.mType.getName()))
				&& (mTableStringsProguard.get(Integer.valueOf(data)) == null)) {
			String raw = this.mTableStrings.get(data).toString();

			String proguard = this.mPkg.getSpecRepplace(this.mResId);

			int secondSlash = raw.lastIndexOf("/");

			if (secondSlash == -1) {
				throw new AndrolibException(
						String.format("can not find \\ or raw string in res path=%s", new Object[] { raw }));
			}

			String newFilePath = (String) this.mOldFileName.get(raw.substring(0, secondSlash));

			if (newFilePath == null) {
				System.err.printf("can not found new res path, raw=%s\n", new Object[] { raw });
				return;
			}

			String result = newFilePath + "/" + proguard;

			int firstDot = raw.indexOf(".");
			if (firstDot != -1)
				result = result + raw.substring(firstDot);

			String compatibaleraw = new String(raw);
			String compatibaleresult = new String(result);

			if (!(File.separator.contains("/"))) {
				compatibaleresult = compatibaleresult.replace("/", File.separator);
				compatibaleraw = compatibaleraw.replace("/", File.separator);
			}

			File resRawFile = new File(
					this.mApkDecoder.getOutTempDir().getAbsolutePath() + File.separator + compatibaleraw);
			File resDestFile = new File(
					this.mApkDecoder.getOutDir().getAbsolutePath() + File.separator + compatibaleresult);

			HashMap compressData = this.mApkDecoder.getCompressData();
			if (compressData.containsKey(raw)) {
				compressData.put(result, (Integer) compressData.get(raw));
			} else {
				System.err.printf("can not find the compress dataresFile=%s\n", new Object[] { raw });
			}

			if (!(resRawFile.exists())) {
				System.err.printf("can not find res file, you delete it? path: resFile=%s\n",
						new Object[] { resRawFile.getAbsolutePath() });
				return;
			}

			if (resDestFile.exists())
				throw new AndrolibException(String.format("res dest file is already  found: destFile=%s",
						new Object[] { resDestFile.getAbsolutePath() }));

			FileOperation.copyFileUsingStream(resRawFile, resDestFile);
			mTableStringsProguard.put(Integer.valueOf(data), result);
		}
	}

	private void writeValue() throws IOException, AndrolibException {
		this.mOut.writeCheckShort(this.mIn.readShort(), (short) 8);

		this.mOut.writeCheckByte(this.mIn.readByte(), (byte) 0);
		byte type = this.mIn.readByte();
		this.mOut.writeByte(type);

		int data = this.mIn.readInt();
		this.mOut.writeInt(data);
	}

	private void readConfigFlags() throws IOException, AndrolibException {
		int size = this.mIn.readInt();
		if (size < 28) {
			throw new AndrolibException("Config size < 28");
		}

		boolean isInvalid = false;

		short mcc = this.mIn.readShort();
		short mnc = this.mIn.readShort();

		char[] language = { (char) this.mIn.readByte(), (char) this.mIn.readByte() };
		char[] country = { (char) this.mIn.readByte(), (char) this.mIn.readByte() };

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
						String.format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
								new Object[] { Integer.valueOf(38) }));
			} else {
				LOGGER.warning(String.format("Config flags size > %d. Exceeding bytes: 0x%X.",
						new Object[] { Integer.valueOf(38), exceedingBI }));
				isInvalid = true;
			}
		}
	}

	private void writeConfigFlags() throws IOException, AndrolibException {
		int size = this.mIn.readInt();
		if (size < 28)
			throw new AndrolibException("Config size < 28");

		this.mOut.writeInt(size);

		this.mOut.writeBytes(this.mIn, size - 4);
	}

	private Header nextChunk() throws IOException {
		return (this.mHeader = Header.read(this.mIn));
	}

	private void checkChunkType(int expectedType) throws AndrolibException {
		if (this.mHeader.type != expectedType)
			throw new AndrolibException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
					new Object[] { Integer.valueOf(expectedType), Short.valueOf(this.mHeader.type) }));
	}

	private void nextChunkCheckType(int expectedType) throws IOException, AndrolibException {
		nextChunk();
		checkChunkType(expectedType);
	}

	private Header writeNextChunk(int diffSize) throws IOException, AndrolibException {
		this.mHeader = Header.readAndWriteHeader(this.mIn, this.mOut, diffSize);
		return this.mHeader;
	}

	private Header writeNextChunkCheck(int expectedType, int diffSize) throws IOException, AndrolibException {
		this.mHeader = Header.readAndWriteHeader(this.mIn, this.mOut, diffSize);
		if (this.mHeader.type != expectedType)
			throw new AndrolibException(String.format("Invalid chunk type: expected=%d, got=%d",
					new Object[] { Integer.valueOf(expectedType), Short.valueOf(this.mHeader.type) }));

		return this.mHeader;
	}

	private boolean isToProguardFile(String name) {
		return ((!(name.equals("string"))) && (!(name.equals("id"))) && (!(name.equals("array"))));
	}

	public static class FlagsOffset {
		public final int offset;
		public final int count;

		public FlagsOffset(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}
	}

	public static class Header {
		public final short type;
		public final int chunkSize;
		public static final short TYPE_NONE = -1;
		public static final short TYPE_TABLE = 2;
		public static final short TYPE_PACKAGE = 512;
		public static final short TYPE_TYPE = 514;
		public static final short TYPE_CONFIG = 513;

		public Header(short type, int size) {
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
//重新开始写arsc文件，开始写文件头，写文件size等
		public static Header readAndWriteHeader(ExtDataInput in, ExtDataOutput out, int diffSize)
				throws IOException, AndrolibException {
			short type = -1;
			int size = -1;
			try {
				type = in.readShort();
				out.writeShort(type);
				out.writeBytes(in, 2);
				size = in.readInt();
				size -= diffSize;
				if (size <= 0)
					throw new AndrolibException(String.format("readAndWriteHeader size < 0: size=%d",
							new Object[] { Integer.valueOf(size) }));

				out.writeInt(size);
			} catch (EOFException ex) {
				return new Header((short) -1, 0);
			}
			return new Header(type, size);
		}
	}

	private class ProguardStringBuilder {
		private int mReplaceCount = 0;
		private List<String> mReplaceStringBuffer = new ArrayList();
		private boolean[] mIsReplaced;
		private boolean[] mIsWhiteList;
		private String[] mAToZ = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
				"r", "s", "t", "u", "v", "w", "x", "y", "z" };
		private String[] mAToAll = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "_", "a", "b", "c", "d", "e",
				"f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y",
				"z" };
		private HashSet<String> mFileNameBlackList;

		public ProguardStringBuilder() {
			this.mFileNameBlackList = new HashSet();
			this.mFileNameBlackList.add("con");
			this.mFileNameBlackList.add("prn");
			this.mFileNameBlackList.add("aux");
			this.mFileNameBlackList.add("nul");
		}
//创建用来混淆的混淆名，a-z /aa-az /aaa-zzz
		public void reset() {
			String first;
			int j;
			String second;
			this.mReplaceStringBuffer.clear();
			for (int i = 0; i < this.mAToZ.length; ++i) {
				this.mReplaceStringBuffer.add(this.mAToZ[i]);
			}

			for (int i = 0; i < this.mAToZ.length; ++i) {
				first = this.mAToZ[i];
				for (j = 0; j < this.mAToAll.length; ++j) {
					second = this.mAToAll[j];
					this.mReplaceStringBuffer.add(first + second);
				}

			}

			for (int i = 0; i < this.mAToZ.length; ++i) {
				first = this.mAToZ[i];
				for (j = 0; j < this.mAToAll.length; ++j) {
					second = this.mAToAll[j];
					for (int k = 0; k < this.mAToAll.length; ++k) {
						String third = this.mAToAll[k];
						String result = first + second + third;
						if (!(this.mFileNameBlackList.contains(result)))
							this.mReplaceStringBuffer.add(first + second + third);
					}
				}
			}
			this.mReplaceCount = 3;

			int size = this.mReplaceStringBuffer.size();
			this.mIsReplaced = new boolean[size];
			this.mIsWhiteList = new boolean[size];
			for (int i = 0; i < size; ++i) {
				this.mIsReplaced[i] = false;
				this.mIsWhiteList[i] = false;
			}
		}

		public void removeStrings(Collection collection) {
			this.mReplaceStringBuffer.removeAll(collection);
		}

		public boolean isReplaced(int id) {
			return this.mIsReplaced[id];
		}

		public boolean isInWhiteList(int id) {
			return this.mIsWhiteList[id];
		}

		public void setInWhiteList(int id,boolean set) {
			this.mIsWhiteList[id] = set;
		}

		public void setInReplaceList(int id,boolean set) {
			this.mIsReplaced[id] = set;
		}

		//获取将要用来混淆的文件名，用完一个移除1个
		public String getReplaceString() throws AndrolibException {
			if (this.mReplaceStringBuffer.isEmpty()) {
				throw new AndrolibException(
						String.format("now can only proguard less than 35594 in a single type\n", new Object[0]));
			}

			return ((String) this.mReplaceStringBuffer.remove(0));
		}

		public int lenght() {
			return this.mReplaceStringBuffer.size();
		}
	}
}