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

public class FeaturesInNewSeason {
	protected String seasonID;
	protected String devFeatureID;
	protected String prodFeatureID;
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
	
	@Test (description="Check that 2 feature files were created")
	public void checkFilesCreated() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==304, "Runtime development feature file was not created");
		response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==304, "Runtime production feature file was not created");
		
	}
	

	@Test (dependsOnMethods = "checkFilesCreated", description="Create a feature in development")
	public void createFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		devFeatureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(devFeatureID.contains("error"), "Feature in dev was not created: " + devFeatureID );
		
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
		
		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(devFeatureID), "Incorrect feature uniqueId in the development file");
	}
	
	@Test (dependsOnMethods = "createFeatureInDev", description="Create a feature in production")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();		
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		prodFeatureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Feature in production was not created: " + prodFeatureID );
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(prodFeatureID), "Incorrect feature uniqueId in the production file");
		
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==2, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(devFeatureID), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(features.getJSONObject(1).getString("uniqueId").equals(prodFeatureID), "Incorrect feature uniqueId in the development file");
	}
	
	@Test (dependsOnMethods = "createFeatureInProd", description="Update feature in development name ")
	public void updateFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();		
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(devFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "devfeature");
		devFeatureID = f.updateFeature(seasonID, devFeatureID, json.toString(), sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==200, "Runtime development feature file was not updated");
		
		//check that development feature was updated in the file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(response.message);
		JSONArray features = root.getJSONArray("features");
		
		Assert.assertTrue(validateFeature(features, devFeatureID, "name", "devfeature"), "Feature was not updated");
		
		response = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");

	}
	
	@Test (dependsOnMethods = "updateFeatureInDev", description="Update feature in production minAppVersion")
	public void updateFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(prodFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "0.5");
		prodFeatureID = f.updateFeature(seasonID, prodFeatureID, json.toString(), sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Test should pass, but instead failed: " + prodFeatureID );

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
		//check that production feature was updated in the development file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");

		Assert.assertTrue(validateFeature(features, prodFeatureID, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, prodFeatureID, "minAppVersion", "0.5"), "Feature was not updated in the production file");

	}
	
	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move feature from development to production")
	public void moveFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();	
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(devFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		devFeatureID = f.updateFeature(seasonID, devFeatureID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		//check that production feature was updated in the development file
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, devFeatureID, "stage", "PRODUCTION"), "Feature was not updated in the development file");
		
		//check that production feature was updated in the production file
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		boolean updatedProd = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(devFeatureID)) {
				
					updatedProd = true;
			}
		}
		Assert.assertTrue(updatedProd, "Feature was not added to the production file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToProd", description="Move feature1 from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();	
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(devFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		devFeatureID = f.updateFeature(seasonID, devFeatureID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Feature was not removed from production file");
		
		//check that the feature was updated in the development file
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateFeature(features, devFeatureID, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete feature in dev")
	public void deleteFeature1() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();	String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		int responseCode = f.deleteFeature(devFeatureID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Feature was not removed from development file");

	}
	
	@Test (dependsOnMethods = "deleteFeature1", description="Move feature from production to development")
	public void moveFeatureToDev2() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(prodFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		prodFeatureID = f.updateFeature(seasonID, prodFeatureID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==200, "Runtime development feature file was not updated");
		response = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

	}
	
	@Test (dependsOnMethods = "moveFeatureToDev2", description="Delete feature ")
	public void deleteFeature2() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		int responseCode = f.deleteFeature(prodFeatureID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Feature was not removed from development file");
	}
	
	private boolean validateFeature(JSONArray features, String uniqueId, String field, String value) throws JSONException{
		boolean updatedDev = false;
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
