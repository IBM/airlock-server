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

public class RolloutPercentageValues {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String m_url;
	
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
	
	@Test (description="Create feature with illegal rolloutPercentage values")
	public void addIllegalRolloutPercentage() throws JSONException, IOException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", 101);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature with illegal percentage value was created");
		
		json.put("rolloutPercentage", 100.001);
		response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature with illegal percentage value was created");
		
		json.put("rolloutPercentage", 52.05056);
		response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature with illegal percentage value was created");

	}
	
	@Test (dependsOnMethods="addIllegalRolloutPercentage", description="Create feature with legal rolloutPercentage values")
	public void addRolloutPercentage() throws JSONException, IOException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", 99.9999);
		json.put("name", "f1");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature with illegal percentage value was created");

		json.put("rolloutPercentage", 0.0001);
		json.put("name", "f2");
		response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature with illegal percentage value was created");

		json.put("rolloutPercentage", 50.005);
		json.put("name", "f3");
		response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature with illegal percentage value was created");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
