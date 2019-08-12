package tests.restapi.scenarios.notificationMail;

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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;

public class EmailNotificationPurchaseItemsScenarios {
	private String sessionToken = "";
	private String adminToken = "";
	private String productID;
	private String seasonID;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	protected String m_url;
	private String filePath ;
	protected EmailNotification notification;
	private String m_notify;
	private String entitlement55ID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify) throws Exception{
		m_url = url;
		filePath = configPath;

		if(notify != null){
			m_notify = notify;
		}

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
		sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName);
		baseUtils.setSessionToken(adminToken);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	}

	/*
	 * 
	 * Create entitlement in dev, follow, update name, move to production, update description
	 * Add 3 children, reorder children, update child name, move 1 child to prod, delete 1 child
	 * Move child1 to production,  move child1 to development
	 * delete child2,
	 * add configuration rule, delete configuration rule
	 * Move parent to development
	 * 
	 */

	@Test (description="Follow parent entitlement")
	public void followParentEntitlement() throws IOException, JSONException{
		notification = baseUtils.setNotification(m_notify, m_url, adminToken);

		//add entitlement in development
		JSONObject entitlementObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlementObj.put("name", "entitlement1");
		String parentID = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentID.contains("error"), "entitlement was not added: " + parentID);

		String response = f.followFeature(parentID, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed to follow: " + response);

		//update entitlement name
		String parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		JSONObject parentJson = new JSONObject(parent);
		parentJson.put("name", "new parent1");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//move parent entitlement to production
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//update parent entitlement description
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("description", "new description");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);


		//add 3 child entitlements in development
		entitlementObj.put("name", "child1");
		String childID1 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID1.contains("error"), "entitlement was not added" + childID1);

		entitlementObj.put("name", "child2");
		String childID2 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID2.contains("error"), "entitlement was not added" + childID2);

		entitlementObj.put("name", "child3");
		String childID3 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID3.contains("error"), "entitlement was not added" + childID3);

		//Move child1 to production, move child1 to development
		String child1 = purchasesApi.getPurchaseItem(childID1, sessionToken);
		JSONObject child1Json = new JSONObject(child1);
		child1Json.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, childID1, child1Json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't move child1 to production");
		child1 = purchasesApi.getPurchaseItem(childID1, sessionToken);
		child1Json = new JSONObject(child1);
		child1Json.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, childID1, child1Json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't move child1 to development");

		//reorder children
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		JSONArray newChildrenList = new JSONArray();
		JSONArray orgChildrenList = parentJson.getJSONArray("entitlements");
		newChildrenList.add(orgChildrenList.getJSONObject(1));
		newChildrenList.add(orgChildrenList.getJSONObject(2));
		newChildrenList.add(orgChildrenList.getJSONObject(0));
		parentJson.put("entitlements", newChildrenList);
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//Delete child2 - email is not sent for deleted sub-features
		int responseCode = purchasesApi.deletePurchaseItem(childID2, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete child2");

		//Add configuration rule
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject configJson = new JSONObject(configuration);
		configJson.put("name", "config1");
		String configRuleID = purchasesApi.addPurchaseItem(seasonID, configJson.toString(), parentID, sessionToken ); 

		//delete configuration rule
		responseCode = purchasesApi.deletePurchaseItem(configRuleID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete configuration rule");

		//move parent feature to development
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);

		//update parent entitlement includedEntitlements
		entitlementObj.put("name", "entitlement55");
		entitlement55ID = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlement55ID.contains("error"), "entitlement was not added: " + entitlement55ID);
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		JSONArray includedEntitlementsArr = new JSONArray();
		includedEntitlementsArr.add(entitlement55ID);
		parentJson.put("includedEntitlements", includedEntitlementsArr);
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//delete entitlement
		responseCode = purchasesApi.deletePurchaseItem(parentID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete parent entitlement");

		//parse follow entitlement result
		JSONObject result = baseUtils.getNotificationResult(notification);
		Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of entitlement followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("new parent1"), "Updated entitlement name  was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("PRODUCTION"), "Updated entitlement stage to production was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("itemType").contains("ENTITLEMENT"), "illegal type");
		
		Assert.assertTrue(getFollowersSize(result, 1)==1, "Incorrect number of entitlement followers");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("new description"), "Updated entitlement description was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 2)==1, "Incorrect number of entitlement followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("details").contains("child1 was created"), "Add child1 entitlement was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 3)==1, "Incorrect number of entitlement followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(4).getString("details").contains("child2 was created"), "Add child2 entitlement was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(4).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(5).getString("details").contains("child3 was created"), "Add child3 entitlement was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(5).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("details").contains("The order of sub-items under entitlement"), "reorder sub items");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("itemType").contains("ENTITLEMENT"), "illegal type");
		 
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(7).getString("details").contains("config1 was created"), "Add configuration rule was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 7)==1, "Incorrect number of entitlement followers");			
		
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(8).getString("details").contains("config1 was deleted"), "Delete configuration rule was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(8).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(9).getString("details").contains("to DEVELOPMENT") && result.getJSONArray("allEmails").getJSONObject(9).getString("item").contains("parent1"), "Parent1 stage upated was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(9).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(getFollowersSize(result, 9)==1, "Incorrect number of entitlement followers");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(10).getString("details").contains("includedEntitlements") && result.getJSONArray("allEmails").getJSONObject(10).getString("details").contains("ns1.entitlement55"), "Delete followed entitlement  was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(10).getString("itemType").contains("ENTITLEMENT"), "illegal type");

		Assert.assertTrue(getFollowersSize(result, 10)==1, "Incorrect number of entitlement followers");
		
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(11).getString("details").contains("new parent1 was deleted"), "Delete followed entitlement  was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 11)==1, "Incorrect number of entitlement followers");
	}

	/*
	 * 
	 *- Add parent entitlement + 3 children entitlement are IN DEVELOPMENT
			- Mark all children to be Followed
			- Move parent feature to Production - 
			- Update parent 
	 */

	@Test (dependsOnMethods = "followParentEntitlement", description="Follow children entitlements")
	public void followChildrenEntitlement() throws IOException, JSONException{
		notification = baseUtils.setNotification(m_notify, m_url, adminToken);

		//add entitlement in development
		JSONObject entitlementObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlementObj.put("name", "entitlement1");
		String parentID = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentID.contains("error"), "entitlement was not added: " + parentID);

		//add 3 child features in development and follow each of them
		entitlementObj.put("name", "child21");
		String childID1 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID1.contains("error"), "entitlement was not added: " + childID1);
		String response = f.followFeature(childID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed to follow: " + response);

		entitlementObj.put("name", "child22");
		String childID2 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID2.contains("error"), "entitlement was not added: " + childID2);
		response = f.followFeature(childID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed to follow: " + response);

		entitlementObj.put("name", "child23");
		String childID3 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), parentID, sessionToken);
		Assert.assertFalse(childID3.contains("error"), "entitlement was not added: " + childID3);
		response = f.followFeature(childID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed to follow: " + response);

		//move parent entitlement to production
		String parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		JSONObject parentJson = new JSONObject(parent);
		parentJson.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//update parent entitlement description
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("description", "new description");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated" + response);

		//Move child1 to production, move child1 to development
		String child1 = purchasesApi.getPurchaseItem(childID1, sessionToken);
		JSONObject child1Json = new JSONObject(child1);
		child1Json.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, childID1, child1Json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't move child21 to production");
		child1 = purchasesApi.getPurchaseItem(childID1, sessionToken);
		child1Json = new JSONObject(child1);
		child1Json.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, childID1, child1Json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't move child21 to development");

		//Delete child2
		int responseCode = purchasesApi.deletePurchaseItem(childID2, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete child2");

		//parse follow feature result
		JSONObject result = baseUtils.getNotificationResult(notification);
		
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("to PRODUCTION") && result.getJSONArray("allEmails").getJSONObject(0).getString("item").contains("child21"), "Feature stage  was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of entitlement followers");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("to DEVELOPMENT") && result.getJSONArray("allEmails").getJSONObject(1).getString("item").contains("child21"), "Feature stage  was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 1)==1, "Incorrect number of entitlement followers");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("child22 was deleted") && result.getJSONArray("allEmails").getJSONObject(2).getString("item").contains("child22"), "Deleted feature  was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 2)==1, "Incorrect number of entitlement followers");

	}

	/*
	 * 
	 * Create purchaseOptions in dev, follow, update name, move to production, update description
	 * Add 3 children, reorder children, update child name, move 1 child to prod, delete 1 child
	 * Move child1 to production,  move child1 to development
	 * delete child2,
	 * add configuration rule, delete configuration rule
	 * Move parent to development
	 * 
	 */

	@Test (dependsOnMethods = "followChildrenEntitlement", description="Follow parent purchaseOptions")
	public void followParentPurchaseOptions() throws IOException, JSONException{
		notification = baseUtils.setNotification(m_notify, m_url, adminToken);

		//add entitlement in prod
		JSONObject entitlementObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlementObj.put("name", "entitlement2");
		entitlementObj.put("stage", "PRODUCTION");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added: " + entitlementID);

				
		//add purchaseOptions in development
		JSONObject purchaseOptionsObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
		purchaseOptionsObj.put("name", "purchaseOptions1");
		String parentID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsObj.toString(), entitlementID, sessionToken);
		Assert.assertFalse(parentID.contains("error"), "purchaseOptions was not added: " + parentID);

		String response = f.followFeature(parentID, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed to follow: " + response);

		//update purchaseOptions name
		String parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		JSONObject parentJson = new JSONObject(parent);
		parentJson.put("name", "new parent1");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not updated" + response);

		//move parent purchaseOptions to production
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not updated" + response);

		//update parent purchaseOptions description
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("description", "new description");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not updated" + response);


		//Add configuration rule
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject configJson = new JSONObject(configuration);
		configJson.put("name", "config1");
		String configRuleID = purchasesApi.addPurchaseItem(seasonID, configJson.toString(), parentID, sessionToken ); 

		//delete configuration rule
		int responseCode = purchasesApi.deletePurchaseItem(configRuleID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete configuration rule");

		//move parent purchaseOptions to development
		parent = purchasesApi.getPurchaseItem(parentID, sessionToken) ;
		parentJson = new JSONObject(parent);
		parentJson.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, parentID, parentJson.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not updated" + response);

		//delete purchaseOptions
		responseCode = purchasesApi.deletePurchaseItem(parentID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete parent purchaseOptions");

		//parse follow purchaseOptions result
		JSONObject result = baseUtils.getNotificationResult(notification);
		Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of purchaseOptions followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("new parent1"), "Updated purchaseOptions name  was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("PRODUCTION"), "Updated purchaseOptions stage to production was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 1)==1, "Incorrect number of feature followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("new description"), "Updated purchaseOptions description was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 2)==1, "Incorrect number of feature followers");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("details").contains("config1 was created"), "Add configuration rule was not registered notification");
		Assert.assertTrue(getFollowersSize(result, 3)==1, "Incorrect number of feature followers");			
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");
		
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(4).getString("details").contains("config1 was deleted"), "Delete configuration rule was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(4).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");
		
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(5).getString("details").contains("to DEVELOPMENT") && result.getJSONArray("allEmails").getJSONObject(5).getString("item").contains("parent1"), "Parent1 stage upated was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(5).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");
		Assert.assertTrue(getFollowersSize(result, 5)==1, "Incorrect number of purchaseOptions followers");

		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("details").contains("new parent1 was deleted"), "Delete followed purchaseOptions  was not registered notification");
		Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("itemType").contains("PURCHASE_OPTIONS"), "illegal type");
		Assert.assertTrue(getFollowersSize(result, 6)==1, "Incorrect number of purchaseOptions followers");
	}
	
	@Test (dependsOnMethods = "followParentPurchaseOptions", description="Update premium feature's entitlement")
	public void updatePremiumFeatureEntitlement() throws IOException, JSONException{
		
		//add purchaseOptions in development
		JSONObject purchaseOptionsObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
		purchaseOptionsObj.put("name", "purchaseOptions55");
		String poID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsObj.toString(), entitlement55ID, sessionToken);
		Assert.assertFalse(poID.contains("error"), "purchaseOptions was not added: " + poID);

		notification = baseUtils.setNotification(m_notify, m_url, adminToken);
		
		//add feature in development
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "feature1");
		String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		
		String response = f.followFeature(parentID, sessionToken);
		
		//update feature premium fields
		String parent = f.getFeature(parentID, sessionToken) ;
		JSONObject jsonF = new JSONObject(parent);
		JSONObject premiumRule = new JSONObject();
		jsonF.put("ruleString", "true;");
		jsonF.put("premiumRule", premiumRule);
		jsonF.put("entitlement", entitlement55ID);
		jsonF.put("premium", "true");
		
		response = f.updateFeature(seasonID, parentID, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
		
		JSONObject result = baseUtils.getNotificationResult(notification);
		Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of purchaseOptions followers");
		JSONObject emailContent = result.getJSONArray("allEmails").getJSONObject(0);
		Assert.assertTrue(emailContent.getString("item").contains("ns1.feature1"), "Updated feature name  was not registered notification");
		Assert.assertTrue(emailContent.getString("itemType").contains("FEATURE"), "illegal type");
		Assert.assertTrue(emailContent.getString("details").contains("'entitlement'"), "Updated feature entitlement was not registered notification");
		Assert.assertTrue(emailContent.getString("details").contains("ns1.entitlement55"), "Updated entitlement name was not registered notification");
		Assert.assertTrue(emailContent.getString("details").contains("'premium'"), "Updated feature premium was not registered notification");
		Assert.assertTrue(emailContent.getString("details").contains("'premiumRule'"), "Updated feature premium rule was not registered notification");
	}


	private int getFollowersSize(JSONObject result, int index) throws JSONException{
		int size = 0;
		if (!result.getJSONArray("allEmails").getJSONObject(index).isNull("followers")) 
			size = result.getJSONArray("allEmails").getJSONObject(index).getJSONArray("followers").size();				

		return size;	
	}

	@AfterTest
	private void reset(){
		p.unfollowProduct(productID, sessionToken);
		baseUtils.reset(productID, sessionToken);
	}
}
