package tests.restapi.scenarios.validations;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class InputSchemaFeatureScenarios
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		
	}

	@Test ( description="F1 -> F2, F3")
	public void scenario1() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		String featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
	
		String featureID1 = config.addFeature ("feature1.txt", "context.device.locale == \"US\"", featureID, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String featureID2 = config.addFeature ("feature1.txt", "context.device.osVersion == \"value\"", featureID, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureWithMinVersion(featureID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, feature in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        
        response = config.updateFeatureStage(featureID, "DEVELOPMENT"); 
        
        //5. move parent stage/minversion
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0_datetime_dev.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureStage(featureID, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was updated");
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0_datetime_dev.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        response = config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureWithMinVersion(featureID, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");

        
        int respCode = config.deleteFeature(featureID);
        Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	
	@Test ( description="F1 -> MTX ->( F2, F3)")
	public void scenario2() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		String featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		String mtxId = config.addMTX("feature-mutual.txt", featureID) ;
	
		String featureID1 = config.addFeature ("feature1.txt", "context.device.locale == \"US\"", mtxId, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String featureID2 = config.addFeature ("feature1.txt", "context.device.osVersion == \"value\"", mtxId, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureWithMinVersion(featureID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, feature in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        
        response = config.updateFeatureStage(featureID, "DEVELOPMENT"); 
        
        //5. move parent stage/minversion
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0_datetime_dev.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureStage(featureID, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was updated");
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0_datetime_dev.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        response = config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureWithMinVersion(featureID, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");

        
        int respCode = config.deleteFeature(featureID);
        Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	@Test ( description="F1 -> MTX ->( F2->MTX->(F4, F5), F3); test F3, F5")
	public void scenario3() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		String featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		
		String mtxId = config.addMTX("feature-mutual.txt", featureID) ;
		String featureID2 = config.addFeature ("feature1.txt", "context.device.osVersion == \"US\"", mtxId, "5.0");
		String featureID3 = config.addFeature ("feature1.txt", "context.device.locale == \"US\"", mtxId, "5.0");
		
		
		String mtxId2 = config.addMTX("feature-mutual.txt", featureID2) ;
		String featureID4 = config.addFeature ("feature1.txt", "context.device.osVersion == \"US\"", mtxId2, "5.0");
		String featureID5 = config.addFeature ("feature1.txt", "context.device.locale == \"value\"", mtxId2, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateFeatureWithMinVersion(featureID5, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");

        response = config.updateFeatureWithMinVersion(featureID3, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");

        //3. move child field to production       
        //move context.device.locale to prod, feature in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID2, "PRODUCTION");
        response = config.updateFeatureStage(featureID3, "PRODUCTION");
        response = config.updateFeatureStage(featureID5, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(featureID5, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        response = config.updateFeatureStage(featureID2, "DEVELOPMENT");
        response = config.updateFeatureStage(featureID3, "DEVELOPMENT");
        response = config.updateFeatureStage(featureID, "DEVELOPMENT"); 
        
        //5. move parent stage/minversion
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0_datetime_dev.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureStage(featureID, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was updated");
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0_datetime_dev.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        response = config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema wan't changed");
        response = config.updateFeatureWithMinVersion(featureID, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");

        
        int respCode = config.deleteFeature(featureID);
        Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}


	@AfterTest
	private void reset(){
		config.reset();
	}
}
