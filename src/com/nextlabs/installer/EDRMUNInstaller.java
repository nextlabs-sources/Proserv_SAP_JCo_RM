package com.nextlabs.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class EDRMUNInstaller {

	private static Scanner scanner;
	
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
		
		System.out.println("----------- EDRM JCO Plugin UnInstallation Starts -----------------");

	
		System.out.println(" Enter the Tomcat home path:");
		CommonInstallerValues.tomcatPath = scanner.nextLine();
		System.out.println(	"TOMCAT_PATH:"+CommonInstallerValues.tomcatPath);
	
		String edrm_jar_path = CommonInstallerValues.tomcatPath + "/nextlabs/dpc/jservice/jar/SAPJCo-EDRM.jar";
		String edrm_jar_propertyFilePath = CommonInstallerValues.tomcatPath
				+ "/nextlabs/dpc/jservice/config/SAPJCo-EDRM.properties";

		try {
			
			
			removeTomcatSharedLibJars(tomcat_shared_lib_jars);
			System.out.println("------------ EDRM JCO Plugin XLIB jars removed from tomcat sharedlib folder -----------------");
			removeFile(edrm_jar_propertyFilePath);
			System.out.println("------------ EDRM JCO Plugin property file removed from DPC folder -----------------");
			removeFile(edrm_jar_path);
			System.out.println("------------ EDRM JCO Plugin jar removed from DPC folder -----------------");

		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		System.out.println("------------ EDRM JCO Plugin UnInstallation Ends -----------------");
	}

	private static void removeTomcatSharedLibJars(ArrayList<String> tomcat_shared_lib_jars) throws IOException {
		String tomcat_path = CommonInstallerValues.tomcatPath + "/nextlabs/shared_lib";
		for(String fileName:tomcat_shared_lib_jars)
		{
			removeFile(tomcat_path+"/"+fileName);
		}
	}


	private static void removeFile(String filePath) throws IOException {
		Path sourcepath = Paths.get(filePath);
		Files.delete(sourcepath);

	}



}
