package tests.restapi.scenarios.encryption;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Properties;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import com.ibm.airlock.admin.serialize.Compression;
import com.ibm.airlock.admin.serialize.Encryption;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.com.ibm.qautils.RestClientUtils.RestCallResults;
import tests.restapi.*;
import tests.restapi.RuntimeRestApi.DateModificationResults;

public class SeasonEncryptionFlagPositive
{
	protected String seasonID1;
	protected String seasonID2;
	protected String seasonID3;

	protected String productID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected TranslationsRestApi translationsApi;
	protected StringsRestApi str;
	protected UtilitiesRestApi u;
	protected BranchesRestApi br;
	private OperationRestApi operApi;


	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	protected String m_url;
	private JSONObject seasonJson; 


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "operationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String operationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);

		str = new StringsRestApi();
		str.setURL(translationsUrl);
		translationsApi = new TranslationsRestApi();
		translationsApi.setURL(translationsUrl);
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		//seasonID = baseUtils.createSeason(productID);	
		String seasonStr = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		seasonJson = new JSONObject(seasonStr);
	}

	@Test(description = "create some seasons encrypted and some not")
	public void testCombinationOfEncryptionSeasons() throws Exception {
		//validate RUNTIME_ENCRYPTION capability in product
		JSONArray capabilities = getCapabilitiesInProduct(productID, sessionToken);
		Assert.assertTrue(capabilityIncluded(capabilities, "RUNTIME_ENCRYPTION"), "RUNTIME_ENCRYPTION is not included in product");

		//create season without runtimeEncryption
		seasonID1 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), false, sessionToken);
		Assert.assertFalse(seasonID1.contains("error") ,  "create season with runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
		
		//add local    	       
		String response = translationsApi.addSupportedLocales(seasonID1,"de",sessionToken);
		Assert.assertTrue(response.equals(""),"could not add german to new season");

		//add branch
		String branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER, seasonID1);
		Assert.assertFalse(branchID.contains("error"), "fail creating branch: " + branchID );

		//add stream utility
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		String utilityIDProd = u.addUtility(seasonID1, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityIDProd.contains("error"), "fail creating stream utility: " + utilityIDProd );

		testAllRuntimeFilesEncryption(false, seasonID1, 0, 0, 0, 0, 1 );
		
		//create season with runtimeEncryption
		seasonJson.put("minVersion", "10");
		seasonID2 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), true, sessionToken);
		Assert.assertFalse(seasonID2.contains("error") , "fail to create season with runtimeEncryption when product includes RUNTIME_ENCRYPTION capability: " + seasonID2);
		testAllRuntimeFilesEncryption(true, seasonID2, 0, 0, 0, 0, 1);
		
		//create season without runtimeEncryption
		seasonJson.put("minVersion", "20");
		seasonID3 = s.addSeasonSpecifyEncryption(productID, seasonJson.toString(), false, sessionToken);
		Assert.assertFalse(seasonID3.contains("error") ,  "create season with runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
		testAllRuntimeFilesEncryption(false, seasonID3, 0, 0, 0, 0, 1 );	
	}

	@Test(dependsOnMethods="testCombinationOfEncryptionSeasons", description = "check runtime encryption after encryption flag updtae")
	public void testRuntimeEbcryptionAfterFlagUpdate() throws Exception {
		updateEncryptionFlag(seasonID1, productID, true);
		testAllRuntimeFilesEncryption(true, seasonID1, 0, 0, 0, 0, 1 );
		
		updateEncryptionFlag(seasonID2, productID, false);
		testAllRuntimeFilesEncryption(false, seasonID2, 0, 0, 0, 0, 1 );
		
		updateEncryptionFlag(seasonID3, productID, true);
		testAllRuntimeFilesEncryption(true, seasonID3, 0, 0, 0, 0, 1 );		
	}

	@Test(dependsOnMethods="testRuntimeEbcryptionAfterFlagUpdate", description = "test encryption after season deletion")
	public void testEncryptionAfterKeyReset() throws Exception {
		String response =  s.resetEncryptionKey(seasonID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "fail reset encryption key: " + response);
		testAllRuntimeFilesEncryption(true, seasonID1, 0, 0, 0, 0, 1 );
		
		response = s.resetEncryptionKey(seasonID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "fail reset encryption key: " + response);
		testAllRuntimeFilesEncryption(true, seasonID3, 0, 0, 0, 0, 1 );		
	}
	
	@Test(dependsOnMethods="testRuntimeEbcryptionAfterFlagUpdate", description = "test encryption after season deletion")
	public void testEncryptionAfterSeasonDeletion() throws Exception {
		int response = s.deleteSeason(seasonID1);
		Assert.assertTrue(response==200, "fail delete season");
		
		testAllRuntimeFilesEncryption(false, seasonID2, 0, 0, 0, 0, 1 );
		testAllRuntimeFilesEncryption(true, seasonID3, 0, 0, 0, 0, 1 );		
	}


	private void updateEncryptionFlag(String seasonID, String productID, boolean encryptRuntime) throws Exception {
		//try to update season to runtimeEncryption
		String seasonTmp = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(seasonTmp);		
		json.put("runtimeEncryption", encryptRuntime);
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") && response.contains("RUNTIME_ENCRYPTION"),  "update season to runtimeEncryption when product does not include RUNTIME_ENCRYPTION capability: " + seasonID1);
	}
	private boolean capabilityIncluded(JSONArray capabilitie, String capability) throws JSONException {
		for (int i=0; i<capabilitie.size(); i++) {
			if (capabilitie.getString(i).equals(capability)) {
				return true;
			}
		}
		return false;
	}
	private String setCapabilitiesInProduct(String productID, JSONArray capabilites, String sessionToken) throws JSONException{
		String product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.remove("seasons");
		json.put("capabilities", capabilites);
		return p.updateProduct(productID, json.toString(), sessionToken);
	}

	private JSONArray getCapabilitiesInProduct(String productID, String sessionToken) throws JSONException{
		String product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		return json.getJSONArray("capabilities");
	}

	private String setGlobalCapabilities(JSONArray capabilitesArr, String sessionToken) throws Exception{

		String capabilitiesStr = operApi.getCapabilities(sessionToken);

		if (capabilitiesStr.contains("error")){
			Assert.fail("Can't get capabilities " + capabilitiesStr);
		}

		JSONObject json = new JSONObject(capabilitiesStr);
		json.put("capabilities", capabilitesArr); 
		return operApi.setCapabilities(json.toString(), sessionToken);
	}

	private JSONArray getGlobalCapabilities(String sessionToken) throws Exception{
		String capabilities = operApi.getCapabilities(sessionToken);

		if (capabilities.contains("error")){
			Assert.fail("Can't get capabilities " + capabilities);
		}

		return new JSONObject(capabilities).getJSONArray("capabilities");
	}
	
	public static RestCallResults getUrlFileContent(String url, String date, String sessionToken) throws IOException{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		if (sessionToken != null)
			con.setRequestProperty ("sessionToken", sessionToken);
		
		return RestClientUtils.buildResult(con);
	}


	private RuntimeRestApi.DateModificationResults validateRuntimeEncryption(boolean encrypted, String fileName, String m_url, String productID, String seasonID, String dateFormat, String sessionToken) throws IOException, JSONException {
		String filePathUrl = RuntimeDateUtilities.buildPath(m_url, productID, seasonID, sessionToken)  + fileName;
		filePathUrl = RuntimeDateUtilities.cleanFileName(filePathUrl);
		RestClientUtils.RestCallResults res = getUrlFileContent(filePathUrl, dateFormat, sessionToken);
		if (res.code == 200) {
			try {
				String keyStr = "";
				try {
					keyStr = s.getEncryptionKeyString(seasonID, sessionToken);
				} catch (Exception e) {
					Assert.assertFalse(encrypted, "runtime file " + filePathUrl + " should be encrypted but isn't");

					//no encryption for season
					return new DateModificationResults (res.message, res.code);
				}

				Assert.assertTrue(encrypted, "runtime file " + filePathUrl + " should not be encrypted but is encrypted");

				//read file content as byte[]
				byte[] data = RuntimeRestApi.getFileContentAsByteArray(filePathUrl, sessionToken);

				//season runtime data is encrypted
				byte[] key = Encryption.fromString(keyStr);

				Encryption e = new Encryption(key);					
				byte[] decrypted = e.decrypt(data);
				String out = Compression.decompress(decrypted);

				return new DateModificationResults(out, res.code);
			} catch (GeneralSecurityException gse) {
				throw new IOException("GeneralSecurityException: " + gse.getMessage());
			} 
		} 
		else {
			return new DateModificationResults (res.message, res.code);
		}
	}
	
	public void testAllRuntimeFilesEncryption(boolean expectingEncryption, String seasonID, int expectedDevFeatures, int expectedProdFeatures, int expectedDevStrings, int expectedProdStrings, int expectedBranches) throws Exception {
		
	/*	//add branch
		String branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
		Assert.assertFalse(branchID.contains("error"), "fail creating branch: " + branchID );
*/
		String dateFormat = f.setDateFormat();
/*
		//add local    	       
		String response = translationsApi.addSupportedLocales(seasonID,"de",sessionToken);
		Assert.assertTrue(response.equals(""),"could not add german to new season");

		//add string
		String string1 = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		String stringID1 = str.addString(seasonID, string1, sessionToken);
		Assert.assertFalse(stringID1.contains("error"), "fail add string: " + response);

		//add stream utility
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		String utilityIDProd = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityIDProd.contains("error"), "fail creating stream utility: " + utilityIDProd );


		//add branch
		String branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
		Assert.assertFalse(branchID.contains("error"), "fail creating branch: " + utilityIDProd );

		//reset encryption key
		response = s.resetEncryptionKey(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "fail reset encryption key: " + response);
*/
		f.setSleep();
		//production changed file 
		RuntimeRestApi.DateModificationResults prodChanged = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.PRODUCTION_CHANGED, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file is not properly encrypted");

		RuntimeRestApi.DateModificationResults responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development utilities file is not properly encrypted");
		String utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");

		//features runtime files
		/*RuntimeRestApi.DateModificationResults*/ responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file is not properly encryptedd");
		RuntimeRestApi.DateModificationResults responseProd = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file is not properly encrypted");

		JSONArray featuresProd = new JSONObject(responseProd.message).getJSONObject("root").getJSONArray("features");
		JSONArray featuresDev = new JSONObject(responseDev.message).getJSONObject("root").getJSONArray("features");

		Assert.assertTrue(featuresProd.size() == expectedProdFeatures, "Incorrect number of features in runtime production file");
		Assert.assertTrue(featuresDev.size() == expectedDevFeatures, "Incorrect number of features in runtime development file");

		//translation files
		responseDev = validateRuntimeEncryption(expectingEncryption, "translations/" + "strings__enDEVELOPMENT.json", m_url, productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development  en strings file is not properly encrypted");
		JSONObject stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() == expectedDevStrings, "Runtime development en strings file not containing right num of strings");

		responseProd = validateRuntimeEncryption(expectingEncryption, "translations/" + "strings__enPRODUCTION.json", m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production en string file is not properly encrypted");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() == expectedProdStrings, "Runtime production en strings file not containing right num of strings");

		responseDev = validateRuntimeEncryption(expectingEncryption, "translations/strings__deDEVELOPMENT.json", m_url, productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development de strings file is not properly encrypted");
		stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() == expectedDevStrings, "Runtime development en strings file not containing right num of strings");

		responseProd = validateRuntimeEncryption(expectingEncryption, "translations/strings__dePRODUCTION.json", m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production de string file is not properly encrypted");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() == expectedProdStrings, "Runtime production en strings file not containing right num of strings");

		//calculate branch id
		String allBranches = br.getAllBranches(seasonID, sessionToken);
		JSONObject jsonBranch = new JSONObject(allBranches);
		Assert.assertTrue(jsonBranch.getJSONArray("branches").size()==expectedBranches+1, "wrong number of branches");
		String branchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");

		//branch runtime files
		responseDev = validateRuntimeEncryption(expectingEncryption, "branches/" + branchID + "/" + RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime branch development feature file is not properly encrypted");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==expectedDevFeatures, "wrong number of features in branch development runtime file");

		responseProd = validateRuntimeEncryption(expectingEncryption, "branches/" + branchID + "/" + RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime branch development feature file is not properly encrypted");
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==expectedProdFeatures, "wrong number of features in branch production runtime file");

		//utilities runtime
		
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development utilities file is not properly encrypted");
		//String utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");

		responseProd = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production utilities file is not properly encrypted");
		utlitiesList = responseProd.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");

		//streams runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_STREAM, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development streams file was not updated");		
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");

		responseProd = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCTION_STREAM, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production streams file was changed");
		json = new JSONObject(responseProd.message);		
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");

		//streams utilities runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_STREAM_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development stream utilities file was not created");
		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");

		responseProd = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCTION_STREAM_UTILITY, m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production stream utilities file was not created");
		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");

		//notification runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_AIRLOCK_NOTIFICATION, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development notifications file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime development file");

		responseProd = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCTION_AIRLOCK_NOTIFICATION, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production notifications file was not changed");
		json = new JSONObject(responseProd.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime production file");

		//branches runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.AIRLOCK_RUNTIME_BRANCHES_FILE_NAME, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime branches file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("branches").size(), 1, "Incorrect number of branches in runtime file");

		//user groups runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_INTERNSAL_USER_GROUPS, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime user groups file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("internalUserGroups").size(), 2, "Incorrect number of user groups in runtime file");

		//product runtime
		responseDev = validateRuntimeEncryption(expectingEncryption, RuntimeDateUtilities.RUNTIME_PRODUCT_FILE_NAME, m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime product file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertTrue(json.getString("uniqueId").equals(productID), "Incorrect product  id in runtime file");
	}



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
