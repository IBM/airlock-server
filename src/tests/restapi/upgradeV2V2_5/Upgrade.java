package tests.restapi.upgradeV2V2_5;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.*;

public class Upgrade {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected ProductsRestApi p;
	protected TranslationsRestApi trans;
	protected UtilitiesRestApi u;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private AirlocklNotificationRestApi notificationApi;
	private StreamsRestApi streamsApi;

	
	protected String sessionToken = "";
	protected String m_url;
	protected String m_Version;
	protected StringsRestApi t;
	private String supportedLangsBeforeUpgrade;
	private String stringID;
	protected String m_translationsUrl;
	protected String featureID;
	private String utilityID;
	private JSONObject body = new JSONObject();
	private String m_testUrl;
	private String productVersion;
	private String adminToken = "";
	private Boolean follow = false;
	private String m_appName = "backend_dev";
	private String to_Version;
	private String adminUser;

	
	@BeforeClass
	@Parameters({"url", "testUrl", "operationsUrl", "translationsUrl", "analyticsUrl", "configPath", "productId",  "seasonId", "sessionToken", "fromVersion", "toVersion", "path", "productName", "minVersion", "productsToDeleteFile", "notify","appName", "admin", "adminPass", "role", "rolePass", "serverVersion"})
	public void init(String url, String testUrl, String operationsUrl, String translationsUrl, String analyticsUrl, String configPath, String productId, String seasonId, @Optional String sToken, String fromVersion, String toVersion, String path, String productName, String minVersion, String productsToDelete, @Optional String notify, @Optional String appName, @Optional String admin, @Optional String adminPass, @Optional String role, @Optional String rolePass, @Optional String serverVersion) throws JSONException, IOException{
		m_url = url;
		m_testUrl = testUrl;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		productID = productId;
		seasonID = seasonId;
		m_Version = fromVersion;	//from version
		to_Version = toVersion;	//to version
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		notificationApi = new AirlocklNotificationRestApi();
		notificationApi.setUrl(url);
		streamsApi = new StreamsRestApi();
		streamsApi.setURL(m_url);
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);


		if(appName != null){
			m_appName = appName;
		}
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, role, rolePass, appName, productsToDelete);
		sessionToken = baseUtils.sessionToken;

		if (notify != null && notify.equals("true")){
			follow = true;

		    
		}
			
		s = new SeasonsRestApi();
		s.setURL(m_url);
		trans = new TranslationsRestApi();
		trans.setURL(m_translationsUrl);
		
		adminToken = baseUtils.getJWTToken(admin,adminPass,m_appName);
	    sessionToken = baseUtils.getJWTToken(role,rolePass,m_appName);
	    
	    productID = productId;
	    seasonID = seasonId;

	
		body.put("path", path + productId + "/" + seasonId);
		body.put("productName", productName);
		body.put("productId", productId);
		body.put("seasonId", seasonId);
		body.put("minVersion", minVersion);
		body.put("serverVersion", serverVersion);
		
		adminUser = admin;

	}
	
	@Test (description="Copy old product to test environment")
	public void copyProduct() throws Exception{
		String response  = p.getProduct(productID, sessionToken);
		if (response.contains("Product not found")){
			RestClientUtils.RestCallResults resCopy = RestClientUtils.sendPost(m_testUrl, body.toString(), adminToken);
			Assert.assertTrue(resCopy.code==200, "Product was not copied");
		}
	}
	
	@Test (/*dependsOnMethods="copyProduct"*/)
	public void test() throws Exception{
		if (to_Version.equals("V2")){
			//before upgrade 2.0
			getServerVersion("V2");
			createStringsActions();
			checkStringsStatuses();
			checkSupportedLangsBeforeUpgrade();
			checkRolloutbitmapBeforeUpgrade();
			checkNoAnalytics();
			createFeature();
			if (follow){
				followProduct();
				followFeature();
			}	
			
			//after upgrade 2.0 to 2.1
			upgradeSeason("V2.1");
			checkVersion(m_Version);
			checkVersionInRuntime2_0(m_Version);
			getDefaultFile(true);
			createStringAfterUpgrade();
			getSupportedLanguages();
			translationUpdate(true);			
			checkTranslationsActionsV2_5();
			checkAllSupportedLanguages();
			checkRolloutbitmapAfterUpgrade();
			createFeature();
			updateFeature();
			if (follow){
				followProduct();
				followFeature();
			}	
			createConfiguration();
			addUtility();
			validateUtility();
			checkNewFormatRuntimeFiles();
			followProduct();
			followFeature();
		} else if (to_Version.equals("V2.1")){
			getServerVersion("2.1");
			checkStringsStatuses();
			checkNoAnalytics();
			checkVersion(m_Version);
			checkVersionInRuntime(m_Version);
			getDefaultFile(false);
			createStringAfterUpgrade();
			translationUpdate(false);
			checkAllSupportedLanguages();			
			createFeature();
			updateFeature();
			checkRolloutbitmapAfterUpgrade();
			createConfiguration();
			addUtility();
			//validateUtility();
			if (follow){
				followProduct();
				followFeature();
			}
		} else if (to_Version.equals("V2.5")){
			getServerVersion("2.5");
			checkStringsStatuses();
			checkVersion(m_Version);
			checkVersionInRuntime(m_Version);
			getDefaultFile(false);
			createStringAfterUpgrade();			
			checkTranslationsActionsV2_5();
			translationUpdate(false);
			checkAllSupportedLanguages();			
			createFeature();
			updateFeature();
			checkRolloutbitmapAfterUpgrade();
			createConfiguration();
			addUtility();
			//validateUtility();
			if (follow){
				followProduct();
				followFeature();
			}
		} else if (to_Version.equals("V4.0")){


			//when product is 3.0 check that new api is not available:
			createStream(true);
			//add experiment before upgrade
			createExperimentAndVariant();
			//to fix default file paths make some action
			featureID = createFeature();
			//upgrade
			upgradeSeason(m_Version);
			getServerVersion("4.0");
			checkVersion("V2.5");
			checkVersionInRuntime("V2.5");
			checkServerVersionInRuntime(to_Version);
			validateStreamFiles();
			createStream(false);
			updateFeature();
			createConfiguration();
			addUtility();
			String branchID = createBranch();
			checkoutFeature(branchID, featureID);
			createFeatureInBranch(branchID);
			checkOriginalsStringsFile();
	
			
		}else if (to_Version.equals("V4.1")){
				//when product is 2.5 check that new api is not available:
				//checkNoBranchCreation();
				//checkNoExperimentCreation();
				//upgrade
				upgradeSeason(m_Version);
				getServerVersion("4.1");
				checkVersion("V2.5");
				checkVersionInRuntime(m_Version);
				checkServerVersionInRuntime(to_Version);
				String featureID = createFeature();
				updateFeature();
				createConfiguration();
				addUtility();
				createExperimentAndVariant();
				String branchID = createBranch();
				checkoutFeature(branchID, featureID);
				createFeatureInBranch(branchID);
				checkOriginalsStringsFile();
		}else if (to_Version.equals("V4.5")){
			//when product is before 4.5 check that new api is not available:
			getCapabilities(false);
			createNotification(true);
			String featureID = createFeature();
			createOrderingRule(featureID, true);
			getSeasonCapabilities(true, false);
			
			//find existing utility in season
			String utility = u.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT");
			JSONObject utilObj = new JSONObject(utility);			
			String oldUtilId = utilObj.getJSONArray("utilities").getJSONObject(0).getString("uniqueId");
			checkUtilityNameBeforeUpgrade(oldUtilId, true);
			
			//upgrade
		
			upgradeSeason(m_Version);			//upgrade from version				
			getServerVersion("4.5");
			checkVersion("V2.5");
			checkVersionInRuntime("V2.5");
						
			//TODO: check new structure
			notificationInRuntime();
			createNotification(false);
			getAllNotifications();
			
			createOrderingRule(featureID, false);
			getSeasonCapabilities(true, true);
			
			//old utility must get a new name
			checkUtilityNameAfterUpgrade(oldUtilId, true);

			addUtilityV45();
			checkUtilityNameAfterUpgrade(utilityID, true);
			
			featureID = createFeature();
			updateFeature();
			createConfiguration();
			
			String branchID = createBranch();
			checkoutFeature(branchID, featureID);
			createFeatureInBranch(branchID);
			
			checkServerVersionInRuntime(to_Version);

		} else {
			Assert.fail("Unknown product version: " + productVersion);
		}
	}
	
	//TODO in 4.5
	/*
	 * AirlockNotification - functionality & runtime files
	 * Folders structure private/public
	 */
	
	public void createStringsActions() throws Exception{
		//description = "Check that all strings and translations actions before upgrade are forbidden"
		//only 2.0, works in 2.1
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false); 
		String response = t.addString(seasonID, str, sessionToken);
		Assert.assertTrue(response.contains("error"), "String was not created" );
		
		response = t.getAllStrings(seasonID, sessionToken);
		Assert.assertTrue(response.contains("error"), "getAllStrings passed in the old season" );
		
		response = trans.stringForTranslation(seasonID, sessionToken);
		Assert.assertTrue(response.contains("error"), "getStringsForTranslation passed in the old season" );
		
		response = trans.getTranslation(seasonID, "en", sessionToken);
		Assert.assertTrue(response.contains("error"), "getTranslation for locale passed in the old season" );
		
		response = t.getSupportedLocales(seasonID, sessionToken);
		Assert.assertTrue(response.contains("error"), "getSupportedLocales passed in the old season" );
		
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
		response = trans.addTranslation(seasonID, "it", frTranslation, sessionToken);
		Assert.assertTrue(response.contains("error"), "addTranslation passed in the old season" );
		
		response = trans.updateTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(response.contains("error"), "addTranslation passed in the old season" );
		
	}
	
	public void checkStringsStatuses() throws Exception{
		//add new functions 2.0 + 2.1
		String[] idsArray = new String[]{};
		String response = trans.getNewStringsForTranslation(seasonID, idsArray, sessionToken);
		Assert.assertTrue(response.contains("Only seasons later") || response.contains("must be upgraded"), "getNewStringsForTranslation passed in the old season" );
		
		response = trans.getStringStatuses(seasonID, sessionToken);
		Assert.assertTrue(response.contains("Only seasons later") || response.contains("must be upgraded"), "getStringStatuses passed in the old season" );
		
		response = trans.getTranslationSummary(seasonID, idsArray, sessionToken);
		Assert.assertTrue(response.contains("Only seasons later") || response.contains("must be upgraded"), "getTranslationSummary passed in the old season" );

	}
	
	public void checkSupportedLangsBeforeUpgrade() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		Assert.assertTrue(json.containsKey("supportedLanguages"), "supportedLanguages field is not found in defaults file ");
		supportedLangsBeforeUpgrade = defaults;	
		//only 2.0
	}
	
	public void followProduct(){
		String response = p.followProduct(productID, sessionToken);
        Assert.assertTrue(response.equals(""), "failed to follow");
        int responseCode = p.unfollowProduct(productID, sessionToken);
        Assert.assertTrue(responseCode==200, "failed to unfollow");
	}
	
	public void followFeature() {
		String response = f.followFeature(featureID, sessionToken);
        Assert.assertTrue(response.equals(""), "failed to follow feature");
        int responseCode = f.unfollowFeature(featureID, sessionToken);
        Assert.assertTrue(responseCode==200, "failed to unfollow feature");
	}
	
	public void checkRolloutbitmapBeforeUpgrade() throws JSONException, IOException, InterruptedException{
		//old version season works with rolloutPercentageBitmap and rolloutPercentage of type int
		//2.0+2.1
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false); 
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not created" );
		
		String response = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not found in the old version season");
		
		json.put("rolloutPercentage", 55.5);
		response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added rolloutPercentage as double");
	}
	
	
	public void checkNoAnalytics() throws JSONException, IOException{
		//old version season cannot add items to analytics
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(anResponse.contains("Only seasons later"), "Analytics supported in the old season");
		//check in runtime that there are no fields for analytics
	}
	
	
	public void upgradeSeason(String version) throws Exception{

		String response = s.upgradeSeason(seasonID, version, sessionToken);
		Assert.assertFalse(response.contains("error"), "Season was not upgraded");

	}
	
	
	
	public void checkVersion(String expectedVersion) throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		Assert.assertTrue(json.containsKey("version"), "Version field is not found in defaults file ");
		String version = json.getString("version");
		Assert.assertTrue(version.equals(expectedVersion), "Version field is incorrect");	
	}
	
	public void checkVersionInRuntime2_0(String expectedVersion) throws JSONException, IOException{
		
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getRuntimeFile (m_url, RuntimeDateUtilities.RUNTIME_OLD_SEASON, productID, seasonID, sessionToken);
		JSONObject body = new JSONObject(res.message);
		Assert.assertTrue(body.getString("version").equals(expectedVersion), "Incorrect season version in the runtime file");
	}
	
	public void checkVersionInRuntime(String expectedVersion) throws JSONException, IOException{
		
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getRuntimeFile (m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, productID, seasonID, sessionToken);
		JSONObject body = new JSONObject(res.message);
		Assert.assertTrue(body.getString("version").equals(expectedVersion), "Incorrect season version in the runtime file");
	}
	
	public void checkServerVersionInRuntime(String expectedVersion) throws JSONException, IOException{
		
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getRuntimeFile (m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, productID, seasonID, sessionToken);
		JSONObject body = new JSONObject(res.message);
		Assert.assertTrue(body.getString("serverVersion").equals(expectedVersion), "Incorrect server version in the runtime file");
	}
	
	 //In V3 in the old seasons the javascript utilities are still in defaults file
	
	public void getDefaultFile(boolean expectedFailure) throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		
		Assert.assertEquals(json.containsKey("javascriptUtilities"), expectedFailure, "javascriptUtilities found in default file in version " + m_Version);
		//String javascriptUtilities = json.getString("javascriptUtilities");
		//Assert.assertEquals(javascriptUtilities.contains("calcDistance"), expectedFailure);
	}
	

	
	
	public void createStringAfterUpgrade() throws Exception{

		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false); 
		stringID = t.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID.contains("error"), "String was not created" );
		
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		Assert.assertFalse(jsonStr.containsKey("status"), "status found in string in old season");
		Assert.assertFalse(jsonStr.containsKey("lastSourceModification"), "lastSourceModification found in string in old season");
		Assert.assertFalse(jsonStr.containsKey("translatorId"), "translatorId found in string in old season");
	
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertTrue(responseCode==200, "The string was not deleted from the old season" );

	}

	
	public void checkTranslationsActionsV2_5() throws Exception{
		String[] idsArray = new String[]{stringID};
		String response = trans.markForTranslation(seasonID, idsArray, sessionToken);
		Assert.assertTrue(response.contains("Only seasons later"), "markForTranslation worked in old season" + response );
		
		response = trans.sendToTranslation(seasonID, idsArray, sessionToken);
		Assert.assertTrue(response.contains("Only seasons later"), "sendToTranslation worked in old season" + response );
		
		String str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false); 
		stringID = t.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID.contains("error"), "String was not created" );

		response = trans.overrideTranslate(stringID, "fr", "bonjour", sessionToken);
		Assert.assertTrue(response.contains("Only seasons later"), "overrideTranslate worked in old season" + response );
		
	}
	
	
	@SuppressWarnings("unchecked")
	
	public void getSupportedLanguages() throws Exception{

		String response = t.getSupportedLocales(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getSupportedLocales was not returned" );
		JSONObject oldLangs = new JSONObject(supportedLangsBeforeUpgrade);
		JSONObject newLangs = new JSONObject(response);
		ArrayList<String>  oldLangsBefore = (ArrayList<String> )oldLangs.get("supportedLanguages");
		ArrayList<String>  newLangsAfter = (ArrayList<String> )newLangs.get("supportedLanguages");
		Assert.assertEqualsNoOrder(oldLangsBefore.toArray(), newLangsAfter.toArray(), "Incorrect list of supported languages");

	}
	
	
	public void translationUpdate(boolean expectedFailure) throws Exception {
		//Add french translation

		String expectedTranslation = FileUtils.fileToString(filePath + "strings/translationExpected.txt", "UTF-8", false);
		expectedTranslation = expectedTranslation.replace("Hello [[[1]]]","Bonjour [[[1]]]");
		String translationMessage = trans.updateTranslation(seasonID,"fr",expectedTranslation,sessionToken) ;
		Assert.assertTrue(translationMessage.equals(""));

		//Update french translation
		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
		frTranslation = frTranslation.replace("Bonjour","SALUT");		
		translationMessage = trans.updateTranslation(seasonID,"fr",frTranslation,sessionToken);
		Assert.assertTrue(translationMessage.equals(""));

		//stage not allowed for old seasons
		String response = trans.getTranslation(seasonID, "fr", "DEVELOPMENT", sessionToken);
		Assert.assertEquals(response.contains("error"), expectedFailure, "Stage parameter is not allowed in pre 2.1 versions");
		
		
		frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
		response = trans.addTranslation(seasonID, "newlocale", frTranslation, sessionToken);
		Assert.assertFalse(response.contains("error"), "addTranslation didn't work in upgraded season" );

		JSONObject allFrTransStrings = getAllLocalString(trans,"fr"); 
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);
		Assert.assertTrue(allFrTransStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
		//Assert.assertTrue(allFrTransStrings.getString(jsonStr.getString("key")).equals("SALUT"), "The string value was not found in translations");

		response = t.getAllStrings(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getAllStrings didn't work in upgraded season" );
		
	}
	
	public void checkAllSupportedLanguages() throws Exception{

		String response = t.getSupportedLocales(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getSupportedLocales was not returned" );
		JSONObject newLangs = new JSONObject(response);
		JSONArray  supportedLocales = newLangs.getJSONArray("supportedLanguages");
		
		String transResponse = "";
		for (int i=0; i< supportedLocales.size(); i++){
			if (m_Version.equals("V2"))
				transResponse = trans.getTranslation(seasonID, supportedLocales.getString(i), sessionToken);
			else if (m_Version.equals("V2.1"))
				transResponse = trans.getTranslation(seasonID, supportedLocales.getString(i), "DEVELOPMENT", sessionToken);
			
			Assert.assertFalse(transResponse.contains("error"), "getTranslation for locale " +  supportedLocales.getString(i) + " was not found");
		}
		
	}
	
	
	public void checkRolloutbitmapAfterUpgrade() throws Exception{
		//only 2.0
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		

		String response = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was found in the upgraded season");

		RuntimeRestApi.DateModificationResults responseDev = new RuntimeRestApi.DateModificationResults("init", 404); 
		Thread.sleep(3000);
		if (m_Version.equals("V2"))
			responseDev = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_OLD_SEASON , m_url, productID, seasonID, sessionToken);
														
		else if (m_Version.equals("V2.1"))
			responseDev = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE , m_url, productID, seasonID, sessionToken);
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap doesn't appear in the old season after upgrade");
		
	}
	
	
	public String createFeature() throws Exception{

		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false); 
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not created" );
		return featureID;
	}
	
	
	public void updateFeature() throws Exception{

		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not updated" );

	}
	
	public void createConfiguration() throws Exception{

		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false); 
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String configID = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not created" );

	}
	
	
	public void addUtility() throws Exception{

		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isTrue(){return true;}");
		utilProps.setProperty("minAppVersion", "1.0");
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Utility was not added" + utilityID);

	}
	
	public void addUtilityV45() throws Exception{

		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isTrue(){return true;}");
		utilProps.setProperty("minAppVersion", "1.0");
		utilProps.setProperty("name", "newUtility1");
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Utility was not added" + utilityID);

	}

	
	public void validateUtility() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		String javascriptUtilities = json.getString("javascriptUtilities");
		Assert.assertTrue(javascriptUtilities.contains("isTrue"), "The utility is not in the defaults file");
	}
	
	
	public void updateUtility() throws Exception{

		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue(){return \"updated utility\";}");
		uJson.put("stage", "PRODUCTION");
		uJson.put("minAppVersion", "1.0");
		String response  =u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	public void checkUtilityNameAfterUpgrade(String utilityId, boolean expectedResult) throws Exception{

		String utility = u.getUtility(utilityId, sessionToken);
		JSONObject utilityObj = new JSONObject(utility);
		Assert.assertEquals(utilityObj.containsKey("name") && !utilityObj.isNull("name") && !utilityObj.getString("name").equals(""), expectedResult, "utility name not found after upgrade");
	}
	
	public void checkUtilityNameBeforeUpgrade(String utilityId, boolean expectedResult) throws Exception{

		String utility = u.getUtility(utilityId, sessionToken);
		JSONObject utilityObj = new JSONObject(utility);
		Assert.assertEquals(utilityObj.containsKey("name") && utilityObj.isNull("name"), expectedResult, "utility name not found before upgrade");
	}
	
	
	public void checkNewFormatRuntimeFiles() throws JSONException, IOException{
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, m_url, productID, seasonID, sessionToken), "The new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE, m_url, productID, seasonID, sessionToken), "The new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, m_url, productID, seasonID, sessionToken), "The new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY, m_url, productID, seasonID, sessionToken), "The new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists("translations/strings__enDEVELOPMENT.json", m_url, productID, seasonID, sessionToken), "The new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY + " found in the old season");

	}
	
	public void getServerVersion(String expectedVersion) throws Exception{
		String response = s.getServerVersion(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.getString("serverVersion").equals(expectedVersion), "Incorrect server version: " + json.getString("serverVersion") );
	}
	
	
	private JSONObject getAllLocalString(TranslationsRestApi trans, String local) {
		String allTranslations="";
		try {
			if (m_Version.equals("V2"))
				allTranslations = trans.getTranslation(seasonID, local, sessionToken);
			else if (m_Version.equals("V2.1"))
				allTranslations = trans.getTranslation(seasonID, local, "DEVELOPMENT", sessionToken);
			JSONObject jsonTrans = new JSONObject(allTranslations);
			if(jsonTrans.has("strings")){
				JSONObject allTransStrings = jsonTrans.getJSONObject("strings");
				return allTransStrings;
			}
			else return jsonTrans;
		} catch (Exception e) {
			Assert.fail("failed to get translastion");
		}
		return null;
	}
	
	public void checkNoBranchCreation() throws JSONException, IOException{
		//old version season cannot add branch
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String anResponse = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(anResponse.contains("error"), "Branch creation supported in the old season");
	}
	
	public void checkNoExperimentCreation() throws JSONException, IOException{
		//old version season cannot add experiment
		JSONObject experiment = new JSONObject( FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false));
		experiment.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		String experimentID = exp.createExperiment(productID, experiment.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment creation supported in the old season");
	}
	

	
	public void createExperimentAndVariant() throws JSONException, IOException{
		//old version season cannot add experiment
		JSONObject experiment = new JSONObject( FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false));
		experiment.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		experiment.put("enabled", false);
		String experimentID = exp.createExperiment(productID, experiment.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create experiment after upgrade: " + experimentID);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		String variantID = exp.createVariant(experimentID, variant, sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create variant after upgrade: " + variantID);
	}
	
	
	public String createBranch() throws JSONException, IOException{
		//old version season cannot add branch
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject brJson = new JSONObject(branch) ;
		brJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String branchID = br.createBranch(seasonID, brJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Can't create branch after upgrade");
		return branchID;
	}
	
	public void checkoutFeature(String branchID, String featureID) throws IOException{
		
		String response = br.checkoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout feature: " + response);

	}
	
	public void createFeatureInBranch(String branchID) throws IOException{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the branch" + featureID2);

	}
	
	public void checkOriginalsStringsFile() throws IOException, JSONException{
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getOriginalStringsFile(m_url, productID, seasonID, sessionToken) ;
		JSONObject json = new JSONObject(responseDev.message);
		JSONArray strings = json.getJSONArray("strings");
		for (int i=0; i< strings.size(); i++){
			JSONObject str = strings.getJSONObject(i);
			Assert.assertFalse(str.containsKey("smartlingId"), "smartlingId was found in string: " + str.getString("key")); 
		}
	}
	
	public void createNotification(boolean expectedResult) throws IOException{
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		String notificationID = notificationApi.createNotification(seasonID, notification, sessionToken);
		Assert.assertEquals(notificationID.contains("error"), expectedResult, "Create notification test failed: " + notificationID);
	}
	
	public void getAllNotifications(){
		String response = notificationApi.getAllNotifications(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get notifications after upgrade: " + response);
		
	}
	
	public void notificationInRuntime() throws IOException, JSONException{
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_AIRLOCK_NOTIFICATION, m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(responseDev.message.contains("error") || responseDev.message.contains("denied"), "Can't get RUNTIME_DEVELOPMENT_AIRLOCK_NOTIFICATION");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_PRODUCTION_AIRLOCK_NOTIFICATION, m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(responseProd.message.contains("error") || responseProd.message.contains("denied"), "Can't get RUNTIME_PRODUCTION_AIRLOCK_NOTIFICATION");

		RuntimeRestApi.DateModificationResults responseNotifGeneral = RuntimeDateUtilities.getRuntimeFileContent("AirlockNotifications.json", m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(responseNotifGeneral.message.contains("error") || responseNotifGeneral.message.contains("denied"), "Can't get AirlockNotifications.json");

	}
	
	public void createStream(boolean expectedFailure){
		try {
			String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(stream) ;
			json.put("name", RandomStringUtils.randomAlphabetic(6));
			String streamID = streamsApi.createStream(seasonID, json.toString(), sessionToken);
			Assert.assertEquals(streamID.contains("error"), expectedFailure, "createStream failed: "+ streamID);

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void validateStreamFiles() throws IOException, JSONException{
		//runtime development stream file
		RuntimeRestApi.DateModificationResults runtimeFile = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_STREAM, m_url, productID, seasonID, sessionToken);		
		JSONObject json = new JSONObject(runtimeFile.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Stream in runtime development file is not empty");

		//runtime production stream file
		runtimeFile = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_PRODUCTION_STREAM, m_url, productID, seasonID, sessionToken);		
		json = new JSONObject(runtimeFile.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Stream in runtime production file is not empty");
		
		//runtime stream file
		runtimeFile = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_STREAM, m_url, productID, seasonID, sessionToken);		
		json = new JSONObject(runtimeFile.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Stream in runtime production file is not empty");
		
		//runtime development stream utilities file
		runtimeFile = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_STREAM_UTILITY, m_url, productID, seasonID, sessionToken);		
		Assert.assertTrue(runtimeFile.message.equals(""), "Stream utilites in runtime development file is not empty");

		//runtime production stream  utilities file
		runtimeFile = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_PRODUCTION_STREAM_UTILITY, m_url, productID, seasonID, sessionToken);		
		Assert.assertTrue(runtimeFile.message.equals(""), "Stream utilities in runtime production file is not empty");
	


	}
	
	public void createString() throws Exception{
		String str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		JSONObject jsonStr = new JSONObject(str);
		jsonStr.put("key", RandomStringUtils.randomAlphabetic(3));
		String stringID = t.addString(seasonID, jsonStr.toString(), sessionToken);
		Assert.assertFalse(stringID.contains("error"), "Can't add string: " + stringID);

	}
	
	private void getCapabilities(boolean expectedFailure){
		try {
			String response = operApi.getCapabilities(sessionToken);
			Assert.assertEquals(response.contains("error"), expectedFailure, "getCapabilities exist on server before upgrade: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	private void getSeasonCapabilities(boolean expectedFailureCapabilities, boolean expectedFailureNotificationCapability) throws Exception{
		
		String response = operApi.getSeasonCapabilities(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		Assert.assertEquals(json.containsKey("capabilities"), expectedFailureCapabilities, "no capabilites in product");
		Assert.assertEquals(response.contains("NOTIFICATIONS"), expectedFailureNotificationCapability, "notification capability found  in old season");
	}

	
	private void createOrderingRule(String parentId, boolean expectedFailure) throws JSONException, IOException{
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), parentId, sessionToken);
		Assert.assertEquals(response.contains("error"), expectedFailure, "createOrderingRule exist on server before upgrade: "+ response);

	}



}			

