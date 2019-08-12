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

public class FeaturesStagesMTX {
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
F1->MTX->(F2+F3)
- 	create dev->dev + dev, update to dev-> prod + dev, then dev-> prod + prod, then prod-> prod +> prod
- 	create prod->dev+dev, update to dev-> prod + dev
- 	create prod->prod+prod, update to dev-> prod+ prod

F1->MTX->F2, F3 -> MTX -> (F4+F5)
- 	create dev->dev+dev -> dev+dev, update to dev-> prod+dev-> prod+dev
- 	create prod->prod+prod-> prod+prod, update to dev-> prod+dev-> prod+prod
 */

	@Test (description = "create dev->dev + dev, update to dev-> prod + dev, then dev-> prod + prod, then prod-> prod +> prod")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			
			//create dev->dev + dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);


			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID1.contains("error"), "Feature was not created under development feature: " + childID1);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);

			//update to dev-> prod + dev
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String childFeature = f.getFeature(childID1, sessionToken);
			JSONObject jsonChild = new JSONObject(childFeature);
			jsonChild.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, childID1, jsonChild.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child1 was not updated: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

			//update to dev-> prod + prod
			dateFormat = f.setDateFormat();
			
			String childFeature2 = f.getFeature(childID2, sessionToken);
			JSONObject jsonChild2 = new JSONObject(childFeature2);
			jsonChild2.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, childID2, jsonChild2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Child2 was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

			//update to prod -> prod + prod
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
	
	@Test (description = "create prod->dev+dev, update to dev-> prod + dev as a tree")
	public void test2() throws JSONException, IOException, InterruptedException{
			
			
			//create prod->dev+dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);


			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID1.contains("error"), "Feature was not created under development feature: " + childID1);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);

			//dev-> prod + dev
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");
			jsonParent.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0).put("stage", "PRODUCTION");			
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent feature was not updated: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
	}
	
	@Test (description = "create prod->prod+prod, update to dev-> prod+ prod")
	public void test3() throws JSONException, IOException, InterruptedException{
			
			
			//create prod->dev+dev
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);


			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID1.contains("error"), "Feature was not created under development feature: " + childID1);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);

			//dev-> prod+ prod
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");		
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Parent feature was not updated: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
	}
	
	@Test (description = "create dev->dev+dev -> dev+dev, update to dev-> dev+dev-> prod+prod, update to dev-> prod+dev-> prod+prod ")
	public void test4() throws JSONException, IOException, InterruptedException{
			
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
			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID4 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID4.contains("error"), "Feature was not created under development feature: " + featureID4);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID5 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID5.contains("error"), "Feature was not created under development feature: " + featureID5);

			//update to dev-> dev+dev-> prod+prod
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String f4 = f.getFeature(featureID4, sessionToken);
			JSONObject json4 = new JSONObject(f4);
			json4.put("stage", "PRODUCTION");		
			String response = f.updateFeature(seasonID, featureID4, json4.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature4 was not updated: " + response);
			
			
			String f5 = f.getFeature(featureID5, sessionToken);
			JSONObject json5 = new JSONObject(f5);
			json5.put("stage", "PRODUCTION");		
			response = f.updateFeature(seasonID, featureID5, json5.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature5 was not updated: " + response);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	
			//update to dev-> prod+dev-> prod+prod
			f.setSleep();
			dateFormat = f.setDateFormat();

			String f2 = f.getFeature(featureID2, sessionToken);
			JSONObject json2 = new JSONObject(f2);
			json2.put("stage", "PRODUCTION");		
			response = f.updateFeature(seasonID, featureID2, json2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature4 was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
			

	
			//update to dev-> prod+prod-> prod+prod
			f.setSleep();
			dateFormat = f.setDateFormat();

			String f3 = f.getFeature(featureID3, sessionToken);
			JSONObject json3 = new JSONObject(f3);
			json3.put("stage", "PRODUCTION");		
			response = f.updateFeature(seasonID, featureID3, json3.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature3 was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
			
			//update to prod-> prod+prod-> prod+prod
			f.setSleep();
			dateFormat = f.setDateFormat();

			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "PRODUCTION");		
			response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature3 was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==5, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");


	}

	
	
	@Test (description = "create prod->prod+prod-> prod+prod, update to dev-> prod+dev-> prod+prod")
	public void test5() throws JSONException, IOException, InterruptedException{
			
			//F1->MTX->F2, F3 -> MTX -> (F4+F5)
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID1 = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID1.contains("error"), "Feature was not added to the season: " + mtxID1);


			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID2 = f.addFeature(seasonID, json.toString(), mtxID1, sessionToken);
			Assert.assertFalse(featureID2.contains("error"), "Feature was not created under development feature: " + featureID2);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID3 = f.addFeature(seasonID, json.toString(), mtxID1, sessionToken);
			Assert.assertFalse(featureID3.contains("error"), "Feature was not created under development feature: " + featureID3);

			String mtxID2 = f.addFeature(seasonID, featureMix, featureID3, sessionToken);
			Assert.assertFalse(mtxID2.contains("error"), "Feature was not added to the season: " + mtxID2);
			
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID4 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID4.contains("error"), "Feature was not created under development feature: " + featureID4);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID5 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
			Assert.assertFalse(featureID5.contains("error"), "Feature was not created under development feature: " + featureID5);

			//dev-> prod+prod-> prod+prod
			f.setSleep();
			String dateFormat = f.setDateFormat();

			String parentFeature = f.getFeature(parentID, sessionToken);
			JSONObject jsonParent = new JSONObject(parentFeature);
			jsonParent.put("stage", "DEVELOPMENT");		
			String response = f.updateFeature(seasonID, parentID, jsonParent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature3 was not updated: " + response);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
			//update to dev-> prod+dev-> prod+prod
			f.setSleep();
			dateFormat = f.setDateFormat();

			String f3 = f.getFeature(featureID3, sessionToken);
			JSONObject json3 = new JSONObject(f3);
			json3.put("stage", "DEVELOPMENT");		
			response = f.updateFeature(seasonID, featureID3, json3.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature3 was not updated: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
			

	

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
			for (int i=0; i< parentFeature.getJSONArray("features").size(); i++){
				if (parentFeature.getJSONArray("features").getJSONObject(i).getString("type").equals("FEATURE") && parentFeature.getJSONArray("features").getJSONObject(i).getString("stage").equals("PRODUCTION")){
					forCount[0]++;
				}
				 countFeatures(parentFeature.getJSONArray("features").getJSONObject(i), forCount);
				
			}
		} 
		return forCount[0];
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
