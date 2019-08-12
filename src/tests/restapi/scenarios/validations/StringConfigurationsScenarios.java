package tests.restapi.scenarios.validations;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


public class StringConfigurationsScenarios
{
	protected Config config;
	private String featureID;
	private String stringID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		stringID = config.addString("strings/string1.txt");	//key=app.hello
		Assert.assertFalse(stringID.contains("error"), "String was not added to the season");		
	}

	@Test ( description="F1-> CR1, CR2")
	public void scenario1() throws Exception
	{

		
		featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
	
		String featureID1 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", featureID, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String configID2 = config.addFeature ("configuration_rule2.txt", "", featureID, "5.0");

		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move string  to dev, feature in prod
			resp = config.updateString(stringID, "DEVELOPMENT");
			Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 
        
        //int respCode = config.deleteFeature(featureID);
        //Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	
	@Test ( description="F1->CR1->(CR2, CR3)")
	public void scenario2() throws Exception
	{
	featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		
		String directChild = config.addFeature ("configuration_rule1.txt", "", featureID, "5.0");
	
		String featureID1 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", directChild, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "", directChild, "5.0");


		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(directChild, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(directChild, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move string  to dev, feature in prod
			resp = config.updateString(stringID, "DEVELOPMENT");
			Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(directChild, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 
	
        //int respCode = config.deleteFeature(featureID);
        //Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	
	@Test ( description="F1->MTXCR->(CR2, CR3)")
	public void scenario3() throws Exception
	{
	featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		
		String directChild = config.addMTX ("configuration_feature-mutual.txt",  featureID);
	
		String featureID1 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", directChild, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "", directChild, "5.0");


		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move string  to dev, feature in prod
			resp = config.updateString(stringID, "DEVELOPMENT");
			Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 
        
        //int respCode = config.deleteFeature(featureID);
       // Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	
	@Test ( description="F1->CR1->MTXCR->(CR2, CR3)")
	public void scenario4() throws Exception
	{
		
		featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		
		String directChild = config.addFeature ("configuration_rule1.txt", "", featureID, "5.0");
		String mtxChild = config.addMTX ("configuration_feature-mutual.txt",  directChild);
	
		String featureID1 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", mtxChild, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String configID2 = config.addFeature ("configuration_rule1.txt", "", mtxChild, "5.0");


		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(directChild, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(directChild, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move string  to dev, feature in prod
			resp = config.updateString(stringID, "DEVELOPMENT");
			Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(directChild, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 
        
        //int respCode = config.deleteFeature(featureID);
        //Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	@Test ( description="F1->MTXCR->(CR2->MTXCR->(CR4, CR5), CR3)")
	public void scenario5() throws Exception
	{
	featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		String mtxChild = config.addMTX ("configuration_feature-mutual.txt",  featureID);
		String configCR2 = config.addFeature ("configuration_rule1.txt", "", mtxChild, "5.0");
		String featureID1 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", mtxChild, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "Configuration rule was not added to the season: " + featureID1);
		
		String MTXCR = config.addMTX ("configuration_feature-mutual.txt",  configCR2);
		
	
		String featureID2 = config.addFeatureWithString ("configuration_rule1.txt", "app.hello", "stam", MTXCR, "5.0");
		Assert.assertFalse(featureID2.contains("error"), "Configuration rule was not added to the season: " + featureID2);
		String configCR4 = config.addFeature ("configuration_rule1.txt", "", MTXCR, "5.0");
		


		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(configCR2, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(configCR2, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	       response = config.updateFeatureStage(featureID2, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move string  to dev, feature in prod
			resp = config.updateString(stringID, "DEVELOPMENT");
			Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(configCR2, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
		 response = config.updateFeatureStage(featureID2, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 

       // int respCode = config.deleteFeature(featureID);
       // Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
