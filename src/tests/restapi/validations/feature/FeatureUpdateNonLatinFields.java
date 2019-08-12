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

public class FeatureUpdateNonLatinFields {
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
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
	}
	
	@Test (description = "French feature name in update")
		public void frenchName() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "française énergie1");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}
	
	@Test (description = "French feature namespace in update")
	public void frenchNamespace() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("namespace", "française énergie");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}

	@Test (description = "French feature description in update")
	public void frenchDescription() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "française énergie");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String newDescription = newJson.getString("description");
		Assert.assertTrue(newDescription.equals("française énergie"), "The descriptions are not equal");
	}
	
	@Test (description = "Chinese feature name in update")
	public void chineseName() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "名称名称");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}
	
	@Test (description = "Chinese feature namespace in update")
	public void chineseNamespace() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("namespace", "名称名称");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}
	
	@Test (description = "Chinese feature description in update")
	public void chineseDescription() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "名称名称");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String description = newJson.getString("description");
		Assert.assertTrue(description.equals("名称名称"), "Descriptions are not equal, description:" +description);
	}
	
	@Test (description = "English and non-English feature descpription in update")
	public void mixedCharsDescription() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "English名称名称");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		JSONObject newJson = new JSONObject(feature);
		String description = newJson.getString("description");
		Assert.assertTrue(description.equals("English名称名称"), "Descriptions are not equal, description:" +description);

	}
	
	@Test (description = "English and non-English feature name in update")
	public void mixedCharsName() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "English名称名称");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}
	
	@Test (description = "English and non-English feature namespace in update")
	public void mixedCharsNamespace() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "English名称名称");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead succeeded: " + response );
	}
		
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	

}
