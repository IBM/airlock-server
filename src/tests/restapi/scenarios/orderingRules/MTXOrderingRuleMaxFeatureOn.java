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

public class MTXOrderingRuleMaxFeatureOn {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String orderingRuleMtxID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
//F1->F2, MTXOR->OR1
	@Test (description = "Create mutually exclusive group of ordering rules with default maxFeaturesOn")
	public void createMutuallyExclusiveGroup() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		
		json.put("name", "parent"+RandomStringUtils.randomAlphabetic(3));		
		String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
		
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, parentID, sessionToken);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't create orderingRule  " + orderingRuleID);
		
	}
	
	
	@Test (dependsOnMethods="createMutuallyExclusiveGroup", description = "Increase maxFeatureOn to 3") 
	public void increaseMaxFeaturesOn() throws JSONException, IOException{
		String parent = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 3);
		f.updateFeature(seasonID, orderingRuleMtxID, json.toString(), sessionToken);
		
		parent = f.getFeature(orderingRuleMtxID, sessionToken);
		json = new JSONObject(parent);
		Assert.assertTrue(json.getInt("maxFeaturesOn")==3, "maxFeaturesOn was not updated");
	}
	
	@Test (dependsOnMethods="increaseMaxFeaturesOn", description = "Decrease maxFeatureOn to 1") 
	public void decreaseMaxFeaturesOn() throws JSONException, IOException{
		String parent = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 1);
		f.updateFeature(seasonID, orderingRuleMtxID, json.toString(), sessionToken);
		
		parent = f.getFeature(orderingRuleMtxID, sessionToken);
		json = new JSONObject(parent);
		Assert.assertTrue(json.getInt("maxFeaturesOn")==1, "maxFeaturesOn was not updated");
	}
	
	
	@Test (dependsOnMethods="decreaseMaxFeaturesOn", description = "Set maxFeatureOn to -1") 
	public void invalidMaxFeaturesOn() throws JSONException, IOException{
		String parent = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", -1);
		f.updateFeature(seasonID, orderingRuleMtxID, json.toString(), sessionToken);
		
		parent = f.getFeature(orderingRuleMtxID, sessionToken);
		json = new JSONObject(parent);
		Assert.assertFalse(json.getInt("maxFeaturesOn")==-1, "maxFeaturesOn was not updated");
	}
	
	@Test (dependsOnMethods="invalidMaxFeaturesOn", description = "Set maxFeatureOn to 0") 
	public void zeroMaxFeaturesOn() throws JSONException, IOException{
		String parent = f.getFeature(orderingRuleMtxID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 0);
		f.updateFeature(seasonID, orderingRuleMtxID, json.toString(), sessionToken);
		
		parent = f.getFeature(orderingRuleMtxID, sessionToken);
		json = new JSONObject(parent);
		Assert.assertFalse(json.getInt("maxFeaturesOn")==0, "maxFeaturesOn was not updated");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
