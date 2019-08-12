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


public class AddCustomAttributesToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
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
	
	//check add attributes as a separate action in api
	
	@Test (description="Add feature and 2 configuration rules to the season")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

	}

	
	@Test (dependsOnMethods="addComponents", description="Add custom attributes to analytics")
	public void addCustomAttributes() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "CUSTOM");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "CUSTOM");
		attributes.add(attr3);
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		//Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==3, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addCustomAttributes", description="Update cutom attribute")
	public void updateCustomAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color[1]");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "title[5].text");
		attr2.put("type", "CUSTOM");
		attributes.add(attr2);

		JSONObject attr3 = new JSONObject();
		attr3.put("name", "mysize.small");
		attr3.put("type", "CUSTOM");
		attributes.add(attr3);
		
		featuresAttributesToAnalytics.getJSONObject(0).put("attributes", attributes);		
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(validateAttributeInAnalytics(anResponse).contains("newtitle"), "Updated (new renamed) attribute found in analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	
	@Test (dependsOnMethods="updateCustomAttribute", description="Remove reported custom attributes from analytics")
	public void removeAttributesFromAnalytics() throws IOException, JSONException, InterruptedException{
		
		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated");
	}

	
	@Test (dependsOnMethods="removeAttributesFromAnalytics", description="Add illegal cutom name")
	public void illegalCutomAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color[0,1,2]");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);		
		
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "color[0-2]");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);		
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "my color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "my.color.");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);

		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "1.attribute");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);

		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "attribute#");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was updated with illegal custom attribute");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		JSONObject jsonDisplay = new JSONObject(display);
		Assert.assertTrue(jsonDisplay.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").size()==0, "Analytics was updated with illegal custom attribute");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="illegalCutomAttribute", description="Update to illegal cutom name")
	public void updateIllegalCutomAttribute() throws JSONException, IOException, InterruptedException{
		JSONArray attributes = new JSONArray();
		
		//add legal custom attribute
		attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);		

		//update to illegal custom attribute
		attr1 = new JSONObject();
		attr1.put("name", "color[0,1,2]");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);		
		
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "color[0-2]");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);		
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "my color");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);
		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "my.color.");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);

		
		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "1.attribute");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);

		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "attribute#");
		attr1.put("type", "CUSTOM");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to analytics" + response);
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").getJSONObject(0).getJSONArray("attributes").getJSONObject(0).getString("name").equals("color"), "1. Analytics was updated with illegal custom attribute");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		JSONObject jsonDisplay = new JSONObject(display);
		
		Assert.assertTrue(jsonDisplay.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").getJSONObject(0).getJSONArray("attributes").getJSONObject(0).getString("name").equals("color"), "2. Analytics was updated with illegal custom attribute");

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
