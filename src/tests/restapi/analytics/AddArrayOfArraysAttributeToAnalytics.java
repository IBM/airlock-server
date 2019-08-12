package tests.restapi.analytics;

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
import tests.restapi.*;


public class AddArrayOfArraysAttributeToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String configID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}

	


	@Test (description="Add feature and configuration rule to the season and to analytics")
	public void addComponents() throws IOException, JSONException, InterruptedException{
			
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add configuration: "display":[\"color\":[1,2,3], \"size\":[1,2,3], \"text\":[1,2,3]]
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		JSONArray arr = new JSONArray();
		JSONObject properties= new JSONObject();

		JSONArray intArray = new JSONArray();
		intArray.add(1);
		intArray.add(2);
		intArray.add(3);
		
		properties.put("color", intArray);
		properties.put("size", intArray);
		properties.put("text", intArray);
		arr.put(properties);
		newConfiguration.put("display", arr);
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
	}
	
	@Test (dependsOnMethods="addComponents", description="Add complete array to analytics")
	public void addArrayToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[0]");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);

		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID,  branchID,"DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="addArrayToAnalytics", description="Add existing json object property to analytics")
	public void addAttributeFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[0].text");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);

		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addAttributeFieldToAnalytics", description="Add several json object property to analytics")
	public void addSeveralAttributeFieldsToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[0].color");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "display[0].text");
		attr2.put("type", "ARRAY");
		attributes.add(attr2);

		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	@Test (dependsOnMethods="addSeveralAttributeFieldsToAnalytics", description="Add non existing json object property to analytics")
	public void addInvalidAttributeFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[0].mytitle");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);

		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);
	}
	
	@Test (dependsOnMethods="addInvalidAttributeFieldToAnalytics", description="Add property with non-existing index to analytics")
	public void addIndexAttributeFieldToAnalytics() throws IOException, JSONException, InterruptedException{

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[10].color");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);

		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Didn't update analytics" + response);

	}
	
	@Test (dependsOnMethods="addIndexAttributeFieldToAnalytics", description="Add some attributes to analytics")
	public void addPartOfAttributesToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "display[1,2].color[0-2]");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);
		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==6, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	private boolean validateFeatureInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics"))
					return true;
			}
		}
		return false;
	}
	
	private JSONArray getAttributesInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics")){
					return feature.getJSONArray("configAttributesForAnalytics");
				}
					
			}
		}
		return new JSONArray(); //if no configAttributesToAnalytics
	}

	private JSONArray validateAttributeInAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		return featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");

	}
	



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
