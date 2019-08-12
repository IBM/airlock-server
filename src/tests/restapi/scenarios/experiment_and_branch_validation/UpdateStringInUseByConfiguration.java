package tests.restapi.scenarios.experiment_and_branch_validation;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class UpdateStringInUseByConfiguration {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private BranchesRestApi br;
	private String branchID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		seasonID = baseUtils.createSeason(productID);
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
	}


	
	@Test (description = "Add string in production stage")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		JSONObject jsonStr = new JSONObject(str);
		jsonStr.put("stage", "PRODUCTION");
		stringID = t.addString(seasonID, jsonStr.toString(), sessionToken);
	}
	
	@Test (dependsOnMethods="addString",  description = "Add feature with configuration")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String featureID = f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken );
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		crJson.put("stage", "PRODUCTION");
		String response = f.addFeatureToBranch(seasonID, branchID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods="addFeature",  description = "Update string in use by configuration to development ")
	public void updateString() throws Exception{
		String response = t.getString(stringID, "BASIC", sessionToken);
		JSONObject jsonStr = new JSONObject(response);
		jsonStr.put("stage", "DEVELOPMENT");
		response = t.updateString(stringID, jsonStr.toString(), sessionToken);
		Assert.assertTrue(response.contains("the stage of the string is development"), "Updated string to development while it is used by configuration in production");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
