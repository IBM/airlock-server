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

public class MTXInNewSeason {
	protected String seasonID;
	protected String mtxID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String utilityID;
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
	
	//TODO
	/*
	 * for all tests here get the files content
	 * 
	 */


	@Test (description="Create an mtx group in development")
	public void createFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID = f.addFeature(seasonID, mtx, "ROOT", sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 featureID1 = f.addFeature(seasonID, feature1, mtxID, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		 featureID2 = f.addFeature(seasonID, feature2, mtxID, sessionToken);
		
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
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mtxID), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Incorrect number of children in mtx group");
		
	}
	
	@Test (dependsOnMethods = "createFeatureInDev", description="Add the 3-d feature in production")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID3 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mtxID), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Incorrect number of children in mtx group");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mtxID), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(getNumberOfFeature(features)==3, "Incorrect number of children in mtx group");

		
	}
	
	@Test (dependsOnMethods="createFeatureInProd", description = "Change featrues order in mutually exclusive group")
	public void testChangeOrder() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String parent = f.getFeature(mtxID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("features");
		JSONArray children = new JSONArray();
		JSONObject child3 = new JSONObject(f.getFeature(featureID3, sessionToken));
		children.put(child3);
		JSONObject child2 = new JSONObject(f.getFeature(featureID2, sessionToken));
		children.put(child2);
		JSONObject child1 = new JSONObject(f.getFeature(featureID1, sessionToken));
		children.put(child1);
		json.put("features", children);
		f.updateFeature(seasonID, mtxID, json.toString(), sessionToken);
		
	
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		JSONObject mtxRoot = features.getJSONObject(0);
		JSONArray newChildren = mtxRoot.getJSONArray("features");
		Assert.assertTrue(newChildren.size()==3, "Incorrect number of children features after the order change.");

		if (newChildren.size() == 3){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), featureID3, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), featureID2, "The second child is incorrect");
			Assert.assertEquals(children.getJSONObject(2).get("uniqueId"), featureID1, "The third child is incorrect");
		}

	}
	
	@Test (dependsOnMethods = "testChangeOrder", description="Update feature in development name ")
	public void updateFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "devfeature");
		featureID1 = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==200, "Runtime development feature file was not updated");
		
		//check that development feature was updated in the file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(response.message);
		JSONArray features = root.getJSONArray("features");
		
		Assert.assertTrue(validateFeature(features, featureID1, "name", "devfeature"), "Feature was not updated");
		
		response = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods = "updateFeatureInDev", description="Update feature in production minAppVersion")
	public void updateFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID3, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "0.5");
		featureID3 = f.updateFeature(seasonID, featureID3, json.toString(), sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Test should pass, but instead failed: " + featureID3 );

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check that production feature was updated in the development file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");

		Assert.assertTrue(validateFeature(features, featureID3, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, featureID3, "minAppVersion", "0.5"), "Feature was not updated in the production file");

	}
	
	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move feature from development to production")
	public void moveFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID1 = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check that production feature was updated in the development file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, featureID1, "stage", "PRODUCTION"), "Feature was not updated in the development file");
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Feature was not added to the production file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToProd", description="Move feature1 from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		featureID1 = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Feature was not removed from production file");
		
		//check that the feature was updated in the development file
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, featureID1, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete feature in dev")
	public void deleteFeature1() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(featureID2, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Feature was not removed from development file");

	}
	
	@Test (dependsOnMethods = "deleteFeature1", description="Move feature from production to development")
	public void moveFeatureToDev2() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID3, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		featureID3 = f.updateFeature(seasonID, featureID3, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Feature was not removed from development file");
		
		 root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		 features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "mtx group was not deleted from production file");

	}
	
	@Test (dependsOnMethods = "moveFeatureToDev2", description="Delete feature ")
	public void deleteMtxGroup() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(mtxID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Feature was not removed from development file");
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
