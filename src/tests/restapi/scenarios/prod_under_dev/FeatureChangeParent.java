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

public class FeatureChangeParent {
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
- move feature prod under dev
- move feature prod under dev->MTX
- create F1dev->F2prod, move F2 under Root, it should appear in prod runtime

 */

	@Test (description = "move feature prod under dev")
	public void test1() throws JSONException, IOException, InterruptedException{
			
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID1, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);
			
			//move feature prod under dev
			String dateFormat = f.setDateFormat();
			
			JSONArray children = new JSONArray();
			String child = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(child);
			children.add(jsonChild);
			
			String feature2 = f.getFeature(parentID2, sessionToken);
			JSONObject jsonF2 = new JSONObject(feature2);
			jsonF2.put("features", children);
			String response = f.updateFeature(seasonID, parentID2, jsonF2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, f.getRootId(seasonID, sessionToken))==1, "Incorrect number of production features.");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			//move F2 under Root
			dateFormat = f.setDateFormat();
			
			
			children = new JSONArray();
			String rootId = f.getRootId(seasonID, sessionToken);
			String rootFeature = f.getFeature(rootId, sessionToken);
			JSONObject jsonRoot = new JSONObject(rootFeature);
			
			String parent1 = f.getFeature(parentID1, sessionToken);
			JSONObject jsonF1 = new JSONObject(parent1);
			children.add(jsonF1);
			
			String parent2 = f.getFeature(parentID2, sessionToken);
			jsonF2 = new JSONObject(parent2);
			jsonF2.put("features", new JSONArray());
			children.add(jsonF2);
			
			child = f.getFeature(childID, sessionToken);
			jsonChild = new JSONObject(child);
			children.add(jsonChild);
			
			jsonRoot.put("features", new JSONArray());
			jsonRoot.put("features", children);
			response = f.updateFeature(seasonID, rootId, jsonRoot.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
	
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, f.getRootId(seasonID, sessionToken))==2, "Incorrect number of production features.");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (description = "move feature prod under dev->MTX")
	public void test2() throws JSONException, IOException, InterruptedException{
			
			
			//F1->Child
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID1, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);

			
			//F2->MTX-F3
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID2, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);
			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);

			String dateFormat = f.setDateFormat();
			//move child under MTX
			
			String featureMTX = f.getFeature(mtxID, sessionToken);
			JSONObject jsonMTX = new JSONObject(featureMTX);
			
			JSONArray children = jsonMTX.getJSONArray("features");
			String child = f.getFeature(childID, sessionToken);
			JSONObject jsonChild = new JSONObject(child);
			children.add(jsonChild);

			jsonMTX.put("features", children);
			String response = f.updateFeature(seasonID, mtxID, jsonMTX.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, f.getRootId(seasonID, sessionToken))==3, "Incorrect number of production features.");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	
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
					if (singleFeature.getString("stage").equals("PRODUCTION")){
						forCount[0]++;
					}
					
					totalFeatures = countFeatures(singleFeature, forCount);
				
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
