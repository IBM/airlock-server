package tests.restapi.scenarios.stream_utilities;

import java.io.File;



import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.UtilitiesRestApi;

public class StreamUtilityInvalidUsageInCreate {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected String productID;
	protected String m_url;
	private String m_analyticsUrl;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	protected FeaturesRestApi f;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);		
		
	}

	//stream utility can't be used in feature/configuration/experiment/variant

	@Test (description="Stream utility")
	public void addStreamUtility() throws Exception{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Stream utility can't be created: " + utilityID);		
	}
	
	@Test (dependsOnMethods="addStreamUtility", description="Stream utility can't be used in features")
	public void createFeature() throws JSONException, IOException{
		//add feature with stream utility
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		fJson.put("rule", rule);
		String response = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in feature" );
	}
	
	@Test (dependsOnMethods="createFeature", description="Stream utility can't be used in features")
	public void createConfiguration() throws JSONException, IOException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(configuration);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		fJson.put("rule", rule);
		String response = f.addFeature(seasonID, fJson.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in configuration rule" );
	}
	
	@Test (dependsOnMethods="createConfiguration", description="Stream utility can't be used in experiment")
	public void createExperiment() throws JSONException, IOException{

		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);

		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		expJson.put("rule", rule);
		String response = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in experiment" );
	}
	
	
	@Test (dependsOnMethods="createExperiment", description="Stream utility can't be used in variant")
	public void createVariant() throws JSONException, IOException{
	
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);
		String response = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create branch: " + response );
		
		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", false);
		String experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create experiment: " + experimentID );
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		variantJson.put("rule", rule);
		
		String resp = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Shouldn't allow to use stream utility in variant" );
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
