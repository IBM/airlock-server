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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class UpdateFeatureWithChildren {
	protected String seasonID;
	protected String parentID1;
	protected String parentID2;
	protected String childID1;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
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
	 * If a feature has children, the whole tree must be passed in update, not just the parent feature fields
	 * @throws IOException 
	 * 
	 */
	
	@Test (description = "Create 2 parent features and 1 sub feature")
	public void addComponents() throws JSONException, IOException{		
			//Parent1 contains parent2. Parent2 contains child1
			String parent1 = FileUtils.fileToString(filePath+"parent-feature.txt", "UTF-8", false);
			parentID1 = f.addFeature(seasonID, parent1, "ROOT", sessionToken);
			String parent2 = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			parentID2 = f.addFeature(seasonID, parent2, parentID1, sessionToken);
			
			//create a child feature under parent2
			String child1 = FileUtils.fileToString(filePath+"feature2.txt", "UTF-8", false);
			child1 = JSONUtils.generateUniqueString(child1, 6, "name");
			childID1 = f.addFeature(seasonID, child1, parentID2, sessionToken);
	}
	
	@Test (dependsOnMethods = "addComponents", description="If a feature has children, the whole tree must be passed in update, not just the parent feature fields")
	public void testUpdateParent1() throws JSONException, IOException{		
			//empty array of children under parent1 and try to update - should fail
			String parent1 = f.getFeature(parentID1, sessionToken);
			JSONObject json1 = new JSONObject(parent1);
			json1.put("name", "new name");
			json1.remove("features");
			json1.put("features", new JSONArray());
			String response = f.updateFeature(seasonID, parentID1, json1.toString(), sessionToken); 
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	@Test (dependsOnMethods="testUpdateParent1", description="If a feature has children, the whole tree must be passed in update, not just the parent feature fields")
	public void testUpdateParent2() throws JSONException{
		try {
			//update parent1 name
			String parent1 = f.getFeature(parentID1, sessionToken);
			JSONObject json1 = new JSONObject(parent1);
			json1.put("name", "new parent1 name");
			
			//empty array of children under parent2
			String parent2 = f.getFeature(parentID2, sessionToken);
			JSONObject json2 = new JSONObject(parent2);
			json2.remove("features");
			json2.put("features", new JSONArray());

			json1.remove("features");
			JSONArray newChildren = new JSONArray();
			newChildren.put(json2);
			json1.put("features", newChildren);
			String response = f.updateFeature(seasonID, parentID1, json1.toString(), sessionToken); 
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
