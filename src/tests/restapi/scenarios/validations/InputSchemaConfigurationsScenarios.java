package tests.restapi.scenarios.validations;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class InputSchemaConfigurationsScenarios
{
	protected Config config;
	private String configID1;
	private String featureID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		
	}

	@Test ( description="F1-> CR1, CR2")
	public void scenario1() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
	
		configID1 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", featureID, "5.0");
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);
		
		String configID2 = config.addFeature ("configuration_rule2.txt", "context.device.osVersion == \"value\"", featureID, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move configuration to lower minVersion
        response = config.updateFeatureWithMinVersion(configID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, config in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(configID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(configID1, "DEVELOPMENT");
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
	
	
	@Test ( description="F1->CR1->(CR2, CR3)")
	public void scenario2() throws Exception
	{
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		
		String directChild = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", featureID, "5.0");
	
		configID1 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", directChild, "5.0");
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", directChild, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move configuration to lower minVersion
        response = config.updateFeatureWithMinVersion(configID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, config in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(directChild, "PRODUCTION");
        response = config.updateFeatureStage(configID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(configID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        response = config.updateFeatureStage(directChild, "DEVELOPMENT");
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
	
	
	@Test ( description="F1->MTXCR->(CR2, CR3)")
	public void scenario3() throws Exception
	{
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		
		String directChild = config.addMTX ("configuration_feature-mutual.txt",  featureID);
	
		configID1 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", directChild, "5.0");
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", directChild, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move configuration to lower minVersion
        response = config.updateFeatureWithMinVersion(configID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, config in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(directChild, "PRODUCTION");
        response = config.updateFeatureStage(configID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(configID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        response = config.updateFeatureStage(directChild, "DEVELOPMENT");
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
	
	
	@Test ( description="F1->CR1->MTXCR->(CR2, CR3)")
	public void scenario4() throws Exception
	{
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		
		String directChild = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", featureID, "5.0");
		String mtxChild = config.addMTX ("configuration_feature-mutual.txt",  directChild);
	
		configID1 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", mtxChild, "5.0");
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", mtxChild, "5.0");

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move configuration to lower minVersion
        response = config.updateFeatureWithMinVersion(configID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, config in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(directChild, "PRODUCTION");
        response = config.updateFeatureStage(configID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(configID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
        response = config.updateFeatureStage(directChild, "DEVELOPMENT");
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
	
	@Test ( description="F1->MTXCR->(CR2->MTXCR->(CR4, CR5), CR3)")
	public void scenario5() throws Exception
	{
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		//context.device.datetime - 4.0, prod
		
		featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		String mtxChild = config.addMTX ("configuration_feature-mutual.txt",  featureID);
		String configCR2 = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", mtxChild, "5.0");
		String configCR3 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", mtxChild, "5.0");
		
		String MTXCR = config.addMTX ("configuration_feature-mutual.txt",  configCR2);
		
	
		configID1 = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"US\"", MTXCR, "5.0");
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);
		String configCR4 = config.addFeature ("configuration_rule1.txt", "context.device.osVersion == \"value\"", MTXCR, "5.0");
		

		//1. move child field to upper minVersion		
		//move context.device.locale child field to 4.0
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //2. move configuration to lower minVersion
        response = config.updateFeatureWithMinVersion(configID1, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");
        response = config.updateFeatureWithMinVersion(configCR3, "2.0");
        Assert.assertTrue(response.contains("error"), "Feature was updated");


        //3. move child field to production       
        //move context.device.locale to prod, config in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);

        response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(configCR2, "PRODUCTION");
        response = config.updateFeatureStage(configID1, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        response = config.updateFeatureStage(configCR3, "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
        
        //4. move child to dev
        response = config.updateFeatureStage(configID1, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
        response = config.updateFeatureStage(configCR3, "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
 
 
        response = config.updateFeatureStage(configCR2, "DEVELOPMENT");
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
