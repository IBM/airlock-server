package com.ibm.airlock.admin.serialize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.util.IOUtils;


public class S3DataSerializer extends BaseDataSerializer implements DataSerializer {

	public static final Logger logger = Logger.getLogger(S3DataSerializer.class.getName());

	public static final String S3_REGION = "s3region";
	public static final String S3_BUCKET_NAME = "s3bucketName";
	public static final String S3_PATH_PREFIX_INTERNAL = "s3pathPrefixInternal";
	public static final String S3_PATH_PREFIX_RUNTIME = "s3pathPrefixRuntime";
	
	public static int S3_ACTION_RETRY_NUM = 3;
	public static int RETRY_INTERVAL_MS = 1000; //sleep 1 second between IO actions trials;
	
	public static String S3_SEPARATOR = "/";

	protected AmazonS3 s3client ;
	private String s3region;
	private String s3bucketName;
	private String s3pathPrefixInternal;
	private String s3pathPrefixRuntime;

	public String getS3bucketName() {
		return s3bucketName;
	}

	public String getS3pathPrefixInternal() {
		return s3pathPrefixInternal;
	}

	public String getS3pathPrefixRuntime() {
		return s3pathPrefixRuntime;
	}

	public String getSeparator() {
		return S3_SEPARATOR;
	}

	//The credentials are taken from AWS role and if does not defined taken from environment params
	public S3DataSerializer(JSONObject storageParamsObj) throws JSONException {
		super(storageParamsObj);
		this.s3region = (String)storageParamsObj.get(S3_REGION);
		this.s3bucketName = (String)storageParamsObj.get(S3_BUCKET_NAME);
		this.s3pathPrefixInternal = (String)storageParamsObj.get(S3_PATH_PREFIX_INTERNAL);
		this.s3pathPrefixRuntime = (String)storageParamsObj.get(S3_PATH_PREFIX_RUNTIME);

		s3client = AmazonS3ClientBuilder.standard()
				.withRegion(s3region)
				.build();
	}

	public static S3DataSerializer generateS3DataSerializer(String region, String bucket, String prefixInternal, String prefixRuntime) throws JSONException {
		JSONObject storageParamsObj = new JSONObject("{\"" + BaseDataSerializer.RUNTIME_PUBLIC_FULL_PATH + "\":\"NA\",\"" + BaseDataSerializer.STORAGE_PUBLIC_PATH + "\":\"NA\",\"" +
				DataSerializer.STORAGE_TYPE + "\":\"" + DataSerializer.STORAGE_TYPE_S3 +
				"\",\"" + S3DataSerializer.S3_REGION + "\":\"" + region + "\",\"" + S3DataSerializer.S3_BUCKET_NAME + "\":\"" + bucket + "\",\"" + S3DataSerializer.S3_PATH_PREFIX_INTERNAL + "\":\"" + prefixInternal  + "\",\"" + S3DataSerializer.S3_PATH_PREFIX_RUNTIME + "\":\"" + prefixRuntime + "\"}");
		return new S3DataSerializer(storageParamsObj);

	}

	public void writeData(String path, String data) throws IOException {
		writeData(path, data, false);
	}

	//try 3 time to write - if fails => throw ioException
	public void writeData(String path, String data, boolean publish) throws IOException {
		boolean succeeded = false;
		
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				doWriteData(path, data, false);
				succeeded =true;
				break;				
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}

		}
		
		if (!succeeded) {
			String err = "Failed writing data to '" + path + "' : " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}
	}
	
	//try 3 time to write - if fails => throw ioException
	public void writeInputStreamData(String path, InputStream data, int length) throws IOException {
		boolean succeeded = false;
		
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				doWriteInputStreamData(path, data, length);
				succeeded =true;
				break;				
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed writing input stream data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed writing input stream data to '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		if (!succeeded) {
			String err = "Failed writing data to '" + path + "' : " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}
	}

	
	//try 3 time to write - if fails => throw ioException
	protected void doCopyFile (String fromPath, String toPath, boolean internal) throws IOException {
		boolean succeeded = false;
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				if (internal) {
					s3client.copyObject(s3bucketName, fromPath, s3bucketName, toPath);
				}
				else { //runtime file should be accessible
					s3client.copyObject(new CopyObjectRequest(s3bucketName, fromPath, s3bucketName, toPath)
							.withCannedAccessControlList(CannedAccessControlList.PublicRead));
				}
					
				succeeded =true;
				break;				
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed copying file from '" + fromPath + " to " + toPath + ", trial number " + (i+1) + ": " +lastErrStr);				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed copying file from '" + fromPath + " to " + toPath + ", trial number " + (i+1) + ": " + lastErrStr);				
			}
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		if (!succeeded) {
			String err = "Failed copying file from '" + fromPath + " to " + toPath + ": " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}	
	}
	
	public void copyFile (String fromPath, String toPath) throws IOException {
		String from;
		String to;
		if (isRuntimeFile(fromPath)) {
			from = s3pathPrefixRuntime+fromPath;
		}
		else {
			from = s3pathPrefixInternal+fromPath;
		}
		
		boolean internal = true;
		if (isRuntimeFile(toPath)) {
			to = s3pathPrefixRuntime+toPath;
			internal = false;
		}
		else {
			to = s3pathPrefixInternal+toPath;
		}
		
		doCopyFile(from, to, internal);
	}

	public void copyFolder (String fromPath, String toPath) throws IOException {
		copyFolder(fromPath, toPath, false);
	}

	//try 3 time to write - if fails => throw ioException
	public void copyFolder (String fromPath, String toPath, boolean publish) throws IOException {
		//internal
		ObjectListing objectListing = doListS3Object(fromPath, true); //try 3 times to list objects
		copyObjects(objectListing, toPath);

		while (objectListing.isTruncated()) {
			objectListing = s3client.listNextBatchOfObjects(objectListing);
			copyObjects(objectListing, toPath);
		}

		if (publish) {
			//runtime
			objectListing = doListS3Object(fromPath, false); //try 3 times to list objects
			copyObjects(objectListing, toPath);

			while (objectListing.isTruncated()) {
				objectListing = s3client.listNextBatchOfObjects(objectListing);
				copyObjects(objectListing, toPath);
			}

		}
	}

	private void copyPublishedFolder(String fromPath, String toPath) {
		//currently nothing. the regular write is public
		//todo write to different path with different location/permission
	}

	private void copyObjects(ObjectListing objectListing, String toPath) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (key.endsWith(S3_SEPARATOR))
				continue;

			String[] keyParts = key.split(S3_SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];
			String destPath;
			boolean internal = true;
			if (isRuntimeFile(fileName)) {
				destPath = s3pathPrefixRuntime + toPath + S3_SEPARATOR + fileName;
				internal = false;
			}
			else {
				destPath = s3pathPrefixInternal + toPath + S3_SEPARATOR + fileName;
			}
			
			doCopyFile(key, destPath, internal);
		}
	}

	private void doWriteData(String path, String data, boolean forceRuntimeFolder)  throws AmazonClientException, AmazonServiceException {

		byte[] contentAsBytes = data.getBytes();
		ObjectMetadata          md = new ObjectMetadata();
		md.setContentLength(contentAsBytes.length);

		String concatPath;
		if (isRuntimeFile(path) || forceRuntimeFolder) {
			concatPath = s3pathPrefixRuntime+path;
			s3client.putObject(new PutObjectRequest(s3bucketName, concatPath, new ByteArrayInputStream(contentAsBytes), md)
					.withCannedAcl(CannedAccessControlList.PublicRead));
		} 
		else {
			concatPath = s3pathPrefixInternal+path;
			//s3client.putObject(new PutObjectRequest(s3bucketName, concatPath, new ByteArrayInputStream(contentAsBytes), md)
			//		.withCannedAcl(CannedAccessControlList.Private));
			s3client.putObject(s3bucketName, concatPath, new ByteArrayInputStream(contentAsBytes), md);
		}
		
	}
	
	private void doWriteInputStreamData(String path, InputStream data, int dataLength) throws AmazonClientException, AmazonServiceException {
		ObjectMetadata md = new ObjectMetadata();
		md.setContentLength(dataLength);

		String concatPath;
		if (isRuntimeFile(path)) {
			concatPath = s3pathPrefixRuntime+path;
			s3client.putObject(new PutObjectRequest(s3bucketName, concatPath, data, md)
					.withCannedAcl(CannedAccessControlList.PublicRead));
		} 
		else {
			concatPath = s3pathPrefixInternal+path;
			//s3client.putObject(new PutObjectRequest(s3bucketName, concatPath, data, md)
			//		.withCannedAcl(CannedAccessControlList.Private));
			s3client.putObject(s3bucketName, concatPath, data, md);
		}
		
		//s3client.putObject(s3bucketName, concatPath, data, md);
		
	}

	public JSONObject readDataToJSON(String path)  throws IOException, JSONException {
		String concatPath;
		if (isRuntimeFile(path)) {
			concatPath = s3pathPrefixRuntime+path;
		} 
		else {
			concatPath = s3pathPrefixInternal+path;
		}
		return doReadDataToJSON(concatPath);
	}
	
	
	public String readDataToString(String path)  throws IOException {
		String concatPath;
		if (isRuntimeFile(path)) {
			concatPath = s3pathPrefixRuntime+path;
		} 
		else {
			concatPath = s3pathPrefixInternal+path;
		}
		
		return doReadDataToString(concatPath);
	}

	public byte[] readDataToByteArray(String path) throws IOException {	
		String concatPath;
		if (isRuntimeFile(path)) {
			concatPath = s3pathPrefixRuntime+path;
		} 
		else {
			concatPath = s3pathPrefixInternal+path;
		}
		
		return doReadDataToByteArray(concatPath);
	}

	public void deleteFolderContent(String path) throws IOException {
		deleteFolderContent(path, true);
	}

	//delete files that reside in the folder. And then delete the folder itself.
	//if folder contains subFolders they won't be deleted.
	public void deleteFolderContent(String path, boolean published) throws IOException /* throws IOException, JSONException*/ {
		//internal
		ObjectListing objectListing = doListS3Object(path, true); //try 3 times to list objects
		deleteObjects(objectListing);

		while (objectListing.isTruncated()) {
			objectListing = s3client.listNextBatchOfObjects(objectListing);
			deleteObjects(objectListing);
		}

		//runtime
		if (published) {
			objectListing = doListS3Object(path, false); //try 3 times to list objects
			deleteObjects(objectListing);

			while (objectListing.isTruncated()) {
				objectListing = s3client.listNextBatchOfObjects(objectListing);
				deleteObjects(objectListing);
			}

		}
	}

	private void deletePublishedFolderContent(String path) {
		//currently nothing. the regular write is public
		//todo write to different path with different location/permission
	}

	private void deleteObjects(ObjectListing objectListing) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (!key.endsWith(S3_SEPARATOR)) //first delete only the files that are in the folder
				doDeleteObject(key); //try 3 time to delete the file
		}
	}

	//try 3 times to delete the object
	private void doDeleteObject(String path) throws IOException {
		
		boolean succeeded = false;
		
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				s3client.deleteObject(s3bucketName, path);
				succeeded =true;
				break;				
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed deleting object '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed deleting object '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		if (!succeeded) {
			String err = "Failed deleting object '" + path + "' : " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}	
	}

	public LinkedList<String> listFileNames(String path) throws IOException {
		LinkedList<String> fileNamesList = new LinkedList<String>();

		//internal
		ObjectListing objectListing = doListS3Object(path, true); //try 3 times
		addObjectsNames(objectListing, fileNamesList);

		while (objectListing.isTruncated()) {
			objectListing = s3client.listNextBatchOfObjects(objectListing);
			addObjectsNames(objectListing, fileNamesList);
		}
		return fileNamesList;
	}

	private void addObjectsNames(ObjectListing objectListing, LinkedList<String> fileNamesList) {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			if (key.equals(S3_SEPARATOR))
				continue;

			String[] keyParts = key.split(S3_SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];

			if (fileName!=null && !fileName.isEmpty()) {
				fileNamesList.add(fileName);
			}

		}
	}
/*
	public LinkedList<JSONObject> listFilesData(String path, String fileName) throws IOException, JSONException {
		LinkedList<JSONObject> objList = new LinkedList<JSONObject>();
		ObjectListing objectListing = doListS3Object(path); //try 3 times
		addObjectsData(objectListing, objList, fileName);

		while (objectListing.isTruncated()) {
			objectListing = s3client.listNextBatchOfObjects(objectListing);
			addObjectsData(objectListing, objList, fileName);
		}
		return objList;
	}
*/
	private void addObjectsData(ObjectListing objectListing, LinkedList<JSONObject> objList, String fileName) throws IOException, JSONException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			//the specified file name (for season's features: AirlockFeatures.json, for season's inputSchemas AirlockInputShemas.json)
			if (key.contains(fileName)) {
				//System.out.println(key);
				System.out.println("key = "  + key);
				JSONObject obj = doReadDataToJSON(key); //try 3 times
				objList.add(obj);
			}

		}
	}


	//try to read 3 times
	private S3Object doReadS3Object (String path, int numberOfTrials) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<numberOfTrials; i++) {
			try {
				return s3client.getObject(s3bucketName, path);
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed reading data from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);
				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed reading data from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			
			try {
				if (i<numberOfTrials-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		String err = "Failed reading data from '" + path + "' : " + lastErrStr;
		logger.severe(err);
		throw new IOException(err);		
	}
	
	//try to read 3 times
	protected ObjectListing doListS3Object (String path, boolean internal) throws IOException {
		String lastErrStr = "";
		String concatPath;
		if (internal) {
			concatPath = s3pathPrefixInternal+path;
		}
		else {
			concatPath = s3pathPrefixRuntime+path;
		}
		
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				
				ListObjectsRequest listObjectRequest = new ListObjectsRequest().
						withBucketName(s3bucketName).
						withPrefix(concatPath);
				return s3client.listObjects(listObjectRequest);										
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed listing S3 objects from '" + concatPath + "', trial number " + (i+1) + ": " + lastErrStr);
				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed listing S3 objects from '" + concatPath + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		String err = "Failed listing S3 objects from '" + path + "' : " + lastErrStr;
		logger.severe(err);
		throw new IOException(err);		
	}
/*
	// TODO use this instead of doListS3Object for full coverage
	protected List<S3ObjectSummary> getFullListing(String path) throws IOException
	{
		ObjectListing objectListing = doListS3Object(path);

		List<S3ObjectSummary> allSummaries = objectListing.getObjectSummaries();
		while (objectListing.isTruncated())
		{
			objectListing = s3client.listNextBatchOfObjects(objectListing);  // TODO try 3 times
			allSummaries.addAll(objectListing.getObjectSummaries());
		}
		return allSummaries;
	}

	public List<String> getAllKeys(String path) throws IOException
	{
		List<S3ObjectSummary> summaries = getFullListing(path);
		List<String> out = new ArrayList<String>();
		for (S3ObjectSummary item : summaries)
			out.add(item.getKey());
		return out;
	}
*/
	private JSONObject doReadDataToJSON(String path)  throws IOException, JSONException {
		try {
			S3Object res = doReadS3Object (path, S3_ACTION_RETRY_NUM);

			InputStream is = res.getObjectContent();
			JSONObject dataObj = new JSONObject(is);
			is.close();
			return dataObj;    
		} catch (IOException ioe) {
			String err = "Failed reading JSON data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		} catch (JSONException je) {
			String err = "Failed reading JSON data from " + path + ": " + je.getMessage();
			throw new JSONException(err);
		}
		
	}
	
	protected String doReadDataToString(String path) throws IOException   {
		try {
			S3Object res = doReadS3Object(path, S3_ACTION_RETRY_NUM);

			InputStream is = res.getObjectContent();

			return IOUtils.toString(is);
		} catch (IOException ioe) {
			String err = "Failed reading String data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		}
	}

	protected byte[] doReadDataToByteArray(String path) throws IOException   {
		try {
			S3Object res = doReadS3Object(path, S3_ACTION_RETRY_NUM);

			InputStream is = res.getObjectContent();

			return IOUtils.toByteArray(is);
		} catch (IOException ioe) {
			String err = "Failed reading String data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		}
	}

	private String AmazonServiceException2ErrMsg (AmazonServiceException ase) {
		return "Caught an AmazonServiceException. Error Message:    " + ase.getMessage() +
				"HTTP Status Code: " + ase.getStatusCode() + "AWS Error Code:   " + ase.getErrorCode() + "Error Type:       " + ase.getErrorType() + 
				"Request ID:       " + ase.getRequestId();
	}
	
	private String AmazonClientException2ErrMsg (AmazonClientException ace) {
		return"Caught an AmazonClientException. Error Message: " + ace.getMessage();
	}

	public void deleteData(String path) throws IOException {
		deleteData(path, false);
	}

	public void deleteData(String path, boolean published) throws IOException {
		String concatPath ;
		if (isRuntimeFile(path)) {
			concatPath = s3pathPrefixRuntime+path;
		}
		else {
			concatPath = s3pathPrefixInternal+path;
		}
		doDeleteObject(concatPath);
		/*if (published) {
			deletePublicData(path);
		}*/
	}

	private void deletePublicData(String path) {
		//currently nothing. the regular write is public
		//todo write to different path with different location/permission
	}

	public String getPathPrefix() {
		return s3bucketName + S3_SEPARATOR + s3pathPrefixInternal;
	}

	public String getPublicPathPrefix() {
		return s3bucketName + S3_SEPARATOR + s3pathPrefixRuntime;
	}

	public boolean isFileExists(String path) {
		/*try {
			doReadS3Object(pathPrefix+path, 1); //try only once to read the file
			return true;
		} catch (IOException e) {
			return false;
		}*/
		try {
			s3client.getObjectMetadata(s3bucketName, s3pathPrefixInternal+path);
			return true;
		} catch (AmazonServiceException ase) {
			return false;
		} catch (AmazonClientException ace) {
			return false;				
		}				
	}
	
	
	//for tests: functions that ignore the prefix
	public boolean fullPathIsFileExists(String fullPath) {
		/*try {
			doReadS3Object(pathPrefix+path, 1); //try only once to read the file
			return true;
		} catch (IOException e) {
			return false;
		}*/
		try {
			s3client.getObjectMetadata(s3bucketName, fullPath);
			return true;
		} catch (AmazonServiceException ase) {
			return false;
		} catch (AmazonClientException ace) {
			return false;				
		}				
	}
	
	public JSONObject fullPathReadDataToJSON(String fullPath)  throws IOException, JSONException {
		return doReadDataToJSON(fullPath);
	}
	
	//try 3 time to write - if fails => throw ioException
	public void fullPathCopyFolder (String fullFromPath, String relativeToPath) throws IOException {
		ObjectListing objectListing = fullPathDoListS3Object(fullFromPath); //try 3 times to list objects
		fullPathCopyObjects(objectListing, fullFromPath, relativeToPath);

		while (objectListing.isTruncated()) {
			objectListing = s3client.listNextBatchOfObjects(objectListing);
			fullPathCopyObjects(objectListing, fullFromPath, relativeToPath);
		}
	}

	private void fullPathCopyObjects(ObjectListing objectListing, String fullFromPath, String relativeToPath) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (key.endsWith(S3_SEPARATOR))
				continue;

			String fileName = key.substring(fullFromPath.length()+1);
			String toPath;
			boolean internal = true;
			if (isRuntimeFile(fileName)) {
				toPath = s3pathPrefixRuntime+relativeToPath + S3_SEPARATOR + fileName;
				internal = false;
			}
			else {
				toPath = s3pathPrefixInternal+relativeToPath + S3_SEPARATOR + fileName;
			}
			
			doCopyFile(key, toPath, internal);
		}
	}
	
	//try to read 3 times
	private ObjectListing fullPathDoListS3Object (String fullPath) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				ListObjectsRequest listObjectRequest = new ListObjectsRequest().
						withBucketName(s3bucketName).
						withPrefix(fullPath);
				return s3client.listObjects(listObjectRequest);										
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed listing S3 objects from '" + fullPath + "', trial number " + (i+1) + ": " + lastErrStr);
				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed listing S3 objects from '" + fullPath + "', trial number " + (i+1) + ": " + lastErrStr);
			}
			
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		String err = "Failed listing S3 objects from '" + fullPath + "' : " + lastErrStr;
		logger.severe(err);
		throw new IOException(err);		
	}




	//S3Encryptor stuff

	private static final String KEY = "alias/airlock_key";

	public void putEncryptedFile(String data, String path) throws Exception
	{
		byte[] plaintext = data.getBytes("UTF-8");
		putEncryptedFile(plaintext, path);
	}
	public void putEncryptedFile(byte[] plaintext, String path) throws Exception
	{
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(plaintext.length);

		PutObjectRequest req = new PutObjectRequest(s3bucketName, s3pathPrefixInternal + S3_SEPARATOR + path,
				new ByteArrayInputStream(plaintext), metadata)
				.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(KEY));

		@SuppressWarnings("unused")
		PutObjectResult putResult = s3client.putObject(req);
	}

	public String getEncryptedFileFullPath(String path) throws Exception
	{
		S3Object s3object = s3client.getObject(s3bucketName, path);
		return IOUtils.toString(s3object.getObjectContent());
	}

	public String getEncryptedFileShortName(String short_name) throws Exception
	{
		String long_name = s3pathPrefixInternal.endsWith("/") ? s3pathPrefixInternal + short_name : s3pathPrefixInternal + "/" + short_name;
		return getEncryptedFileFullPath(long_name);
	}

	//for seasons that do not support internal/runtime folders separation - some internal files must be written to the runtime folder
	//for example "AirlockBranches.json"
	@Override
	public void writeDataToRuntimeFolder(String path, String data) throws IOException {
		boolean succeeded = false;
		
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				doWriteData(path, data, true); //forceRuntimeFolder
				succeeded =true;
				break;				
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		if (!succeeded) {
			String err = "Failed writing data to runtime folder '" + path + "' : " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}

	}


}
