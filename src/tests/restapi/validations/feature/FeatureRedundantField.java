package tests.restapi.validations.feature;

import java.io.IOException;







import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureRedundantField {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
	
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		m_url = url;
		f = new FeaturesRestApi();
		f.setURL(url);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		json = new JSONObject(feature);
 		
	}
	
	@Test (description = "Add a redundant field to feature json in creation")
		public void redundantFieldInCreation() throws JSONException, IOException{
		json.put("newField", "test");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should succeed, but instead failed: " + featureID );
	}
	
	@Test (description = "Add a redundant field to feature json in udpate")
	public void redundantFieldInUpdate() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("newField", "test");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	}

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
