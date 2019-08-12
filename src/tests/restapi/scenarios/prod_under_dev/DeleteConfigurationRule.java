package tests.restapi.scenarios.prod_under_dev;

import java.io.IOException;



import org.apache.commons.lang3.RandomStringUtils;
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


public class DeleteConfigurationRule {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private SeasonsRestApi s;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}

	
	@Test (description = "Create dev->prod+dev")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			

			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);
			
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted"), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			
			response = f.simulateDeleteFeature(configID1, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't delete configuration rule in simulate mode");
			respCode = f.deleteFeature(configID1, sessionToken);
			Assert.assertEquals(respCode,  200, "Incorrect response code when deleting config rule");

	}
	
	@Test (dependsOnMethods = "test1", description = "Create dev->CRMTX ->(prod+dev)")
	public void test2() throws JSONException, IOException, InterruptedException{
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String configMTX = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, configMTX.toString(), parentID, sessionToken);
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), mtxID, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), mtxID, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);
			
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			response = f.simulateDeleteFeature(mtxID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}

	
	@Test (dependsOnMethods = "test2", description = "Create dev->CRMTX1 ->CRMTX2 -> (prod+dev)")
	public void test3() throws JSONException, IOException, InterruptedException{
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String configMTX = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
			String mtxID1 = f.addFeature(seasonID, configMTX.toString(), parentID, sessionToken);
			
			String mtxID2 = f.addFeature(seasonID, configMTX.toString(), mtxID1, sessionToken);
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), mtxID2, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), mtxID2, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);
			
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			response = f.simulateDeleteFeature(mtxID1, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID1, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}
	
	@Test (dependsOnMethods = "test3", description = "Create dev->dev->prod")
	public void test4() throws JSONException, IOException, InterruptedException{
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), configID1, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);

			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			response = f.simulateDeleteFeature(configID1, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(configID1, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}
	
	@Test (dependsOnMethods="test4", description = "Delete season")
	public void deleteSeasonAndProduct() {
		int respCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertFalse(respCode == 200, "season was deleted with features in production");
		
		respCode = p.deleteProduct(productID, sessionToken);
		Assert.assertFalse(respCode == 200, "product was deleted with features in production");
	}

	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
