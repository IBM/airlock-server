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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureCreateNonLatinFields {
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
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
		
		filePath = configPath;
		m_url = url;
		f = new FeaturesRestApi();
		f.setURL(url);
	}
	
	@Test (description = "Feature name in French")
		public void frenchName() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_frenchName.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken); 
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}
	
	@Test (description = "Feature namespace in French")
	public void frenchNamespace() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_frenchNamespace.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}

	@Test (description = "Feature description in French")
	public void frenchDescription() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_frenchDescription.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		JSONObject json = new JSONObject(feature);
		String name = json.getString("description");
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String newName = newJson.getString("description");
		Assert.assertTrue(name.equals(newName), "The descriptions are not equal");
	}
	
	@Test (description = "Feature name in Chinese")
	public void chineseName() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_ChineseName.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken); 
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}
	
	@Test (description = "Feature namespace in Chinese")
	public void chineseNamespace() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_ChineseNamespace.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken); 
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}
	
	@Test (description = "Feature description in Chinese")
	public void chineseDescription() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_ChineseDescription.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		JSONObject json = new JSONObject(feature);
		String name = json.getString("description");
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String newName = newJson.getString("description");
		Assert.assertTrue(name.equals(newName), "The descriptions are not equal");
	}
	
	@Test (description = "Feature descriptions in English and non-English")
	public void mixedCharsDescription() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_MixedCharsDescription.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		JSONObject json = new JSONObject(feature);
		String name = json.getString("description");
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String newName = newJson.getString("description");
		Assert.assertTrue(name.equals(newName), "The descriptions are not equal");
	}
	
	@Test(description = "Feature name in English and non-English")
	public void mixedCharsName() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_MixedCharsName.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken); 
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}
	
	@Test (description = "Feature namespace in English and non-English")
	public void mixedCharsNamespace() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature_MixedCharsNamespace.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test failed: " + featureID );
	}
		
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	


}
