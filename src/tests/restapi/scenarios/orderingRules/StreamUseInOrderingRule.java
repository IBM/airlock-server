package tests.restapi.scenarios.orderingRules;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.scenarios.orderingRules.Config;

public class StreamUseInOrderingRule
{

	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String orderingRule;
	protected String productID;
	protected Config config;
	protected String featureID;
	private String streamID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "8.0");

		featureID = config.addFeature("feature1.txt", null, "ROOT", "8.0");
		
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		streamID = config.createStream("5.0");

		
	}

	@Test ( description="Stream field in rule")
	public void scenario1() throws Exception
	{
		
		//context.device.locale - 5.0, dev
		
		String orId1 = config.addFeature ("orderingRule/orderingRule1.txt", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsNumber == 10)", featureID,  "5.0");
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//schema field doesn't exist
		String response = config.addFeature ("orderingRule/orderingRule1.txt", "context.streams.video_played.newField == 10", featureID, "4.0");
		Assert.assertTrue(response.contains("error"), "OrderingRule was  created with stream field that doesn't exist: " + response);

		
		//1. move orderingRule field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateStream(streamID, "minAppVersion", "4.0") ;
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateStream(streamID, "minAppVersion", "6.0");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureField(orId1, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "OrderingRule version was updated");


        //3. move  field to production       
        //
        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "OrderingRule was updated to production" + response);

        response =  config.updateStream(streamID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Steam should be allowed to change - " + response);

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to production" + response);
        //move field to dev 
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to development: " + response);
 
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Stream was not moved to development - " + response);

        //delete input schema field used orderingRule
        int respCode =  config.deleteStream(streamID);
        Assert.assertNotEquals(respCode, 200, "Stream shouldn't be deleted - " + respCode);
        
 
	}

	
	@Test (dependsOnMethods="scenario1", description="Stream field used in configuration")
	public void scenario2() throws Exception
	{
		String childID1 = config.addFeature("feature1.txt", null, featureID, "8.0");
		
		String response =  config.updateStream(streamID, "minAppVersion", "5.0");
		Assert.assertFalse (response.contains("error"), "Can't update stream: " + response);
		
		//stream field - 5.0, dev
		String orId1 = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsNumber == 10)", featureID, "5.0", "{\"" + childID1 + "\":context.streams.video_played.adsNumber}");
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//schema field doesn't exist
		response = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", "(context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsNumber == 10)", featureID, "5.0", "{\"" + childID1 + "\":context.streams.video_played.newField}");
		Assert.assertTrue(response.contains("error"), "OrderingRule was created with non-existing stream field: " + response);
		
		//1. move orderingRule field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateStream(streamID, "minAppVersion", "4.0");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =   config.updateStream(streamID, "minAppVersion", "6.0");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureField(orId1, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "OrderingRule was updated");

        //3. move  field to production       
        //move context.device.locale to prod, notif in dev

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "OrderingRule was moved to production" + response);
 
        response =  config.updateStream(streamID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated" + response);
        //move field to dev 
        response =   config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated: " + response);
        
       //delete screenWidth from schema
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Stream was not updated - " + response);

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
