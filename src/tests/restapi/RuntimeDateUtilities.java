package tests.restapi;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.serialize.Compression;
import com.ibm.airlock.admin.serialize.Encryption;

import tests.com.ibm.qautils.RestClientUtils.RestCallResults;
import tests.restapi.RuntimeRestApi.DateModificationResults;

public class RuntimeDateUtilities {
	
	public static String RUNTIME_DEVELOPMENT_FEATURE = "AirlockRuntimeDEVELOPMENT.json";
	public static String RUNTIME_PRODUCTION_FEATURE = "AirlockRuntimePRODUCTION.json";
	
	public static String RUNTIME_DEVELOPMENT_STREAM = "AirlockStreamsDEVELOPMENT.json";
	public static String RUNTIME_PRODUCTION_STREAM = "AirlockStreamsPRODUCTION.json";
	public static String RUNTIME_STREAM = "AirlockStreams.json";
	public static String RUNTIME_DEVELOPMENT_UTILITY = "AirlockUtilitiesDEVELOPMENT.txt";
	public static String RUNTIME_PRODUCTION_UTILITY = "AirlockUtilitiesPRODUCTION.txt";
	public static String RUNTIME_DEVELOPMENT_STREAM_UTILITY = "AirlockStreamsUtilitiesDEVELOPMENT.txt";
	public static String RUNTIME_PRODUCTION_STREAM_UTILITY = "AirlockStreamsUtilitiesPRODUCTION.txt";	
	public static String RUNTIME_BRANCHES_DEVELOPMENT = "AirlockRuntimeBranchDEVELOPMENT.json";
	public static String RUNTIME_BRANCHES_PRODUCTION = "AirlockRuntimeBranchPRODUCTION.json";
	public static String RUNTIME_AIRLOCK_FEATURES = "AirlockFeatures.json";
	public static String RUNTIME_AIRLOCK_BRANCH_FEATURES = "AirlockBranchFeatures.json";
	public static String RUNTIME_OLD_SEASON = "AirlockRuntime.json";
	public static String PRODUCTION_CHANGED = "productionChanged.txt";
	public static String ORIGINAL_TRANSLATIONS = "original.json";
	public static String RUNTIME_DEVELOPMENT_AIRLOCK_NOTIFICATION = "AirlockNotificationsDEVELOPMENT.json";
	public static String RUNTIME_PRODUCTION_AIRLOCK_NOTIFICATION = "AirlockNotificationsPRODUCTION.json";
	public static String RUNTIME_INTERNSAL_USER_GROUPS = "AirlockUserGroupsRuntime.json";
	public static String AIRLOCK_RUNTIME_BRANCHES_FILE_NAME = "AirlockBranchesRuntime.json";	
	public static String RUNTIME_PRODUCT_FILE_NAME = "productRuntime.json";
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	//304 - the file hasn't changed
	//200 - file changed

	public static RuntimeRestApi.DateModificationResults getOriginalStringsFile(String m_url, String productID, String seasonID, String sessionToken) throws JSONException, IOException{

		String filePath = buildPath(m_url, productID, seasonID, sessionToken)  + "translations/" + ORIGINAL_TRANSLATIONS;
		RestCallResults res;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			res = RuntimeRestApi.getRuntimeFile(filePath, sessionToken, seasonID, m_url);
		}
		else {
			res = fsGetRuntimeFile(filePath);
		}
		return new DateModificationResults (res.message, res.code);

	}
	
	public static RuntimeRestApi.DateModificationResults getFileModificationDate(String fileName, String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		String filePath = buildPath(m_url, productID, seasonID, sessionToken)  + fileName;
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getFileModificationDate(filePath, dateFormat, sessionToken, seasonID, m_url);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;

	}
	
	public static RuntimeRestApi.DateModificationResults getNonEncryptedFileModificationDate(String fileName, String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		String filePath = buildPath(m_url, productID, seasonID, sessionToken)  + fileName;
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getNonEncryptedFileModificationDate(filePath, dateFormat, sessionToken);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;

	}
	

	public static RuntimeRestApi.DateModificationResults getRuntimeFileContent(String fileName, String m_url, String productID, String seasonID, String sessionToken) throws IOException, JSONException{
		String filePath = buildPath(m_url, productID, seasonID, sessionToken)  + fileName;
		RestCallResults res;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			res = RuntimeRestApi.getRuntimeFile(filePath, sessionToken, seasonID, m_url);
		}
		else {
			res = fsGetRuntimeFile(filePath);
		}
		return new DateModificationResults (res.message, res.code);
	}


	//branches	
	public static RuntimeRestApi.DateModificationResults getRuntimeBranchFileContent(String fileName, String m_url, String productID, String seasonID, String branchID, String sessionToken) throws IOException, JSONException{
		String filePath = buildBranchPath(m_url, productID, seasonID, branchID, sessionToken)  + fileName;
		RestCallResults res;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			res = RuntimeRestApi.getRuntimeFile(filePath, sessionToken, seasonID, m_url);
		}
		else {
			res = fsGetRuntimeFile(filePath);
		}
		return new DateModificationResults (res.message, res.code);
	}
	
	public static RuntimeRestApi.DateModificationResults getDevelopmentBranchFileDateModification(String m_url, String productID, String seasonID, String branchID, String dateFormat, String sessionToken) throws JSONException, IOException{
		String filePath;
		if (branchID.equals(BranchesRestApi.MASTER)) {
			filePath = buildPath(m_url, productID, seasonID, sessionToken) + RUNTIME_DEVELOPMENT_FEATURE;
		} else{
			filePath = buildBranchPath(m_url, productID, seasonID, branchID, sessionToken) + RUNTIME_BRANCHES_DEVELOPMENT;
		}
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getFileModificationDate(filePath, dateFormat, sessionToken, seasonID, m_url);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;
	}


	public static RuntimeRestApi.DateModificationResults getProductionBranchFileDateModification(String m_url, String productID, String seasonID, String branchID, String dateFormat, String sessionToken) throws JSONException, IOException{

		String filePath;
		if (branchID.equals(BranchesRestApi.MASTER)) {
			filePath = buildPath(m_url, productID, seasonID, sessionToken) + RUNTIME_PRODUCTION_FEATURE;
		} else {
			filePath = buildBranchPath(m_url, productID, seasonID, branchID, sessionToken) + RUNTIME_BRANCHES_PRODUCTION;
		}
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getFileModificationDate(filePath, dateFormat, sessionToken, seasonID, m_url);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;
	}

	private static DateModificationResults fsGetFileModificationDate(String filePath, String dateFormat, String seasonID, String url, String sessionToken) throws 
IOException {
		int code;
		String message = "";
		byte[] messageBytes = null;
		File f = new File(filePath);
		long fileLastModified = f.lastModified();
		try {
			long dateToCheck = simpleDateFormat.parse(dateFormat).getTime();
			if (fileLastModified > dateToCheck) {
				code = 200;
				messageBytes = FileUtils.readFileToByteArray(new File(filePath));//FileUtils.readFileToString(new File(filePath), Charset.forName("UTF-8"));
			}
			else {
				code = 304;
				message = "";
			}
		} catch (ParseException e) {
			code = 404;
			message = "";
		} catch (IOException e) {
			code = 404;
			message = "";
		}

		if (messageBytes == null) {
			return new DateModificationResults(message, code);
		}
		else {
			SeasonsRestApi s;
			s = new SeasonsRestApi();
			s.setURL(url);
			
			try {
				String keyStr = s.getEncryptionKeyString(seasonID, sessionToken);				
				byte[] key = Encryption.fromString(keyStr);				
				Encryption e = new Encryption(key);					
			
				byte[] decrypted = e.decrypt(messageBytes);
				String out = Compression.decompress(decrypted);
			     
			    return new DateModificationResults(out, code);
			} catch (GeneralSecurityException gse) {
				throw new IOException("GeneralSecurityException: " + gse.getMessage());
			} catch (JSONException je) {
				throw new IOException("JSONException: " + je.getMessage());
			}
			
		}
	}

	private static RestCallResults fsGetRuntimeFile(String filePath) {
		int code;
		String message;
		File f = new File(filePath);
		try {
			message = FileUtils.readFileToString(new File(filePath), Charset.forName("UTF-8"));
			code = 200;
		} catch (IOException e) {
			message = e.getMessage();
			code = 404;
		}

		return new RestCallResults(message, code);
	}

	//utilities
	public static RuntimeRestApi.DateModificationResults getDevelopmentUtilitiesDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_DEVELOPMENT_UTILITY , m_url, productID, seasonID, dateFormat, sessionToken);
	}

	public static RuntimeRestApi.DateModificationResults getProductionUtilitiesDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCTION_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
	}

	public static RuntimeRestApi.DateModificationResults getDevelopmentStreamUtilitiesDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_DEVELOPMENT_STREAM_UTILITY , m_url, productID, seasonID, dateFormat, sessionToken);
	}

	public static RuntimeRestApi.DateModificationResults getProductionStreamUtilitiesDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCTION_STREAM_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	//features
	public static RuntimeRestApi.DateModificationResults getDevelopmentFileDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
			return getFileModificationDate(RUNTIME_DEVELOPMENT_FEATURE , m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	public static RuntimeRestApi.DateModificationResults getProductionFileDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCTION_FEATURE, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	public static RuntimeRestApi.DateModificationResults getProductionChangedDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(PRODUCTION_CHANGED, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	
	public static RuntimeRestApi.DateModificationResults getRuntimeFile(String m_url, String fileName, String productID, String seasonID, String sessionToken) throws JSONException, IOException{
		return getRuntimeFileContent(fileName, m_url, productID, seasonID, sessionToken);
	}
	
	public static String getProductionChangedFile(String m_url, String productID, String seasonID, String sessionToken) throws JSONException, IOException{
		RuntimeRestApi.DateModificationResults response = getRuntimeFileContent(PRODUCTION_CHANGED, m_url, productID, seasonID, sessionToken);
		return response.message;
	}
	
	public static boolean ifProductionChangedContent(String productionChangedOriginal, String productionChangedNew){
		return productionChangedOriginal.equals(productionChangedNew) ? true : false;
	}
	
	//translations
	public static String getStringProductionFileName(String locale, String m_url, String productID, String seasonID, String sessionToken) throws JSONException{
		return buildPath(m_url, productID, seasonID, sessionToken)  + "translations/" + "strings__" + locale + "PRODUCTION.json";
	}
	
	public static String getStringDevelopmentFileName(String locale, String m_url, String productID, String seasonID, String sessionToken) throws JSONException{
		return buildPath(m_url, productID, seasonID, sessionToken)  + "translations/" + "strings__" + locale + "DEVELOPMENT.json";
	}

	public static RuntimeRestApi.DateModificationResults getDevelopmentTranslationDateModification(String m_url, String locale, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		String filePath = getStringDevelopmentFileName(locale, m_url, productID, seasonID, sessionToken);
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getFileModificationDate(filePath, dateFormat, sessionToken, seasonID, m_url);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;

	}
	
	//streams
	public static RuntimeRestApi.DateModificationResults getDevelopmentStreamsDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_DEVELOPMENT_STREAM , m_url, productID, seasonID, dateFormat, sessionToken);
	}

	public static RuntimeRestApi.DateModificationResults getProductionStreamsDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCTION_STREAM, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	//airlock notification files
	public static RuntimeRestApi.DateModificationResults getDevelopmentNotificationDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_DEVELOPMENT_AIRLOCK_NOTIFICATION , m_url, productID, seasonID, dateFormat, sessionToken);
	}

	public static RuntimeRestApi.DateModificationResults getProductionNotificationDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCTION_AIRLOCK_NOTIFICATION, m_url, productID, seasonID, dateFormat, sessionToken);
	}

	//airlock branches runtime file
	public static RuntimeRestApi.DateModificationResults getRuntimeBranchesDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(AIRLOCK_RUNTIME_BRANCHES_FILE_NAME , m_url, productID, seasonID, dateFormat, sessionToken);
	}

	//user groups runtime file
	public static RuntimeRestApi.DateModificationResults getInternalUserGroupsRuntimeDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_INTERNSAL_USER_GROUPS, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	//season product runtime file
	public static RuntimeRestApi.DateModificationResults getProductRuntimeDateModification(String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		return getFileModificationDate(RUNTIME_PRODUCT_FILE_NAME, m_url, productID, seasonID, dateFormat, sessionToken);
	}
	
	public static RuntimeRestApi.DateModificationResults getProductionTranslationDateModification(String m_url, String locale, String productID, String seasonID, String dateFormat, String sessionToken) throws JSONException, IOException{
		String filePath = getStringProductionFileName(locale, m_url, productID, seasonID, sessionToken);
		RuntimeRestApi.DateModificationResults response;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = cleanFileName(filePath);
			response = RuntimeRestApi.getFileModificationDate(filePath, dateFormat, sessionToken, seasonID, m_url);
		}
		else {
			response = fsGetFileModificationDate(filePath, dateFormat, seasonID, m_url, sessionToken);
		}
		return response;

	}
	
	public static boolean ifFileExists(String fileName, String m_url, String productID, String seasonID, String sessionToken) throws JSONException, IOException{

		String filePath = buildPath(m_url, productID, seasonID, sessionToken)  + fileName;
		if (System.getProperty("fsPathPrefix") == null) {
			filePath = filePath.replaceAll("//", "/");
			filePath = filePath.substring(7);
			filePath = "https://" + filePath;
		}
		return  new File(filePath).exists();
	}

	

	
	public static String getCurrentTimeStamp() {
		//return a timestamp of the current time
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return simpleDateFormat.format(new Date());
	}
	
	public static String buildPath(String m_url, String productID, String seasonID, String sessionToken) throws JSONException{
		if (System.getProperty("fsPathPrefix") == null) {
			SeasonsRestApi s = new SeasonsRestApi();
			s.setURL(m_url);
			String defaults = s.getDefaults(seasonID, sessionToken);
			JSONObject json = new JSONObject(defaults);
			return json.getString("s3Path") + "/seasons/" + productID + "/" + seasonID + "/";
		}
		else {
			return System.getProperty("fsPathPrefix") + "/seasons/" + productID + "/" + seasonID + "/";
		}
	}

	
	private static String buildBranchPath(String m_url, String productID, String seasonID, String branchID, String sessionToken) throws JSONException{
		if (System.getProperty("fsPathPrefix") == null) {
			SeasonsRestApi s = new SeasonsRestApi();
			s.setURL(m_url);
			String defaults = s.getDefaults(seasonID, sessionToken);
			JSONObject json = new JSONObject(defaults);
			return json.getString("s3Path") + "/seasons/" + productID + "/" + seasonID + "/branches/" + branchID + "/";
		}
		else {
			return System.getProperty("fsPathPrefix") + "/seasons/" + productID + "/" + seasonID + "/branches/" + branchID + "/";
		}
	}

	
	public static JSONObject getFeaturesList(String result) throws JSONException{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("root")){
				JSONObject root = json.getJSONObject("root");
				return root;
			} else {
				json.put("error", "Response doesn't contain a root feature");
				return json;
			}
		} catch (Exception e){
				json.put("error", "Response is not a valid json");
				return json;
		}
	}
	
	public static JSONObject getInAppPurchasesList(String result) throws JSONException{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("entitlementsRoot")){
				JSONObject root = json.getJSONObject("entitlementsRoot");
				return root;
			} else {
				json.put("error", "Response doesn't contain a purchasesRoot feature");
				return json;
			}
		} catch (Exception e){
				json.put("error", "Response is not a valid json");
				return json;
		}
	}
	
	public static JSONObject getBranchFeaturesList(String result) throws JSONException{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("branches")){
				org.apache.wink.json4j.JSONArray branchesArray = json.getJSONArray("branches");
				if (branchesArray == null || branchesArray.size() == 0) {
					json.put("error", "Response doesn't contain branches array");
					return json;
				}
					
				return branchesArray.getJSONObject(0);
				/*if (branch.containsKey("features")){
					org.apache.wink.json4j.JSONArray featuresArray = branch.getJSONArray("features");
					if (featuresArray == null || featuresArray.size() == 0) {
						json.put("error", "Response doesn't contain features for first branch");
						return json;
					}
					return featuresArray.getJSONObject(0);
				}
				else {
					json.put("error", "Response doesn't contain features for first branche");
					return json;
				}*/
			} else {
				json.put("error", "Response doesn't contain branches array");
				return json;
			}
		} catch (Exception e){
				json.put("error", "Response is not a valid json");
				return json;
		}
		

	}
	
	public static String cleanFileName(String fileName){
		String filePathPrefix = fileName.substring(0, 7);
		String filePathNoPrefix = fileName.substring(7); //remove http// or https//
		
		filePathNoPrefix = filePathNoPrefix.replaceAll("//", "/");
		
		String filePath = filePathPrefix + filePathNoPrefix;
		//int posFirstSlash = filePath.indexOf("/");
		//filePath = filePath.substring(7);
		//filePath = "https://" + filePath;
		return filePath;
	}
	
	public static String setDateFormat() throws InterruptedException{
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		return dateFormat;
	}
	
	
	public static void setSleep() throws InterruptedException{
		Thread.sleep(5000);
	}

}
