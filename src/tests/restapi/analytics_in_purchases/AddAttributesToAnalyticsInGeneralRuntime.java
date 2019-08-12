package tests.restapi.analytics_in_purchases;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class AddAttributesToAnalyticsInGeneralRuntime {
	protected String seasonID;
	protected String productID;
	protected String entitlementID1;
	protected String entitlementID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
	private String m_analyticsUrl;
	protected InAppPurchasesRestApi purchasesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	//check add attributes as a separate action in api
	/*
	 * - add dev & prod attr in master
	 * - report attribute in master
	 * - check out entitlements
	 * - add attribute to analytics in branch
	 * - remove attribute to analytics from master
	 * - remove attributes to analytics from branch
	 */
	
	@Test (description="Add entitlements and 2 configuration rules to the season")
	public void addComponents() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		JSONObject entitlement2 = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false));
		entitlement2.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to the season" + entitlementID1);

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement1, entitlementID2, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added to the season" + entitlementID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		//add second configuration
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "test");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);

	}
	
	@Test (dependsOnMethods="addComponents", description="Add attributes to analytics in master")
	public void addAttributesToAnalyticsInMaster() throws Exception{
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		String response = an.addAttributesToAnalytics(entitlementID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);				
		JSONArray attributesProd = new JSONArray();
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributesProd.add(attr3);
		
		response = an.addAttributesToAnalytics(entitlementID2, BranchesRestApi.MASTER, attributesProd.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		

		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==2, "Incorrect number of attributes in analytics");

		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==2, "Incorrect number of attributes in analytics in branch");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development entitlements file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production entitlements file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//since no entitlement was checked out - verify that no entitlements exist in branch in general runtime files
		Assert.assertTrue (numberOfEntitlementsInBrancheInGenaralRuntime(responseDev.message) == 0, "several entitlements exist in branche in general dev runtime file even though no entitlement was checked-out");
		Assert.assertTrue (numberOfEntitlementsInBrancheInGenaralRuntime(responseProd.message) == 0, "several entitlements exist in branche in general prod runtime file even though no entitlement was checked-out");
				
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="addAttributesToAnalyticsInMaster", description="checkout entitlement")
	public void checkoutEntitlement() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed checkout");
		response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed checkout");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development entitlements file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production entitlements file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID1)==1, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID2)==1, "Incorrect number of attributes in dev runtime for branches");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseProd.message, entitlementID2)==1, "Incorrect number of attributes in dev runtime for branches");
		
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
	}

	@Test (dependsOnMethods="checkoutEntitlement", description="Add attributes to analytics in branch")
	public void addAttributesToAnalyticsInBranch() throws Exception{
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		
		String response = an.addAttributesToAnalytics(entitlementID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);				
				
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==2, "Incorrect number of attributes in analytics");

		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==3, "Incorrect number of attributes in analytics in branch");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID1)==2, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID2)==1, "Incorrect number of attributes in dev runtime for branches");
				
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}

	@Test (dependsOnMethods="addAttributesToAnalyticsInBranch", description="Remove attributes from analytics in master")
	public void removeAttributesFromAnalyticsInMaster() throws Exception{
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		
		
		String response = an.addAttributesToAnalytics(entitlementID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not removed from analytics" + response);				
		
		response = an.addAttributesToAnalytics(entitlementID2, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not removed from analytics" + response);				
		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics");

		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==3, "Incorrect number of attributes in analytics in branch");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID1)==2, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID2)==1, "Incorrect number of attributes in dev runtime for branches");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseProd.message, entitlementID2)==1, "Incorrect number of attributes in dev runtime for branches");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}

	
	@Test (dependsOnMethods="removeAttributesFromAnalyticsInMaster", description="Remove attributes from analytics in branch")
	public void removeAttributesFromAnalyticsInBranch() throws Exception{
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		
		
		String response = an.addAttributesToAnalytics(entitlementID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not removed from analytics" + response);				
		
		response = an.addAttributesToAnalytics(entitlementID2, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not removed from analytics" + response);				
		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics");

		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics in branch");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID1)==0, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseDev.message, entitlementID2)==0, "Incorrect number of attributes in dev runtime for branches");
		
		Assert.assertTrue(getAttributesInRuntimeInBranch(responseProd.message, entitlementID2)==0, "Incorrect number of attributes in dev runtime for branches");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
	}


	private int numberOfEntitlementsInBrancheInGenaralRuntime(String result) throws Exception{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("branches")){
				org.apache.wink.json4j.JSONArray branchesArray = json.getJSONArray("branches");
				if (branchesArray == null || branchesArray.size() == 0) {
					throw new Exception("Response doesn't contain branches array");
				}
					
				JSONObject branch = branchesArray.getJSONObject(0);
				if (branch.containsKey("entitlements")){
					org.apache.wink.json4j.JSONArray entitlementsArray = branch.getJSONArray("entitlements");
					if (entitlementsArray == null || entitlementsArray.size() == 0) {
						return 0;
						
					}
					return entitlementsArray.size();
				}
				else {
					return 0;
				}
			} else {
				throw new Exception( "Response doesn't contain branches array");
			}
		} catch (Exception e){
				throw new Exception( "Response is not a valid json");
		}
	}


	private int getAttributesInRuntimeInBranch(String input, String entitlementsId) throws JSONException{
		//runtime dev/prod branches section
		JSONArray entitlements = new JSONObject(input).getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements");
		for (int i=0; i< entitlements.size(); i++){
			JSONObject entitlementObj =  entitlements.getJSONObject(i);
			if (entitlementObj.getString("uniqueId").equals(entitlementsId) && entitlementObj.containsKey("configAttributesForAnalytics")){
				return entitlementObj.getJSONArray("configAttributesForAnalytics").size();
			}
			if (entitlementObj.containsKey("entitlements")) {
				JSONArray subentitlements = entitlementObj.getJSONArray("entitlements");
				for (int j=0; j< subentitlements.size(); j++){
					JSONObject subentitlementObj =  subentitlements.getJSONObject(j);
					if (subentitlementObj.getString("uniqueId").equals(entitlementsId) && subentitlementObj.containsKey("configAttributesForAnalytics")){
						return subentitlementObj.getJSONArray("configAttributesForAnalytics").size();
					}
				}
			}
		}	
		return 0; //if no configAttributesToAnalytics
	}
	/*
	private int getAttributesInFeatureRuntimeInExperiment(String input, String featureId) throws JSONException{
		//runtime dev/prod experiments section
		JSONObject feature = new JSONObject(purchasesApi.getPurchaseItemFromBranch(featureId, branchID, sessionToken));
		String name = feature.getString("namespace") + "." + feature.getString("name");
		
		JSONArray features = new JSONObject(input).getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAttributesForAnalytics");
		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals(name) && features.getJSONObject(i).containsKey("attributes")){
				return features.getJSONObject(i).getJSONArray("attributes").size();
			}
		}	
		return 0; //if no attributes
	}
	
	private int getAttributesInRuntimeBranch(String input, String featureId ) throws JSONException{
		//runtime branches
		JSONArray features = new JSONObject(input).getJSONArray("features");

		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureId) && features.getJSONObject(i).containsKey("configAttributesForAnalytics")){
				return features.getJSONObject(i).getJSONArray("configAttributesForAnalytics").size();
			}
		}	
		return 0; //if no configAttributesToAnalytics
	}
*/

	private int validateAttributeInAnalytics(String analytics) throws JSONException{
		//runtime dev/prod
		int attributes=0;
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		for(int i=0; i<featuresAttributesToAnalytics.size(); i++){
			attributes = attributes + featuresAttributesToAnalytics.getJSONObject(i).getJSONArray("attributes").size();
		}	
		return attributes;
	}
	
	private String addExperiment(String experimentName, boolean enabled) throws IOException, JSONException{
		return baseUtils.addExperiment(m_analyticsUrl, true, enabled);

	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
