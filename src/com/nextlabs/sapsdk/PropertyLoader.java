package com.nextlabs.sapsdk;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertyLoader {

	private static final Log LOG = LogFactory.getLog(PropertyLoader.class);

	public Properties loadProperties() {
		
		String name = "/jservice/config/SAPJCo-EDRM.properties";

		// In DPC environment, this SDK plugin need to resolve paths based on
		// DPC Install Home directory.
		String dpcInstallHome = System.getProperty("dpc.install.home");
		if (dpcInstallHome == null || dpcInstallHome.trim().length() < 1) {
			dpcInstallHome = ".";
		}
		LOG.info("DPC Install Home :" + dpcInstallHome);

		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("Invalid file name");

		name = dpcInstallHome + name;
		Properties result = null;

		try {
			File file = new File(name);
			LOG.info("Properties File Path:: " + file.getAbsolutePath());
			System.out.println("Properties File Path:: " + file.getAbsolutePath());
			if (file != null) {
				FileInputStream fis = new FileInputStream(file);
				result = new Properties();
				result.load(fis); // Can throw IOException
			}
		} catch (Exception e) {
			LOG.error("Error parsing properties file ", e);
			result = null;
		}
		return result;
	}

	public String findInstallFolder() {

		String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

		try {
			path = URLDecoder.decode(path, "UTF-8");

		} catch (Exception e) {
			LOG.error("Exeception while decoding the path:", e);
		}

		int endIndex = path.indexOf("jservice/jar");

		if (OSUtils.isWindows()) {
			path = path.substring(1, endIndex);
		} else {
			path = path.substring(0, endIndex);
		}
		return path;
	}
}
