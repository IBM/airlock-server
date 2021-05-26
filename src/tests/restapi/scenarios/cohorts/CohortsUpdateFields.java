package tests.restapi.scenarios.cohorts;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.CohortsRestApi;

import java.io.IOException;


public class CohortsUpdateFields {
	protected String filePath;
	protected String m_url;
	private CohortsRestApi cohortsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String cohortID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

		m_url = url;
	
		cohortsApi = new CohortsRestApi();
		cohortsApi.setUrl(url);
		
	}
	
	@Test (description="update and validate field values")
	public void testFields() throws JSONException, IOException{
		String notification = FileUtils.fileToString(filePath + "cohorts/cohort1.txt", "UTF-8", false);
		cohortID = cohortsApi.createCohort(productID, notification, sessionToken);
		Assert.assertFalse(cohortID.contains("error"), "Can't create cohort: " + cohortID);
		
		
		JSONObject json1 = new JSONObject(cohortsApi.getCohort(cohortID, sessionToken));
		json1.put("name", "new cohort name");
		json1.put("queryCondition", "premium = false and sessions_30d > 20");
		json1.put("description", "new cohort description");

		json1.put("enabled", false);
		json1.put("updateFrequency", "DAILY");
		json1.put("queryAdditionalValue", "2");

		
		String response = cohortsApi.updateCohort(cohortID, json1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update cohort: " + response);
		JSONObject json2 = new JSONObject(cohortsApi.getCohort(cohortID, sessionToken));
		
		validateParameter(json1.getString("name"), json2.getString("name"), "name");
		validateParameter(json1.getString("queryCondition"), json2.getString("queryCondition"), "queryCondition");
		validateParameter(json1.getString("calculationStatus"), json2.getString("calculationStatus"), "calculationStatus");
		validateParameter(json1.getString("description"), json2.getString("description"), "description");
		validateParameter(json1.getString("updateFrequency"), json2.getString("updateFrequency"), "updateFrequency");
		validateParameter(json1.getString("queryAdditionalValue"), json2.getString("queryAdditionalValue"), "queryAdditionalValue");
		Assert.assertEquals(json1.getBoolean("enabled"), json2.getBoolean("enabled"), "Parameter enabled differs from the expected.");

	}
	
	
	@Test (dependsOnMethods="testFields", description="update and validate field values")
	public void negativeTestFields() throws JSONException {
		JSONObject json1 = new JSONObject(cohortsApi.getCohort(cohortID, sessionToken));
		json1.put("creationDate", System.currentTimeMillis());
		json1.put("lastModified", System.currentTimeMillis());
		cohortsApi.updateCohort(cohortID, json1.toString(), sessionToken);
		JSONObject resultJson = new JSONObject(cohortsApi.getCohort(cohortID, sessionToken));
		Assert.assertNotEquals(json1.getLong("creationDate"), resultJson.getLong("creationDate"), "Incorrect value for field creationDate ");
		Assert.assertNotEquals(json1.getLong("lastModified"), resultJson.getLong("lastModified"), "Incorrect value for field lastModified ");
		
		
		json1 = new JSONObject(cohortsApi.getCohort(cohortID, sessionToken));
		json1.put("creator", "vicky1");		//can't change creator in update
		String response = cohortsApi.updateCohort(cohortID, json1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect update for field creator");
	}

	private void validateParameter(String oldString, String newString, String param){
	
		Assert.assertEquals(newString, oldString, "Parameter " + param + " differs from the expected.");
	
	}
	


	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
