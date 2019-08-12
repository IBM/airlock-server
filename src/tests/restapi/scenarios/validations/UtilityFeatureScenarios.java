package tests.restapi.scenarios.validations;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class UtilityFeatureScenarios
{
	protected Config config;
	private String utilityID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		utilityID = config.addUtility("1.1.1", "function isTrue() {return true;}");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season");
		
	}

	@Test ( description="F1 -> F2, F3")
	public void scenario1() throws Exception
	{
		String featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
	
		String featureID1 = config.addFeature ("feature1.txt", "isTrue()", featureID, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "feature rule was not added to the season: " + featureID1);
		
		String featureID2 = config.addFeature ("feature1.txt", "", featureID, "5.0");

		
		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move utility to prod
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID, utility.toString());
			Assert.assertTrue(resp.contains("error"), "Utility was updated: " + resp);

		//move feature to dev, utility in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);
		 
        
       // int respCode = config.deleteFeature(featureID);
       // Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	
	@Test ( description="F1 -> MTX ->( F2, F3)")
	public void scenario2() throws Exception
	{

		
		String featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		String mtxId = config.addMTX("feature-mutual.txt", featureID) ;
	
		String featureID1 = config.addFeature ("feature1.txt", "isTrue()", mtxId, "5.0");
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season: " + featureID1);
		
		String featureID2 = config.addFeature ("feature1.txt", "", mtxId, "5.0");

		
		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID1, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move utility to prod
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(featureID1, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID, utility.toString());
			Assert.assertTrue(resp.contains("error"), "Utility was updated: " + resp);

		//move feature to dev, utility in prod
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID1, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        
      //  int respCode = config.deleteFeature(featureID);
      //  Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}
	
	@Test ( description="F1 -> MTX ->( F2->MTX->(F4, F5), F3); test F3, F5")
	public void scenario3() throws Exception
	{
		
		String featureID = config.addFeature ("feature1.txt", "", "ROOT", "4.0");
		
		String mtxId = config.addMTX("feature-mutual.txt", featureID) ;
		String featureID2 = config.addFeature ("feature1.txt", "", mtxId, "5.0");
		String featureID3 = config.addFeature ("feature1.txt", "isTrue()", mtxId, "5.0");
		
		
		String mtxId2 = config.addMTX("feature-mutual.txt", featureID2) ;
		String featureID4 = config.addFeature ("feature1.txt", "", mtxId2, "5.0");
		String featureID5 = config.addFeature ("feature1.txt", "isTrue()", mtxId2, "5.0");

		
		
		//move feature to prod, utility in dev
        String  response = config.updateFeatureStage(featureID, "PRODUCTION");
        response = config.updateFeatureStage(featureID3, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
        response = config.updateFeatureStage(featureID5, "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Feature was not updated" + response);
       
        //move utility to prod
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
	       response = config.updateFeatureStage(featureID, "PRODUCTION");
	       response = config.updateFeatureStage(featureID2, "PRODUCTION");
	       response = config.updateFeatureStage(featureID3, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	       response = config.updateFeatureStage(featureID5, "PRODUCTION");
	       Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID, utility.toString());
			Assert.assertTrue(resp.contains("error"), "Utility was updated: " + resp);

		//move feature to dev, utility in prod
		 
		 response = config.updateFeatureStage(featureID3, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
		 response = config.updateFeatureStage(featureID5, "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
		 response = config.updateFeatureStage(featureID2, "DEVELOPMENT");
		 response = config.updateFeatureStage(featureID, "DEVELOPMENT");
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

       
		
    //    int respCode = config.deleteFeature(featureID);
     //   Assert.assertEquals(respCode,  200, "Feature was not deleted");
	}


	@AfterTest
	private void reset(){
		config.reset();
	}
}
