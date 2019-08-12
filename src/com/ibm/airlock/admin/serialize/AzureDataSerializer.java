package com.ibm.airlock.admin.serialize;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.AirLockContextListener;
import com.ibm.airlock.Constants;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;


public class AzureDataSerializer extends BaseDataSerializer implements DataSerializer {

	public static final Logger logger = Logger.getLogger(AzureDataSerializer.class.getName());

	public static String AZURE_STORAGE_ACCOUNT_NAME = "azureStorageAccountName";
	public static String AZURE_STORAGE_ACCOUNT_KEY = "azureStorageAccountKey";
	public static String AZURE_INTERNAL_CONTSAINER_NAME = "azureInternalContainerName";
	public static String AZURE_RUNTIME_CONTSAINER_NAME = "azureRuntimeContainerName";

	public static int S3_ACTION_RETRY_NUM = 3;
	public static int RETRY_INTERVAL_MS = 1000; //sleep 1 second between IO actions trials;

	public static String SEPARATOR = "/";
	
	private String azureStorageAccountName;
	private String azureStorageAccountKey;
	private String azureInternalContainerName;
	private String azureRuntimeContainerName;

	private CloudBlobContainer internalContainer;
	private CloudBlobContainer runtimeContainer;
	
	public String getSeparator() {
		return SEPARATOR;
	}

	public AzureDataSerializer(JSONObject storageParamsObj) throws JSONException, InvalidKeyException, URISyntaxException, StorageException{
		super(storageParamsObj);

		this.azureStorageAccountName = (String)storageParamsObj.get(AZURE_STORAGE_ACCOUNT_NAME);
		this.azureStorageAccountKey = (String)storageParamsObj.get(AZURE_STORAGE_ACCOUNT_KEY);
		this.azureInternalContainerName = (String)storageParamsObj.get(AZURE_INTERNAL_CONTSAINER_NAME);
		this.azureRuntimeContainerName = (String)storageParamsObj.get(AZURE_RUNTIME_CONTSAINER_NAME);

		String storageConnectionString =
				"DefaultEndpointsProtocol=https;" +
						"AccountName=" + azureStorageAccountName  + ";" +
						"AccountKey=" + azureStorageAccountKey;

		CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

		this.internalContainer = blobClient.getContainerReference(azureInternalContainerName);
		this.runtimeContainer = blobClient.getContainerReference(azureRuntimeContainerName);
	}

	public static AzureDataSerializer generateAzureDataSerializer(String azureStorageAccountName, String azureStorageAccountKey, String azureInternalContainerName, String azureRuntimeContainerName) throws JSONException, InvalidKeyException, URISyntaxException, StorageException {
		JSONObject storageParamsObj = new JSONObject("{\"" + BaseDataSerializer.RUNTIME_PUBLIC_FULL_PATH + "\":\"NA\",\"" + BaseDataSerializer.STORAGE_PUBLIC_PATH + "\":\"NA\",\"" +
				DataSerializer.STORAGE_TYPE + "\":\"" + DataSerializer.STORAGE_TYPE_AZURE +
				"\",\"" + AzureDataSerializer.AZURE_STORAGE_ACCOUNT_NAME + "\":\"" + azureStorageAccountName + "\",\"" + AzureDataSerializer.AZURE_STORAGE_ACCOUNT_KEY + "\":\"" + azureStorageAccountKey +
				",\"" + AzureDataSerializer.AZURE_INTERNAL_CONTSAINER_NAME + "\":\"" + azureInternalContainerName +
				",\"" + AzureDataSerializer.AZURE_RUNTIME_CONTSAINER_NAME + "\":\"" + azureRuntimeContainerName +
				"\"}");
		return new AzureDataSerializer(storageParamsObj);

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
			} catch (StorageException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);	
			} catch (URISyntaxException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);
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
			} catch (StorageException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);	
			} catch (URISyntaxException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed writing data to '" + path + "', trial number " + (i+1) + ": " +lastErrStr);
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

	 // Wait until the copy complete.
     private static void waitForCopyToComplete(CloudBlockBlob blob) throws InterruptedException, StorageException {
        CopyStatus copyStatus = CopyStatus.PENDING;
        while (copyStatus == CopyStatus.PENDING) {
            Thread.sleep(1000);
            copyStatus = blob.getCopyState().getStatus();
        }
    }
     
	//try 3 time to write - if fails => throw ioException
	protected void doCopyFile (String fromPath, String toPath) throws IOException {
		boolean succeeded = false;
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				CloudBlockBlob from;
				if (isRuntimeFile(fromPath)) {
					from = runtimeContainer.getBlockBlobReference(fromPath);
				}
				else {
					from = internalContainer.getBlockBlobReference(fromPath);
				}
				
				CloudBlockBlob to;
				if (isRuntimeFile(toPath)) {
					to = runtimeContainer.getBlockBlobReference(toPath);
				}
				else {
					to = internalContainer.getBlockBlobReference(toPath);
				}
				
				to.startCopy(from);
				waitForCopyToComplete(to);
				succeeded =true;
				break;				
			} catch (StorageException se) {
				lastErrStr = se.getMessage();
				logger.warning("Failed copying file from '" + fromPath + " to " + toPath + ", trial number " + (i+1) + ": " +lastErrStr);				
			} catch (URISyntaxException ue) {
				lastErrStr = ue.getMessage();
				logger.warning("Failed copying file from '" + fromPath + " to " + toPath + ", trial number " + (i+1) + ": " + lastErrStr);				
			} catch (InterruptedException e) {
			    //do nothing
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
		doCopyFile(fromPath, toPath);
	}

	public void copyFolder (String fromPath, String toPath) throws IOException {
		copyFolder(fromPath, toPath, false);
	}

	//try 3 time to write - if fails => throw ioException
	public void copyFolder (String fromPath, String toPath, boolean publish) throws IOException {
		//internal
		Iterable<ListBlobItem> blobs = doListBlobObjectsInFolder(fromPath, true);

		for (ListBlobItem blobItem : blobs) {
			String uri = blobItem.getUri().toString();
			System.out.println(uri);
			if (uri.endsWith(SEPARATOR))
				continue;

			//split the path to http perefix and real path in storage.
			//uri is: https://airlocktestfiles.blob.core.windows.net/iritcont/AirlockAPIKeys.json
			String[] pathParts = uri.split(azureInternalContainerName+SEPARATOR);
			if (pathParts.length!=2) {
				throw new IOException("Illegal URU: " + uri);
			}

			String blobPath = pathParts[1];

			String[] keyParts = blobPath.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];
			doCopyFile(blobPath, toPath + SEPARATOR + fileName);
		}

		if (!publish)
			return;
		
		//runtime
		blobs = doListBlobObjectsInFolder(fromPath, false);

		for (ListBlobItem blobItem : blobs) {
			String uri = blobItem.getUri().toString();
			System.out.println(uri);
			if (uri.endsWith(SEPARATOR))
				continue;

			//split the path to http perefix and real path in storage.
			//uri is: https://airlocktestfiles.blob.core.windows.net/iritcont/AirlockAPIKeys.json
			String[] pathParts = uri.split(azureRuntimeContainerName+SEPARATOR);
			if (pathParts.length!=2) {
				throw new IOException("Illegal URU: " + uri);
			}

			String blobPath = pathParts[1];

			String[] keyParts = blobPath.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];
			doCopyFile(blobPath, toPath + SEPARATOR + fileName);
		}

	}

	private void doWriteData(String path, String data)  throws StorageException, URISyntaxException, IOException {
		CloudBlockBlob blob;
		if (isRuntimeFile(path)) {
			blob = runtimeContainer.getBlockBlobReference(path);
		}
		else {
			blob = internalContainer.getBlockBlobReference(path);
		}
		//blob.getProperties().setContentType("application/json");
		blob.uploadText(data);

		logger.info("Succeeded writing data to '" + path + "'");					
	}

	private void doWriteInputStreamData(String path, InputStream data, int dataLength) throws StorageException, URISyntaxException, IOException {
		CloudBlockBlob blob;
		if (isRuntimeFile(path)) {
			blob = runtimeContainer.getBlockBlobReference(path);
		}
		else {
			blob = internalContainer.getBlockBlobReference(path);
		}
		blob.upload(data, dataLength);

		logger.info("Succeeded writing data to '" + path + "'");			
	}

	public JSONObject readDataToJSON(String path)  throws IOException, JSONException {	
		try {
			String res = doReadAzureObject (path, S3_ACTION_RETRY_NUM);

			JSONObject dataObj = new JSONObject(res);
			return dataObj;    
		} catch (IOException ioe) {
			String err = "Failed reading JSON data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		} catch (JSONException je) {
			String err = "Failed reading JSON data from " + path + ": " + je.getMessage();
			throw new JSONException(err);
		}
	}


	public String readDataToString(String path)  throws IOException {	
		try {
			return doReadAzureObject(path, S3_ACTION_RETRY_NUM);
		} catch (IOException ioe) {
			String err = "Failed reading String data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		}
	}
	
	public byte[] readDataToByteArray(String path)  throws IOException {	
		try {
			return doReadAzureObjectToByteArray(path, S3_ACTION_RETRY_NUM);
		} catch (IOException ioe) {
			String err = "Failed reading String data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		}
	}

	public void deleteFolderContent(String path) throws IOException {
		deleteFolderContent(path, true);
	}

	//delete files that reside in the folder. And then delete the folder itself.
	//if folder contains subFolders delete its content as well.
	public void deleteFolderContent(String path, boolean published) throws IOException /* throws IOException, JSONException*/ {
		//internal
		Iterable<ListBlobItem> blobs = doListBlobObjectsInFolder(path, true);
		for (ListBlobItem blobItem : blobs) {
			if (blobItem instanceof CloudBlob) {
				CloudBlob blob = (CloudBlob) blobItem;
				doDeleteBlob(blob);
			}
			if (blobItem instanceof CloudBlobDirectory) {
				CloudBlobDirectory dir = (CloudBlobDirectory)blobItem;
				deleteFolderContent(dir.getPrefix(), published);
			}
		}
		
		//runtime
		if (published) {
			blobs = doListBlobObjectsInFolder(path, false);
			for (ListBlobItem blobItem : blobs) {
				if (blobItem instanceof CloudBlob) {
					CloudBlob blob = (CloudBlob) blobItem;
					doDeleteBlob(blob);
				}
				if (blobItem instanceof CloudBlobDirectory) {
					CloudBlobDirectory dir = (CloudBlobDirectory)blobItem;
					deleteFolderContent(dir.getPrefix(), published);
				}
			}
		}
	}

	//try 3 times to delete the object
	private void doDeleteObject(String path) throws IOException {

		boolean succeeded = false;

		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				CloudBlockBlob blob;
				if (isRuntimeFile(path)) {
					blob = runtimeContainer.getBlockBlobReference(path);
				}
				else {
					blob = internalContainer.getBlockBlobReference(path);	
				}
				
				blob.delete();

				succeeded =true;
				break;				
			} catch (StorageException se) {
				lastErrStr = se.getMessage();
				logger.warning("Failed deleting object '" + path + "', trial number " + (i+1) + ": " + lastErrStr);				
			} catch (URISyntaxException ace) {
				lastErrStr = ace.getMessage();
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

	
	//try 3 times to delete the object
	private void doDeleteBlob(CloudBlob blob) throws IOException {

		boolean succeeded = false;

		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				//s3client.deleteObject(s3bucketName, path);
				blob.delete();

				succeeded =true;
				break;				
			} catch (StorageException se) {
				lastErrStr = se.getMessage();
				logger.warning("Failed deleting object '" + blob.getName() + "', trial number " + (i+1) + ": " + lastErrStr);				
			} 
			
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}

		if (!succeeded) {
			String err = "Failed deleting object '" + blob.getName() + "' : " + lastErrStr;
			logger.severe(err);
			throw new IOException(err);
		}	
	}


	//return from both internal and runtime folders
	public LinkedList<String> listFileNames(String path) throws IOException {
		LinkedList<String> fileNamesList = new LinkedList<String>();

		//internal
		Iterable<ListBlobItem> blobs = doListBlobObjectsInFolder(path, true);
		for (ListBlobItem blobItem : blobs) {
			String uri = blobItem.getUri().toString();
			
			//split the path to http perefix and real path in storage.
			//uri is: https://airlocktestfiles.blob.core.windows.net/iritcont/AirlockAPIKeys.json
			String[] pathParts = uri.split(azureInternalContainerName+SEPARATOR);
			if (pathParts.length!=2) {
				throw new IOException("Illegal URU: " + uri);
			}

			String blobPath = pathParts[1];

			if (blobPath.equals(SEPARATOR))
				continue;

			String[] keyParts = blobPath.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];

			if (fileName!=null && !fileName.isEmpty()) {
				fileNamesList.add(fileName);
			}
		}
		
		//runtime
		blobs = doListBlobObjectsInFolder(path, false);
		for (ListBlobItem blobItem : blobs) {
			String uri = blobItem.getUri().toString();
			
			//split the path to http perefix and real path in storage.
			//uri is: https://airlocktestfiles.blob.core.windows.net/iritcont/AirlockAPIKeys.json
			String[] pathParts = uri.split(azureRuntimeContainerName+SEPARATOR);
			if (pathParts.length!=2) {
				throw new IOException("Illegal URU: " + uri);
			}

			String blobPath = pathParts[1];

			if (blobPath.equals(SEPARATOR))
				continue;

			String[] keyParts = blobPath.split(SEPARATOR);
			if (keyParts == null || keyParts.length == 0)
				continue;


			String fileName = keyParts[keyParts.length-1];

			if (fileName!=null && !fileName.isEmpty()) {
				fileNamesList.add(fileName);
			}
		}

		return fileNamesList;
	}
	//try to read 3 times
	private String doReadAzureObject (String path, int numberOfTrials) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<numberOfTrials; i++) {
			try {
				CloudBlockBlob blob;
				if (isRuntimeFile(path)) {
					blob = runtimeContainer.getBlockBlobReference(path);
				}
				else {
					blob = internalContainer.getBlockBlobReference(path);
				}
				return blob.downloadText();
			} catch (StorageException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed reading data from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);			
			} catch (URISyntaxException e) {
				lastErrStr = e.getMessage();
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
	private byte[] doReadAzureObjectToByteArray (String path, int numberOfTrials) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<numberOfTrials; i++) {
			try {
				CloudBlockBlob blob;
				if (isRuntimeFile(path)) {
					blob = runtimeContainer.getBlockBlobReference(path);
				}
				else {
					blob = internalContainer.getBlockBlobReference(path);
				}
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				blob.download(os);
				return os.toByteArray();
			} catch (StorageException e) {
				lastErrStr = e.getMessage();
				logger.warning("Failed reading data from '" + path + "', trial number " + (i+1) + ": " + lastErrStr);			
			} catch (URISyntaxException e) {
				lastErrStr = e.getMessage();
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
	protected Iterable<ListBlobItem> doListBlobObjectsInFolder (String dirPath, boolean internal) throws IOException {
		String lastErrStr = "";
		for (int i=0; i<S3_ACTION_RETRY_NUM; i++) {
			try {
				CloudBlobDirectory dir;
				if (internal) {
					dir = internalContainer.getDirectoryReference(dirPath);
				}
				else {
					dir = runtimeContainer.getDirectoryReference(dirPath);
				}
				return dir.listBlobs();
														
			} catch (URISyntaxException ue) {
				lastErrStr = ue.getMessage();
				logger.warning("Failed listing " + (internal ? "internal":"runtime") + " S3 objects from '" + dirPath + "', trial number " + (i+1) + ": " + lastErrStr);
				
			} catch (StorageException se) {
				lastErrStr = se.getMessage();
				logger.warning("Failed listing " + (internal ? "internal":"runtime") + " S3 objects from '" + dirPath + "', trial number " + (i+1) + ": " + lastErrStr);				
			}
			
			try {
				if (i<S3_ACTION_RETRY_NUM-1) //sleep between trials (but not after the last)
					Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
		
		String err = "Failed listing " + (internal ? "internal":"runtime") + " S3 objects from '" + dirPath + "' : " + lastErrStr;
		logger.severe(err);
		throw new IOException(err);				
	}

	private JSONObject doReadDataToJSON(String path)  throws IOException, JSONException {
		try {
			String res = doReadAzureObject (path, S3_ACTION_RETRY_NUM);

			JSONObject dataObj = new JSONObject(res);
			return dataObj;    
		} catch (IOException ioe) {
			String err = "Failed reading JSON data from " + path + ": " + ioe.getMessage();
			throw new IOException(err);
		} catch (JSONException je) {
			String err = "Failed reading JSON data from " + path + ": " + je.getMessage();
			throw new JSONException(err);
		}

	}

	public void deleteData(String path) throws IOException {
		deleteData(path, false);
	}

	public void deleteData(String path, boolean published) throws IOException {
		doDeleteObject(path);
	}

	public String getPathPrefix() {
		return azureInternalContainerName + SEPARATOR;
	}

	public String getPublicPathPrefix() {
		return azureRuntimeContainerName + SEPARATOR;
	}

	public boolean isFileExists(String path) {
		try {
			CloudBlockBlob blob;
			if (isRuntimeFile(path)) {
				blob = runtimeContainer.getBlockBlobReference(path);
			}
			else {
				blob = internalContainer.getBlockBlobReference(path);
			}
			return blob.exists();
		} catch (StorageException se) {
			return false;
		} catch (URISyntaxException use) {
			return false;				
		}				
	}


	//for tests: functions that ignore the prefix
	public boolean fullPathIsFileExists(String fullPath) {
		return isFileExists(fullPath); //we dont have prefix in azure serializer so path==fullPath			
	}

	public JSONObject fullPathReadDataToJSON(String fullPath)  throws IOException, JSONException {
		return doReadDataToJSON(fullPath);
	}

	//try 3 time to write - if fails => throw ioException
	public void fullPathCopyFolder (String fullFromPath, String relativeToPath) throws IOException {
		copyFolder(fullFromPath, relativeToPath);
	}

	//S3Encryptor stuff

	private static final String KEY = "alias/airlock_key";

	public void putEncryptedFile(String data, String path) throws Exception
	{
		byte[] plaintext = data.getBytes("UTF-8");
		putEncryptedFile(plaintext, path);
	}
	public void putEncryptedFile(byte[] plaintext, String path) throws Exception
	{//TODO:
		/*ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(plaintext.length);

		PutObjectRequest req = new PutObjectRequest(s3bucketName, path,
				new ByteArrayInputStream(plaintext), metadata)
				.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(KEY));

		@SuppressWarnings("unused")
		PutObjectResult putResult = s3client.putObject(req);*/
	}

	public String getEncryptedFileFullPath(String path) throws Exception
	{
		return null;
		//TODO:
		/*S3Object s3object = s3client.getObject(s3bucketName, path);
		return IOUtils.toString(s3object.getObjectContent());*/
	}

	public static void main (String[] args) {
		//upload files from local folder to a folder in azure 
		if (args.length!=2) {
			System.err.println("Usage: sourceFolderPath destFolderPath" );
			System.exit(-1);		
		}
		
		String fromPath = args[0];
		String toPath = args[1];
		
		File fromFolder = new File(fromPath);
		if (!fromFolder.exists() || !fromFolder.isDirectory()) {
			System.err.println(fromPath + " does not exist or not a folder" );
			System.exit(-1);		
		}

		AzureDataSerializer ads = null;
		try {
			ads = new AzureDataSerializer(new JSONObject(AirLockContextListener.getEnv(Constants.ENV_STORAGE_PARAMS)));
		} catch (Exception e) {
			System.err.println("cannot create AzureDataSerializer: " + e.getMessage());
			System.exit(-1);	
		}
		
		try {
			ads.deleteFolderContent(toPath);
			copyFolderContent(ads, fromFolder, toPath);
		} catch (IOException e) {
			System.err.println("cannot upload folder: " + e.getMessage());
			System.exit(-1);	
		}
		
		System.out.println("Terminated successfully");
	}
	
	public static void copyFolderContent(AzureDataSerializer ads, File fromFolder, String toPath) throws IOException {
		//Iterator it = FileUtils.iterateFiles(fromFolder, null, false);
        //while(it.hasNext()){
        //		File subFile = (File) it.next();
		File[] listOfFiles = fromFolder.listFiles();
		for (File subFile : listOfFiles) {
        		if (subFile.isDirectory()) {
        			//folder
        			copyFolderContent(ads, subFile, toPath+SEPARATOR+subFile.getName());
        		}
        		else {
        			//file
        			
        			String fileName = subFile.getName();
        			if (fileName.startsWith("."))
        				continue;
        			String content = new String ( Files.readAllBytes( Paths.get(subFile.getPath()) ) );
        			ads.writeData(toPath+SEPARATOR+subFile.getName(), content);
        			System.out.println("copy file " + subFile.getPath() + " to " + toPath+SEPARATOR+fileName);
        		}
            
        }
		
	}
	
	@Override
	public void writeDataToRuntimeFolder(String path, String data) throws IOException {
		// TODO for now not implemented since needed only in s3 serializer
		
	}
}
