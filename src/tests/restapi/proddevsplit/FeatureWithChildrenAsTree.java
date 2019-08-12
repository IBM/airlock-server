package tests.restapi.proddevsplit;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
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
import tests.restapi.SeasonsRestApi;

public class FeatureWithChildrenAsTree {
	protected String seasonID;
	protected String parentFeatureID;
	protected String childID1;
	protected String childID2;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		s.setURL(m_url);
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	@Test ( description="Create a feature in development")
	public void createFeatureWithChildrenInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		parentFeatureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(parentFeatureID.contains("error"), "Test should pass, but instead failed: " + parentFeatureID );
		
		String feature1 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		childID1 = f.addFeature(seasonID, feature1, parentFeatureID, sessionToken);
		String feature2 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		childID2 = f.addFeature(seasonID, feature2, parentFeatureID, sessionToken);

		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Incorrect number of children in the development file");
		
	}
	
	@Test (dependsOnMethods = "createFeatureWithChildrenInDev", description="Move parent feature to production, its children are in development ")
	public void moveParentAndChildrenToProduction() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(parentFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		
		String child1 = f.getFeature(childID1, sessionToken);
		JSONObject jsonChild1 = new JSONObject(child1);
		jsonChild1.put("stage", "PRODUCTION");
		
		String child2 = f.getFeature(childID2, sessionToken);
		JSONObject jsonChild2 = new JSONObject(child2);
		jsonChild2.put("stage", "PRODUCTION");
		
		JSONArray children = new JSONArray();
		children.put(jsonChild1);
		children.put(jsonChild2);
		json.remove("features");
		json.put("features", children);

		parentFeatureID = f.updateFeature(seasonID, parentFeatureID, json.toString(), sessionToken);
		Assert.assertFalse(parentFeatureID.contains("error"), "Test should pass, but instead failed: " + parentFeatureID );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("PRODUCTION"), "Parent feature was not updated in the development file");
		Assert.assertTrue(validateFeature(features, childID1, "stage", "PRODUCTION"), "Child1 feature was not updated in the development file");
		Assert.assertTrue(validateFeature(features, childID2, "stage", "PRODUCTION"), "Child2 feature was not updated in the development file");
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Feature was not updated in the production file");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Incorrect number of children in the production file");
		

	}
	
	@Test (dependsOnMethods = "moveParentAndChildrenToProduction", description="Update feature in development name ")
	public void moveChildFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(childID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		childID1 = f.updateFeature(seasonID, childID1, json.toString(), sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		
		//check that development feature was updated in the file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, childID1, "stage", "DEVELOPMENT"), "Feature was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not changed");

		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Incorrect number of children in the production file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	}
	
	
	
	@Test (dependsOnMethods = "moveChildFeatureToDev", description="Move parent feature and its child to production ")
	public void moveParentAndChildrenToDevelopment() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(parentFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		
		String child1 = f.getFeature(childID1, sessionToken);
		JSONObject jsonChild1 = new JSONObject(child1);
		jsonChild1.put("stage", "DEVELOPMENT");
		
		String child2 = f.getFeature(childID2, sessionToken);
		JSONObject jsonChild2 = new JSONObject(child2);
		jsonChild2.put("stage", "DEVELOPMENT");
		
		JSONArray children = new JSONArray();
		children.put(jsonChild1);
		children.put(jsonChild2);
		json.remove("features");
		json.put("features", children);

		parentFeatureID = f.updateFeature(seasonID, parentFeatureID, json.toString(), sessionToken);
		Assert.assertFalse(parentFeatureID.contains("error"), "Test should pass, but instead failed: " + parentFeatureID );
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("DEVELOPMENT"), "Parent feature was not updated in the development file");
		Assert.assertTrue(validateFeature(features, childID1, "stage", "DEVELOPMENT"), "Child1 feature was not updated in the development file");
		Assert.assertTrue(validateFeature(features, childID2, "stage", "DEVELOPMENT"), "Child2 feature was not updated in the development file");
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Feature was not updated in the production file");
		
	}
	
	@Test (dependsOnMethods = "moveParentAndChildrenToDevelopment", description="Delete parent feature ")
	public void deleteParentFeature() throws InterruptedException, JSONException, IOException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(parentFeatureID, sessionToken);
		Assert.assertTrue(responseCode==200, "Parent feature was not deleted");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");

		Assert.assertTrue(features.size()==0, "Feature was not deleted from the development file");
	}
	
	private int getNumberOfFeature(JSONArray features) throws JSONException{

		return features.getJSONObject(0).getJSONArray("features").size();
	}
	
	private boolean validateFeature(JSONArray rootFeature, String uniqueId, String field, String value) throws JSONException{
		boolean updatedDev = false;
		JSONObject mtxRoot = rootFeature.getJSONObject(0);
		JSONArray features = mtxRoot.getJSONArray("features");
		
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(uniqueId)) {
				if (features.getJSONObject(i).getString(field).equals(value))
					updatedDev = true;
			}
		}
		return updatedDev;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
