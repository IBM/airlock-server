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

public class DeleteFeatures {
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

/*

 */

	@Test (description = "Create dev->prod, delete F1")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);
			
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");
	
	}
	

	@Test (description = "create dev->MTX -> (prod + dev)")
	public void test2() throws JSONException, IOException, InterruptedException{
			

			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);


			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID1.contains("error"), "Feature was not created under development feature: " + childID1);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);

			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}	

	
	@Test (description = "create dev->MTX ->(dev+dev -> MTX->(prod+dev))")
	public void test3() throws JSONException, IOException, InterruptedException{
			
			//F1->MTX->F2, F3 -> MTX -> (F4+F5)
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID1 = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID1.contains("error"), "Feature was not added to the season: " + mtxID1);


			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID2 = f.addFeature(seasonID, json.toString(), mtxID1, sessionToken);
			Assert.assertFalse(featureID2.contains("error"), "Feature was not created under development feature: " + featureID2);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID3 = f.addFeature(seasonID, json.toString(), mtxID1, sessionToken);
			Assert.assertFalse(featureID3.contains("error"), "Feature was not created under development feature: " + featureID3);

			String mtxID2 = f.addFeature(seasonID, featureMix, featureID3, sessionToken);
			Assert.assertFalse(mtxID2.contains("error"), "Feature was not added to the season: " + mtxID2);
			
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID4 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID4.contains("error"), "Feature was not created under development feature: " + featureID4);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID5 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID5.contains("error"), "Feature was not created under development feature: " + featureID5);
			
			//delete parent feature
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			//delete MTX1
			response = f.simulateDeleteFeature(mtxID1, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID1, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			//delete MTX2
			response = f.simulateDeleteFeature(mtxID2, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID2, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			//delete F5
			response = f.simulateDeleteFeature(featureID5, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't delete F5 in simulate mode");
			respCode = f.deleteFeature(featureID5, sessionToken);
			Assert.assertEquals(respCode,  200, "Incorrect response code when deleting F5");

	}
	
	@Test (description = "create dev->dev->prod")
	public void test4() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev->prod
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);
			
			//delete parent feature
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted"), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}
	
	@Test (description = "create dev->MTX1 -> MTX2 ->(dev+prod)")
	public void test5() throws JSONException, IOException, InterruptedException{
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID1 = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID1.contains("error"), "Feature was not added to the season: " + mtxID1);

			String mtxID2 = f.addFeature(seasonID, featureMix, mtxID1, sessionToken);
			Assert.assertFalse(mtxID2.contains("error"), "Feature was not added to the season: " + mtxID2);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID2 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID2.contains("error"), "Feature was not created under development feature: " + featureID2);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID3 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID3.contains("error"), "Feature was not created under development feature: " + featureID3);


			//delete parent feature
			String response = f.simulateDeleteFeature(parentID, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			int respCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			//delete MTX1
			response = f.simulateDeleteFeature(mtxID1, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID1, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

			//delete MTX2
			response = f.simulateDeleteFeature(mtxID2, sessionToken);
			Assert.assertTrue(response.contains("The item cannot be deleted."), "Incorrect response when trying to delete feature in simulate mode");
			respCode = f.deleteFeature(mtxID2, sessionToken);
			Assert.assertEquals(respCode,  400, "Incorrect response code when deleting feature");

	}
	
	@Test (dependsOnMethods="test5", description = "Delete season")
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
