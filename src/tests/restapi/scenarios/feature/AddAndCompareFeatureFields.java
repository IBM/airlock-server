package tests.restapi.scenarios.feature;

import java.io.IOException;




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

public class AddAndCompareFeatureFields {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	//protected Notification notification;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}


	/**
	 * Test creating, retrieving, and comparing feature parameters
	 * @throws IOException 
	 */
	@Test (description = "Add a feature and validate all of its field values")
	public void testAddFeatureAndCompareParameters() throws JSONException, IOException{

			String feature1 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
			JSONObject json1 = new JSONObject(feature1);
			featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
			//notification.followFeature(featureID); // optional action depending on configuration
			String feature2 = f.getFeature(featureID, sessionToken);
			JSONObject json2 = new JSONObject(feature2);
			validateParameter(json1.getString("name"), json2.getString("name"), "name");
			validateParameter(json1.getString("type"), json2.getString("type"), "type");
			validateParameter(json1.getString("stage"), json2.getString("stage"), "stage");
			validateParameter(json1.getString("namespace"), json2.getString("namespace"), "namespace");
			validateParameter(json1.getString("creator"), json2.getString("creator"), "creator");
			validateParameter(json1.getString("description"), json2.getString("description"), "description");
			validateParameter(json1.getString("minAppVersion"), json2.getString("minAppVersion"), "minAppVersion");
			validateParameter(json1.getString("owner"), json2.getString("owner"), "owner");
			Assert.assertEquals(json1.getBoolean("enabled"), json2.getBoolean("enabled"), "Parameter enabled differs from the expected.");
			Assert.assertEquals(json1.getBoolean("defaultIfAirlockSystemIsDown"), json2.getBoolean("defaultIfAirlockSystemIsDown"), "Parameter defaultIfAirlockSystemIsDown differs from the expected.");
			Assert.assertEquals(json1.getDouble("rolloutPercentage"), json2.getDouble("rolloutPercentage"), "Parameter rolloutPercentage differs from the expected.");
			Assert.assertEquals(json1.getJSONArray("internalUserGroups"), json2.getJSONArray("internalUserGroups"), "Parameter internalUserGroups differs from the expected.");
			
			Assert.assertEquals(json1.getJSONObject("configurationSchema"), json2.getJSONObject("configurationSchema"), "Parameter configurationSchema differs from the expected.");
			Assert.assertEquals(json1.getString("defaultConfiguration"), json2.getString("defaultConfiguration"), "Parameter defaultConfiguration differs from the expected.");

			//json1 contains rule parameter "force", json2 should not contain it
			validateParameter(json1.getJSONObject("rule").getString("ruleString"), json2.getJSONObject("rule").getString("ruleString"), "rule");
			Assert.assertFalse(json2.getJSONObject("rule").has("force"), "The server returned rule parameter \"force\".");
		
	}
	
	private void validateParameter(String oldString, String newString, String param){
		
		Assert.assertEquals(newString, oldString, "Parameter " + param + " differs from the expected.");
		
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
