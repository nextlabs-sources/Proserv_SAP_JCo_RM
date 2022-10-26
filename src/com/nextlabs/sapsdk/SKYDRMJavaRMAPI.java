package com.nextlabs.sapsdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.crypt.IDecryptor;
import com.bluejungle.framework.crypt.IEncryptor;
import com.bluejungle.framework.crypt.ReversibleEncryptor;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.RightsManager;

public class SKYDRMJavaRMAPI {
	private static final Log LOG = LogFactory.getLog(SKYDRMJavaRMAPI.class);
	String routerURL;
	String appKey;
	int appId;

	public String getRouterURL() {
		return routerURL;
	}

	public void setRouterURL(String routerURL) {
		this.routerURL = routerURL;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public int getAppId() {
		return appId;
	}

	public void setAppId(int appId) {
		this.appId = appId;
	}

	SKYDRMJavaRMAPI(String routerURL, String appKey, int appId) {
		this.routerURL = routerURL;
		this.appKey = appKey;
		this.appId = appId;
	}

	SKYDRMJavaRMAPI(Properties allProps) {
		if (allProps != null && allProps.getProperty("Skydrm_router_url") != null) {
			this.routerURL = allProps.getProperty("Skydrm_router_url");
		} else {
			LOG.warn("SAPEDRM: SKYDRM Router URL is missing");
		}
		if (allProps != null && allProps.getProperty("Skydrm_app_key") != null) {
			this.appKey = decryptPassword(allProps.getProperty("Skydrm_app_key"));

		} else {
			LOG.warn("SAPEDRM: SKYDRM APP Key is missing");
		}
		if (allProps != null && allProps.getProperty("Skydrm_app_id") != null) {
			this.appId = Integer.parseInt(allProps.getProperty("Skydrm_app_id"));
		} else {
			LOG.warn("SAPEDRM: SKYDRM app id is missing");
		}

	}

	public boolean isManagerInitialized() {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			LOG.info("SAPEDRM: SKYDRM router Connection established");
		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while performing Encryption");
			LOG.error(e);
			return false;
		}
		return true;
	}

	public boolean isNxlFile(String filePath) {
		RightsManager manager;
		boolean result = true;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			result = manager.isNXL(filePath);
			LOG.info("SAPEDRM: SKYDRM router Connection established");
		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while performing Encryption");
			LOG.error(e);
			result = false;
		}
		return result;
	}

	public boolean encryptFile(String inputFile, String outputFile, String tenantName, Map<String, String[]> tagMap) {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			manager.encrypt(inputFile, outputFile, null, null, tagMap, tenantName,
					TokenGroupType.TOKENGROUP_SYSTEMBUCKET);
			LOG.info("SAPEDRM: Encryption completed");
		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while performing Encryption");
			LOG.error(e);
			return false;
		}

		return true;
	}

	public Map<String, String[]> readTags(String fileName) {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			if (manager.isNXL(fileName)) {
				Map<String, String[]> tagMap = manager.readTags(fileName);
				LOG.info("SAPEDRM: Reading Tags Completed");
				LOG.info("SAPEDRM:  Tag Map" + tagMap);
				return tagMap;
			}

		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while reading tags from file");
			LOG.error(e);
		}
		return null;
	}

	public byte[] readNXLHeader(String fileName,int nxlHeaderSize) {
		
		byte[] bytes =null;
		try {
			InputStream insputStream = new FileInputStream(new File(fileName));
			bytes = new byte[nxlHeaderSize];
			insputStream.read(bytes);
			insputStream.close();
	
			LOG.info("SAPEDRM: Reading NXLHeader Completed");
		} catch (FileNotFoundException e) {
			LOG.info("SAPEDRM: File Not Found");
			LOG.error(e);
		} catch (IOException e) {
			LOG.info("SAPEDRM: IO Read Exception");
			LOG.error(e);
		}
		return bytes;
	}

	public boolean updateTags(String fileName, String tenantName, Map<String, String[]> tagMap) {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			manager.updateTags(fileName, tagMap, tenantName, TokenGroupType.TOKENGROUP_SYSTEMBUCKET, null);
			LOG.info("SAPEDRM: updating Tags Completed");

		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while updating tags to the file");
			LOG.error(e);
			return false;
		}
		return true;
	}

	public boolean appendTags(String fileName, String tenantName, Map<String, String[]> tagMap) {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			Map<String, String[]> currentFileTag = readTags(fileName);
			if (null != currentFileTag && currentFileTag.size() > 0) {
				for (String key : tagMap.keySet()) {
					String[] value = tagMap.get(key);
					if (null != value && value.length > 0) {
						String[] putResult = currentFileTag.putIfAbsent(key, value);
						if (putResult != null) {
							currentFileTag.put(key, combine(value, putResult));
						}
					}

				}
			} else {
				currentFileTag = tagMap;
			}
			manager.updateTags(fileName, currentFileTag, tenantName, TokenGroupType.TOKENGROUP_SYSTEMBUCKET, null);
			LOG.info("SAPEDRM: Appending Tags Completed");

		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while Appending tags to the file");
			LOG.error(e);
			return false;
		}
		return true;
	}

	private String[] combine(String[] value, String[] putResult) {
		HashSet<String> resultString = new HashSet<String>();
		for (String val : value) {
			resultString.add(val);
		}
		for (String val : putResult) {
			resultString.add(val);
		}
		String arrayString[] = new String[resultString.size()];
		int arrayCount = 0;
		for (String val : resultString) {
			arrayString[arrayCount++] = val;
		}
		return arrayString;
	}

	public boolean decryptFile(String inputFile, String outputFile, String tenantName) {
		RightsManager manager;
		try {
			manager = new RightsManager(routerURL, appId, appKey);
			manager.decrypt(inputFile, outputFile, tenantName, TokenGroupType.TOKENGROUP_SYSTEMBUCKET);
			LOG.info("SAPEDRM: Decryption completed");

		} catch (NxlException e) {

			LOG.info("SAPEDRM: Error while decrypting tags to the file");
			LOG.error(e);
			return false;
		}
		return true;

	}

	private String decryptPassword(String encryptedPassword) {
		IDecryptor decryptor = new ReversibleEncryptor();
		return decryptor.decrypt(encryptedPassword);
	}

	public static void main(String args[]) {
		String routerUrl = "https://trainskydrmrhel74.qapf1.qalab01.nextlabs.com:8443/router";
		String appkey = "F1E170AAF75B0FC2A342BC8CD108A491";
		int appid = 1;
		SKYDRMJavaRMAPI rmapi = new SKYDRMJavaRMAPI(routerUrl, appkey, appid);
		Map<String, String[]> tagMap = new HashMap<>();
		String[] classification = { "ITAR" };
		tagMap.put("ExportComplaince", classification);
		String inputFile = "D:\\test\\Day 1-1 CC Policy Training and authoring.pptx";
		String outputFile = "D:\\test\\Day 1-1 CC Policy Training and authoring.pptx.nxl";
		String TenantName = "afaddd35-4958-426c-b6e2-f52d5148be82"; //
		// rmapi.updateTags(outputFile, TenantName, tagMap); //
		//rmapi.readNXLHeader(outputFile,16384);
		// rmapi.decryptFile(outputFile, inputFile, TenantName);
		IEncryptor encryptor= new ReversibleEncryptor();
		System.out.println(encryptor.encrypt("F1E170AAF75B0FC2A342BC8CD108A491"));
	}
}
