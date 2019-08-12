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

public class AddPurchasesToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected BranchesRestApi br;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        br = new BranchesRestApi();
        br.setURL(url);
        m_branchType = branchType;
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
	
	//using separate API
	
	/*Test flow:
	 * add mix of features
	 * add configuration
	 * add mix of configurations
	 * add root
	 */
	

	@Test (description="Add mix to featuresAndConfigurationsForAnalytics")
	public void addMix() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add inAppPurchase MIX
		String ipMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, ipMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "inAppPurchase MIX was not added to the season");
			
		String response = an.addFeatureToAnalytics(mixId, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase MIX was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalytics(root, mixId, "entitlements"), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	

	@Test (dependsOnMethods="addMix", description="Add configuration to featuresAndConfigurationsForAnalytics")
	public void addConfiguration() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add inAppPurchase and configuration
		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false); 
		String inAppPurchaseId = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchase, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configId = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration, inAppPurchaseId, sessionToken);
		Assert.assertFalse(configId.contains("error"), "inAppPurchase was not added to the season");
		
		//add featureID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(configId, branchID, sessionToken);

		Assert.assertFalse(response.contains("error"), "Configuration rule was not added to analytics: " + response);

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalyticsConfiguration(root, inAppPurchaseId, configId, "entitlements"), "The field \"sendToAnalytics\" was not updated for inAppPurchase");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addConfiguration", description="Add configuration mix to featuresAndConfigurationsForAnalytics")
	public void addConfigurationMix() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature and configuration
		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false); 
		String inAppPurchaseId = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchase, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseId.contains("error"), "inAppPurchaseId was not added: " + inAppPurchaseId);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String configId = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration, inAppPurchaseId, sessionToken);
		Assert.assertFalse(configId.contains("error"), "mix was not added to the season");
		
		String response = an.addFeatureToAnalytics(configId, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX Configuration rule was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalyticsConfiguration(root, inAppPurchaseId, configId, "entitlements"), "The field \"sendToAnalytics\" was not updated for mix");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	
	
	@Test (dependsOnMethods="addConfigurationMix", description="Add root to featuresAndConfigurationsForAnalytics")
	public void addRootToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//find root id
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);

		if(!m_branchType.equals("Master")){
			String response = br.checkoutFeature(branchID,rootId,sessionToken);
		}

		String response = an.addFeatureToAnalytics(rootId, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration rule was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");

		if(m_branchType.equals("Master")) {
			JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
			Assert.assertTrue(root.containsKey("sendToAnalytics"), "The field \"sendToAnalytics\" was not updated for root");
		}else {
			Assert.assertTrue(new JSONObject(responseDev.message).getJSONArray("entitlements").getJSONObject(0).getBoolean("sendToAnalytics"), "The field \"sendToAnalytics\" was not updated for root");
		}
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	//children type is "features" or "entitlements"
	private boolean validateSentToAnalytics(JSONObject root, String featureId, String childrenType) throws JSONException{
		JSONArray features = root.getJSONArray(childrenType);
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(featureId)) {
				if (feature.has("sendToAnalytics"))
					return feature.getBoolean("sendToAnalytics");
			}	
		}
		return false;
	}
	
	
	private boolean validateSentToAnalyticsConfiguration(JSONObject root, String parentId, String featureId, String childrenType) throws JSONException{
		JSONArray features = root.getJSONArray(childrenType);
		//get the first feature
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(parentId)){
				JSONArray configs = feature.getJSONArray("configurationRules");
				for (Object c : configs) {
					JSONObject configuration = new JSONObject(c);
					if (configuration.getString("uniqueId").equals(featureId)) {
						if (configuration.has("sendToAnalytics"))
							return configuration.getBoolean("sendToAnalytics");
					}
				}
			}	
		}
		
		return false;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
