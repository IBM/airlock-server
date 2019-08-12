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
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CreateNewSeasonFromOldSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String featureID;
	private String stringID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected UtilitiesRestApi u;
	protected TranslationsRestApi trans;
	protected String sessionToken = "";
	protected String m_url;
	protected String m_translationsUrl;
	protected String m_fromVersion;
	protected StringsRestApi t;

	
	@BeforeClass
	@Parameters({"url", "translationsUrl", "configPath", "productId",  "seasonId", "sessionToken", "version"})
	public void init(String url, String translationsUrl, String configPath, String productId, String seasonId, @Optional String sToken, String version){
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		productID = productId;
		seasonID = seasonId;
		m_fromVersion = version;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);

		if (sToken != null)
			sessionToken = sToken;

		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		trans = new TranslationsRestApi();
		trans.setURL(m_translationsUrl);
	}

	
	@Test (description = "Create new season")
	public void createNewSeason() throws Exception{
		//add feature to old season to check rolloutPercentageBitmap after creating new season
	/*	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		JSONObject jsonF = new JSONObject();
		jsonF.put("name", "a"+RandomStringUtils.randomAlphanumeric(5));
		featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not created" );
*/
		
		String season = "{\"minVersion\":\"8.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Season was not created" );

	}
	
	@Test (dependsOnMethods="createNewSeason", description = "New runtime files should be created")
	public void checkNewFormatRuntimeFilesInNewSeason() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
//		Assert.assertTrue(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, m_url, productID, seasonID2, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE + " not found in the new season");
//		Assert.assertTrue(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE, m_url, productID, seasonID2, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE + " not found in the new season");
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==304, "Runtime development feature file was not created");
		response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==304, "Runtime production feature file was not created");
		
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists("AirlockRuntime.json", m_url, productID, seasonID, sessionToken), "The old AirlockRuntime.json file found in the new  season");
		
		//Assert.assertTrue(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, m_url, productID, seasonID2, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY + " not found in the new season");
		//Assert.assertTrue(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY, m_url, productID, seasonID2, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY + " not found in the new season");
	}
	
	
	@Test (dependsOnMethods="checkNewFormatRuntimeFilesInNewSeason", description = "Check there are no files in the new format in the old season")
	public void checkNoNewFormatRuntimeFilesInOldSeason() throws JSONException, IOException{
		/*	Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, m_url, productID, seasonID, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE, m_url, productID, seasonID, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_FEATURE + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, m_url, productID, seasonID, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY + " found in the old season");
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists(RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY, m_url, productID, seasonID, sessionToken), "Tne new " + RuntimeDateUtilities.RUNTIME_PRODUCTION_UTILITY + " found in the old season");
	*/
	}
	
	@Test (dependsOnMethods="checkNoNewFormatRuntimeFilesInOldSeason", description = "Check percentage and bitmap")
	public void checkRolloutbitmapOldFeatureInNewSeason() throws JSONException, IOException, InterruptedException{
		
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		

		//bitmap should remain in old features in copied season, but should not change
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken) ;
		String featureInNewSeasonId = 	features.getJSONObject(0).getString("uniqueId");	
		String response = f.getFeature(featureInNewSeasonId, sessionToken);
		JSONObject originalJson = new JSONObject(response);
		Assert.assertTrue(originalJson.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not found in the old version season");

		JSONObject newJson = originalJson;
		newJson.put("rolloutPercentage", 34.5);
		response = f.updateFeature(seasonID2, featureInNewSeasonId, newJson.toString(), sessionToken);
		String feature = f.getFeature(featureInNewSeasonId, sessionToken);
		newJson = new JSONObject(feature);
		Assert.assertTrue(originalJson.getString("rolloutPercentageBitmap").equals(newJson.getString("rolloutPercentageBitmap")), "rolloutPercentageBitmap was changed in new season when percentage was updated");
/*
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev =  RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray featuresInRuntime = root.getJSONArray("features");
		Assert.assertTrue(featuresInRuntime.getJSONObject(0).containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap doesn't appear in the new season created from the old season");
*/
	}

	
	@Test (dependsOnMethods="checkRolloutbitmapOldFeatureInNewSeason", description = "Create feature in production in the new season")
	public void createFeatureInProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("name", "a"+RandomStringUtils.randomAlphanumeric(5));
		featureID = f.addFeature(seasonID2, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	}
	
	@Test (dependsOnMethods="createFeatureInProduction", description = "Update a feature after upgrade")
	public void updateFeature() throws Exception{

		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "new description");
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not updated" );
		
		Assert.assertFalse(RuntimeDateUtilities.ifFileExists("AirlockRuntime.json", m_url, productID, seasonID, sessionToken), "The old AirlockRuntime.json file found in the new  season");

	}
	
	@Test (dependsOnMethods = "updateFeature", description="Create a utility in production")
	public void createUtilityInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		
		UtilitiesRestApi u = new UtilitiesRestApi();
		u.setURL(m_url);
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.put("stage", "PRODUCTION");
		String utilityIDProd = u.addUtility(seasonID2, utilProps, sessionToken);
		Assert.assertFalse(utilityIDProd.contains("error"), "Utility was not created: " + utilityIDProd );
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development utility file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production utility file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	}
	
	@Test(dependsOnMethods = "createUtilityInProd", description = "Check utility and version in defaults file")	
	public void getDefaultFile() throws JSONException, IOException{
		String defaults = s.getDefaults(seasonID2, sessionToken);
		JSONObject json = new JSONObject(defaults);
		Assert.assertFalse(json.has("javascriptUtilities"), "The utilities are in the defaults file");
		
		Assert.assertTrue(json.containsKey("version"), "Version field is not found in defaults file ");
		String version = json.getString("version");
		Assert.assertTrue(version.equals("V2.5"), "Version field is incorrect");	
		
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getRuntimeFile (m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, productID, seasonID2, sessionToken);
		JSONObject body = new JSONObject(res.message);
		Assert.assertTrue(body.getString("version").equals("V2.5"), "Incorrect season version in the runtime file");

	}
	
	@Test(dependsOnMethods = "getDefaultFile", description = "Check version in defaults file in old season")	
	public void getDefaultFileOldSeason() throws JSONException, IOException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		Assert.assertTrue(json.containsKey("version"), "Version field is not found in defaults file ");
		String version = json.getString("version");
		Assert.assertTrue(version.equals(m_fromVersion), "Version field is incorrect");	
		
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getRuntimeFile (m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_FEATURE, productID, seasonID, sessionToken);
		JSONObject body = new JSONObject(res.message);
		Assert.assertTrue(body.getString("version").equals(m_fromVersion), "Incorrect season version in the runtime file");

	}
	
	@Test (dependsOnMethods="getDefaultFileOldSeason", description = "Create a string in new season")
	public void createString() throws Exception{
		
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		JSONObject js = new JSONObject(str);
		js.put("key", "app."+RandomStringUtils.randomAlphabetic(3));
		stringID = t.addString(seasonID2, js.toString(), sessionToken);
		Assert.assertFalse(stringID.contains("error"), "String was not created" );
		
		JSONObject allFrTransStrings = getAllLocalString(trans,"fr", "DEVELOPMENT"); 
		str = t.getString(stringID, sessionToken);
		JSONObject jsonStr = new JSONObject(str);

		Assert.assertTrue(allFrTransStrings.has(jsonStr.getString("key")), "The string key was not found in translations");
		//Assert.assertTrue(allFrTransStrings.getString(jsonStr.getString("key")).equals("SALUT"), "The string value was not found in translations");

		Assert.assertFalse(RuntimeDateUtilities.ifFileExists("translations/strings__en.json", m_url, productID, seasonID2, sessionToken), "The old strings__en.json file found in the new season");

		
		

	}
	
	@Test (dependsOnMethods="createString", description = "Update translation and check if succeed")
	public void translationUpdate() throws Exception {
		String expectedTranslation = FileUtils.fileToString(filePath + "strings/translationExpected.txt", "UTF-8", false);
		expectedTranslation = expectedTranslation.replace("Hello [[[1]]]","Bonjour [[[1]]]");

		//Add french translation
		//TODO: check for each translation - has translation status translated
		
		if (m_fromVersion.equals("V2.1")){
			ArrayList<String> idsArray = new ArrayList<String>();
			//get all strings for season

			String allStrings = t.getAllStrings(seasonID2, "BASIC", sessionToken);
			JSONObject allStringsJson = new JSONObject(allStrings);
			for (int i=0; i<allStringsJson.getJSONArray("strings").size(); i++){
				if (allStringsJson.getJSONArray("strings").getJSONObject(i).getString("status").equals("NEW_STRING"))
					idsArray.add(allStringsJson.getJSONArray("strings").getJSONObject(i).getString("uniqueId"));
			}
			String response = trans.markForTranslation(seasonID2,idsArray.toArray(new String[idsArray.size()]),sessionToken);
			Assert.assertFalse(response.contains("error"));
			response = trans.sendToTranslation(seasonID2, idsArray.toArray(new String[idsArray.size()]), sessionToken);			
			Assert.assertFalse(response.contains("error"));
			
			String translationMessage = trans.updateTranslation(seasonID2,"fr",expectedTranslation,sessionToken);
			Assert.assertFalse(translationMessage.contains("error"));
		}
		
		
		//stage not allowed for old seasons
		if (m_fromVersion.equals("V2")){
			String translationMessage = trans.updateTranslation(seasonID2,"fr",expectedTranslation,sessionToken);
			Assert.assertTrue(translationMessage.equals(""));
			
			String frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
			frTranslation = frTranslation.replace("Bonjour","SALUT");		
			translationMessage = trans.updateTranslation(seasonID2,"fr",frTranslation,sessionToken);
			Assert.assertTrue(translationMessage.equals(""));

			
			String response = trans.getTranslation(seasonID2, "fr", "", sessionToken);
			Assert.assertTrue(response.contains("error"), "Stage parameter required in new seasons" + response);
		}
		
	}
	
	
	@Test (dependsOnMethods="translationUpdate", description = "Check that all strings and translations actions work in new season")
	public void createStringsInNewSeason() throws Exception{

		
		String response = t.getAllStrings(seasonID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "getAllStrings did not pass in the new season" );
		
		response = trans.stringForTranslation(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getStringsForTranslation did not pass in the new season" );


		String frTranslation = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
		//response = trans.addTranslation(seasonID2, "nl", frTranslation, sessionToken);
		response = trans.overrideTranslate(stringID, "nl", frTranslation, sessionToken);
		Assert.assertFalse(response.contains("error"), "overrideTranslate did not pass in the new season" );
		
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertTrue(responseCode==200, "The string was not deleted in the new season" );

	}
	
	
	@Test (dependsOnMethods="createStringsInNewSeason", description = "Get supported languages in new season")
	public void getSupportedLanguages() throws Exception{

		String response = t.getSupportedLocales(seasonID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "getSupportedLocales was not returned" );
		JSONObject newLangs = new JSONObject(response);
		JSONArray  supportedLocales = newLangs.getJSONArray("supportedLanguages");
		
		for (int i=0; i< supportedLocales.size(); i++){
			String transResponse = trans.getTranslation(seasonID2, supportedLocales.getString(i), "DEVELOPMENT", sessionToken);
			Assert.assertFalse(transResponse.contains("error"), "getTranslation for locale " +  supportedLocales.getString(i) + " was not found");
		}
		
	}
	
	private JSONObject getAllLocalString(TranslationsRestApi trans, String local, String stage) {
		try {
			String allTranslations = trans.getTranslation(seasonID2, local, stage, sessionToken);
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

}
