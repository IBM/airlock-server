package tests.restapi.scenarios.cohorts;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;


public class DeleteEnabledCohort {
	protected String filePath;
	protected String m_url;
	private CohortsRestApi cohortsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String cohortID;
	private CohortsRestApi c;
	private ProductsRestApi p;
	
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
		p = new ProductsRestApi();
		p.setURL(url);
		
	}
	
	@Test (description="add enabled cohort")
	public void addEnabledCohort() throws JSONException, IOException, InterruptedException{
		
		String notification = FileUtils.fileToString(filePath + "cohorts/cohort1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("enabled", true);
		cohortID = cohortsApi.createCohort(productID, json.toString(), sessionToken);
		Assert.assertFalse(cohortID.contains("error"), "Can't create cohort: " + cohortID);

	}
	
	
	@Test (dependsOnMethods="addEnabledCohort", description="delete production notification")
	public void deleteNotification() throws JSONException, IOException, InterruptedException{
		int respCode = cohortsApi.deleteCohort(cohortID, sessionToken);
		Assert.assertFalse(respCode == 200, "Enabled cohort was deleted");
	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
