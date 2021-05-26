package tests.restapi.scenarios.strings;


import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UnusedStrings {
	private String seasonID;
	private String stringID1;
	private String stringID2;
	private String stringID3;
	private String stringID4;
	private String filePath;
	private StringsRestApi stringsApi;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String productID;
	private String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private FeaturesRestApi f;
	private UtilitiesRestApi u;
	private SeasonsRestApi s;
	private BranchesRestApi br ;
	

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		stringsApi = new StringsRestApi();
		stringsApi.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);


		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);


	}

	@Test (description = "Add strings ")
	public void addStrings() throws Exception{
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID1 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID1.contains("error"), "String 1 was not added to the season");

		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID2.contains("error"), "String 2 was not added to the season");

		str = FileUtils.fileToString(filePath + "strings/string3.txt", "UTF-8", false);
		stringID3 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID3.contains("error"), "String 3 was not added to the season");

		str = FileUtils.fileToString(filePath + "strings/string4.txt", "UTF-8", false);
		stringID4 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID4.contains("error"), "String 4 was not added to the season");
	}

	/*
	 * 	F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
				->F3 -> CR3 -> CR4
	 */

	@Test (dependsOnMethods="addStrings",  description = "Add hierarchy of features. String1 in utility but not in configuration rules")
	public void scenario0() throws Exception{		
		//add utility with string
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1_translate.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeature(seasonID, feature3, mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeature(seasonID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR2");
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = f.addFeature(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");

		String response = stringsApi.getStringUsage(stringID1, sessionToken);

		JSONObject respJson = new JSONObject(response);
		Assert.assertEquals(respJson.getJSONArray("UsedByUtilities").getString(0), utilityID, "Utility not found in string usage");

		ArrayList<String> ids = getConfigurations(respJson);
		Assert.assertEquals(ids.size(), 0, "Incorrect number of configurations in usage");

		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 3, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(2).getString("uniqueId").equals(stringID4), "wrong id in unusedStrings list");
		
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
		
		respCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
	}
	
	@Test (dependsOnMethods="scenario0",  description = "Add hierarchy of features. 1-3rd CR use String1, 4th CR use String4")
	public void scenario1() throws Exception{		
		
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeature(seasonID, feature3, mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeature(seasonID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration);
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", configuration);
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		jsonCR.put("configuration", configuration);
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		String configuration2 =  "{ \"text\" :  translate(\"app.fallback\", \"testing string\")	}" ;
		
		jsonCR.put("name", "CR4");
		jsonCR.put("configuration", configuration2);
		String configID4 = f.addFeature(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");

		String response = stringsApi.getStringUsage(stringID1, sessionToken);

		JSONObject respJson = new JSONObject(response);
		Assert.assertEquals(respJson.getJSONArray("UsedByUtilities").size(), 0, "wrong utilities size");

		ArrayList<String> ids = getConfigurations(respJson);
		Assert.assertEquals(ids.size(), 3, "Incorrect number of configurations in usage");

		Assert.assertTrue(ids.contains(configID1), "configID1 not found in string usage");
		Assert.assertTrue(ids.contains(configID2), "configID2 not found in string usage");
		Assert.assertTrue(ids.contains(configID3), "configID3 not found in string usage");
		
		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 2, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
	}


	/*
	 * 	 F1 -> MIX-> F2 -> CR1 -> MTXCR -> CR2+CR3
	 */

	@Test (dependsOnMethods="scenario1", description = "Add hierarchy of features. CR are using String4")
	public void scenario2() throws Exception{

		String configuration =  "{ \"text\" :  translate(\"app.fallback\", \"testing string\")	}" ;
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");


		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration);
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");


		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeature(seasonID, configurationMix, configID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");


		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", configuration);
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		jsonCR.put("configuration", configuration);
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),mixConfigID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		String response = stringsApi.getStringUsage(stringID4, sessionToken);
		JSONObject respJson = new JSONObject(response);
		ArrayList<String> ids = getConfigurations(respJson);
		Assert.assertEquals(ids.size(), 3, "Incorrect number of configurations in usage");

		Assert.assertTrue(ids.contains(configID1), "configID1 not found in string usage");
		Assert.assertTrue(ids.contains(configID2), "configID2 not found in string usage");
		Assert.assertTrue(ids.contains(configID3), "configID3 not found in string usage");

		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 3, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID1), "wrong id in unusedStrings list");	
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(2).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
	}


	/*
	 * 	 F1 -> MIX->F2 ->CR1+ CR2 -> CR3
	 */
	@Test (dependsOnMethods="scenario2", description = "Add utility and hierarchy of features. All 4 strings are being used")
	public void scenario3() throws Exception{

		//add utility with string
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1_translate.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season: " + utilityID);

				
		String configuration2 =  "{ \"text\" :  translate(\"app.hi\", \"testing string\", \"testing string\")	}" ;
		String configuration3 =  "{ \"text\" :  translate(\"app.helloProd\", \"testing string\")	}" ;
		String configuration4 =  "{ \"text\" :  translate(\"app.fallback\", \"testing string\")	}" ;
		
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);


		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration2);
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season: " + configID1);

		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", configuration3);
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season: " + configID2);


		jsonCR.put("name", "CR3");
		jsonCR.put("configuration", configuration4);
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),configID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season: " + configID3);

		
		String response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		JSONObject respJson = new JSONObject(response);
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 0, "wrong unusedStrings size");
		
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
		
		respCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
	}
	
	/*
	 * 	 F1 -> MIX->F2 ->CR1+ CR2 -> CR3
	 */
	@Test (dependsOnMethods="scenario3", description = "Add utility and hierarchy of features. check out F2/ All 4 strings are being used")
	public void scenario4() throws Exception{

		//add utility with string
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1_translate.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season: " + utilityID);

				
		String configuration2 =  "{ \"text\" :  translate(\"app.hi\", \"testing string\", \"testing string\")	}" ;
		String configuration3 =  "{ \"text\" :  translate(\"app.helloProd\", \"testing string\")	}" ;
		String configuration4 =  "{ \"text\" :  translate(\"app.fallback\", \"testing string\")	}" ;
		
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);


		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration2);
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season: " + configID1);

		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", configuration3);
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season: " + configID2);

		jsonCR.put("name", "CR3");
		jsonCR.put("configuration", configuration4);
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),configID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season: " + configID3);

		String response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		JSONObject respJson = new JSONObject(response);
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 0, "wrong unusedStrings size");
		
		//create branch and checkout F2
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch was not added to the season: " + branchID);

		String res = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "F2 was not checked out: " + res);
		
		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 0, "wrong unusedStrings size");
		
		//delete F1 from master 
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
		
		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 0, "wrong unusedStrings size");
		
		//delete branch
		respCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertEquals(respCode, 200, "Branch was not deleted");
		
		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		
		unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 3, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(2).getString("uniqueId").equals(stringID4), "wrong id in unusedStrings list");
		
		respCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
	}

	/*
	 * 	 F1 -> MIX->F2 ->CR1+ CR2 -> CR3
	 */
	@Test (dependsOnMethods="scenario4", description = "Add utility and hierarchy of features. add string2 to feature1")
	public void scenario5() throws Exception{
		String response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		JSONObject respJson = new JSONObject(response);
		
		JSONArray unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 4, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID1), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(2).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(3).getString("uniqueId").equals(stringID4), "wrong id in unusedStrings list");
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject fObj2 = new JSONObject(feature2);
		String ruleString =  "  translate(\"app.hello\", \"testing string\");  true;	" ;
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		fObj2.put("rule", ruleObj);
		String featureID2 = f.addFeature(seasonID, fObj2.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);


		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season: " + configID1);

		jsonCR.put("name", "CR2");
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season: " + configID2);

		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		
		unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 3, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID2), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(2).getString("uniqueId").equals(stringID4), "wrong id in unusedStrings list");
		
		jsonCR.put("name", "CR3");
		ruleString =  "  translate(\"app.hi\", \"testing string1\",  \"testing string2\");  true;	" ;
		ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),configID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season: " + configID3);

		response = stringsApi.getUnusageStrings(seasonID, sessionToken);
		respJson = new JSONObject(response);
		
		unusedStrings = respJson.getJSONArray("unusedStrings");
		Assert.assertTrue(unusedStrings.size() == 2, "wrong unusedStrings size");
		Assert.assertTrue(unusedStrings.getJSONObject(0).getString("uniqueId").equals(stringID3), "wrong id in unusedStrings list");
		Assert.assertTrue(unusedStrings.getJSONObject(1).getString("uniqueId").equals(stringID4), "wrong id in unusedStrings list");
	}
	
	//@SuppressWarnings({ "rawtypes", "unchecked" })
	private ArrayList<String> getConfigurations(JSONObject json) throws JSONException{
		ArrayList<String> allIds = new ArrayList<String>();
		for (int i=0; i<json.getJSONArray("UsedByConfigurations").size(); i++){
			JSONObject obj = json.getJSONArray("UsedByConfigurations").getJSONObject(i);
			allIds.add(obj.getString("configID"));

		}
		return allIds;
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
