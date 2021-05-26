package tests.restapi.scenarios.cohorts;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;
import tests.restapi.scenarios.webhooks.WebhookListenerRunner;

import java.io.IOException;

public class CohortsSimpleScenarios {
	private String sessionToken = "";
	private String adminToken = "";
	private String productID;
	private String seasonID;
	protected String filePath;
	private ProductsRestApi p;
	private CohortsRestApi c;
	private AirlockUtils baseUtils;
	private OperationRestApi opsApi;
	protected String m_url;
	protected String webhookID;
	protected String cohort;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appNameSimple", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appNameSimple, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify, String operationsUrl) throws Exception{
		m_url = url;
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appNameSimple, productsToDeleteFile);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appNameSimple);
		sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appNameSimple);
		baseUtils.setSessionToken(adminToken);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		p = new ProductsRestApi();
		p.setURL(m_url);
		c = new CohortsRestApi();
		c.setUrl(m_url);
		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		cohort = FileUtils.fileToString(configPath + "cohort1.txt", "UTF-8", false);
	}



	@Test (description="create simple cohort", enabled=true)
	public void createCohortTest() throws JSONException, InterruptedException {
		JSONObject json = new JSONObject(cohort);
		String cohortID = addCohort(json, false);
		deleteCohort(cohortID);
	}

	@Test (description="create simple cohort and export", enabled=true)
	public void exportCohortTest() throws JSONException, InterruptedException {
		JSONObject json = new JSONObject(cohort);
		String cohortID = addCohort(json, false);
		JSONObject object = exportCohort(cohortID, false);
		Assert.assertEquals(object.getString("calculationStatus"), "PENDING", "calculationStatus after export is: "+object.get("calculationStatus"+". expected PENDING"));
		deleteCohort(cohortID);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);

	}

	private JSONObject exportCohort(String cohortID, boolean expectedResult) {
		try {
			String response = c.exportCohort(cohortID, sessionToken);
			Assert.assertNotEquals(response, "", "failed in exporting cohort");
			JSONObject cohortObj = new JSONObject(response);
			return cohortObj;
		} catch (JSONException e) {
			e.printStackTrace();
			Assert.assertFalse(true, "failed exporting cohort");
			return null;
		}
	}
	private String addCohort(JSONObject json, boolean expectedResult) throws JSONException{
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		try {
			String response = c.createCohort(productID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "failed adding cohort: "+ response);
			return response;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "failed adding cohort");
			return null;
		}	
	}

	private void deleteCohort(String cohortID) {
		try {
			c.deleteCohort(cohortID, sessionToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
