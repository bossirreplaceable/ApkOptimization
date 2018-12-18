package com.dongnao.ricky.resourceproguard;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Configuration {
	protected static final String TAG_ISSUE = "issue";
	protected static final String ATTR_VALUE = "value";
	protected static final String ATTR_ID = "id";
	protected static final String ATTR_ACTIVE = "isactive";
	protected static final String PROPERTY_ISSUE = "property";
	protected static final String WHITELIST_ISSUE = "whitelist";
	protected static final String COMPRESS_ISSUE = "compress";
	protected static final String MAPPING_ISSUE = "keepmapping";
	protected static final String SIGN_ISSUE = "sign";
	protected static final String ATTR_7ZIP = "seventzip";
	protected static final String ATTR_SIGNFILE = "metaname";
	protected static final String ATTR_SIGNFILE_PATH = "path";
	protected static final String ATTR_SIGNFILE_KEYPASS = "keypass";
	protected static final String ATTR_SIGNFILE_STOREPASS = "storepass";
	protected static final String ATTR_SIGNFILE_ALIAS = "alias";
	protected static final String ATTR_ZIPALIGN = "zipalign";
	private Main mClient;
	private final File mConfigFile;
	public boolean mUse7zip = true;
	public String mMetaName = "META-INF";
	public boolean mUseSignAPk;
	public File mSignatureFile;
	public File mOldMappingFile;
	public String mKeyPass;
	public String mStorePass;
	public String mStoreAlias;
	public boolean mUseWhiteList;
	public boolean mUseCompress;
	public boolean mUseKeepMapping;
	public HashMap<String, HashMap<String, HashSet<Pattern>>> mWhiteList;
	public HashMap<String, HashMap<String, HashMap<String, String>>> mOldResMapping;
	public HashMap<String, String> mOldFileMapping;
	public HashSet<Pattern> mCompressPatterns;

	public Configuration(File config, Main m) {
		this.mConfigFile = config;
		this.mClient = m;
	}

	public File getConfigFile() {
		return this.mConfigFile;
	}

	private void readProperty(Node node) throws IOException {
		NodeList childNodes = node.getChildNodes();

		if (childNodes.getLength() > 0) {
			int j = 0;
			for (int n = childNodes.getLength(); j < n; ++j) {
				Node child = childNodes.item(j);
				if (child.getNodeType() == 1) {
					Element check = (Element) child;
					String tagName = check.getTagName();
					String vaule = check.getAttribute("value");
					if (vaule.length() == 0) {
						throw new IOException(String.format("Invalid config file: Missing required attribute %s\n",
								new Object[] { "value" }));
					}

					if (tagName.equals("seventzip")) {
						this.mUse7zip = ((vaule != null) ? vaule.equals("true") : false);
					} else if (tagName.equals("metaname")) {
						this.mMetaName = vaule;
						this.mMetaName = this.mMetaName.trim();
					} else {
						System.err.println("unknown tag " + tagName);
					}
				}
			}
		}
	}

	private void readOldMapping(Node node) throws IOException {
		NodeList childNodes = node.getChildNodes();

		if (childNodes.getLength() > 0) {
			int j = 0;
			for (int n = childNodes.getLength(); j < n; ++j) {
				Node child = childNodes.item(j);
				if (child.getNodeType() == 1) {
					Element check = (Element) child;
					String vaule = check.getAttribute("value");
					if (vaule.length() == 0) {
						throw new IOException(String.format("Invalid config file: Missing required attribute %s\n",
								new Object[] { "value" }));
					}

					this.mOldMappingFile = new File(vaule);

					if (!(this.mOldMappingFile.exists())) {
						throw new IOException(String.format("the old mapping file do not exit, raw path= %s\n",
								new Object[] { this.mOldMappingFile.getAbsolutePath() }));
					}

					processOldMappingFile();
					System.out.printf("you are using the keepmapping mode to proguard resouces: old mapping path:%s\n",
							new Object[] { this.mOldMappingFile.getAbsolutePath() });
				}
			}
		}
	}

	private void processOldMappingFile() throws IOException {
		this.mOldResMapping = new HashMap<String, HashMap<String, HashMap<String, String>>>();
		this.mOldFileMapping = new HashMap<String, String>();
		this.mOldResMapping.clear();
		this.mOldFileMapping.clear();

		FileReader fr = null;
		try {
			fr = new FileReader(this.mOldMappingFile);
		} catch (FileNotFoundException ex) {
			throw new IOException(String.format("Could not find old mapping file %s",
					new Object[] { this.mOldMappingFile.getAbsolutePath() }));
		}

		BufferedReader br = new BufferedReader(fr);
		try {
			String line = br.readLine();
			Pattern pattern = Pattern.compile("\\s+(.*)->(.*)");
			while (line != null) {
				if (line.length() > 0) {
					Matcher mat = pattern.matcher(line);

					if (mat.find()) {
						String nameAfter = mat.group(2);
						String nameBefore = mat.group(1);
						nameAfter = nameAfter.trim();
						nameBefore = nameBefore.trim();

						if (line.contains("/")) {
							this.mOldFileMapping.put(nameBefore, nameAfter);
						} else {
							HashMap<String, HashMap<String, String>> typeMap;
							HashMap<String, String> namesMap;
							int packagePos = nameBefore.indexOf(".R.");
							if (packagePos == -1)
								throw new IOException(String.format(
										"the old mapping file packagename is malformed, it should be like com.tencent.mm.R.attr.test, yours %s\n",
										new Object[] { nameBefore }));

							String packageName = nameBefore.substring(0, packagePos);

							int nextDot = nameBefore.indexOf(".", packagePos + 3);
							String typeName = nameBefore.substring(packagePos + 3, nextDot);

							String beforename = nameBefore.substring(nextDot + 1);
							String aftername = nameAfter.substring(nameAfter.indexOf(".", packagePos + 3) + 1);

							if (this.mOldResMapping.containsKey(packageName))
								typeMap = (HashMap<String, HashMap<String, String>>) this.mOldResMapping.get(packageName);
							else {
								typeMap = new HashMap<String, HashMap<String, String>>();
							}

							if (typeMap.containsKey(typeName))
								namesMap = (HashMap<String, String>) typeMap.get(typeName);
							else
								namesMap = new HashMap<String, String>();

							namesMap.put(beforename, aftername);

							typeMap.put(typeName, namesMap);
							this.mOldResMapping.put(packageName, typeMap);
						}
					}

				}

				line = br.readLine();
			}
		} catch (IOException ex) {
			throw new RuntimeException("Error while mapping file");
		} finally {
			try {
				if (br != null)
					br.close();
				if (fr == null)
					return;
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	//读取签名文件
	private void readSign(Node node) throws IOException {
		NodeList childNodes = node.getChildNodes();

		if (childNodes.getLength() > 0) {
			int j = 0;
			for (int n = childNodes.getLength(); j < n; ++j) {
				Node child = childNodes.item(j);
				if (child.getNodeType() == 1) {
					Element check = (Element) child;
					String tagName = check.getTagName();
					String vaule = check.getAttribute("value");
					if (vaule.length() == 0) {
						throw new IOException(String.format("Invalid config file: Missing required attribute %s\n",
								new Object[] { "value" }));
					}

					if (tagName.equals("path")) {
						this.mSignatureFile = new File(vaule);
						if (this.mSignatureFile.exists())
							continue;
						throw new IOException(String.format("the signature file do not exit, raw path= %s\n",
								new Object[] { this.mSignatureFile.getAbsolutePath() }));
					}

					if (tagName.equals("storepass")) {
						this.mStorePass = vaule;
						this.mStorePass = this.mStorePass.trim();
					} else if (tagName.equals("keypass")) {
						this.mKeyPass = vaule;
						this.mKeyPass = this.mKeyPass.trim();
					} else if (tagName.equals("alias")) {
						this.mStoreAlias = vaule;
						this.mStoreAlias = this.mStoreAlias.trim();
					} else {
						System.err.println("unknown tag " + tagName);
					}
				}
			}
		}
	}
//设置签名文件密码别名等
	public void setSignData(File SigntureFile, String keypass, String storealias, String storepass, boolean signApk)
			throws IOException {
		this.mUseSignAPk = signApk;
		if (this.mUseSignAPk) {
			this.mSignatureFile = SigntureFile;

			if (!(this.mSignatureFile.exists())) {
				throw new IOException(String.format("the signature file do not exit, raw path= %s\n",
						new Object[] { this.mSignatureFile.getAbsolutePath() }));
			}

			this.mKeyPass = keypass;
			this.mStoreAlias = storealias;
			this.mStorePass = storepass;
		}
	}

	//读取映射文件，设置输入映射文件地址,开启则会输出修改前的映射关系，否则不会输出
	public void setKeepMappingData(File mappingFile, boolean usemapping) throws IOException {
		this.mUseKeepMapping = usemapping;
		if (this.mUseKeepMapping) {
			this.mOldMappingFile = mappingFile;

			if (!(this.mOldMappingFile.exists())) {
				throw new IOException(String.format("the old mapping file do not exit, raw path= %s\n",
						new Object[] { this.mOldMappingFile.getAbsolutePath() }));
			}

			processOldMappingFile();
		}
	}

	//读取白名单，不需要混淆的文件清单
	private void readWhiteList(Node node) throws IOException {
		NodeList childNodes = node.getChildNodes();
		this.mWhiteList = new HashMap<String, HashMap<String, HashSet<Pattern>>>();

		if (childNodes.getLength() > 0) {
			int j = 0;
			for (int n = childNodes.getLength(); j < n; ++j) {
				Node child = childNodes.item(j);
				if (child.getNodeType() == 1) {
					HashMap<String, HashSet<Pattern>> typeMap;
					HashSet<Pattern> patterns;
					Element check = (Element) child;
					String vaule = check.getAttribute("value");
					if (vaule.length() == 0) {
						throw new IOException("Invalid config file: Missing required attribute value");
					}

					int packagePos = vaule.indexOf(".R.");
					if (packagePos == -1) {
						throw new IOException(String.format(
								"please write the full package name,eg com.tencent.mm.R.drawable.dfdf, but yours %s\n",
								new Object[] { vaule }));
					}

					vaule = vaule.trim();
					String packageName = vaule.substring(0, packagePos);

					int nextDot = vaule.indexOf(".", packagePos + 3);
					String typeName = vaule.substring(packagePos + 3, nextDot);

					String name = vaule.substring(nextDot + 1);

					if (this.mWhiteList.containsKey(packageName))
						typeMap = (HashMap<String, HashSet<Pattern>>) this.mWhiteList.get(packageName);
					else {
						typeMap = new HashMap<String, HashSet<Pattern>>();
					}

					if (typeMap.containsKey(typeName))
						patterns = (HashSet<Pattern>) typeMap.get(typeName);
					else {
						patterns = new HashSet<Pattern>();
					}

					name = convetToPatternString(name);

					Pattern pattern = Pattern.compile(name);

					patterns.add(pattern);
					typeMap.put(typeName, patterns);
					this.mWhiteList.put(packageName, typeMap);
				}
			}
		}
	}
//格式转化，主要是*类型通配
	private String convetToPatternString(String input) {
		if (input.contains(".")) {
			input = input.replaceAll("\\.", "\\\\.");
		}

		if (input.contains("?")) {
			input = input.replaceAll("\\?", "\\.");
		}

		if (input.contains("*")) {
			input = input.replace("*", ".+");
		}

		return input;
	}
//读取compress
	private void readCompress(Node node) throws IOException {
		NodeList childNodes = node.getChildNodes();
		this.mCompressPatterns = new HashSet<Pattern>();

		if (childNodes.getLength() > 0) {
			int j = 0;
			for (int n = childNodes.getLength(); j < n; ++j) {
				Node child = childNodes.item(j);
				if (child.getNodeType() == 1) {
					Element check = (Element) child;
					String vaule = check.getAttribute("value");
					if (vaule.length() == 0) {
						throw new IOException(String.format("Invalid config file: Missing required attribute %s\n",
								new Object[] { "value" }));
					}

					vaule = convetToPatternString(vaule);

					Pattern pattern = Pattern.compile(vaule);

					this.mCompressPatterns.add(pattern);
				}
			}
		}
	}

	//开始读配置文件，解析xml
	public void readConfig() throws IOException, ParserConfigurationException, SAXException {
		if (!(this.mConfigFile.exists()))
			return;

		System.out.printf("reading config file, %s\n", new Object[] { this.mConfigFile.getAbsolutePath() });

		BufferedInputStream input = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			input = new BufferedInputStream(new FileInputStream(this.mConfigFile));
			InputSource source = new InputSource(input);
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(source);
			NodeList issues = document.getElementsByTagName("issue");

			int i = 0;
			for (int count = issues.getLength(); i < count; ++i) {
				Node node = issues.item(i);

				Element element = (Element) node;
				String id = element.getAttribute("id");
				String isActive = element.getAttribute("isactive");
				if (id.length() == 0) {
					System.err.println("Invalid config file: Missing required issue id attribute");
				} else {
					boolean active = (isActive != null) ? isActive.equals("true") : false;

					if (id.equals("property")) {
						readProperty(node);
					} else if (id.equals("whitelist")) {
						this.mUseWhiteList = active;
						if (this.mUseWhiteList)
							readWhiteList(node);
					} else if (id.equals("compress")) {
						this.mUseCompress = active;
						if (this.mUseCompress)
							readCompress(node);
					} else if (id.equals("sign")) {
						if (!(this.mClient.getSetSignThroughCmd())) {
							this.mUseSignAPk = active;
							if (this.mUseSignAPk)
								readSign(node);
						}
					} else if (id.equals("keepmapping")) {
						if (!(this.mClient.getSetMappingThroughCmd())) {
							this.mUseKeepMapping = active;
							if (this.mUseKeepMapping)
								readOldMapping(node);
						}
					} else {
						System.err.println("unknown issue " + id);
					}
				}
			}
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					System.exit(-1);
				}
		}
	}
}