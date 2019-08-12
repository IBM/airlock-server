package tests.restapi.scenarios.orderingRules;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.scenarios.orderingRules.Config;

public class UtilityUseInOrderingRule
{

	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String orderingRule;
	protected String productID;
	protected Config config;
	protected String featureID;
	protected String utilityID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "8.0");

		featureID = config.addFeature("feature1.txt", null, "ROOT", "8.0");

		utilityID = config.addUtility("function isInt(){ return 1.0}");
		
	}

	@Test ( description="Utility used in rule")
	public void scenario1() throws Exception
	{

		
		//context.device.locale - 5.0, dev
		
		String orId1 = config.addFeature ("orderingRule/orderingRule1.txt", "isInt() == 5", featureID,  "5.0");
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//schema field doesn't exist
		String response = config.addFeature ("orderingRule/orderingRule1.txt", "noUtil()==5", featureID, "4.0");
		Assert.assertTrue(response.contains("error"), "OrderingRule was  created with utility that doesn't exist: " + response);
       
		
		//   move orderingRule to prod, utility in dev    
		response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "OrderingRule was updated to production" + response);

       //move utility to prod
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);
		
		//move orderingRule to production
		response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to production" + response);

        //move utility to dev
 		response = config.getUtility(utilityID);
 		utility = new JSONObject(response);
 		utility.put("stage", "DEVELOPMENT");
 		resp = config.updateUtility(utilityID, utility.toString());
 		Assert.assertTrue(resp.contains("error"), "Utility was updated to dev when feature is in prod: " + resp);

 		//4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to development: " + response);
        
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);
		
        //delete utility
        int respCode = config.deleteUtility(utilityID);
        Assert.assertNotEquals(respCode, 200,  "Utility was deleted ");
	}

	
	@Test (description="Utility used in configuration")
	public void scenario2() throws Exception
	{
		String childID1 = config.addFeature("feature1.txt", null, featureID, "9.0");

		
		//context.device.locale - 5.0, dev
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "isInt()");
		
		String orId1 = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", null, featureID, "5.0", configJson.toString());
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//side check that the conf is a legal json
		orderingRule = config.getFeature(orId1);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		String currentConfStr = jsonOR.getString("configuration");
		JSONObject currentConfJson = new JSONObject(currentConfStr);
		
		//schema field doesn't exist
		configJson = new JSONObject();
		configJson.put(childID1, "noUtil()");
		
		String response = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", null, featureID, "5.0", configJson.toString());
		Assert.assertTrue(response.contains("error"), "OrderingRule was created with non-existing schema field: " + response);
		
		//    move orderingRule to prod when utility in dev   
		response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "OrderingRule was updated to prod" + response);

        //3. move  field to production       
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated" + response);
       
        //move field to dev 
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertTrue(resp.contains("error"), "Utility was updated to dev when feature in prod: " + resp);
        
        //4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated: " + response);
        
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        
       //delete utility
        int respCode = config.deleteUtility(utilityID);
        Assert.assertNotEquals(respCode, 200,  "Utility was deleted ");

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
