package com.nextlabs.installer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

public class EDRMInstaller {

	private static Scanner scanner;
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static void main(String args[]) {
		scanner = new Scanner(System.in);

		ArrayList<String> tomcat_shared_lib_jars = new ArrayList<String>();
		tomcat_shared_lib_jars.add("commons-codec-1.10.jar");
		tomcat_shared_lib_jars.add("commons-io-2.4.jar");
		tomcat_shared_lib_jars.add("commons-lang3-3.5.jar");
		tomcat_shared_lib_jars.add("dom4j-1.6.1.jar");
		tomcat_shared_lib_jars.add("gson-2.3.1.jar");
		tomcat_shared_lib_jars.add("guava-18.0.jar");
		tomcat_shared_lib_jars.add("log4j-api-2.10.0.jar");
		tomcat_shared_lib_jars.add("nextlabs-jtagger.jar");
		tomcat_shared_lib_jars.add("pdfbox-app-2.0.0.jar");
		tomcat_shared_lib_jars.add("poi-3.11-20141221.jar");
		tomcat_shared_lib_jars.add("poi-ooxml-3.11-20141221.jar");
		tomcat_shared_lib_jars.add("poi-ooxml-schemas-3.11-20141221.jar");
		tomcat_shared_lib_jars.add("rmjavasdk-ng.jar");
		tomcat_shared_lib_jars.add("shared.jar");
		tomcat_shared_lib_jars.add("xmlbeans-2.6.0.jar");
		tomcat_shared_lib_jars.add("bcpkix-jdk15on-1.57.jar");
		tomcat_shared_lib_jars.add("bcprov-jdk15on-1.57.jar");
		tomcat_shared_lib_jars.add("commons-text-1.8.jar");
		tomcat_shared_lib_jars.add("commons-configuration2-2.5.jar");
		
		
		System.out.println("----------- EDRM JCO Plugin Installation Starts -----------------");

		System.out.println(" Enter the Tomcat home path:");
		CommonInstallerValues.tomcatPath = scanner.nextLine();
		System.out.println("TOMCAT_PATH:" + CommonInstallerValues.tomcatPath);

		String zip_package_root_path = new EDRMInstaller().getClass().getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		System.out.println("Zip Package path:" + zip_package_root_path);
		try {
			zip_package_root_path = URLDecoder.decode(zip_package_root_path, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			System.out.println(e1.getMessage());
		}
		
		if (isWindows())
			zip_package_root_path = zip_package_root_path.substring(1, zip_package_root_path.indexOf("jar"));
		else
			zip_package_root_path = zip_package_root_path.substring(0, zip_package_root_path.indexOf("jar"));
		String jar_path = zip_package_root_path + "jars";
		String config_path = zip_package_root_path + "config";
		String xlib_path = zip_package_root_path + "xlib/jar";
		System.out.println("Zip Package path:" + zip_package_root_path);

		String edrm_jar_path = CommonInstallerValues.tomcatPath + "/nextlabs/dpc/jservice/jar/SAPJCo-EDRM.jar";
		String edrm_jar_propertyFilePath = CommonInstallerValues.tomcatPath
				+ "/nextlabs/dpc/jservice/config/SAPJCo-EDRM.properties";
		String package_edrm_jar_path = jar_path + "/SAPJCo-EDRM.jar";
		String package_edrm_propertyFilePath = config_path + "/SAPJCo-EDRM.properties";
		try {
		
			copyTomcatSharedLibJars(tomcat_shared_lib_jars, xlib_path);

			PropertyFileConfigurer.configureProperties(package_edrm_propertyFilePath);
			copyFiles(package_edrm_jar_path, edrm_jar_path);
			System.out.println("------------ EDRM JCO Plugin property file copied to DPC folder -----------------");
			copyFiles(package_edrm_propertyFilePath, edrm_jar_propertyFilePath);
			System.out.println("------------ EDRM JCO Plugin jar copied to DPC folder -----------------");

		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		System.out.println("------------ EDRM JCO Plugin Installation Ends -----------------");
	}

	private static void copyFiles(String sourcePath, String destinationPath) throws IOException {
		Path sourcepath = Paths.get(sourcePath);
		Path destinationepath = Paths.get(destinationPath);
		Files.copy(sourcepath, destinationepath, StandardCopyOption.REPLACE_EXISTING);

	}

	private static void copyTomcatSharedLibJars(ArrayList<String> tomcat_shared_lib_jars, String xlib_path)
			throws IOException {
		String tomcat_path = CommonInstallerValues.tomcatPath + "/nextlabs/shared_lib";
		for (String fileName : tomcat_shared_lib_jars) {
			Path sourcepath = Paths.get(xlib_path + "/" + fileName);
			Path destinationepath = Paths.get(tomcat_path + "/" + fileName);
			Files.copy(sourcepath, destinationepath, StandardCopyOption.REPLACE_EXISTING);
		}

		System.out.println("------------ EDRM JCO Plugin XLIB jars copied to tomcat folder -----------------");

	}


	public static boolean isWindows() {

		return (OS.indexOf("win") >= 0);

	}

	public static boolean isUnix() {

		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);

	}

}
