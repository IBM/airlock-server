package tests.restapi.scenarios.feature;

import java.io.IOException;





import org.apache.wink.json4j.JSONArray;
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

public class AddFeatureFields {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
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
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		json = new JSONObject(feature);
	}
	


	@Test
	public void testLastModified() throws JSONException, IOException{
			addDateField("lastModified");
			
	}
	
	@Test 
	public void testCreationDate() throws JSONException, IOException{
		addDateField("creationDate");
	}
	
	
	@Test
	public void testUniqueID() throws JSONException, IOException{
			addStringField("uniqueId", "780cd507-1b86-56c3-88b8-1f44910c0f94");
			
	}
	
	@Test
	public void testSeasonID() throws JSONException, IOException{
			addStringField("seasonId", "e2d4efc6-90cf-40f7-a4aa-323daf14e0e3");
			
	}
	
	@Test 
	public void testRolloutPercentageBitmap() throws JSONException, IOException{
			//addStringField("rolloutPercentageBitmap", "////////////////Dw==");
			
	}
	
	@Test
	public void testFeaturesArray() throws JSONException, IOException{
		String child1 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);

		JSONArray children = json.getJSONArray("features");
		children.put(child1);
		json.put("features", children);
		f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
	}
	
	
	private void addStringField(String name, String value) throws JSONException, IOException{
		json.put(name, value);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		
	}

	private void addDateField(String name) throws JSONException, IOException{
		long timestamp = System.currentTimeMillis();
		json.put(name, timestamp);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);

	}
}
