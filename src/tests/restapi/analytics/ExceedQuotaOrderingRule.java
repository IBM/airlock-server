package tests.restapi.analytics;

import java.io.IOException;





import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class ExceedQuotaOrderingRule {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String ORId1;
	protected String ORId2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String m_branchType;
	private ExperimentsRestApi expApi;
	
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
		expApi = new ExperimentsRestApi();
		expApi.setURL(analyticsUrl);
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
		
		m_branchType = branchType;
	}
	
	/*
	add dev feature with dev OR, report both to analytics
	move OR to production (prod under dev)
	move feature to prod
	move OR to dev
	remove OR from analytics
	remove parent feature from analytics
	 */

	//F1->F2, OR1
	@Test (description="Set quota to 2")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="add feature in production to analytics")
	public void addProdFeaturesToAnalytics() throws Exception{
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		feature.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		feature.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);


		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		
		//report F2 to analytics - 1 production item
		String response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature2 was not added to analytics" + response);

		//report F1 to analytics - exceeds quota, as OR is counted with F1, 2 production items
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Parent feature was not added to analytics" + response);
	
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addProdFeaturesToAnalytics", description="Set quota to 3")
	public void updateQuotaTo3() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuotaTo3", description="Add ordering rule to analytics")
	public void addOrderingRuleToAnalytics() throws IOException, JSONException, InterruptedException{
		
		//report F1 to analytics - reach quota, as OR is counted with F1, 2 production items
		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not added to analytics" + response);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

		//move OR to production (prod under dev case)
		String orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production" + response);
		
		//report OR to analytics
		response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Ordering rule was added to analytics and quota was exceeded " + response);

		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		
	}
	@Test (dependsOnMethods="addOrderingRuleToAnalytics", description="Remove feature from analytics")
	public void removeFeatureAnalytics() throws IOException, JSONException, InterruptedException{

		//remove F2 from analytics

		String response = 	an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove feature2 from analytics" + response);
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		
		

		//report OR to analytics
		response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Ordering rule was added to analytics and quota was exceeded " + response);

		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="removeFeatureAnalytics", description="Add new ordering rule")
	public void addOrderingRule2() throws IOException, JSONException, InterruptedException{

		//add ordering rule in dev stage
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		ORId2 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), ORId1, sessionToken);
		Assert.assertFalse(ORId2.contains("error"), "Ordering rule2 was not added to the season" + ORId2);
		
		//report OR to analytics
		String response = an.addFeatureToAnalytics(ORId2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Ordering rule was not added to analytics " + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

	}

	
	@Test (dependsOnMethods="addOrderingRule2", description="Move ordering rule to production")
	public void moveOrderingRuleToProduction() throws Exception{

		String orderingRule = f.getFeatureFromBranch(ORId2, branchID, sessionToken);
		JSONObject jsonOR2 = new JSONObject(orderingRule);
		jsonOR2.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, ORId2, jsonOR2.toString(), sessionToken);
	
			if(m_branchType.equals("Master") || m_branchType.equals("ProdExp")) {
				Assert.assertTrue(response.contains("error"), "Moved ordering rule to production when quota limit is reached");
				String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
				
				Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
				Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

			}
			else {
				Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production over limit when running StandAlone or DevExp");
				String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
				
				Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
				Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");

			}

	}
	
	@Test (dependsOnMethods="moveOrderingRuleToProduction", description="Move dev experiment to production")
	public void moveExperimentToProduction() throws Exception{
		//test relevant only for mode DevExp. Quota is exceeded
		if(m_branchType.equals("DevExp")){ 
			//move experiment to production
			String experiments = expApi.getAllExperiments(productID, sessionToken);
			JSONObject json = new JSONObject(experiments);
			String experimentID = json.getJSONArray("experiments").getJSONObject(0).getString("uniqueId");
			String experiment = expApi.getExperiment(experimentID, sessionToken);
			JSONObject jsonExp = new JSONObject(experiment);
			jsonExp.put("stage", "PRODUCTION");
			String response = expApi.updateExperiment(experimentID, jsonExp.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment was not moved to production");
			
			String variantId = jsonExp.getJSONArray("variants").getJSONObject(0).getString("uniqueId");
			String variant = expApi.getVariant(variantId, sessionToken);
			JSONObject jsonVariant = new JSONObject(variant);
			jsonVariant.put("stage", "PRODUCTION");
			response = expApi.updateVariant(variantId, jsonVariant.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Variant was  moved to production with exceeded quota limitation");
		}
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
