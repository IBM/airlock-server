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

public class FeaturesStagesAdvanced {
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
F1->F2->F3
- 	create dev->dev->dev, update to dev-> prod -> dev, then dev-> prod -> prod, then prod-> prod -> prod
- 	create dev->dev->dev, update to dev-> dev-> prod, then dev-> prod -> prod, then prod-> prod -> prod
- 	create dev->dev->dev, update to prod-> dev-> prod, then dev-> prod -> prod - separate updates
- 	create dev->dev->dev, update to prod-> dev-> prod, then dev-> prod -> prod, then prod-> prod-> prod - tree update
- 	create prod->dev->dev, update to dev-> prod -> dev, then to dev-> prod -> prod - as a tree
- 	create prod->dev->dev, update to prod-> dev-> prod
- 	create prod->prod->prod, update to dev-> prod -> prod
- 	create prod->prod->prod, update to prod-> dev-> prod
 */

	@Test (description = "create dev->dev->dev, update to dev-> prod -> dev, then dev-> prod -> prod, then prod-> prod -> prod")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			//update to dev-> prod -> dev
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String childFeature = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(childFeature);
			jsonChild.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, childID, jsonChild.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child1 was not updated to production: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

			//update to dev-> prod -> prod
			dateFormat = f.setDateFormat();
			
			String childFeature2 = f.getFeature(secondChildID, sessionToken);
			JSONObject jsonChild2 = new JSONObject(childFeature2);
			jsonChild2.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, secondChildID, jsonChild2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child2 was not updated to production: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

			//update to prod -> prod -> prod
			dateFormat = f.setDateFormat();
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent was not updated to production: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==3, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
	}
	
	
	@Test (description = "create dev->dev->dev, update to dev-> dev-> prod, then dev-> prod -> prod, then prod-> prod -> prod")
	public void test2() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			//update to dev-> dev -> prod
			f.setSleep();
			String dateFormat = f.setDateFormat();
			
			String childFeature2 = f.getFeature(secondChildID, sessionToken);
			JSONObject jsonChild2 = new JSONObject(childFeature2);
			jsonChild2.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, secondChildID, jsonChild2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child2 was not updated to production: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

			
			//update to dev-> prod -> prod
			dateFormat = f.setDateFormat();
			
			String childFeature = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(childFeature);
			jsonChild.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, childID, jsonChild.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child1 was not updated to production: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");


			//update to prod -> prod -> prod
			dateFormat = f.setDateFormat();
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent was not updated to production: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==3, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
	}
	
	
	@Test (description = "create dev->dev->dev, update to prod-> dev-> prod, then dev-> prod -> prod - separate updates ")
	public void test3() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			//update to prod-> dev-> prod
			f.setSleep();
			String dateFormat = f.setDateFormat();
			
			String childFeature2 = f.getFeature(secondChildID, sessionToken);
			JSONObject jsonChild2 = new JSONObject(childFeature2);
			jsonChild2.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, secondChildID, jsonChild2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child2 was not updated to production: " + response);
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent was not updated to production: " + response);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==1, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");


			//update to dev-> prod-> prod
			dateFormat = f.setDateFormat();
			
			parentFeature = f.getFeature(parentID, sessionToken);
			jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent was not updated to development: " + response);

			
			String childFeature = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(childFeature);
			jsonChild.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, childID, jsonChild.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child1 was not updated to production: " + response);
			

			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	
	@Test (description = "create dev->dev->dev, update to prod-> dev-> prod, then dev-> prod -> prod, then prod-> prod-> prod - tree update")
	public void test4() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			//update to prod-> dev-> prod
			f.setSleep();
			String dateFormat = f.setDateFormat();
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");
			jsonParent.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0).put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Tree was not updated: " + response);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==1, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");


			//update to dev-> prod-> prod
			dateFormat = f.setDateFormat();
			
			parentFeature = f.getFeature(parentID, sessionToken);
			jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");
			jsonParent.getJSONArray("features").getJSONObject(0).put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Tree was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			
			//update to prod-> prod-> prod
			dateFormat = f.setDateFormat();
			
			parentFeature = f.getFeature(parentID, sessionToken);
			jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Tree was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==3, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (description = "create prod->dev->dev, update to dev-> prod -> dev, then to dev-> prod -> prod - as a tree")
	public void test5() throws JSONException, IOException, InterruptedException{
			
			
			//create prod->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			//update to dev-> prod -> dev
			f.setSleep();
			String dateFormat = f.setDateFormat();
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");
			jsonParent.getJSONArray("features").getJSONObject(0).put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Tree was not updated: " + response);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");


			//update to dev-> prod-> prod
			dateFormat = f.setDateFormat();
			
			parentFeature = f.getFeature(parentID, sessionToken);
			jsonParent = new JSONObject(parentFeature);
			jsonParent.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0).put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Tree was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (description = "create prod->dev->dev, update to prod-> dev-> prod")
	public void test6() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
			
			//create prod->dev->dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==1, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			
			//update to prod-> dev-> prod
			f.setSleep();
			dateFormat = f.setDateFormat();
			
			String childFeature2 = f.getFeature(secondChildID, sessionToken);
			JSONObject jsonChild2 = new JSONObject(childFeature2);
			jsonChild2.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, secondChildID, jsonChild2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child2 was not updated to production: " + response);

			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	@Test (description = "create prod->prod->prod, update to dev-> prod -> prod")
	public void test7() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
			
			//create prod->prod->prod
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==3, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			
			//update to dev-> prod -> prod
			f.setSleep();
			dateFormat = f.setDateFormat();
			
			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent was not updated to production: " + response);

			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	
	@Test (description = "create prod->prod->prod, update to prod-> dev -> prod")
	public void test8() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
			
			//create prod->prod->prod
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Feature was not created under development feature: " + childID);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String secondChildID = f.addFeature(seasonID, json.toString(), childID, sessionToken);
			Assert.assertFalse(secondChildID.contains("error"), "Feature was not created under development feature: " + secondChildID);

			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==3, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			
			//update to dev-> prod -> prod
			f.setSleep();
			dateFormat = f.setDateFormat();
			
			String childFeature = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(childFeature);
			jsonChild.put("stage", "DEVELOPMENT");
			String response = f.updateFeature(seasonID, childID, jsonChild.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child1 was not updated to production: " + response);

			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==1, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
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
		if (parentFeature.getJSONArray("features").size() != 0){
			if (parentFeature.getJSONArray("features").getJSONObject(0).getString("stage").equals("PRODUCTION")){
				forCount[0]++;
			}
			countFeatures(parentFeature.getJSONArray("features").getJSONObject(0), forCount);
		} 
		return forCount[0];
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
