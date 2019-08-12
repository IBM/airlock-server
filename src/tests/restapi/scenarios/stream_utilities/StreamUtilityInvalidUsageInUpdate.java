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

public class StreamUtilityInvalidUsageInUpdate {
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
	private String featureID;
	
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
	public void updateFeature() throws JSONException, IOException{
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature: " + featureID );

		
		JSONObject fJson = new JSONObject(f.getFeature(featureID, sessionToken));
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		fJson.put("rule", rule);
		String response = f.updateFeature(seasonID, featureID, fJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in feature" );
	}
	
	@Test (dependsOnMethods="updateFeature", description="Stream utility can't be used in features")
	public void updateConfiguration() throws JSONException, IOException{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID, configuration, featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create configuration rule: " + configID );
		
		JSONObject fJson = new JSONObject(f.getFeature(configID, sessionToken));
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		fJson.put("rule", rule);
		String response = f.updateFeature(seasonID, configID, fJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in configuration rule" );
	}
	
	@Test (dependsOnMethods="updateConfiguration", description="Stream utility can't be used in experiment")
	public void updateExperiment() throws Exception{

		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", false);
		String experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create experiment: " + experimentID );

		experiment = exp.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		expJson.put("rule", rule);
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use stream utility in experiment" );
	}
	
	
	@Test (dependsOnMethods="updateExperiment", description="Stream utility can't be used in variant")
	public void updateVariant() throws Exception{
	
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);
		String response = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create branch: " + response );
		
		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment2.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", false);
		String experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create experiment: " + experimentID );
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		String variantID = exp.createVariant(experimentID, variant, sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Can't create variant: " + variantID );
		
		
		JSONObject variantJson = new JSONObject(exp.getVariant(variantID, sessionToken));
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFalse()");
		variantJson.put("rule", rule);
		
		String resp = exp.updateVariant(variantID, variantJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Shouldn't allow to use stream utility in variant" );
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
