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

public class MTXInsideMTXInNewSeason {
	protected String seasonID;
	protected String mtxID1;
	protected String mtxID2;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String featureID4;
	protected String featureID5;
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


	@Test (description="Create an mtx group in development under ROOT")
	public void createFirstMTX() throws JSONException, IOException, InterruptedException{		
		String dateFormat = f.setDateFormat();
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID1 = f.addFeature(seasonID, mtx, "ROOT", sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 featureID1 = f.addFeature(seasonID, feature1, mtxID1, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		 featureID2 = f.addFeature(seasonID, feature2, mtxID1, sessionToken);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	
	@Test (dependsOnMethods = "createFirstMTX", description="Create an mtx group in development under ROOT")
	public void createSecondMTX() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID2 = f.addFeature(seasonID, mtx, featureID1, sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		 featureID3 = f.addFeature(seasonID, feature1, mtxID2, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		 featureID4 = f.addFeature(seasonID, feature2, mtxID2, sessionToken);
		
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
		
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		
		Assert.assertTrue(mtx2.size()==1, "The inner mtx group is not under the parent feature");
		
	}
	
	
	@Test (dependsOnMethods = "createSecondMTX", description="Move feature from development to production")
	public void moveParentFeatureToProd() throws JSONException, IOException, InterruptedException{
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response  = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"),  "Feature was not moved to production");
	}
	
	@Test (dependsOnMethods = "moveParentFeatureToProd", description="Add the 3-d feature in production to the innermost mtx")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("name", "prodFeature");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
		
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

		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mtxID1), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Incorrect number of children in mtx group");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==3, "Incorrect number of children in mtx group");

		
	}

	
	@Test (dependsOnMethods="createFeatureInProd", description = "Change featrues order in mutually exclusive group")
	public void testChangeOrder() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String parent = f.getFeature(mtxID2, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("features");
		JSONArray children = new JSONArray();
		JSONObject child3 = new JSONObject(f.getFeature(featureID5, sessionToken));
		children.put(child3);
		JSONObject child2 = new JSONObject(f.getFeature(featureID4, sessionToken));
		children.put(child2);
		JSONObject child1 = new JSONObject(f.getFeature(featureID3, sessionToken));
		children.put(child1);
		json.put("features", children);
		f.updateFeature(seasonID, mtxID2, json.toString(), sessionToken);
		
	
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
		
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		JSONArray newChildren = mtx2.getJSONObject(0).getJSONArray("features");
		
		Assert.assertTrue(newChildren.size()==3, "Incorrect number of children features after the order change.");

		if (newChildren.size() == 3){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), featureID5, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), featureID4, "The second child is incorrect");
			Assert.assertEquals(children.getJSONObject(2).get("uniqueId"), featureID3, "The third child is incorrect");
		}

	}
	
	@Test (dependsOnMethods = "testChangeOrder", description="Update feature in development name ")
	public void updateFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID3, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "devfeature");
		featureID3 = f.updateFeature(seasonID, featureID3, json.toString(), sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==200, "Runtime development feature file was not updated");
		
		//check that development feature was updated in the file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(response.message);
		JSONArray features = root.getJSONArray("features");
		
		Assert.assertTrue(validateFeature(features, featureID3, "name", "devfeature"), "Feature was not updated");
		
		response = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods = "updateFeatureInDev", description="Update feature in production minAppVersion")
	public void updateFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID5, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "0.5");
		featureID5 = f.updateFeature(seasonID, featureID5, json.toString(), sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Test should pass, but instead failed: " + featureID3 );

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

		Assert.assertTrue(validateFeature(features, featureID5, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, featureID5, "minAppVersion", "0.5"), "Feature was not updated in the production file");

	}
	
	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move feature from development to production")
	public void moveFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID3, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID3 = f.updateFeature(seasonID, featureID3, json.toString(), sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not moved to production");
		
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
		Assert.assertTrue(validateFeature(features, featureID3, "stage", "PRODUCTION"), "Feature was not updated in the development file");
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==2, "Feature was not added to the production file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToProd", description="Move feature1 from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
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

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Feature was not removed from production file");
		
		//check that the feature was updated in the development file
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, featureID3, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete feature in dev")
	public void deleteFeature1() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(featureID3, sessionToken);
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

		
		String feature1 = f.getFeature(featureID5, sessionToken);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "DEVELOPMENT");
		featureID5 = f.updateFeature(seasonID, featureID5, json.toString(), sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature5 was not moved to develoment" + featureID5);
		
		String feature2 = f.getFeature(featureID1, sessionToken);
		JSONObject json2 = new JSONObject(feature2);
		json.put("stage", "DEVELOPMENT");
		featureID1 = f.updateFeature(seasonID, featureID1, json2.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not moved to develoment" + featureID1);

	}
	
	@Test (dependsOnMethods = "moveFeatureToDev2", description="Delete feature ")
	public void deleteMtxGroup() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature( mtxID2, sessionToken);
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
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(mtx2.size()==0, "The second mtx group was not removed from development file");
	}
	
	private int getNumberOfFeature(JSONArray features) throws JSONException{
		//get the number of children in the innermost mtx group
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		return mtx2.getJSONObject(0).getJSONArray("features").size();
	}
	
	private boolean validateFeature(JSONArray rootFeature, String uniqueId, String field, String value) throws JSONException{
		boolean updatedDev = false;
		JSONArray mtx1 = rootFeature.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		JSONArray features = mtx2.getJSONObject(0).getJSONArray("features")	;	
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
