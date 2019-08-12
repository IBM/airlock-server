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


public class MutuallyExclusiveFeatureOrder {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String filePath;
	protected String mutualGroupFile;
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
	
	/**
	 * Create mutually exclusive group
	 */
	@Test (description = "Create mutually exclusive group")
	public void testCreateMutuallyExclusiveFromJson() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			
			String child1 = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);
			String child2 = FileUtils.fileToString(filePath+"feature2.txt", "UTF-8", false);
			childID2 = f.addFeature(seasonID, child2, parentID, sessionToken);
			
			parent = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			JSONArray children = json.getJSONArray("features");
			if (children.size() == 2){
				Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID1, "The first child is incorrect");
				Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID2, "The second child is incorrect");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test (dependsOnMethods="testCreateMutuallyExclusiveFromJson", description = "Change featrues order in mutually exclusive group")
	public void testChangeOrder() throws JSONException, IOException{
		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("features");
		JSONArray children = new JSONArray();
		JSONObject child2 = new JSONObject(f.getFeature(childID2, sessionToken));
		children.put(child2);
		JSONObject child1 = new JSONObject(f.getFeature(childID1, sessionToken));
		children.put(child1);
		json.put("features", children);
		f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		
		
		parent = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parent);
		children = json.getJSONArray("features");
		if (children.size() == 2){
			Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID2, "The first child is incorrect");
			Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID1, "The second child is incorrect");
		}

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
