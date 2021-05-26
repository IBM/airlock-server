package tests.restapi.scenarios.cohorts;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.CohortsRestApi;

import java.io.IOException;


public class CohortsAddFields {
	protected String filePath;
	protected String m_url;
	protected String cohort;
	private CohortsRestApi cohortsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

		m_url = url;

		cohort = FileUtils.fileToString(filePath + "cohorts/cohort1.txt", "UTF-8", false);
		cohortsApi = new CohortsRestApi();
		cohortsApi.setUrl(url);
		
	}
	
	@Test (description="add and validate field values")
	public void testFields() throws JSONException, IOException{
		String cohort = FileUtils.fileToString(filePath + "cohorts/cohort1.txt", "UTF-8", false);
		JSONObject json1 = new JSONObject(cohort);
		String cohortID = cohortsApi.createCohort(productID, json1.toString(), sessionToken);
		
		String resultCohort = cohortsApi.getCohort(cohortID, sessionToken);
		JSONObject json2 = new JSONObject(resultCohort);
		validateParameter(json1.getString("name"), json2.getString("name"), "name");
		
		validateParameter(json1.getString("queryCondition"), json2.getString("queryCondition"), "queryCondition");
		validateParameter(json1.getString("queryAdditionalValue"), json2.getString("queryAdditionalValue"), "queryAdditionalValue");
		validateParameter(json1.getString("description"), json2.getString("description"), "description");
		validateParameter(json1.get("calculationStatus"), json2.get("calculationStatus"), "calculationStatus");
		validateParameter(json1.getString("creator"), json2.getString("creator"), "creator");
		Assert.assertEquals(json1.getBoolean("enabled"), json2.getBoolean("enabled"), "Parameter enabled differs from the expected.");
		Assert.assertEquals(json1.get("calculationStatusMessage"), json2.get("calculationStatusMessage"), "Parameter calculationStatusMessage differs from the expected.");
		Assert.assertEquals(json1.getLong("usersNumber"), json2.getLong("usersNumber"), "Parameter usersNumber differs from the expected.");
		Assert.assertEquals(json1.getJSONObject("exports"), json2.getJSONObject("exports"), "Parameter exports differs from the expected.");
	}

	private void validateParameter(String oldString, String newString, String param){
	if (oldString == null && newString == null) return;
	Assert.assertEquals(newString, oldString, "Parameter " + param + " differs from the expected.");
	
	}

	private void validateParameter(Object oldString, Object newString, String param){
		if (oldString == null && newString == null) return;
		Assert.assertEquals(newString, oldString, "Parameter " + param + " differs from the expected.");

	}
	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
