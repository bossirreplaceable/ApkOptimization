package com.dongnao.ricky.resourceproguard;

import com.dongnao.ricky.androlib.AndrolibException;
import com.dongnao.ricky.androlib.ApkDecoder;
import com.dongnao.ricky.androlib.ResourceApkBuilder;
import com.dongnao.ricky.androlib.ResourceRepackage;
import com.dongnao.ricky.directory.DirectoryException;
import com.dongnao.ricky.util.FileOperation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Main {
    private static final int ERRNO_ERRORS = 1;
    private static final int ERRNO_USAGE = 2;
    private static String mRunningLocation;
    private static final String ARG_HELP = "--help";
    private static final String ARG_OUT = "-out";
    private static final String ARG_CONFIG = "-config";
    private static final String ARG_7ZIP = "-7zip";
    private static final String ARG_ZIPALIGN = "-zipalign";
    private static final String ARG_SIGNATURE = "-signature";
    private static final String ARG_KEEPMAPPING = "-mapping";
    private static final String ARG_REPACKAGE = "-repackage";
    private Configuration mConfiguration;
    private File mOutDir;
    private static long mBeginTime;
    private static long mRawApkSize;
    private boolean mSetSignThroughCmd = false;
    private boolean mSetMappingThroughCmd = false;
    private String m7zipPath = null;
    private String mZipalignPath = null;

    public static void main(String[] args) {
        mBeginTime = System.currentTimeMillis();

        Main m = new Main();
        getRunningLocation(m);
        String[] files = new String[]{"TLint-debug.apk", "-config","config.xml","-7zip","7za.exe","-zipalign","zipalign.exe"};

        m.run(files);
    }

    public String get7zipPath() {
        return mRunningLocation+this.m7zipPath;
    }

    public String getZipalignPath() {
        return mRunningLocation+this.mZipalignPath;
    }

    boolean getSetSignThroughCmd() {
        return this.mSetSignThroughCmd;
    }

    boolean getSetMappingThroughCmd() {
        return this.mSetMappingThroughCmd;
    }

    //获取执行环境路径，一般是x盘/workspace/projectName/bin
    private static void getRunningLocation(Main m) {
        mRunningLocation = m.getClass().getProtectionDomain().getCodeSource().getLocation
                ().getPath();
        System.out.println("1--------mRunningLocation=" + mRunningLocation);
        try {
            mRunningLocation = URLDecoder.decode(mRunningLocation, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (mRunningLocation.endsWith(".jar"))
            mRunningLocation = mRunningLocation.substring(0,
                    mRunningLocation.lastIndexOf(File.separator) + 1);
        System.out.println("2--------mRunningLocation=" + mRunningLocation);
//        File f = new File(mRunningLocation);
//        mRunningLocation = f.getAbsolutePath();
        System.out.println("3--------mRunningLocation=" + mRunningLocation);
    }

    private void run(String[] args) {
        if (args.length < 1) {
            goToError();
        }

        File configFile = null;//配置文件
        File outputFile = null;//输出文件
        String apkFileName = null;//apk输入文件

        File signatureFile = null;//签名秘钥
        File mappingFile = null;//修改后映射文件
        String keypass = null;//秘钥主密码
        String storealias = null;//别名
        String storepass = null;//别名密码

        String signedFile = null;//签名apk
        //检查参数
        for (int index = 0; index < args.length; ++index) {
            String arg = args[index];
            if ((arg.equals("--help")) || (arg.equals("-h"))) {
                goToError();
            } else if (arg.equals("-config")) {//配置文件不能最后读，后面的执行需要依赖配置文件，或者不是.xml则报错
                if ((index == args.length - 1) || (!(args[(index + 1)].endsWith(".xml")))) {
                    System.err.println("Missing XML configuration file argument");
                    goToError();
                }
                //参数后面的参数才是config配置文件的路径
                configFile = new File(mRunningLocation+"/out/"+args[(++index)]);
                if (!(configFile.exists())) {
                    System.err.println(configFile.getAbsolutePath() + " does not exist");
                    goToError();
                }
                System.out.printf("special configFile file path: %s\n", new Object[]{configFile.getAbsolutePath()});
            } else if (arg.equals("-out")) {
                if (index == args.length - 1) {
                    System.err.println("Missing output file argument");
                    goToError();
                }
                outputFile = new File(args[(++index)]);
                File parent = outputFile.getParentFile();
                if ((parent != null) && (!(parent.exists())))
                    parent.mkdirs();

                System.out.printf("special output directory path: %s\n", new Object[]{outputFile.getAbsolutePath()});
            } else if (arg.equals("-signature")) {
                if (index == args.length - 1) {
                    System.err.println("Missing signature data argument, should be -signature signature_file_path storepass keypass storealias");

                    goToError();
                }

                signatureFile = new File(args[(++index)]);

                if (index == args.length - 1) {
                    System.err.println("Missing signature data argument, should be -signature signature_file_path storepass keypass storealias");

                    goToError();
                }

                storepass = args[(++index)];

                if (index == args.length - 1) {
                    System.err.println("Missing signature data argument, should be -signature signature_file_path storepass keypass storealias");

                    goToError();
                }

                keypass = args[(++index)];

                if (index == args.length - 1) {
                    System.err.println("Missing signature data argument, should be -signature signature_file_path storepass keypass storealias");

                    goToError();
                }

                storealias = args[(++index)];

                this.mSetSignThroughCmd = true;
            } else if (arg.equals("-mapping")) {
                if (index == args.length - 1) {
                    System.err.println("Missing mapping file argument");
                    goToError();
                }

                mappingFile = new File(args[(++index)]);

                this.mSetMappingThroughCmd = true;
            } else if (arg.equals("-7zip")) {
                if (index == args.length - 1) {
                    System.err.println("Missing 7zip path argument");
                    goToError();
                }
                this.m7zipPath = args[(++index)];
            } else if (arg.equals("-zipalign")) {
                if (index == args.length - 1) {
                    System.err.println("Missing zipalign path argument");
                    goToError();
                }

                this.mZipalignPath = args[(++index)];
            } else if (arg.equals("-repackage")) {
                if (index == args.length - 1) {
                    System.err.println("Missing the signed apk file argument");
                    goToError();
                }

                signedFile = args[(++index)];
            } else {
                apkFileName = arg;
            }

        }

        if (signedFile != null) {
            ResourceRepackage repackage = new ResourceRepackage(this, new File(signedFile));
            try {
                if (outputFile != null)
                    repackage.setOutDir(outputFile);

                repackage.repackageApk();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        //参数里没有配置文件，在默认根目录下\bin下去找
        if (configFile == null) {
            configFile = new File(mRunningLocation + File.separator + "config.xml");
            if (!(configFile.exists())) {
                System.err.printf("the config file %s does not exit", new Object[]{configFile.getAbsolutePath()});
                printUsage(System.err);
                System.exit(2);
            }
        }

        System.out.printf("resourceprpguard begin\n", new Object[0]);
        this.mConfiguration = new Configuration(configFile, this);
        try {
            //找到配置文件开始读配置文件
            this.mConfiguration.readConfig();
            if (this.mSetSignThroughCmd) {
                this.mConfiguration.setSignData(signatureFile, keypass, storealias, storepass, true);
            }
            System.out.println("start decoder");
            if (!(this.mSetMappingThroughCmd)) {

            } else {
                this.mConfiguration.setKeepMappingData(mappingFile, true);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            goToError();
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
            goToError();
        } catch (SAXException e1) {
            e1.printStackTrace();
            goToError();
        }
        ApkDecoder decoder = new ApkDecoder(this);

        File apkFile = new File(mRunningLocation+apkFileName);
        if (!(apkFile.exists())) {
            System.err.printf("the input apk %s does not exit", new Object[]{apkFile.getAbsolutePath()});
            goToError();
        }
        mRawApkSize = FileOperation.getFileSizes(apkFile);
        decoder.setApkFile(apkFile);
        if (outputFile == null)
            this.mOutDir = new File(mRunningLocation + File.separator + apkFile.getName().substring(0, apkFile.getName().indexOf(".apk")));
        else
            this.mOutDir = outputFile;

        try {
            decoder.setOutDir(this.mOutDir.getAbsoluteFile());
            decoder.decode();
        } catch (AndrolibException e) {
            e.printStackTrace();
            goToError();
        } catch (IOException e) {
            e.printStackTrace();
            goToError();
        } catch (DirectoryException e) {
            e.printStackTrace();
            goToError();
        }

        ResourceApkBuilder builder = new ResourceApkBuilder(this);

        String apkBasename = apkFile.getName();
        apkBasename = apkBasename.substring(0, apkBasename.indexOf(".apk"));
        try {
            builder.setOutDir(this.mOutDir, apkBasename);
            builder.buildApk(decoder.getCompressData());
        } catch (AndrolibException e) {
            e.printStackTrace();
            goToError();
        } catch (IOException e) {
            e.printStackTrace();
            goToError();
        } catch (InterruptedException e) {
            e.printStackTrace();
            goToError();
        }

        System.out.printf("resources proguard done, total time cost: %fs\n", new Object[]{Double.valueOf(diffTimeFromBegin())});
        System.out.printf("resources proguard done, you can go to file to find the output %s\n", new Object[]{this.mOutDir.getAbsolutePath()});
    }

    public double diffTimeFromBegin() {
        long end = System.currentTimeMillis();
        return ((end - mBeginTime) / 1000.0D);
    }

    public double diffApkSizeFromRaw(long size) {
        return ((mRawApkSize - size) / 1024.0D);
    }

    private void goToError() {
        printUsage(System.err);
        System.exit(2);
    }

    public String getRunningLocation() {
        return mRunningLocation;
    }

    public String getMetaName() {
        return this.mConfiguration.mMetaName;
    }

    public File getConfigFile() {
        return ((this.mConfiguration != null) ? this.mConfiguration.getConfigFile() : null);
    }

    public boolean isUseWhiteList() {
        return this.mConfiguration.mUseWhiteList;
    }

    public HashMap<String, HashMap<String, HashSet<Pattern>>> getWhiteList() {
        return this.mConfiguration.mWhiteList;
    }

    public boolean isUseCompress() {
        return this.mConfiguration.mUseCompress;
    }

    public HashSet<Pattern> getCompressPatterns() {
        return this.mConfiguration.mCompressPatterns;
    }

    public boolean isUseSignAPk() {
        return this.mConfiguration.mUseSignAPk;
    }

    public File getSignatureFile() {
        return this.mConfiguration.mSignatureFile;
    }

    public String getKeyPass() {
        return this.mConfiguration.mKeyPass;
    }

    public String getStorePass() {
        return this.mConfiguration.mStorePass;
    }

    public String getStoreAlias() {
        return this.mConfiguration.mStoreAlias;
    }

    public boolean isUse7zip() {
        return this.mConfiguration.mUse7zip;
    }

    public boolean isUseKeepMapping() {
        return this.mConfiguration.mUseKeepMapping;
    }

    public HashMap<String, String> getOldFileMapping() {
        return this.mConfiguration.mOldFileMapping;
    }

    public HashMap<String, HashMap<String, HashMap<String, String>>> getOldResMapping() {
        return this.mConfiguration.mOldResMapping;
    }

    private static void printUsage(PrintStream out) {
        String command = "resousceproguard.jar";
        out.println();
        out.println();
        out.println("Usage: java -jar " + command + " input.apk");
        out.println("if you want to special the output path or config file path, you can input:");
        out.println("Such as: java -jar " + command + " " + "input.apk " + "-config" + " yourconfig.xml " + "-out" + " output_directory");
        out.println("if you want to special the sign or mapping data, you can input:");
        out.println("Such as: java -jar " + command + " " + "input.apk " + "-config" + " yourconfig.xml " + "-out" + " output_directory " +
                "-signature" + " signature_file_path storepass keypass storealias " + "-mapping" + " mapping_file_path");

        out.println("if you want to special 7za or zipalign path, you can input:");
        out.println("Such as: java -jar " + command + " " + "input.apk " + "-7zip" + " /home/shwenzhang/tools/7za " + "-zipalign" + "/home/shwenzhang/sdk/tools/zipalign");

        out.println("if you just want to repackage an apk compress with 7z:");
        out.println("Such as: java -jar " + command + " " + "-repackage" + " input.apk");
        out.println("if you want to special the output path, 7za or zipalign path, you can input:");
        out.println("Such as: java -jar " + command + " " + "-repackage" + " input.apk" + "-out" + " output_directory " + "-7zip" + " /home/shwenzhang/tools/7za " + "-zipalign" + "/home/shwenzhang/sdk/tools/zipalign");
        out.println();
        out.println("Flags:\n");

        printUsage(out, new String[]{
                "--help", "This message.",
                "-h", "short for -help",
                "-out", "set the output directory yourself, if not, the default directory is the running location with name of the input file",
                "-config", "set the config file yourself, if not, the default path is the running location with name config.xml",
                "-signature", "set sign property, following by parameters: signature_file_path storepass keypass storealias",
                "  ", "if you set these, the sign data in the config file will be overlayed",
                "-mapping", "set keep mapping property, following by parameters: mapping_file_path",
                "  ", "if you set these, the mapping data in the config file will be overlayed",
                "-7zip", "set the 7zip path, such as /home/shwenzhang/tools/7za, window will be end of 7za.exe",
                "-zipalign", "set the zipalign, such as /home/shwenzhang/sdk/tools/zipalign, window will be end of zipalign.exe",
                "-repackage", "usually, when we build the channeles apk, it may destroy the 7zip.",
                "  ", "so you may need to use 7zip to repackage the apk"});

        out.println();
        out.println("if you donot know how to write the config file, look at the comment in the default config.xml");
        out.println("if you want to use 7z, you must install the 7z command line version in window;");
        out.println("sudo apt-get install p7zip-full in linux");

        out.println();
        out.println("further more:");
        out.println("welcome to use resourcesprogurad, it is used for proguard resource, aurthor: shwenzhang, com.tencet.mm");
        out.println("if you find any problem, please contact shwenzhang at any time!");
    }

    private static void printUsage(PrintStream out, String[] args) {
        int argWidth = 0;
        for (int i = 0; i < args.length; i += 2) {
            String arg = args[i];
            argWidth = Math.max(argWidth, arg.length());
        }
        argWidth += 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argWidth; ++i)
            sb.append(' ');

        String indent = sb.toString();
        String formatString = "%1$-" + argWidth + "s%2$s";

        for (int i = 0; i < args.length; i += 2) {
            String arg = args[i];
            String description = args[(i + 1)];
            if (arg.length() == 0)
                out.println(description);
            else
                out.print(wrap(String.format(formatString, new Object[]{arg, description}),
                        300, indent));
        }
    }

    static String wrap(String explanation, int lineWidth, String hangingIndent) {
        int explanationLength = explanation.length();
        StringBuilder sb = new StringBuilder(explanationLength * 2);
        int index = 0;

        while (index < explanationLength) {
            int next;
            int lineEnd = explanation.indexOf(10, index);

            if ((lineEnd != -1) && (lineEnd - index < lineWidth)) {
                next = lineEnd + 1;
            } else {
                lineEnd = Math.min(index + lineWidth, explanationLength);
                if (lineEnd - index < lineWidth) {
                    next = explanationLength;
                } else {
                    int lastSpace = explanation.lastIndexOf(32, lineEnd);
                    if (lastSpace > index) {
                        lineEnd = lastSpace;
                        next = lastSpace + 1;
                    } else {
                        next = lineEnd + 1;
                    }
                }
            }

            if (sb.length() > 0)
                sb.append(hangingIndent);
            else {
                lineWidth -= hangingIndent.length();
            }

            sb.append(explanation.substring(index, lineEnd));
            sb.append('\n');
            index = next;
        }

        return sb.toString();
    }
}