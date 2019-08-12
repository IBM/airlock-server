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


public class CustomAttributesScenarios {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
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
	
	/*
	 * - add feature with custom attribute, change to production, change to development, delete
	 * - add feature + CR1 + custom attribute, update CR1 attributes, change to production, change to development, delete CR1, delete feature
	 * - add feature + CR1 + custom attribute, move CR1 to other parent
	 */
	
	
	@Test (description="add feature with custom attribute, change to production, change to development, delete feature")
	public void scenario1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//change feature stage to production
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		//Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		JSONObject rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(getAttributesInRuntime(rootProd, featureID1).size()==1, "Incorrect number of attributes in runtime production file ");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
		//change feature stage to development
		dateFormat = an.setDateFormat();

		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");		
		
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(getAttributesInRuntime(rootProd, featureID1).size()==0, "Incorrect number of attributes in runtime production file ");
		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}

		//delete feature
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode ==200, "Feature was not deleted");
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==0, "Custom attribute was not deleted from analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	
	@Test (dependsOnMethods="scenario1", description="add feature + CR1 + custom attribute, update CR1 attributes, change to production, change to development, delete CR1, delete feature")
	public void scenario2() throws IOException, JSONException, InterruptedException{
		
		String dateFormat = an.setDateFormat();

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("size", "small");
		newConfiguration.put("title", "my title");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		
		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);

		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//change feature stage to production
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==3, "Incorrect number of attributes in runtime development file ");		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		JSONObject rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(getAttributesInRuntime(rootProd, featureID1).size()==3, "Incorrect number of attributes in runtime production file ");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
		
		//change CR1 stage to production
		config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//update configuration attribute
		config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("newtitle", "my new title");
		newConfiguration.put("size", "large");
		jsonConfig.put("configuration", newConfiguration);
		response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//change CR1 stage to development
		config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "DEVELOPMENT");
		response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//delete CR1
		int responseCode = f.deleteFeatureFromBranch(configID1, branchID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Configuration rule was not deleted");
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//change feature stage to development
		dateFormat = an.setDateFormat();//RuntimeDateUtilities.getCurrentTimeStamp();
		an.setSleep();
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==3, "Incorrect number of attributes in runtime development file ");		
		
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(getAttributesInRuntime(rootProd, featureID1).size()==0, "Incorrect number of attributes in runtime production file ");
		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}

		//delete feature
		responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode ==200, "Feature was not deleted");
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==0, "Custom attribute was not deleted from analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	
	
	@Test (dependsOnMethods="scenario2", description="add feature + CR1 + custom attribute, move CR1 to other parent")
	public void scenario3() throws IOException, JSONException, InterruptedException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		//add configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		
		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);

		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==2, "Incorrect number of attributes in analytics");
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add feature2
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);
		
		//move configuration rule to feature2
		feature2 = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(feature2);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==2, "Incorrect number of attributes in analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//delete feature2
		int responseCode = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(responseCode ==200, "Feature2 was not deleted");
		
		//delete feature1
		responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode ==200, "Feature1 was not deleted");
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==0, "Custom attribute was not deleted from analytics");
		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

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
		if (featuresAttributesToAnalytics.size() > 0)
			return featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");
		else 
			return new JSONArray();

	}
	



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
