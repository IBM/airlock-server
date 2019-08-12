package tests.restapi.scenarios.encryption;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;


import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class ResetEncryptionKey
{
    protected String seasonID;
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
	
    private String sessionToken = "";
    protected AirlockUtils baseUtils;
    protected String m_url;
    

    @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
    		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
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
		seasonID = baseUtils.createSeason(productID);	
    	}

    @Test(description = "Get and reset encryption key")
    public void getAndResetEncryptionKey() throws Exception {
    		//get encryption key
    		String response = s.getEncryptionKey(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);
		String  origKey = json.getString("encryptionKey");
		Assert.assertTrue(origKey.length()==16, "Incorrect encryption key length " + origKey);
		
		//reset encryption key
		response = s.resetEncryptionKey(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "fail reset encryption key: " + response);
		
		json = new JSONObject(response);
		String newKey = json.getString("encryptionKey");
		Assert.assertTrue(newKey.length()==16, "Incorrect encryption key length " + origKey);
		Assert.assertTrue(!newKey.equals(origKey), "encryption key was  not reseted");
		
		//get encryption key after reset
		response = s.getEncryptionKey(seasonID, sessionToken);
		json = new JSONObject(response);
		String  getNewKey = json.getString("encryptionKey");
		Assert.assertTrue(getNewKey.equals(newKey), "Incorrect encryption key after reset");
    }
    
    @Test(dependsOnMethods="getAndResetEncryptionKey", description = "add feature and check it in the runtime files")
    public void testFeatureInRuntimeFile() throws IOException, JSONException, InterruptedException {
    		String dateFormat = f.setDateFormat();
    		String feature = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
		JSONObject featureObj = new JSONObject(feature);
		featureObj.put("stage", "PRODUCTION");
		String featureId = f.addFeature(seasonID, featureObj.toString(), "ROOT", sessionToken);
		
		Assert.assertFalse(featureId.contains("error"), "feature was not created: " + featureId);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		
		JSONObject featureProd = new JSONObject(responseProd.message).getJSONObject("root").getJSONArray("features").getJSONObject(0);
		JSONObject featureDev = new JSONObject(responseDev.message).getJSONObject("root").getJSONArray("features").getJSONObject(0);
		
		Assert.assertTrue(featureProd.get("uniqueId").equals(featureId), "feature id is correct in prod runtime");
		Assert.assertTrue(featureDev.get("uniqueId").equals(featureId), "feature id is correct in dev runtime");
    }
    
    @Test(dependsOnMethods="testFeatureInRuntimeFile", description = "reset encryption key and validate that all runtime files are encrypted with the new key")
    public void testRuntimeFilesAfterReset() throws Exception {
    		String dateFormat = f.setDateFormat();
		
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
		Assert.assertFalse(branchID.contains("error"), "fail creating branch: " + branchID );
		
        //reset encryption key
    		response = s.resetEncryptionKey(seasonID, sessionToken);
    		Assert.assertFalse(response.contains("error"), "fail reset encryption key: " + response);
    		
    		
    		f.setSleep();
    		//production changed file 
    		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
    		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

    		//features runtime files
    		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
    		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
    		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
    		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
    		
		JSONArray featuresProd = new JSONObject(responseProd.message).getJSONObject("root").getJSONArray("features");
		JSONArray featuresDev = new JSONObject(responseDev.message).getJSONObject("root").getJSONArray("features");
		
		Assert.assertTrue(featuresProd.size() == 1, "Incorrect number of features in runtime production file");
		Assert.assertTrue(featuresDev.size() == 1, "Incorrect number of features in runtime development file");
		
		//translation files
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		JSONObject stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==1, "Runtime development en strings file not containing right  num of strings");
		
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was changed");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==0, "Runtime production en strings file not containing right  num of strings");
		

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "de", productID,seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==1, "Runtime development de strings file not containing right  num of strings");
		
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "de", productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was changed");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==0, "Runtime production de strings file not containing right  num of strings");
		
		//branch runtime files
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "wrong number of features in branch development runtime file");
				
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "wrong number of features in branch production runtime file");
				
		//utilities runtime
		responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development utilities file was not created");
		String utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");
		
		responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production utilities file was not created");
		utlitiesList = responseProd.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");
		
		//streams runtime
		responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development streams file was not updated");		
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");
		
		responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production streams file was changed");
	    json = new JSONObject(responseProd.message);		
	    Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");
		
	    //streams utilities runtime
  		responseDev = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
  		Assert.assertTrue(responseDev.code ==200, "Runtime development stream utilities file was not created");
  		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");
		
  		responseProd = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
  		Assert.assertTrue(responseProd.code ==200, "Runtime production stream utilities file was not created");
  		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");
		
		//notification runtime
		responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development notifications file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime development file");

		responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production notifications file was not changed");
		json = new JSONObject(responseProd.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime production file");

		//branches runtime
		responseDev = RuntimeDateUtilities.getRuntimeBranchesDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime branches file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("branches").size(), 1, "Incorrect number of branches in runtime file");

		//user groups runtime
		responseDev = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime user groups file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("internalUserGroups").size(), 2, "Incorrect number of user groups in runtime file");
		
		//product runtime
		responseDev = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime product file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertTrue(json.getString("uniqueId").equals(productID), "Incorrect product  id in runtime file");
    }

   
    @Test(dependsOnMethods="testRuntimeFilesAfterReset", description = "craete new season and validate that all runtime files are encrypted with the new key")
    public void testRuntimeFilesAfterDuplicateSeason() throws Exception {
    		String dateFormat = f.setDateFormat();
		
    		//create new season
    		JSONObject season = new JSONObject();
    		season.put("minVersion", "5.0");
    		String seasonID2 =s.addSeason(productID,season.toString(), sessionToken);
    		
    		String oldKey = s.getEncryptionKeyString(seasonID, sessionToken);
    		String newKey = s.getEncryptionKeyString(seasonID2, sessionToken);
    		Assert.assertTrue(oldKey.equals(newKey),"encryption key is not identical in the new season");
        	
    		String allBranches = br.getAllBranches(seasonID2, sessionToken);
		JSONObject jsonBranch = new JSONObject(allBranches);
		String branchInNewSeasonId = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		
    		f.setSleep();
    		//production changed file 
    		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
    		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

    		//features runtime files
    		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
    		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
    		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
    		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
    		
		JSONArray featuresProd = new JSONObject(responseProd.message).getJSONObject("root").getJSONArray("features");
		JSONArray featuresDev = new JSONObject(responseDev.message).getJSONObject("root").getJSONArray("features");
		
		Assert.assertTrue(featuresProd.size() == 1, "Incorrect number of features in runtime production file");
		Assert.assertTrue(featuresDev.size() == 1, "Incorrect number of features in runtime development file");
		
		//translation files
		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "en", productID,seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		JSONObject stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==1, "Runtime development en strings file not containing right  num of strings");
		
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "en", productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was changed");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==0, "Runtime production en strings file not containing right  num of strings");
		

		responseDev = RuntimeDateUtilities.getDevelopmentTranslationDateModification(m_url, "de", productID,seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development strings file was not updated");
		stringsObj = new JSONObject(responseDev.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==1, "Runtime development de strings file not containing right  num of strings");
		
		responseProd = RuntimeDateUtilities.getProductionTranslationDateModification(m_url, "de", productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production string file was changed");
		stringsObj = new JSONObject(responseProd.message);
		Assert.assertTrue(stringsObj.getJSONObject("strings").size() ==0, "Runtime production de strings file not containing right  num of strings");
		
		//branch runtime files
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, branchInNewSeasonId, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "wrong number of features in branch development runtime file");
				
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID2, branchInNewSeasonId, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "wrong number of features in branch production runtime file");
				
		//utilities runtime
		responseDev = RuntimeDateUtilities.getDevelopmentUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development utilities file was not created");
		String utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");
		
		responseProd = RuntimeDateUtilities.getProductionUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production utilities file was not created");
		utlitiesList = responseProd.message;
		Assert.assertTrue(utlitiesList.contains("isCelsius()"), "Updated utility doesn't appear in the development file");
		
		//streams runtime
		responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development streams file was not updated");		
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");
		
		responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production streams file was changed");
	    json = new JSONObject(responseProd.message);		
	    Assert.assertEquals(json.getJSONArray("streams").size(), 0, "Incorrect number of streams in runtime development file");
		
	    //streams utilities runtime
  		responseDev = RuntimeDateUtilities.getDevelopmentStreamUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
  		Assert.assertTrue(responseDev.code ==200, "Runtime development stream utilities file was not created");
  		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");
		
  		responseProd = RuntimeDateUtilities.getProductionStreamUtilitiesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
  		Assert.assertTrue(responseProd.code ==200, "Runtime production stream utilities file was not created");
  		utlitiesList = responseDev.message;
		Assert.assertTrue(utlitiesList.contains("isFalse()"), "Updated utility doesn't appear in the development file");
		
		//notification runtime
		responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development notifications file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime development file");

		responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production notifications file was not changed");
		json = new JSONObject(responseProd.message);
		Assert.assertEquals(json.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime production file");

		//branches runtime
		responseDev = RuntimeDateUtilities.getRuntimeBranchesDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime branches file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("branches").size(), 1, "Incorrect number of branches in runtime file");

		//user groups runtime
		responseDev = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime user groups file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("internalUserGroups").size(), 2, "Incorrect number of user groups in runtime file");

		//product runtime
		responseDev = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime product file was not updated");
		json = new JSONObject(responseDev.message);
		Assert.assertTrue(json.getString("uniqueId").equals(productID), "Incorrect product  id in runtime file");

    }

    @AfterTest
	private void reset(){
    		baseUtils.reset(productID, sessionToken);
	}
}
