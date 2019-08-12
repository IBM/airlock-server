package tests.restapi.validations.feature;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class FeatureUpdateSpecialCharacters {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected String feature;
	protected List<String> illegalCharacters;
	protected List<String> legalCharacters;
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
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		illegalCharacters= new ArrayList<String>(Arrays.asList("(", ")", "[", "]", "{", "}", "|", "/", "\\", "\"", ">", "<", 
				",", "!", "?", "@", "#", "$", "%", "^", "&", "*", "~", ";", "'"));
		legalCharacters= new ArrayList<String>(Arrays.asList(".", "-","_"));
	}
	
	@Test (description = "Validate legal special characters in name")
	public void legalCharactersInName() throws JSONException, IOException{
			feature = f.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			json.put("name", "name 123");
			f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			feature = f.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			json.put("name", "name.123a");
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test ( description = "Validate dot in name")
	public void dotInName() throws JSONException, IOException{
		//should fail as space (above) and dot are both translated to the same character 
			json = new JSONObject(feature);
			json.put("name", "name.123");	
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Validate space in namespace")
	public void spaceInNamespace() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);	
		JSONObject json = new JSONObject(feature);
		json.put("namespace", "name 123");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);	
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test  (description = "Validate illegal special characters in name")
	public void illegalCharactersInName() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		for (String character : illegalCharacters) {
			json.put("name", "name" + character + "123");
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		}		
	}
	
	@Test (description = "Validate illegal special characters in namespace")
	public void illegalCharactersInNamespace() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		for (String character : illegalCharacters) {
			json.put("namespace", "name" + character + "123");
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		}		
	}
			
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	

}
