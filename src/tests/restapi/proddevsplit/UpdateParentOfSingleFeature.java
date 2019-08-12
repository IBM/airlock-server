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

public class UpdateParentOfSingleFeature {

	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String childID;
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
	
	@Test (description = "Create 2 features: one in dev and one in prod, add child to feature in dev")
	public void createComponents() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);	//parent feature1
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);	//parent feature2
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		childID = f.addFeature(seasonID, feature3, featureID2, sessionToken); // add child to feature2

	}


	@Test (dependsOnMethods="createComponents", description = "Move child from parent in dev to parent in prod")
	public void changeParent() throws IOException, JSONException, InterruptedException {
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		//move child from parent2 to parent1
		String feature = f.getFeature(featureID1, sessionToken);
		JSONArray children = new JSONArray();
		String child = f.getFeature(childID, sessionToken);
		JSONObject json = new JSONObject(child);
		children.add(json);
		JSONObject obj = new JSONObject(feature);
		obj.put("features", children);
		String response = f.updateFeature(seasonID, featureID1, obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");
	
	}


	@Test (dependsOnMethods = "changeParent", description = "This test validates that a feature can be moved from a parent feature in prod to the ROOT")
	public void moveToRoot() throws Exception{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		//move child from parent1 to root
		String parent1 = f.getFeature(featureID1, sessionToken);
		JSONObject jsonParent1 = new JSONObject(parent1);
		jsonParent1.put("features", new JSONArray());

		String child = f.getFeature(childID, sessionToken);
		String parent2 = f.getFeature(featureID2, sessionToken);
		JSONObject childJson = new JSONObject(child);
		JSONObject jsonParent2 = new JSONObject(parent2);

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
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==3, "Incorrect number of children in development file");

	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
