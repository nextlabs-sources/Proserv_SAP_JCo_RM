package com.nextlabs.sapsdk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.jtagger.Tagger;
import com.nextlabs.jtagger.TaggerFactory;
import com.sap.conn.jco.AbapClassException;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class SAPRMAPIHandler implements JCoServerFunctionHandler {

	private static final Log LOG = LogFactory.getLog(SAPRMAPIHandler.class);
	private Properties allProps;
	private String delimiter = "|";
	private String delimiterEscape = "\\|";
	private SKYDRMJavaRMAPI skydrmManager;
	private String tenantName;
	private int nxlHeaderSize;

	public SAPRMAPIHandler() {

	}

	public SAPRMAPIHandler(Properties allProps) {
		super();
		this.allProps = allProps;
		if (allProps != null) {
			delimiter = allProps.getProperty("delimiter", "|");
			tenantName = allProps.getProperty("SKYDRM_tenant_name");
			delimiterEscape = "\\" + allProps.getProperty("delimiter", "|");
			nxlHeaderSize = Integer.parseInt(allProps.getProperty("SKYDRM_NXL_Header_size"));
		}
		skydrmManager = new SKYDRMJavaRMAPI(allProps);
	}

	@Override
	public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
			throws AbapException, AbapClassException {

		LOG.info("SAPRMAPIHandler :: handleRequest() RMAPI request received");

		// Get Structure data for export
		String sLogID = function.getImportParameterList().getString("NXL_V_LOG_ID");

		LOG.info("SAPRMAPIHandler :: handleRequest() Log id from SAP " + sLogID);
		
		
		String doProtectWithNXLExtension = function.getImportParameterList().getString("NXL_V_NOT_RETAIN_EXT");
		if (doProtectWithNXLExtension == null)
			doProtectWithNXLExtension = "";

		LOG.info("SAPRMAPIHandler :: handleRequest() doProtectWithNXLExtension from SAP " + doProtectWithNXLExtension);

		JCoTable documentList = function.getImportParameterList().getTable("NXL_T_API_ENCR_MUL");

		JCoTable resultTable = function.getChangingParameterList().getTable("NXL_T_RETURN");

		for (int i = 0; i < documentList.getNumRows(); i++, documentList.nextRow()) {

			LOG.info("SAPRMAPIHandler handleRequest() Processing document " + i);

			// getting structure paramenters
			String sRefId = documentList.getString(SAPJcoConstant.INPUT_FIELD_REF_ID);
			String sInputFile = documentList.getString(SAPJcoConstant.INPUT_FIELD_INPUTFILE);
			String sOutputFile = documentList.getString(SAPJcoConstant.INPUT_FIELD_OUTPUTFILE);
			String sOpType = documentList.getString(SAPJcoConstant.INPUT_FIELD_OPERATION_TYPE);
			JCoStructure tagStructure = documentList.getStructure(SAPJcoConstant.INPUT_FIELD_TAGS);
			JCoTable tagList = null;
			String tagMode = null;
			if (tagStructure != null) {
				tagList = tagStructure.getTable(SAPJcoConstant.INPUT_FIELD_TAGS_TABLE);
				tagMode = tagStructure.getString(SAPJcoConstant.INPUT_FIELD_TAGS_MODE);
			}
			LOG.info("SAPRMAPIHandler :: tagMode-" + tagMode);
			// set default tag mode
			if (tagMode == null) {
				tagMode = "Append";
			}
			LOG.info("SAPRMAPIHandler :: tagMode-" + tagMode);
			// append the input paramenters to the result table
			resultTable.appendRow();
			resultTable.setValue(SAPJcoConstant.INPUT_FIELD_REF_ID, sRefId);
			resultTable.setValue(SAPJcoConstant.INPUT_FIELD_OPERATION_TYPE, sOpType);

			Map<String, String[]> requiredTagList = null;
			String fileExtention = FilenameUtils.getExtension(sInputFile);
			File file = new File(sInputFile);
			String outputFilePath = null;
			int numberOfUniqueUpdatedValues = 0;
			// RightsManager rm = null;

			// reading input tags table
			Map<String, HashSet<String>> tagsFromJCOTable = new HashMap<String, HashSet<String>>();
			if (tagList != null && tagList.getNumRows() > 0) {

				LOG.info("SAPRMAPIHandler :: handleRequest() Start processing tag table");

				for (int k = 0; k < tagList.getNumRows(); k++, tagList.nextRow()) {

					String key = tagList.getString(SAPJcoConstant.INPUT_FIELD_KEY);
					String value = tagList.getString(SAPJcoConstant.INPUT_FIELD_VALUE);

					LOG.info("SAPRMAPIHandler :: handleRequest() Adding tag " + key + " to required tag with value "
							+ value);

					HashSet<String> values = tagsFromJCOTable.get(key);

					if (values == null) {
						values = new HashSet<String>();
					}
					if (value != null && value.length() > 0) {
						values.add(value);
						tagsFromJCOTable.put(key, values);
					}
				}
			}

			LOG.info("SAPRMAPIHandler :: handleRequest() tagsFromJCOTable:" + tagsFromJCOTable);
			requiredTagList = getRequiredTagList(tagsFromJCOTable);
			LOG.info("SAPRMAPIHandler :: handleRequest() requiredTagList:" + requiredTagList.size());
			// Verifying input file
			LOG.info("SAPRMAPIHandler :: handleRequest() Input file name is " + sInputFile);
			Path inputFilePath = Paths.get(sInputFile);

			String inputFileName = inputFilePath.getFileName().toString();

			if (inputFileName == null || Files.notExists(inputFilePath, LinkOption.NOFOLLOW_LINKS)) {
				LOG.info("SAPRMAPIHandler :: handleRequest() File not exist in system");
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "File not found!");
				continue;
			}

			// Verifying output file
			LOG.info("SAPRMAPIHandler :: handleRequest() Output file folder is " + sOutputFile);
			if (sOutputFile != null && sOutputFile.length() > 0) {

				Path outputFileFolder = Paths.get(sOutputFile);

				if (outputFileFolder != null) {
					File outputFolder = outputFileFolder.toFile();
					if (!outputFolder.exists()) {
						try {
							outputFolder.mkdirs();
						} catch (SecurityException se) {
							LOG.info("SAPRMAPIHandler :: handleRequest() Failed to create output folder(s): "
									+ se.getMessage(), se);
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
									SAPJcoConstant.MESSAGE_TYPE_ERROR);
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
									"Encounter security error while creating output folder(s)!");
							continue;
						}
					}
				}
			}

			// start operation
			switch (sOpType) {
			case SAPJcoConstant.OPERATION_TYPE_READ_NXL_HEADER:
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is read nxl header");
				if (fileExtention.equals("nxl")) {
					if (!skydrmManager.isManagerInitialized()) {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Cannot initialize Right Management SDK!");
						continue;
					}
					byte[] nxlHeader = skydrmManager.readNXLHeader(sInputFile, nxlHeaderSize);
					if (nxlHeader != null) {
						if (nxlHeader.length == nxlHeaderSize) {
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
									SAPJcoConstant.MESSAGE_TYPE_SUCCESS);
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
									"Reading of NXL Header was completed successfully");
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_NXLHEADER, nxlHeader);
							break;
						} else {
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
									SAPJcoConstant.MESSAGE_TYPE_ERROR);
							resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
									"Not able to read first 16KB header from the file");
						}

					} else {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Unsupported file format or file extension!");
					}
				} else {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Unsupported file format or file extension!");
				}

				break;
			case SAPJcoConstant.OPERATION_TYPE_READ_TAGS:

				// reading tags
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is read tag");

				JCoTable tagsTable = resultTable.getTable(SAPJcoConstant.INPUT_FIELD_TAGS);

				Map<String, String[]> hTags = null;

				if (fileExtention.equals("nxl")) {

					// to read an nxl file tags
					LOG.info("SAPRMAPIHandler :: handleRequest() Reading tags of an nxl file");

					if (!skydrmManager.isManagerInitialized()) {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Cannot initialize Right Management SDK!");
						continue;
					}

					boolean isNxlFile = skydrmManager.isNxlFile(sInputFile);

					if (!isNxlFile) {
						LOG.info("SAPRMAPIHandler :: handleRequest() Input file is not NXL file");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Input file is not an nxl file!");
						continue;
					}

					hTags = skydrmManager.readTags(sInputFile);
					if (hTags == null) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to read nxl file tags ");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to read nxl file tags!");
						continue;
					}
					LOG.info("SAPRMAPIHandler :: handleRequest() Read Tags from NXL file:" + hTags);

				} else {

					// to read a non-nxl file tags
					LOG.info("SAPRMAPIHandler :: handleRequest() Reading tags of a non-nxl file");

					Tagger tagger = null;
					try {
						tagger = TaggerFactory.getTagger(file.getAbsolutePath());
						hTags = new HashMap<String, String[]>();

						// put the result into the htags
						for (Entry<String, Object> entry : tagger.getAllTags().entrySet()) {
							String[] values = entry.getValue().toString().split(delimiterEscape);
							hTags.put(entry.getKey(), values);
						}
					} catch (NullPointerException e) {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Unsupported file format or file extension!");
						continue;
					} catch (Exception e) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to read non-nxl file tags: "
								+ e.getMessage(), e);

						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to read non-nxl file tags!");
						continue;
					}
				}

				// put the result into the tag table
				if (requiredTagList != null && requiredTagList.size() < 1) {
					LOG.info("SAPRMAPIHandler :: Read Tags from NXL file Tag size:" + hTags.size());

					for (String key : hTags.keySet()) {

						for (String value : hTags.get(key)) {

							tagsTable.appendRow();
							tagsTable.setValue(SAPJcoConstant.INPUT_FIELD_KEY, key);
							tagsTable.setValue(SAPJcoConstant.INPUT_FIELD_VALUE, value);
							LOG.info("SAPRMAPIHandler :: handleRequest() Reading tag with key/value " + key + "="
									+ value);
						}
					}

				} else {
					LOG.info("SAPRMAPIHandler ::Read Tags from NXL file Tag size:" + hTags.size());
					for (Map.Entry<String, String[]> entry : hTags.entrySet()) {
						// Check agaist request tag from SAP, only send back
						// required tag by SAP
						if (requiredTagList.get(entry.getKey().toLowerCase()) != null) {
							for (String value : entry.getValue()) {
								tagsTable.appendRow();
								tagsTable.setValue(SAPJcoConstant.INPUT_FIELD_KEY, entry.getKey());
								tagsTable.setValue(SAPJcoConstant.INPUT_FIELD_VALUE, value);
								LOG.info("SAPRMAPIHandler :: handleRequest() Reading tag with key/value "
										+ entry.getKey() + "=" + value);
							}
						}
					}
				}

				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_SUCCESS);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Reading of tags completed successfully");
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_TAGS, tagsTable);
				break;

			case SAPJcoConstant.OPERATION_TYPE_TAGGING:

				// tag file
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is write tag");

				// check if the input tags table is valid or not
				if (requiredTagList == null) {

					LOG.info("SAPRMAPIHandler :: handleRequest() Tags table is empty, not able to perform tagging");
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Classification values are required for this operation!");
					continue;
				}

				if (fileExtention.equals("nxl")) {

					// to update an nxl file tag
					LOG.info("SAPRMAPIHandler :: handleRequest() Writting tags to an nxl file");

					if (!skydrmManager.isManagerInitialized()) {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Cannot initialize Right Management SDK!");
						continue;
					}

					boolean isNxlFile = skydrmManager.isNxlFile(sInputFile);

					if (!isNxlFile) {
						LOG.info("SAPRMAPIHandler :: handleRequest() Input file is not NXL file");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Input file is not an nxl file!");
						continue;
					}

					boolean flag = skydrmManager.appendTags(sInputFile, tenantName, requiredTagList);
					if (!flag) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to update file tags ");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to tag the nxl file!");
						continue;
					}

				} else {

					// to update a non-nxl file tags
					LOG.info("SAPRMAPIHandler :: handleRequest() Writing tags to a non-nxl file");
					LOG.info("SAPRMAPIHandler :: handleRequest() NON NXL File path:" + file.getAbsoluteFile());
					Tagger tagger = null;

					try {
						LOG.info("SAPRMAPIHandler :: Initialzing tagger");
						tagger = TaggerFactory.getTagger(file.getAbsolutePath());
						LOG.info("SAPRMAPIHandler :: Tagger initalized");
						HashMap<String, Object> existingTags = tagger.getAllTags();
						LOG.info("SAPRMAPIHandler :: existingTags-" + existingTags);
						LOG.info("SAPRMAPIHandler :: TagsFromSAPJCOTable-" + requiredTagList);

						if (tagMode.equalsIgnoreCase("append")) {
							existingTags = convertKeysToLowerCase(existingTags);
							requiredTagList = convertKeyToLowerCase(requiredTagList);
							LOG.info("SAPRMAPIHandler :: Append Mode");
							Set<String> keySet = getCombinedKeys(existingTags.keySet(), requiredTagList.keySet());
							for (String key : keySet) {
								String valueEx = null;
								String[] valueJCO = null;
								HashSet<String> values = new HashSet<String>();
								StringBuilder concateValue = new StringBuilder();
								if (existingTags.get(key) != null)
									valueEx = existingTags.get(key).toString();

								if (requiredTagList.get(key) != null)
									valueJCO = requiredTagList.get(key);

								if (valueEx == null && valueJCO == null) {
									tagger.deleteTag(key);
									continue;
								}

								if (valueEx != null && valueEx.length() > 0) {
									String[] existingValues = valueEx.split(delimiterEscape);
									for (String val : existingValues)
										values.add(val);
								}
								if (valueJCO != null) {
									for (String value : valueJCO) {
										values.add(value);
									}
								}
								if (values.size() > 0) {
									for (String val : values) {
										concateValue.append(val);
										concateValue.append(delimiter);
									}
									String value = concateValue.toString();
									if (value.endsWith(delimiter)) {
										value = value.substring(0, value.length() - 1);
									}
									LOG.info("SAPRMAPIHandler :: handleRequest() Writing tag with key/value " + key
											+ "=" + value);
									tagger.addTag(key, value);
								} else {
									tagger.deleteTag(key);
								}

							}
						} else {
							for (String key : requiredTagList.keySet()) {
								String[] valueJCO = null;
								StringBuilder concateValue = new StringBuilder();
								if (requiredTagList.get(key) != null)
									valueJCO = requiredTagList.get(key);
								if (valueJCO != null) {
									for (String value : valueJCO) {
										concateValue.append(value);
										concateValue.append(delimiter);
									}
								}

								if (concateValue.toString().length() > 0) {
									String value = concateValue.toString();
									if (value.endsWith(delimiter)) {
										value = value.substring(0, value.length() - 1);
									}
									LOG.info("SAPRMAPIHandler :: handleRequest() Writing tag with key/value " + key
											+ "=" + value);
									tagger.addTag(key, value);
								}

							}
						}

						tagger.save(file.getAbsolutePath());

					} catch (NullPointerException e) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to update file tags: " + e.getMessage(),
								e);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Unsupported file format or file extension");
						continue;
					} catch (Exception e) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to update file tags: " + e.getMessage(),
								e);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to write tags!");
						continue;
					}
				}

				outputFilePath = sOutputFile + inputFileName;
				try {
					FileUtils.copyFile(file, new File(outputFilePath));
				} catch (IOException e) {
					LOG.error("SAPRMAPIHandler :: handleRequest() Failed to copy file to out folder: " + e.getMessage(),
							e);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to copy file to out folder!");
					continue;
				}

				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_SUCCESS);

				if (numberOfUniqueUpdatedValues > 0) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"File classification completed successfully! Number of unique updated classification values is "
									+ numberOfUniqueUpdatedValues);
				} else {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Processing of classifications is complete. No updates are necessary");
				}
				break;

			case SAPJcoConstant.OPERATION_TYPE_ENCRYPTION_AND_TAGGING:
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is encrypt and tag file");

				// to update an nxl file tag
				LOG.info("SAPRMAPIHandler :: handleRequest() Writing tags to an nxl file");

				if (!skydrmManager.isManagerInitialized()) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Cannot initialize Right Management SDK!");
					continue;
				}

				boolean isInputNxl = skydrmManager.isNxlFile(sInputFile);
				/*
				 * if (!isInputNxl) {
				 * 
				 * LOG.info(
				 * "SAPRMAPIHandler :: handleRequest() Input file is already an nxl file. Skipping encryption and only trying to tag the file "
				 * ); continue; }
				 */

				boolean requiredTagging = true;

				if (requiredTagList == null) {
					LOG.info("SAPRMAPIHandler :: handleRequest() Tags table is empty, not able to perform tagging.");
					requiredTagging = false;
				}

				if (isInputNxl && !requiredTagging) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Can't perform any operation among protection and classification!");
					continue;
				}

				if (!isInputNxl) {

					outputFilePath = sOutputFile + inputFileName;

					if (!doProtectWithNXLExtension.equalsIgnoreCase("X"))
						outputFilePath = outputFilePath + ".nxl";

					boolean isEncrypted = skydrmManager.encryptFile(sInputFile, outputFilePath, tenantName,
							requiredTagList);
					if (!isEncrypted) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to encrypt and tag the file:");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Failed to protect and classify the file!");
						continue;

					}

					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);

				} else {

					boolean isUpdateTagsSuccesfull = skydrmManager.appendTags(sInputFile, tenantName, requiredTagList);
					if (!isUpdateTagsSuccesfull) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to update file tags: ");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Failed to protect and classiify the file!");
						continue;
					}

					outputFilePath = sOutputFile + inputFileName;
					try {
						FileUtils.copyFile(file, new File(outputFilePath));
					} catch (IOException e) {
						LOG.error("SAPRMAPIHandler :: handleRequest() Failed to copy file to out folder: "
								+ e.getMessage(), e);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE,
								SAPJcoConstant.MESSAGE_TYPE_ERROR);
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Failed to copy file to out folder!");
						continue;
					}
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);
				}

				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_SUCCESS);
				if (numberOfUniqueUpdatedValues > 0) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"File protection and classification completed successfully! Number of unique updated classification values is "
									+ numberOfUniqueUpdatedValues);
				} else {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"File protection and classification completed successfully! No updates are necessary");
				}

				break;

			case SAPJcoConstant.OPERATION_TYPE_ENCRYPTION:
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is encrypt file");

				outputFilePath = sOutputFile + inputFileName;
				if (!doProtectWithNXLExtension.equalsIgnoreCase("X"))
					outputFilePath = outputFilePath + ".nxl";
				if (!skydrmManager.isManagerInitialized()) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Cannot initialize Right Management SDK!");
					continue;
				}

				boolean isNxl = skydrmManager.isNxlFile(sInputFile);

				if (isNxl) {

					LOG.info(
							"SAPRMAPIHandler :: handleRequest() Input file is already an nxl file. Skipping encryption and only trying to tag the file ");
					continue;
				}
				boolean encryptFile = skydrmManager.encryptFile(sInputFile, outputFilePath, tenantName, null);
				if (encryptFile) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_SUCCESS);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "File protection completed successfully");
				} else {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "File protection failed");
				}

				break;

			case SAPJcoConstant.OPERATION_TYPE_DECRYPTION:
				LOG.info("SAPRMAPIHandler :: handleRequest() Operation type is decrypt file");

				outputFilePath = sOutputFile + inputFileName.replace(".nxl", "");

				if (!skydrmManager.isManagerInitialized()) {
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
							"Cannot initialize Right Management SDK!");
					continue;
				}

				isNxl = skydrmManager.isNxlFile(sInputFile);

				if (!isNxl) {
					LOG.info("SAPRMAPIHandler :: handleRequest() Input file is not NXL file");
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Input file is not an nxl file!");
					continue;
				}

				try {
					skydrmManager.decryptFile(sInputFile, outputFilePath, tenantName);
				} catch (Exception e) {
					LOG.error("SAPRMAPIHandler :: handleRequest() Failed to decrypt the file: " + e.getMessage(), e);

					resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);

					if (e.getMessage() != null && e.getMessage().contains(
							"The process cannot access the file because it is being used by another process")) {
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Failed to remove protection from the file: The process cannot access the file because it is being used by another process!");
					} else {
						LOG.debug("1.Setting SAP return message to \"Failed to remove protection of the file!\"");
						resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
								"Failed to remove protection of the file!");
					}
					continue;
				}

				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_FILELOC, outputFilePath);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_SUCCESS);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE,
						"File protection has been removed successfully for view/edit");

				break;

			default:

				LOG.info("SAPRMAPIHandler :: handleRequest() Operation " + sOpType + " not supported");
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE_TYPE, SAPJcoConstant.MESSAGE_TYPE_ERROR);
				resultTable.setValue(SAPJcoConstant.OUTPUT_FIELD_MESSAGE, "Operation " + sOpType + " not supported");
				continue;
			}
			LOG.info("SAPRMAPIHandler handleRequest() Finished processing document " + i);
		}
	}

	private Map<String, String[]> convertKeyToLowerCase(Map<String, String[]> requiredTagList) {
		Map<String, String[]> convertedList = new HashMap<String, String[]>();
		for (String key : requiredTagList.keySet()) {
			String[] value = requiredTagList.get(key);
			convertedList.put(key.toLowerCase(), value);
		}
		return convertedList;
	}

	private HashMap<String, Object> convertKeysToLowerCase(HashMap<String, Object> existingTags) {
		HashMap<String, Object> convertedList = new HashMap<String, Object>();
		for (String key : existingTags.keySet()) {
			Object value = existingTags.get(key);
			convertedList.put(key.toLowerCase(), value);
		}
		return convertedList;
	}

	/**
	 * This method combines two keys from jcotable tags and existing tags from the
	 * file
	 * 
	 * @param keySet  key from existing tags
	 * @param keySet2 key from jco table
	 * @return
	 */
	private Set<String> getCombinedKeys(Set<String> existingTags, Set<String> jcoTags) {
		Set<String> keySet = new HashSet<String>();
		for (String key : existingTags)
			keySet.add(key);
		for (String key : jcoTags)
			keySet.add(key);

		return keySet;
	}

	private HashMap<String, List<String>> convertToList(Map<String, String[]> requiredTagList) {
		HashMap<String, List<String>> sampleMap = new HashMap<String, List<String>>();
		for (String key : requiredTagList.keySet()) {
			String[] values = requiredTagList.get(key);
			List<String> resValues = new ArrayList<String>();
			for (String value : values) {
				resValues.add(value);
			}
			sampleMap.put(key, resValues);
		}
		return sampleMap;
	}

	public Map<String, String[]> getRequiredTagList(Map<String, HashSet<String>> setMap) {
		Map<String, String[]> convertedList = new HashMap<String, String[]>();
		for (String key : setMap.keySet()) {
			HashSet<String> setValue = setMap.get(key);
			int length = setValue.size();
			if (length > 0) {
				String values[] = new String[length];
				int valCount = 0;
				for (String setVal : setValue) {
					values[valCount++] = setVal;

				}
				convertedList.put(key, values);
			} else
				convertedList.put(key, null);
		}
		return convertedList;
	}

}
