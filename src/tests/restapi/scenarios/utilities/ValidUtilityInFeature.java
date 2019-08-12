package tests.restapi.scenarios.utilities;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class ValidUtilityInFeature {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	@Test(description = "Add valid utility")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.put("utility", "function getLocation(){return true;}");
		String response = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test(dependsOnMethods = "addUtility", description = "Add feature with rule using utility")
	public void addFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getLocation()");
		fJson.put("rule", rule);
		featureID = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	@Test(dependsOnMethods = "addFeature", description = "Add valid utility 2")
	public void addUtility2() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.put("utility", "function isFase(){return false;}");
		
		String response =u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test(dependsOnMethods = "addUtility2", description = "Update feature with rule using utility")
	public void updateFeature() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isFase()");
		fJson.put("rule", rule);
		String response =f.updateFeature(seasonID, featureID, fJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
