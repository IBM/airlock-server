package tests.restapi.scenarios.orderingRules;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.scenarios.orderingRules.Config;

public class InputSchemaUseInOrderingRule
{

	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String orderingRule;
	protected String productID;
	protected Config config;
	protected String featureID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "8.0");

		featureID = config.addFeature("feature1.txt", null, "ROOT", "8.0");

		
	}

	@Test ( description="Schema field used in rule")
	public void scenario1() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		
		String orId1 = config.addFeature ("orderingRule/orderingRule1.txt", "context.device.locale == \"value\"", featureID,  "5.0");
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//schema field doesn't exist
		String response = config.addFeature ("orderingRule/orderingRule1.txt", "context.device.newfield == \"value\"", featureID, "4.0");
		Assert.assertTrue(response.contains("error"), "OrderingRule was  created with schema field that doesn't exist: " + response);

		
		//1. move orderingRule field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureField(orId1, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "OrderingRule version was updated");


        //3. move  field to production       
        //move context.device.locale to prod, notif in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to production" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated to development: " + response);
        
        //delete input schema field used orderingRule
        response =  config.updateSchema("validationsTests/inputSchema_update_device_no_locale.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //use non-existing schema field
        response = config.updateFeature(orId1,  "context.device.newfield == \"value\"");
        Assert.assertTrue(response.contains("error"), "OrderingRule should not be updated with non-existing field: " + response);

	}

	
	@Test (description="Schema field used in configuration")
	public void scenario2() throws Exception
	{
		String childID1 = config.addFeature("feature1.txt", null, featureID, "9.0");
		
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		
		String orId1 = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", null, featureID, "5.0", "{\"" + childID1 + "\":context.device.screenWidth}");
		Assert.assertFalse(orId1.contains("error"), "OrderingRule was not created: " + orId1);
		
		//schema field doesn't exist
		String response = config.addFeatureWithConfiguration ("orderingRule/orderingRule1.txt", null, featureID, "5.0", "{\"" + childID1 + "\":context.device.newField}");
		Assert.assertTrue(response.contains("error"), "OrderingRule was created with non-existing schema field: " + response);
		
		//1. move orderingRule field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureField(orId1, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "OrderingRule was updated");

        //3. move  field to production       
        //move context.device.locale to prod, notif in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureField(orId1, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move orderingRule to dev
        response = config.updateFeatureField(orId1, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "OrderingRule was not updated: " + response);
        
       //delete screenWidth from schema
        response =  config.updateSchema("validationsTests/inputSchema_update_device_no_screenWidth.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
