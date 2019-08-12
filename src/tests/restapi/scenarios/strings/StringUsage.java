package tests.restapi.scenarios.strings;


import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class StringUsage {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String str;
	protected StringsRestApi stringsApi;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private FeaturesRestApi f;
	private UtilitiesRestApi u;
	private SeasonsRestApi s;
	
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

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
	}
	

/*
 * 	F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
				->F3 -> CR3 -> CR4
 */
	
	@Test (description = "Add string and hierarchy of features")
	public void scenario1() throws Exception{
		
		
		//add utility with string
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1_translate.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);		

		
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
		String rule =  "{ \"ruleString\" : \"translate('app.hello', 'param') =='hi'\"}" ;
		
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

		jsonCR.put("name", "CR4");
		jsonCR.put("configuration", configuration);
		String configID4 = f.addFeature(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
		
		String response = stringsApi.getStringUsage(stringID, sessionToken);
		
		
		JSONObject respJson = new JSONObject(response);
		Assert.assertEquals(respJson.getJSONArray("UsedByUtilities").getString(0), utilityID, "Utility not found in string usage");
		
		ArrayList<String> ids = getConfigurations(respJson);
		Assert.assertEquals(ids.size(), 4, "Incorrect number of configurations in usage");
		
		Assert.assertTrue(ids.contains(configID1), "configID1 not found in string usage");
		Assert.assertTrue(ids.contains(configID2), "configID2 not found in string usage");
		Assert.assertTrue(ids.contains(configID3), "configID3 not found in string usage");
		Assert.assertTrue(ids.contains(configID4), "configID4 not found in string usage");

		
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
		
	}
	
	
	
	/*
	 * 	 F1 -> MIX-> F2 -> CR1 -> MTXCR -> CR2+CR3
	 */
		
		@Test (description = "Add string and hierarchy of features")
		public void scenario2() throws Exception{
						
			String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
			String rule =  "{ \"ruleString\" : \"translate('app.hello', 'param') =='hi'\"}" ;
			
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
			
			String response = stringsApi.getStringUsage(stringID, sessionToken);
			JSONObject respJson = new JSONObject(response);
			ArrayList<String> ids = getConfigurations(respJson);
			Assert.assertEquals(ids.size(), 3, "Incorrect number of configurations in usage");
			
			Assert.assertTrue(ids.contains(configID1), "configID1 not found in string usage");
			Assert.assertTrue(ids.contains(configID2), "configID2 not found in string usage");
			Assert.assertTrue(ids.contains(configID3), "configID3 not found in string usage");

			
			int respCode = f.deleteFeature(featureID1, sessionToken);
			Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
			
		}
		
		
		/*
		 * 	 F1 -> MIX->F2 ->CR1+ CR2 -> CR3+CR4

		 */
			
			@Test (description = "Add string and hierarchy of features")
			public void scenario3() throws Exception{
				
				String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
				String rule =  "{ \"ruleString\" : \"translate('app.hello', 'param') =='hi'\"}" ;
				
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
				
				jsonCR.put("name", "CR2");
				jsonCR.put("configuration", configuration);
				String configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
				Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");
						
	
				jsonCR.put("name", "CR3");
				jsonCR.put("configuration", configuration);
				String configID3 = f.addFeature(seasonID, jsonCR.toString(),configID2, sessionToken);
				Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");
				
				jsonCR.put("name", "CR4");
				jsonCR.put("configuration", configuration);
				String configID4 = f.addFeature(seasonID, jsonCR.toString(),configID2, sessionToken);
				Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
				
				String response = stringsApi.getStringUsage(stringID, sessionToken);
				JSONObject respJson = new JSONObject(response);
				
				ArrayList<String> ids = getConfigurations(respJson);
				Assert.assertEquals(ids.size(), 4, "Incorrect number of configurations in usage");
				
				Assert.assertTrue(ids.contains(configID1), "configID1 not found in string usage");
				Assert.assertTrue(ids.contains(configID2), "configID2 not found in string usage");
				Assert.assertTrue(ids.contains(configID3), "configID3 not found in string usage");
				Assert.assertTrue(ids.contains(configID4), "configID4 not found in string usage");
				
				int respCode = f.deleteFeature(featureID1, sessionToken);
				Assert.assertEquals(respCode, 200, "Parent feature was not deleted");
				
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
