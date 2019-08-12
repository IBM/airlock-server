package tests.restapi.analytics_in_purchases;

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


public class AddAttributesToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String inAppPurchaseID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	//protected FeaturesRestApi f;
	protected InAppPurchasesRestApi purchasesApi;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String purchaseOptionsID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		//f = new FeaturesRestApi();
		//f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		schema = new InputSchemaRestApi();
		schema.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);


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

	@Test (description="Add inAppPurchase and 2 configuration rules to the season")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchase, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season" + inAppPurchaseID1);

		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		//add second configuration
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "test");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);


		//add inAppPurchase and attribute to analytics
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
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		String response = an.addAttributesToAnalytics(inAppPurchaseID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "attributes were not added to analytics" + response);


		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		//Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, inAppPurchaseID1, "entitlements").size()==3, "Incorrect number of attributes in runtime development file ");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}

	@Test (dependsOnMethods="addComponents", description="Remove reported attributes from analytics")
	public void removeAttributesFromAnalytics() throws IOException, JSONException, InterruptedException{

		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(inAppPurchaseID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);

		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated");
	}

	@Test (dependsOnMethods="removeAttributesFromAnalytics", description="add purchaseOptions, configurations and send to analytics")
	public void testPurcahseOptionsAttributes() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purchaseOptions, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "purchaseOptions was not added to the season" + purchaseOptionsID);

		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("name", "c2");
		
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), purchaseOptionsID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		//add second configuration
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "test");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("name", "c3");
		
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), purchaseOptionsID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);


		//add inAppPurchase and attribute to analytics
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
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		String response = an.addAttributesToAnalytics(purchaseOptionsID, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "attributes were not added to analytics" + response);


		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root.getJSONArray("entitlements").getJSONObject(0), purchaseOptionsID, "purchaseOptions").size()==3, "Incorrect number of attributes in runtime development file ");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}

	@Test (dependsOnMethods="testPurcahseOptionsAttributes", description="Remove reported attributes from analytics")
	public void removePurOptAttributesFromAnalytics() throws IOException, JSONException, InterruptedException{

		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(purchaseOptionsID, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);

		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated");
	}


	private JSONArray getAttributesInRuntime(JSONObject root, String id, String childrenType) throws JSONException{
		JSONArray features = root.getJSONArray(childrenType);
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
