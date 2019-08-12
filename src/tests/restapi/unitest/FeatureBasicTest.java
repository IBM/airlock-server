package tests.restapi.unitest;

import java.io.IOException;






import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureBasicTest {
	
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath + "feature1.txt";
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
	
	@Test 
	public void testGetAllFeatures(){
		f.getAllFeatures(sessionToken);			
	}
	
	@Test
	public void testAddFeature(){
		try {
			String featureJson = FileUtils.fileToString(filePath, "UTF-8", false);
			featureID = f.addFeature(seasonID, featureJson, "ROOT", sessionToken);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 			
	}
	
	@Test (dependsOnMethods="testAddFeature")
	public void testGetFeature(){
		json = f.getFeature(featureID, sessionToken);
 			
	}
	
	@Test (dependsOnMethods="testGetFeature")
	public void testGetFeaturesBySeason(){
		f.getFeaturesBySeason(seasonID, sessionToken);
 			
	}
	
	@Test (dependsOnMethods="testGetFeaturesBySeason")
	public void testUpdateFeature() throws JSONException, IOException{

			JSONObject featureJson = new JSONObject(json);
			featureJson.put("name", "New name");
			f.updateFeature(seasonID, featureID, featureJson.toString(), sessionToken);
	 			
	}
	
	@Test (dependsOnMethods="testUpdateFeature")
	public void testDeleteFeature(){
		f.deleteFeature(featureID, sessionToken);
 			
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
		
}
