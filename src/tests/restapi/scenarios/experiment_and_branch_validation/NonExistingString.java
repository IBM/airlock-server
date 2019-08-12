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

public class NonExistingString {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String featureID;
	protected String configID;
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
		f = new FeaturesRestApi();
		f.setURL(url);
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
	}


	//configuration
	@Test (description = "Add feature with configuration using non-existing string")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", context.userPreferences.velocity)	}" ;		
		crJson.put("configuration", configuration);
		configID = f.addFeatureToBranch(seasonID, branchID, crJson.toString(), featureID, sessionToken );
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
