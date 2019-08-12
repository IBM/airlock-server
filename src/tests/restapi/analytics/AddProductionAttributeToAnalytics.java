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


public class AddProductionAttributeToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String configID1;
	protected String filePath;
	protected String m_url;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
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
		m_branchType = branchType;
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
	
	/*Test flow:
	 * 
	 * 	add 2 production attributes to analytics 
	 * 	update production attribute1 name in configuration
	 * 	delete production attribute1 from analytics - the field "configAttributesToAnalytics" should be updated
	 * 	update configuration from prod to dev - check in runtime
	 * 	delete development configuration (feature in prod) when it is reported to analytics
	 * 
	 */
	


	@Test (description="Add feature and 2 configuration rules to the season and to analytics")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		newConfiguration.put("text", "medium");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("stage", "PRODUCTION");
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "text");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);

		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==3, "Incorrect number of production items");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==3, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime production file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==3, "Incorrect number of attributes in runtime production file ");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="addComponents", description="Update configuration in configuration rule and validate that analytics is updated")
	public void updateConfigurationAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("newsize", "test");
		newConfiguration.put("color", "red");
		newConfiguration.put("title", "newtitle");
		jsonConfig.put("configuration", newConfiguration);
		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeNameInAnalytics(anResponse, "size"), "Updated (old renamed) attribute  was not found in analytics");
		Assert.assertFalse(validateAttributeNameInAnalytics(anResponse, "newsize"), "Updated (new renamed) attribute found in analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==3, "Incorrect number of production items");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		//Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).contains("size"), "Attribute was not updated in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		//Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime production file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).contains("size"), "Attribute was not updated in runtime production file ");

		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}	
	
	
	@Test (dependsOnMethods="updateConfigurationAttribute", description="Delete attribute from analytics")
	public void deleteAttributeFromAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject analytics = new JSONObject(response);
		JSONObject featuresAttributesToAnalytics = analytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").getJSONObject(0);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "title");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		featuresAttributesToAnalytics.remove("attributes");
		featuresAttributesToAnalytics.put("attributes", attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, analytics.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not updated in analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==1, "Incorrect number of production items");

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");
		Assert.assertFalse(getAttributesInRuntime(root, featureID1).contains("color"), "Incorrect attribute in runtime development file ");
		

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime production file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime production file ");
		Assert.assertFalse(getAttributesInRuntime(root, featureID1).contains("color"), "Incorrect attribute in runtime production file ");

		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="deleteAttributeFromAnalytics", description="Delete all feature attributes from analytics")
	public void deleteAllAttributesFromAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject analytics = new JSONObject(response);
		analytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").remove(0);
		
		response = an.updateGlobalDataCollection(seasonID, branchID, analytics.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not updated in analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==0, "Incorrect number of production items");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not removed in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not removed in runtime production file ");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	

	@Test (dependsOnMethods="deleteAllAttributesFromAnalytics", description="Update configuration from prod to dev")
	public void updateConfigurationToDevelopment() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature and attribute to analytics as it was deleted by the previous test
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "newsize");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
	
		
		String responseUpdate = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(responseUpdate);
		jsonConfig.put("stage", "DEVELOPMENT");
		responseUpdate = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(responseUpdate.contains("error"), "Configuration stage was not updated" + responseUpdate);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==2, "Incorrect number of production items");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was incorrectly updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was incorrectly updated");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="updateConfigurationToDevelopment", description="delete development configuration (feature in prod)")
	public void deleteConfigurationReportedToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(configID1, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "Feature was not deleted");
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==1, "Analytics was not updated");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, analytics).equals("true"));

		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(display)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(display)==2, "Incorrect number of production items");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not deleted");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was  changed");

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
	
	private boolean validateAttributeNameInAnalytics(String analytics, String attributeName) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		boolean attrExists = false;
		for(int i =0; i < featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes").size(); i++){
			JSONObject attr = featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes").getJSONObject(i);
			if (attr.getString("name").equals(attributeName))
				attrExists = true;
		}
		
		return attrExists;
	}
	
	private int getDevelopmentItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getInt("developmentItemsReportedToAnalytics");
	}
	
	private int getProductionItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		//System.out.println("productionItem = " + json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics"));
		return json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics");
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
