package com.nextlabs.sapsdk;

public class SAPJcoConstant {

	//Operation Type
	public static final String OPERATION_TYPE_READ_TAGS = "1";
	public static final String OPERATION_TYPE_TAGGING = "2";
	public static final String OPERATION_TYPE_ENCRYPTION = "3";
	public static final String OPERATION_TYPE_ENCRYPTION_AND_TAGGING = "4";
	public static final String OPERATION_TYPE_DECRYPTION = "5";
	public static final String OPERATION_TYPE_READ_NXL_HEADER = "6";
	public static final String OPERATION_TYPE_WRITE_NXL_TAGS = "7";
	public static final String OPERATION_TYPE_READ_NXL_TAGS = "8";
	public static final String MESSAGE_TYPE_ERROR = "E";
	public static final String MESSAGE_TYPE_SUCCESS = "S";
	
	public static final String INPUT_FIELD_REF_ID = "REF_ID";
	public static final String INPUT_FIELD_INPUTFILE = "INPUTFILE";
	public static final String INPUT_FIELD_OUTPUTFILE = "OUTPUTFILE";
	public static final String INPUT_FIELD_OPERATION_TYPE = "OPER_TYPE";
	public static final String INPUT_FIELD_TAGS = "TAGS";
	public static final String INPUT_FIELD_TAGS_TABLE = "NXL_TAGS";
	public static final String INPUT_FIELD_TAGS_MODE = "MODE";
	
	public static final String INPUT_FIELD_KEY = "KEY";
	public static final String INPUT_FIELD_VALUE = "VALUE";
	public static final String INPUT_FIELD_CARDINALITY = "CARDINALITY";
	
	public static final String OUTPUT_FIELD_TAGS = "TAGS";
	public static final String OUTPUT_FIELD_FILELOC = "FILELOC";
	public static final String OUTPUT_FIELD_MESSAGE_TYPE = "MESSAGE_TYPE";
	public static final String OUTPUT_FIELD_MESSAGE = "MESSAGE";
	public static final String OUTPUT_FIELD_NXLHEADER= "NXLHDR";
	
}
