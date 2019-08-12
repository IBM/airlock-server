package tests.restapi.scenarios.orderingRules;

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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class OrderingRuleFullCycle {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String orderingRule;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
	}
	
	/*
	 * create OR under MTX, update, delete
	 * create OR under feature, update delete
	 * create MTXOR under MTX, update, delete
	 * create MTXOR under feature, update, delete
	 */

	@Test (description = "create OR under feature, update, delete")
	public void orderingRuleUnderFeature() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature); 
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error") || featureID.contains("Invalid") , "Can't create feature: " + featureID	);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under another feature: " + orderingRuleID	);	
		
		String orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("description", "new desc or");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under another feature: " + response);
		
		int respCode = f.deleteFeature(orderingRuleID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete ordering rule under feature");

	}
	
	@Test (description = "Create orderingRule under MTX")
	public void orderingRuleUnderMTX() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under mtx: " + orderingRuleID);
		
		String orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("description", "new desc or");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under mtx: " + response);
		
		int respCode = f.deleteFeature(orderingRuleID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete ordering rule under mtx");
	}
	

	@Test (description = "create MTXOR under feature, update, delete")
	public void mtxOrderingRuleUnderFeature() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature); 
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error") || featureID.contains("Invalid") , "Can't create feature: " + featureID	);
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, featureID, sessionToken);
		Assert.assertFalse(orderingRuleMtxID.contains("error") || orderingRuleMtxID.contains("Invalid") , "Can't create Ordering rule MTX under another feature: " + orderingRuleMtxID);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under another feature: " + orderingRuleID);
		
		String orderingRule4Update = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule4Update);
		jsonOR.put("description", "new desc or");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under another feature: " + response);
		
		orderingRuleMtx = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject mtxJsonOR = new JSONObject(orderingRuleMtx);
		mtxJsonOR.put("maxFeaturesOn", 2);
		response = f.updateFeature(seasonID, orderingRuleMtxID, mtxJsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under another feature: " + response);
		
		int respCode = f.deleteFeature(orderingRuleID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete ordering rule under feature");
		
		
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under another feature: " + orderingRuleID);

		respCode = f.deleteFeature(orderingRuleMtxID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete mtx ordering rule under feature");
	}
	
	
	@Test (description = "create MTXOR under feature, update, delete")
	public void mtxOrderingRuleUnderMTX() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, mtxID, sessionToken);
		Assert.assertFalse(orderingRuleMtxID.contains("error") || orderingRuleMtxID.contains("Invalid") , "Can't create Ordering rule MTX under another feature: " + orderingRuleMtxID);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under another feature: " + orderingRuleID);
		
		String orderingRule4Update = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule4Update);
		jsonOR.put("description", "new desc or");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under another feature: " + response);
		
		orderingRuleMtx = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject mtxJsonOR = new JSONObject(orderingRuleMtx);
		mtxJsonOR.put("maxFeaturesOn", 2);
		response = f.updateFeature(seasonID, orderingRuleMtxID, mtxJsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't update Ordering rule under another feature: " + response);
		
		int respCode = f.deleteFeature(orderingRuleID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete ordering rule under feature");
		
		
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error") || orderingRuleID.contains("Invalid") , "Can't create Ordering rule under another feature: " + orderingRuleID);

		respCode = f.deleteFeature(orderingRuleMtxID, sessionToken);
		Assert.assertEquals(respCode,  200, "Can't delete mtx ordering rule under feature");
	}
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
