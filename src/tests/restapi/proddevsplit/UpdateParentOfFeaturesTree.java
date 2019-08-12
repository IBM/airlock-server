package tests.restapi.proddevsplit;

import java.io.IOException;


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

public class UpdateParentOfFeaturesTree {

	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String childID;
	protected String childID1;
	protected String childID2;
	protected String m_url;
	private String sessionToken = "";
	protected String filePath;
	private AirlockUtils baseUtils;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		f = new FeaturesRestApi();
		f.setURL(m_url);
	}
	
	@Test (description = "Create features structure")
	public void createComponents() throws IOException, JSONException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);	//parent feature1
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);	//parent feature2
		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		JSONObject jsonChild = new JSONObject(feature3);
		jsonChild.put("stage", "PRODUCTION");
		childID = f.addFeature(seasonID, jsonChild.toString(), featureID1, sessionToken); // add child to feature1
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, mtx, childID, sessionToken);
		Assert.assertFalse(mtxID.contains("error"), "Mtx group was not created" + mtxID);
		
		String feature4 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		JSONObject jsonFeature4 = new JSONObject(feature4);
		jsonFeature4.put("stage", "PRODUCTION");
		jsonFeature4.put("name", "child1");
		childID1 = f.addFeature(seasonID, jsonFeature4.toString(), mtxID, sessionToken); // add child1 to mtx
		Assert.assertFalse(childID1.contains("error"), "Test should pass, but instead failed: " + childID1 );

		JSONObject jsonFeature5 = new JSONObject(feature4);
		jsonFeature5.put("stage", "PRODUCTION");
		jsonFeature5.put("name", "child2");
		childID2 = f.addFeature(seasonID, jsonFeature5.toString(), mtxID, sessionToken); // add child2 to mtx
		Assert.assertFalse(childID2.contains("error"), "Test should pass, but instead failed: " + childID2 );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}


	@Test (dependsOnMethods="createComponents", description = "Move child from parent1 to parent2 in prod")
	public void changeParent() throws IOException, JSONException, InterruptedException {
		String dateFormat = f.setDateFormat();
		
		//move child from parent1 to parent2
		String feature = f.getFeature(featureID2, sessionToken);
		JSONObject obj = new JSONObject(feature);
		obj.put("stage", "PRODUCTION");
		JSONArray children = new JSONArray();
		String child = f.getFeature(childID, sessionToken);
		JSONObject json = new JSONObject(child);
		children.add(json);
		obj.put("features", children);
		String response = f.updateFeature(seasonID, featureID2, obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		boolean foundFeatures=false;
		for (int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureID2)){
				JSONArray firstLevelChildren = features.getJSONObject(i).getJSONArray("features");
				JSONArray mtx = firstLevelChildren.getJSONObject(0).getJSONArray("features");
				JSONArray secondLevelChildren = mtx.getJSONObject(0).getJSONArray("features");
				if (secondLevelChildren.size()==2)
					foundFeatures=true;
			}
		}
		Assert.assertTrue(foundFeatures, "Incorrect features structure");
	}


	@Test (dependsOnMethods = "changeParent", description = "This test validates that a feature can be moved from a parent feature in prod to the ROOT")
	public void moveToRoot() throws Exception{
		String dateFormat = f.setDateFormat();
		
		//move child from parent1 to root
		String parent1 = f.getFeature(featureID1, sessionToken);
		JSONObject jsonParent1 = new JSONObject(parent1);
		
		String child = f.getFeature(childID, sessionToken);
		String parent2 = f.getFeature(featureID2, sessionToken);
		JSONObject childJson = new JSONObject(child);
		JSONObject jsonParent2 = new JSONObject(parent2);
		jsonParent2.put("features", new JSONArray());

		//retrieve season's root uniqueId and a list of features
		
		String rootUniqueId = f.getRootId(seasonID, sessionToken);
		String rootObj = f.getFeature(rootUniqueId, sessionToken);
		JSONObject seasonJson = new JSONObject(rootObj);

		JSONArray newFeatures = new JSONArray();
		newFeatures.put(jsonParent1);
		newFeatures.put(jsonParent2);
		newFeatures.put(childJson);
		seasonJson.put("features", newFeatures);

		String response = f.updateFeature(seasonID, rootUniqueId, seasonJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==3, "Incorrect number of children in production file");

	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
