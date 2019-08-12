package tests.restapi.scenarios.mutual_exclusion_features;

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

public class MIUpdateTreeOfFeaturesInProduction_ChangeOrder {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String childID3;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	
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
	
	/* create MIX with maxFeaturesOn=2
	 * add 2 features in prod with true and 1 in prod with false
	 * Change the order of the features from true, false, true TO true, true, false - do not update features
	 */
	@Test (description = "Create mutually exclusive group with maxFeaturesOn=2 and 2 features in Production with defaultSystemIsDown=true ")
	public void createMutuallyExclusiveGroup() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			JSONObject parentJson = new JSONObject(parent);
			parentJson.put("maxFeaturesOn", 2);
			parentID = f.addFeature(seasonID, parentJson.toString(), "ROOT", sessionToken);
			
			//2 features in prod
			String child1 = createFeature("feature1.txt");
			String child2 = createFeature("feature2.txt");
			
			//feature in dev
			String child3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
			
			//features in production order: true, false, true
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);	//true
			childID3 = f.addFeature(seasonID, child3, parentID, sessionToken);	//false
			childID2 = f.addFeature(seasonID, child2, parentID, sessionToken);	//true
			
			
			parent = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			JSONArray children = json.getJSONArray("features");
			Assert.assertTrue(children.size()==3, "Incorrect number of children in MI group");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Test (dependsOnMethods = "createMutuallyExclusiveGroup", description = "Update  MI tree from 'true, false, true' features order to 'true, true, false'")
	public void changeFeaturesOrder() throws JSONException, IOException{

		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("features");
		JSONArray children = new JSONArray();
		JSONObject child1 = new JSONObject(f.getFeature(childID1, sessionToken)); // true
		children.put(child1);
		JSONObject child2 = new JSONObject(f.getFeature(childID2, sessionToken)); // true
		children.put(child2);
		JSONObject child3 = new JSONObject(f.getFeature(childID3, sessionToken));	//false
		children.put(child3);
		json.put("features", children);
		String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	private String createFeature(String fileName) throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("defaultIfAirlockSystemIsDown", true);
		return json.toString();
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
