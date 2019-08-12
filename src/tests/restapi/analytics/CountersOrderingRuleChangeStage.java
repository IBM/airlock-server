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

public class CountersOrderingRuleChangeStage {
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
	add dev feature with dev OR, report both to analytics
	move OR to production (prod under dev)
	move feature to prod
	move OR to dev
	remove OR from analytics
	remove parent feature from analytics
	 */

	@Test (description="Set quota to 5")
	public void aaUpdateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 5, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (description="add feature in production to analytics, change feature stage to development")
	public void addFeatureToAnalyticsScenario1() throws Exception{
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		//report parent feature and OR to analytics
		String response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not added to analytics" + response);

		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
		
		//move OR to production (prod under dev case)
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
		
		//move parent feature to production 
		String parent = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject parentJson = new JSONObject(parent);
		parentJson.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, featureID1, parentJson.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move parent feature to production" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		
		//move OR to development
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to development" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		//count OR as its parent is in production stage

		//remove OR from analytics

		response = 	an.deleteFeatureFromAnalytics(ORId1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove OR from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		//count OR as its parent is in production stage

		//remove parent feature from analytics

		response = 	an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove parent feature from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");		

	}
	
	
	
	/*
	add mtx with dev OR, report both to analytics
	move OR to production 
	move OR to dev
	remove OR from analytics
	remove mtx from analytics
	 */

	
	@Test (description="create MTX and OR and report to analytics to analytics")
	public void addFeatureToAnalyticsScenario2() throws Exception{
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false))  ;
		
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "mtx was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		//report parent feature and OR to analytics
		String response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX was not added to analytics" + response);

		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
		
		//move OR to production 
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
				
		//move OR to development
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to development" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");		

		//remove OR from analytics

		response = 	an.deleteFeatureFromAnalytics(ORId1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove OR from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");		

		//remove parent feature from analytics

		response = 	an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove parent feature from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");		

	}
	
	/*
	add dev feature with MTXOR_>dev OR, report both to analytics
	move OR to production (prod under dev)
	move feature to prod
	move OR to dev
	remove OR from analytics
	remove parent feature from analytics
	 */
	
	@Test (description="add feature  to analytics, change feature stage to development")
	public void addFeatureToAnalyticsScenario3() throws Exception{
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeatureToBranch(seasonID, branchID, orderingRuleMtx, featureID1, sessionToken);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		//report parent feature, MTXOR and OR to analytics
		String response = an.addFeatureToAnalytics(orderingRuleMtxID, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not added to analytics" + response);

		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
		
		//move OR to production (prod under dev case)
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
		
		//move parent feature to production 
		String parent = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject parentJson = new JSONObject(parent);
		parentJson.put("stage", "PRODUCTION");
		response = 	f.updateFeatureInBranch(seasonID, branchID, featureID1, parentJson.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move parent feature to production" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");
		
		//move OR to development
		orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		response = 	f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to development" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		//count OR as its parent is in production stage

		//remove OR from analytics

		response = 	an.deleteFeatureFromAnalytics(ORId1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove OR from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		//count OR as its parent is in production stage

		
		//remove MTXOR from analytics
		response = 	an.deleteFeatureFromAnalytics(orderingRuleMtxID, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove parent feature from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");		

		//remove parent feature from analytics
		response = 	an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Can't remove parent feature from analytics" + response);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");		

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
