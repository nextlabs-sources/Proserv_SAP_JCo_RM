package com.nextlabs.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;

import com.bluejungle.framework.crypt.IEncryptor;
import com.bluejungle.framework.crypt.ReversibleEncryptor;

public class PropertyFileConfigurer {
	private static Scanner scanner;

public static void main(String args[])
{
	PropertyFileConfigurer.configureProperties("C:\\BitBucket\\sap_jco_edrm\\lib\\SAPJCo-EDRM.properties");
}
	public static void configureProperties(String propFilePath) {
		File file = new File(propFilePath);
		IEncryptor encryptor= new ReversibleEncryptor();
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(file));
			PropertiesConfiguration config = new PropertiesConfiguration();
			PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
			layout.load(config, is);
			is.close();
			scanner = new Scanner(System.in);
			System.out.println("----EDRM Plugin Props Configuration starts----");

			config.setProperty("jar-path",
					CommonInstallerValues.tomcatPath + "/nextlabs/dpc/jservice/jar/SAPJCo-EDRM.jar");
			System.out.println("----JAR Path Configured----");

			System.out.println("----JCO SAP Prperty Configuration Starts----");
			
			System.out.println("Enter SAP Application Server Host Name:");
			config.setProperty("SERVEDRM_jco.client.ashost", scanner.next());
			
			System.out.println("Enter SAP Application System Number:");
			config.setProperty("SERVEDRM_jco.client.sysnr", scanner.next());
			
			System.out.println("Enter SAP Application Server Client No:");
			config.setProperty("SERVEDRM_jco.client.client", scanner.next());
			
			System.out.println("Enter SAP Application Server RFC User Name:");
			config.setProperty("SERVEDRM_jco.client.user", scanner.next());
			
			System.out.println("Enter SAP Application Server RFC User password:");
			config.setProperty("SERVEDRM_jco.client.passwd",encryptor.encrypt( scanner.next()));
			
			System.out.println("Enter SAP Application Server  Gateway Host Name:");
			config.setProperty("SERVEDRM_jco.server.gwhost", scanner.next());
			
			System.out.println("Enter SAP Application Gateway serv Name:");
			config.setProperty("SERVEDRM_jco.server.gwserv", scanner.next());
			
			System.out.println("Enter SAP Application Server JCO EDRM Prog ID:");
			config.setProperty("SERVEDRM_jco.server.progid", scanner.next());
			
			System.out.println("----JCO SAP Prperty Configuration ENDS----");
			
			System.out.println("----SKYDRM Prperty Configuration Starts----");
			
			System.out.println("Enter SKYDRM Tenant name:");
			config.setProperty("SKYDRM_tenant_name", scanner.next());
			
			System.out.println("Enter SKYDRM Router URL:");
			config.setProperty("Skydrm_router_url", scanner.next());
			
			System.out.println("Enter SKYDRM APP ID:");
			config.setProperty("Skydrm_app_id", scanner.next());
			
			System.out.println("Enter SKYDRM APP KEY:");
			config.setProperty("Skydrm_app_key", encryptor.encrypt( scanner.next()));
			
		
			
			System.out.println("----SKYDRM Prperty Configuration ENDS----");
			System.out.println("----EDRM Plugin Props Configuration ENDS----");
			
			FileWriter fw = new FileWriter(propFilePath);
			layout.save(config, fw);
			fw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
