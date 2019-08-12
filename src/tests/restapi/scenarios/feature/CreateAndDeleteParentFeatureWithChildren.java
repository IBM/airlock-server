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
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;

public class CreateAndDeleteParentFeatureWithChildren {
	protected String seasonID;
	protected String parentID;
	protected String productID;
	protected String childID1;
	protected String childID2;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected EmailNotification notification;
	private AirlockUtils baseUtils;


	@BeforeClass

	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String notify) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		notification = baseUtils.setNotification(notify, url, sessionToken);

	}


	/**
	 * Test creating a parent feature and adding 2 sub-features
	 * Create a parent feature, create 2 additional features and assign them a parent
	 */
	@Test (description = "Create a parent feature, create 2 additional features and assign them a parent")
	public void testAddParent() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath+"parent-feature.txt", "UTF-8", false);
			parent = JSONUtils.generateUniqueString(parent, 7, "name");
			parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			notification.followFeature(parentID);
			
			String child1 = FileUtils.fileToString(filePath+"feature1.txt", "UTF-8", false);
			child1 = JSONUtils.generateUniqueString(child1, 5, "name");
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);
			String child2 = FileUtils.fileToString(filePath+"feature2.txt", "UTF-8", false);
			child2 = JSONUtils.generateUniqueString(child2, 5, "name");
			childID2 = f.addFeature(seasonID, child2, parentID, sessionToken);
			
			parent = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			JSONArray children = json.getJSONArray("features");
			Assert.assertEquals(children.size(), 2, "Sub-features were not assigned to a parent.");
			
			if (children.size() == 2){
				JSONObject child = children.getJSONObject(0);
				String childId1 = child.getString("uniqueId");
				Assert.assertEquals(childId1, childID1);
				child = children.getJSONObject(1);
				String childId2 = child.getString("uniqueId");
				Assert.assertEquals(childId2, childID2);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	@Test (dependsOnMethods="testAddParent", description = "Delete a feature with 2 sub-features and validate that sub-features were deleted")
	public void testDeleteParent() throws JSONException{
		int response = f.deleteFeature(parentID, sessionToken);
		Assert.assertEquals(response, 200, "Parent feature was not removed.");
		String testChild = f.getFeature(childID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(testChild, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Child1 feature was not removed.");
		testChild = f.getFeature(childID2, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(testChild, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Child2 feature was not removed.");
	}

	@AfterTest
	private void reset(){
		f.unfollowFeature(parentID, sessionToken);
		baseUtils.getNotificationResult(notification);
		baseUtils.reset(productID, sessionToken);

	}

}
