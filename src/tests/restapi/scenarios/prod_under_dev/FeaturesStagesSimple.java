package tests.restapi.scenarios.prod_under_dev;

import java.io.IOException;












import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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

//test static runtime files as well
public class FeaturesStagesSimple {
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
 *  F1->F2
- create dev->prod
- create prod->prod, update to dev->prod as updateFeature on subfeature, then prod->prod
- create dev->dev, update to dev->prod as updateFeature on subfeature, then update to prod->prod
- create dev->dev, update to dev->prod as a tree
 */

	@Test (description = "Create dev->prod")
	public void test1() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
/*
			RuntimeRestApi.DateModificationResults staticResponseDev = RuntimeDateUtilities.getStaticDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(staticResponseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(countFeatures(staticResponseDev.message)==2, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults staticResponseProd = RuntimeDateUtilities.getStaticProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(staticResponseProd.code ==304, "Runtime production feature file was changed");
*/			
	
	}
	
	@Test (description = "Create prod->prod, update to dev->prod, update to prod->prod")
	public void test2() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);
			
			String feature1 = f.getFeature(parentID, sessionToken);
			json = new JSONObject(feature1);
			json.put("stage", "DEVELOPMENT");
			String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "can't update parent feature to dev stage: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		/*	
			//static runtime files
			responseDev = RuntimeDateUtilities.getStaticDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(countFeatures(responseDev.message)==4, "Incorrect number of development features");
			responseProd = RuntimeDateUtilities.getStaticProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(countFeatures(responseProd.message)==0, "Incorrect number of production features");
			
			feature1 = f.getFeature(parentID, sessionToken);
			json = new JSONObject(feature1);
			json.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "can't update parent feature to dev stage: " + response);
			
			f.setSleep();
			responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID)==2, "Incorrect number of development features");
			responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==2, "Incorrect number of production features");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

			//static runtime files
			responseDev = RuntimeDateUtilities.getStaticDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(countFeatures(responseDev.message)==4, "Incorrect number of development features");
			responseProd = RuntimeDateUtilities.getStaticProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
			Assert.assertTrue(countFeatures(responseProd.message)==2, "Incorrect number of production features");
			
			//validate static file features fields
			checkStaticFileFields(responseDev.message, 4);
			checkStaticFileFields(responseProd.message, 2);
	*/
	}
	
	private void checkStaticFileFields(String input, int numberOfFeatures) {
		Assert.assertTrue(StringUtils.countMatches(input, "namespace") == numberOfFeatures, "wrong static file field - namespace");
		Assert.assertTrue(StringUtils.countMatches(input, "\"name\"") == numberOfFeatures, "wrong static file field - name");
		Assert.assertTrue(StringUtils.countMatches(input, "defaultConfiguration") == numberOfFeatures, "wrong static file field - defaultConfiguration");
		Assert.assertTrue(StringUtils.countMatches(input, "type") -1 == numberOfFeatures, "wrong static file field - type");
		Assert.assertTrue(StringUtils.countMatches(input, "minAppVersion") == numberOfFeatures, "wrong static file field - minAppVersion");
		Assert.assertTrue(StringUtils.countMatches(input, "features") -1 == numberOfFeatures, "wrong static file field - features");
		Assert.assertTrue(StringUtils.countMatches(input, "enabled") == numberOfFeatures, "wrong static file field - enabled");
		
		Assert.assertTrue(StringUtils.countMatches(input, "stage") == 0, "wrong static file field - stage");
		Assert.assertTrue(StringUtils.countMatches(input, "uniqueId") == 0, "wrong static file field - uniqueId");
		Assert.assertTrue(StringUtils.countMatches(input, "additionalInfo") == 0, "wrong static file field - additionalInfo");
		Assert.assertTrue(StringUtils.countMatches(input, "creator") == 0, "wrong static file field - creator");
		Assert.assertTrue(StringUtils.countMatches(input, "internalUserGroups") == 0, "wrong static file field - internalUserGroups");
		Assert.assertTrue(StringUtils.countMatches(input, "description") == 0, "wrong static file field - description");
		Assert.assertTrue(StringUtils.countMatches(input, "rule") == 0, "wrong static file field - rule");
		Assert.assertTrue(StringUtils.countMatches(input, "owner") == 0, "wrong static file field - owner");
		Assert.assertTrue(StringUtils.countMatches(input, "rolloutPercentage") == 0, "wrong static file field - rolloutPercentage");
		Assert.assertTrue(StringUtils.countMatches(input, "defaultIfAirlockSystemIsDown") == 0, "wrong static file field - defaultIfAirlockSystemIsDown");
		Assert.assertTrue(StringUtils.countMatches(input, "configurationSchema") == 0, "wrong static file field - configurationSchema");
		Assert.assertTrue(StringUtils.countMatches(input, "configurationRules") == 0, "wrong static file field - configurationRules");
		Assert.assertTrue(StringUtils.countMatches(input, "orderingRules") == 0, "wrong static file field - orderingRules");
		
		
	}

	@Test (description = "create dev->dev, update to dev->prod as a tree")
	public void test3() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Production feature was not created under development feature: " + childID);
			
			String feature1 = f.getFeature(parentID, sessionToken);
			json = new JSONObject(feature1);
			json.put("stage", "DEVELOPMENT");
			JSONObject child = json.getJSONArray("features").getJSONObject(0);
			child.put("stage", "PRODUCTION");
			JSONArray newChildren = new JSONArray();
			newChildren.add(child);
			json.put("features", newChildren);
			
			String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "can't update parent feature to dev stage: " + response);
			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(validateProductionFeatures(responseDev.message, parentID)==1, "Incorrect number of development features");
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		/*	
			//static runtime files
			responseDev = RuntimeDateUtilities.getStaticDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			Assert.assertTrue(countFeatures(responseDev.message)==6, "Incorrect number of development features");
			responseProd = RuntimeDateUtilities.getStaticProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		*/	
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

	private int countFeatures(String input) throws JSONException{
		return StringUtils.countMatches(input, "namespace");
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
