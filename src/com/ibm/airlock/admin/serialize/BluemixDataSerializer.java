package com.ibm.airlock.admin.serialize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.cloud.objectstorage.AmazonClientException;
import com.ibm.cloud.objectstorage.AmazonServiceException;
import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.auth.AWSCredentials;
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.ibm.cloud.objectstorage.oauth.BasicIBMOAuthCredentials;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder;
import com.ibm.cloud.objectstorage.services.s3.model.CannedAccessControlList;
import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsRequest;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectListing;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata;
import com.ibm.cloud.objectstorage.services.s3.model.PutObjectRequest;
import com.ibm.cloud.objectstorage.services.s3.model.S3Object;
import com.ibm.cloud.objectstorage.services.s3.model.S3ObjectSummary;
import com.ibm.cloud.objectstorage.util.IOUtils;

public class BluemixDataSerializer extends BaseDataSerializer implements DataSerializer {
	
	public static final Logger logger = Logger.getLogger(S3DataSerializer.class.getName());

	public static final String BUCKET_NAME = "bucketName";
	public static final String INTERNAL_PATH_PREFIX = "internalPathPrefix";
	public static final String RUNTIME_PATH_PREFIX = "runtimePathPrefix";
	private static String COS_ENDPOINT = "endpoint";
	private static String COS_API_KEY_ID =  "apiKey";
	private static String COS_SERVICE_CRN =  "serviceCrn";
	private static String COS_BUCKET_LOCATION = "location";
	
	public static int S3_ACTION_RETRY_NUM = 3;
	public static int RETRY_INTERVAL_MS = 1000; //sleep 1 second between IO actions trials;
	
	public static String SEPARATOR = "/";

	protected AmazonS3 bluemixClient ;
	//private String s3region;
	private String bucketName;
	private String internalPathPrefix;
	private String runtimePathPrefix;

	public String getS3bucketName() {
		return bucketName;
	}

	public String getInternalPathPrefix() {
		return internalPathPrefix;
	}
	
	public String getRuntimePathPrefix() {
		return runtimePathPrefix;
	}

	public String getSeparator() {
		return SEPARATOR;
	}

	public BluemixDataSerializer(JSONObject storageParamsObj) throws JSONException {
		super(storageParamsObj);
		
		this.bucketName = (String)storageParamsObj.get(BUCKET_NAME);
		this.internalPathPrefix = (String)storageParamsObj.get(INTERNAL_PATH_PREFIX);
		this.runtimePathPrefix = (String)storageParamsObj.get(RUNTIME_PATH_PREFIX);
		
		bluemixClient = createClient(storageParamsObj.getString(COS_API_KEY_ID), storageParamsObj.getString(COS_SERVICE_CRN), storageParamsObj.getString(COS_ENDPOINT), storageParamsObj.getString(COS_BUCKET_LOCATION));
	}

	private AmazonS3 createClient(String api_key, String service_instance_id, String endpoint_url, String location)
	{
	    AWSCredentials credentials = new BasicIBMOAuthCredentials(api_key, service_instance_id);
	    ClientConfiguration clientConfig = new ClientConfiguration().withRequestTimeout(5000);
	    clientConfig.setUseTcpKeepAlive(true);

	    AmazonS3 cos = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
	            .withEndpointConfiguration(new EndpointConfiguration(endpoint_url, location)).withPathStyleAccessEnabled(true)
	            .withClientConfiguration(clientConfig).build();

	    return cos;
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
				doWriteData(path, data);
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
	protected void doCopyFile (String fromPath, String toPath) throws IOException {
		boolean succeeded = false;
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				bluemixClient.copyObject(bucketName, fromPath, bucketName, toPath);
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
		String fromPathPrefix = isRuntimeFile(fromPath) ?  runtimePathPrefix : internalPathPrefix;
		String toPathPrefix = isRuntimeFile(toPath) ?  runtimePathPrefix : internalPathPrefix;
		
		doCopyFile(fromPathPrefix+fromPath, toPathPrefix+toPath);
	}

	public void copyFolder (String fromPath, String toPath) throws IOException {
		copyFolder(fromPath, toPath, false);
	}

	//try 3 time to write - if fails => throw ioException
	//for now support only internal data copy. (doListS3Object internalFolder=true) 
	public void copyFolder (String fromPath, String toPath, boolean publish) throws IOException {
		ObjectListing objectListing = doListS3Object(fromPath, true); //try 3 times to list objects
		copyObjects(objectListing, toPath);

		while (objectListing.isTruncated()) {
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing);
			copyObjects(objectListing, toPath);
		}

		if (publish) {
			copyPublishedFolder(fromPath, toPath);
		}
	}

	private void copyPublishedFolder(String fromPath, String toPath) {
		//currently nothing. the regular write is public
		
	}

	private void copyObjects(ObjectListing objectListing, String toPath) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (key.endsWith(SEPARATOR))
				continue;

			String[] keyParts = key.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];
			
			String pathPrefix = isRuntimeFile(toPath) ?  runtimePathPrefix : internalPathPrefix;
			doCopyFile(key, pathPrefix + toPath + SEPARATOR + fileName);
		}
	}

	private void doWriteData(String path, String data)  throws AmazonClientException, AmazonServiceException {
		byte[] contentAsBytes = data.getBytes();
		ObjectMetadata          md = new ObjectMetadata();
		md.setContentLength(contentAsBytes.length);

		boolean isRuntimeFile = isRuntimeFile(path);
		String pathPrefix = isRuntimeFile ?  runtimePathPrefix : internalPathPrefix;
		if (isRuntimeFile) {
			bluemixClient.putObject(new PutObjectRequest(bucketName, pathPrefix+path, new ByteArrayInputStream(contentAsBytes), md)
								.withCannedAcl(CannedAccessControlList.PublicRead));
		} 
		else {
			bluemixClient.putObject(bucketName, pathPrefix+path, new ByteArrayInputStream(contentAsBytes), md);
			
		}
	}
	
	private void doWriteInputStreamData(String path, InputStream data, int dataLength) throws AmazonClientException, AmazonServiceException {
		ObjectMetadata md = new ObjectMetadata();
		md.setContentLength(dataLength);

		boolean isRuntimeFile = isRuntimeFile(path);
		String pathPrefix = isRuntimeFile ?  runtimePathPrefix : internalPathPrefix;
		if (isRuntimeFile) {
			bluemixClient.putObject(new PutObjectRequest(bucketName, pathPrefix+path, data, md)
				.withCannedAcl(CannedAccessControlList.PublicRead));
		}
		else {
			bluemixClient.putObject(bucketName, pathPrefix+path, data, md);
		}
	}

	public JSONObject readDataToJSON(String path)  throws IOException, JSONException {
		String pathPrefix = isRuntimeFile(path) ?  runtimePathPrefix : internalPathPrefix;
		
		return doReadDataToJSON(pathPrefix+path);
	}
	
	
	public String readDataToString(String path)  throws IOException {	
		String pathPrefix = isRuntimeFile(path) ?  runtimePathPrefix : internalPathPrefix;
		
		return doReadDataToString(pathPrefix+path);
	}

	public byte[] readDataToByteArray(String path) throws IOException {	
		String pathPrefix = isRuntimeFile(path) ?  runtimePathPrefix : internalPathPrefix;
		
		return doReadDataToByteArray(pathPrefix+path);
	}

	public void deleteFolderContent(String path) throws IOException {
		deleteFolderContent(path, false);
	}

	//delete files that reside in the folder. And then delete the folder itself.
	//if folder contains subFolders they won't be deleted.
	public void deleteFolderContent(String path, boolean published) throws IOException /* throws IOException, JSONException*/ {
		ObjectListing objectListing = doListS3Object(path, true); //try 3 times to list objects
		deleteObjects(objectListing);

		while (objectListing.isTruncated()) {
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing);
			deleteObjects(objectListing);
		}

		if (published) {
			deletePublishedFolderContent(path);
		}
	}

	private void deletePublishedFolderContent(String path) {
		//currently nothing. the regular write is public
	}

	private void deleteObjects(ObjectListing objectListing) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (!key.endsWith(SEPARATOR)) //first delete only the files that are in the folder
				doDeleteObject(key); //try 3 time to delete the file
		}
	}

	//try 3 times to delete the object
	private void doDeleteObject(String path) throws IOException {
		
		boolean succeeded = false;
		
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				bluemixClient.deleteObject(bucketName, path);
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

		ObjectListing objectListing = doListS3Object(path, true); //try 3 times
		addObjectsNames(objectListing, fileNamesList);

		while (objectListing.isTruncated()) {
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing);
			addObjectsNames(objectListing, fileNamesList);
		}
		return fileNamesList;
	}

	private void addObjectsNames(ObjectListing objectListing, LinkedList<String> fileNamesList) {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			if (key.equals(SEPARATOR))
				continue;

			String[] keyParts = key.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];

			if (fileName!=null && !fileName.isEmpty()) {
				fileNamesList.add(fileName);
			}

		}
	}

	public LinkedList<JSONObject> listFilesData(String path, String fileName) throws IOException, JSONException {
		LinkedList<JSONObject> objList = new LinkedList<JSONObject>();
		ObjectListing objectListing = doListS3Object(path, true); //try 3 times
		addObjectsData(objectListing, objList, fileName);

		while (objectListing.isTruncated()) {
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing);
			addObjectsData(objectListing, objList, fileName);
		}
		return objList;
	}

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
				return bluemixClient.getObject(bucketName, path);
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
	protected ObjectListing doListS3Object (String path, boolean internalFolder) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				
				String pathPrefix = internalFolder ?  internalPathPrefix : runtimePathPrefix;
				
				ListObjectsRequest listObjectRequest = new ListObjectsRequest().
						withBucketName(bucketName).
						withPrefix(pathPrefix+path);
				return bluemixClient.listObjects(listObjectRequest);										
			} catch (AmazonServiceException ase) {
				lastErrStr = AmazonServiceException2ErrMsg(ase);
				logger.warning("Failed listing S3 objects from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);
				
			} catch (AmazonClientException ace) {
				lastErrStr = AmazonClientException2ErrMsg(ace);
				logger.warning("Failed listing S3 objects from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
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

	//use this instead of doListS3Object for full coverage
	protected List<S3ObjectSummary> getFullListing(String path) throws IOException
	{
		ObjectListing objectListing = doListS3Object(path, true);

		List<S3ObjectSummary> allSummaries = objectListing.getObjectSummaries();
		while (objectListing.isTruncated())
		{
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing); 
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
		String pathPrefix = isRuntimeFile(path) ?  runtimePathPrefix : internalPathPrefix;
		
		doDeleteObject(pathPrefix+path);
		if (published) {
			deletePublicData(path);
		}
	}

	private void deletePublicData(String path) {
		//currently nothing. the regular write is public
	}

	public String getPathPrefix() {
		return bucketName + SEPARATOR + internalPathPrefix;
	}

	public String getPublicPathPrefix() {
		return bucketName + SEPARATOR + runtimePathPrefix;
	}

	public boolean isFileExists(String path) {
		
		try {
			String pathPrefix = isRuntimeFile(path) ?  runtimePathPrefix : internalPathPrefix;
			
			bluemixClient.getObjectMetadata(bucketName, pathPrefix+path);
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
			bluemixClient.getObjectMetadata(bucketName, fullPath);
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
			objectListing = bluemixClient.listNextBatchOfObjects(objectListing);
			fullPathCopyObjects(objectListing, fullFromPath, relativeToPath);
		}
	}

	private void fullPathCopyObjects(ObjectListing objectListing, String fullFromPath, String relativeToPath) throws IOException {
		for (S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
			String key = objectSummary.getKey();
			System.out.println(key);
			if (key.endsWith(SEPARATOR))
				continue;

			String fileName = key.substring(fullFromPath.length()+1);
			
			String pathPrefix = isRuntimeFile(fileName) ?  runtimePathPrefix : internalPathPrefix;
			
			doCopyFile(key, pathPrefix+relativeToPath + SEPARATOR + fileName);
		}
	}
	
	//try to read 3 times
	private ObjectListing fullPathDoListS3Object (String fullPath) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				ListObjectsRequest listObjectRequest = new ListObjectsRequest().
						withBucketName(bucketName).
						withPrefix(fullPath);
				return bluemixClient.listObjects(listObjectRequest);										
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

	@Override
	public void writeDataToRuntimeFolder(String path, String data) throws IOException {
		//for now not implemented since needed only in s3 serializer
	}
}
