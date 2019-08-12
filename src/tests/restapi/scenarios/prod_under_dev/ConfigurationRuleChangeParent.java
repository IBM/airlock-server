package tests.restapi.scenarios.prod_under_dev;

import java.io.IOException;

















import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class ConfigurationRuleChangeParent {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String m_url;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}

/*
- 	move CR prod under F2dev->Crdev
- 	move CR prod under F2dev->CRMTX
- 	move CR prod under CR dev

 */

	@Test (description = "move CR prod under feature dev")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID = f.addFeature(seasonID, jsonCR.toString(), parentID1, sessionToken);
			Assert.assertFalse(configID.contains("error"), "Feature was not created: " + configID);

			
			//move config prod under feature dev
			String dateFormat = f.setDateFormat();
			
			JSONArray children = new JSONArray();
			String child = f.getFeature(configID, sessionToken);
			JSONObject jsonChild = new JSONObject(child);
			children.add(jsonChild);
			
			String feature2 = f.getFeature(parentID2, sessionToken);
			JSONObject jsonF2 = new JSONObject(feature2);
			jsonF2.put("configurationRules", children);
			String response = f.updateFeature(seasonID, parentID2, jsonF2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID2)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID2)==0, "Incorrect number of production features.");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID1)==1, "Incorrect number of production features.");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");


	}
	
	@Test (description = "move CR prod under F2dev->CRMTX")
	public void test2() throws JSONException, IOException, InterruptedException{
			
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID = f.addFeature(seasonID, jsonCR.toString(), parentID1, sessionToken);
			Assert.assertFalse(configID.contains("error"), "Feature was not created: " + configID);

			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			String configMTX = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, configMTX.toString(), parentID2, sessionToken);

			

			
			//move config prod under feature dev
			String dateFormat = f.setDateFormat();
			
			JSONArray children = new JSONArray();
			String child = f.getFeature(configID, sessionToken);
			JSONObject jsonChild = new JSONObject(child);
			children.add(jsonChild);
			
			String feature2 = f.getFeature(mtxID, sessionToken);
			JSONObject jsonF2 = new JSONObject(feature2);
			jsonF2.put("configurationRules", children);
			String response = f.updateFeature(seasonID, mtxID, jsonF2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID2)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID2)==0, "Incorrect number of production features.");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");


	}
	
	@Test (description = "move CR prod under dev->Crdev")
	public void test3() throws JSONException, IOException, InterruptedException{
			
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID = f.addFeature(seasonID, jsonCR.toString(), parentID1, sessionToken);
			Assert.assertFalse(configID.contains("error"), "Feature was not created: " + configID);

			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), parentID2, sessionToken);
			Assert.assertFalse(configID.contains("error"), "Feature was not created: " + configID);
			
			//move config prod under feature dev
			String dateFormat = f.setDateFormat();
			
			JSONArray children = new JSONArray();
			String child = f.getFeature(configID, sessionToken);
			JSONObject jsonChild = new JSONObject(child);
			children.add(jsonChild);
			
			String feature2 = f.getFeature(configID2, sessionToken);
			JSONObject jsonF2 = new JSONObject(feature2);
			jsonF2.put("configurationRules", children);
			String response = f.updateFeature(seasonID, configID2, jsonF2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID2)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID2)==0, "Incorrect number of production features.");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");


	}

	private int validateProductionFeatures(String input, String featureID) throws JSONException{
		int totalFeatures = 0;
		Integer[] forCount = new Integer[1];
		forCount[0]=0;
		JSONObject runtime = new JSONObject(input);
		JSONArray features = runtime.getJSONObject("root").getJSONArray("features");

		if (features.size()==0)
			return 0;
		else {
			for (int i=0; i< features.size(); i++){
				JSONObject singleFeature = features.getJSONObject(i);
				if (singleFeature.getString("uniqueId").equals(featureID)){
					if (singleFeature.getString("stage").equals("PRODUCTION")){
						forCount[0]++;
					}
					
					totalFeatures = countFeatures(singleFeature, forCount);
				}
			}
		}
		
		return totalFeatures;
	}
	
	private int countFeatures(JSONObject parentFeature, Integer[] forCount) throws JSONException{
		if (parentFeature.getJSONArray("configurationRules").size() != 0){
			for (int i =0; i<parentFeature.getJSONArray("configurationRules").size(); i++) {
				if (!parentFeature.getJSONArray("configurationRules").getJSONObject(i).getString("type").equals("CONFIG_MUTUAL_EXCLUSION_GROUP") && parentFeature.getJSONArray("configurationRules").getJSONObject(i).getString("stage").equals("PRODUCTION")){
					forCount[0]++;
				}
				countFeatures(parentFeature.getJSONArray("configurationRules").getJSONObject(i), forCount);
			}
			
		} 
		return forCount[0];
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
