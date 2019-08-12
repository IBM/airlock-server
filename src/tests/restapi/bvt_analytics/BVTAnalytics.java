package tests.restapi.bvt_analytics;



import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils.RestCallResults;
import tests.restapi.AirlockUtils;
import tests.restapi.ExperimentAnalyticsApi;



public class BVTAnalytics {
	protected String analyticsExperimentID;
	protected String experimentID4Test;
	private ExperimentAnalyticsApi expAnalyticsApi;
	protected String sessionToken;
	protected AirlockUtils baseUtils;
	protected String config;
	protected String m_url;
	protected String m_analyticsServerUrl;
	protected String analyticsUrl;
	protected String experimentName = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsServerUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
 	public void init(String url, String analyticsServerUrl, String a_url, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;		
		m_analyticsServerUrl = analyticsServerUrl;
		analyticsUrl = a_url;
		config = configPath;

		expAnalyticsApi = new ExperimentAnalyticsApi();
		expAnalyticsApi.setURL(m_analyticsServerUrl);
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
     
      	sessionToken = baseUtils.getJWTToken(userName, userPassword, appName);
 
	}
	

	@Test (description="add experiment")
	public void addExperiment(){
		experimentName = "experiment."+RandomStringUtils.randomAlphabetic(3);
		try {
			String experiment = FileUtils.fileToString(config + "experiments/experimentForAnalyticsServer.txt", "UTF-8", false);
			JSONObject json = new JSONObject(experiment);
			json.put("name", experimentName);
	        analyticsExperimentID = expAnalyticsApi.addExperiment(json.getString("experimentId"), json.toString(), sessionToken);
	        Assert.assertFalse(analyticsExperimentID.contains("error") || analyticsExperimentID.contains("SecurityPolicyException") || analyticsExperimentID.contains("User does not have permission"),  "addExperiment failed: " + analyticsExperimentID);

		}catch (Exception e){

				Assert.fail("addExperiment failed with exception:\n" +e.getLocalizedMessage());

		}
	}
	
	@Test (dependsOnMethods="addExperiment", description="get status")
	public void getStatus1(){
		try {
	        
	        String response = expAnalyticsApi.getStatus(sessionToken);
	        Assert.assertTrue(response.contains(experimentName), "experiment not found in status " + response);
		}catch (Exception e){
				Assert.fail("getStatus failed with exception:\n" +e.getLocalizedMessage());
		}
	}
	
	@Test (dependsOnMethods="getStatus1", description="move indexExperiment to true")
	public void updateExperiment(){
		
		try {
			String airlockExperiment = expAnalyticsApi.getExperiment(analyticsExperimentID, sessionToken);
			JSONObject expJson = new JSONObject(airlockExperiment);
			expJson.put("indexExperiment", true);
			RestCallResults response = expAnalyticsApi.updateExperiment(analyticsExperimentID, expJson.toString(), sessionToken);
			Assert.assertFalse(response.message.contains("error") || response.message.contains("SecurityPolicyException")  || response.message.contains("User does not have permission"), "updateExperiment failed: " + response.message);

		} catch (Exception e) {
			Assert.fail("updateExperiment failed with exception:\n" +e.getLocalizedMessage());
		}

	}
	
	@Test (dependsOnMethods="updateExperiment", description="get status")
	public void getStatus2(){
		try {
	        
	        String response = expAnalyticsApi.getStatus(sessionToken);
	        Assert.assertTrue(response.contains(experimentName), "experiment not found in status " + response);
		}catch (Exception e){
				Assert.fail("getStatus failed with exception:\n" +e.getLocalizedMessage());
		}
	}
	
	@Test (dependsOnMethods="getStatus2", description="update description")
	public void updateExperimentDescription(){
		
		try {
			String airlockExperiment = expAnalyticsApi.getExperiment(analyticsExperimentID, sessionToken);
			JSONObject expJson = new JSONObject(airlockExperiment);
			expJson.put("description", "new description of experiment " + experimentName);
			RestCallResults response = expAnalyticsApi.updateExperiment(analyticsExperimentID, expJson.toString(), sessionToken);
			Assert.assertFalse(response.message.contains("error") || response.message.contains("SecurityPolicyException")  || response.message.contains("User does not have permission"), "updateExperiment failed: " + response.message);
	        String status = expAnalyticsApi.getStatus(sessionToken);
	        Assert.assertTrue(status.contains("new description of experiment " + experimentName), "experiment not found in status " + status);

		} catch (Exception e) {
				Assert.fail("updateExperiment failed with exception:\n" +e.getLocalizedMessage());
		}

	}
	
	
	@Test (dependsOnMethods="updateExperimentDescription", description="delete experiment")
	public void deleteExperiment() throws Exception{
	        int respCode = expAnalyticsApi.deleteExperiment(analyticsExperimentID, sessionToken);
	        Assert.assertTrue(respCode == 200,  "deleteExperiment failed: code " + respCode);
	}

    @AfterTest
    public void reset() throws Exception {
    	 expAnalyticsApi.deleteExperiment(analyticsExperimentID, sessionToken);
    }
	
}
