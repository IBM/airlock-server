package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.*;

public class UpdateQuota {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	@Test (description="get default quota")
	public void getDefaultQuota() throws Exception{
		String response = an.getQuota(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

	}
	@Test (dependsOnMethods="getDefaultQuota", description="Update quota")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 5, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="get udpated  quota")
	public void getUpdatedQuota() throws Exception{
		String response = an.getQuota(seasonID, sessionToken);
		JSONObject quota = new JSONObject(response);
		Assert.assertTrue(quota.getInt("analyticsQuota")==5, "Incorrect quota was not returned " + quota.getInt("analyticsQuota"));

	}
	
	@Test (dependsOnMethods="getUpdatedQuota", description="Invalid  quota")
	public void setInvalidQuota1() throws Exception{
		String response = an.updateQuota(seasonID, -5, sessionToken);
		Assert.assertTrue(response.contains("error"), "Quota set to -5");

	}
	
	/*
	@Test (dependsOnMethods="getUpdatedQuota", description="Invalid  quota")
	public void setInvalidQuota2() throws Exception{
		String response = an.updateQuota(seasonID, 0, sessionToken);
		Assert.assertTrue(response.contains("error"), "Quota set to 0");

	}
	*/

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
