package tests.restapi.scenarios.constants;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.StringUtils;
import tests.restapi.*;

import java.io.IOException;

public class ConstantsFilesWithEntitlements {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String featureIDMaster;
	private String featureIDBranch;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private String constantsAndroid;
	private String constantsiOS;
	private String constantsCSharp;
	protected InAppPurchasesRestApi purchasesApi;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
	
	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);	
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		constantsAndroid = baseUtils.getConstants(seasonID, "Android");
		constantsiOS = baseUtils.getConstants(seasonID, "iOS");
		constantsCSharp = baseUtils.getConstants(seasonID, "c_sharp");

	}
	
	/*
	Add new entitlement to branch
	Add same entitlement to branch twice
	Add entitlement to master and checkout in branch
	Add entitlement to branch with name like in master
	Add all entitlement types
	- checkout from master and add new entitlement under it to branch
	-add new entitlement to branch with master entitlement as parent
	 */

	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		branch = FileUtils.fileToString(filePath + "experiments/branch2.txt", "UTF-8", false);
		branchID2= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").equals(constantsAndroid));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").equals(constantsiOS));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").equals(constantsCSharp));

	}
	
	@Test (dependsOnMethods="addBranch", description ="Add entitlement to master") 
	public void addEntitlementToMaster () throws IOException, JSONException, InterruptedException {
		
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject entitlementObj = new JSONObject(entitlementStr);
		entitlementObj.put("namespace", "airlockEntitlement");
		entitlementObj.put("name", "entitlement1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (entitlementID1.contains("error"), "Can't add entitlementID1 to branch: " + entitlementID1);
			
		String entitlementName = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlementName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlementName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlementName));
	}

	@Test (dependsOnMethods="addEntitlementToMaster", description ="Add purchaseOptions to master") 
	public void addPurchaseOptionsToMaster() throws IOException, JSONException, InterruptedException {
		
		String purchaseOptionsStr = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject purchaseOptionsObj = new JSONObject(purchaseOptionsStr);
		purchaseOptionsObj.put("namespace", "airlockEntitlement");
		purchaseOptionsObj.put("name", "purchaseOptions1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsObj.toString(), entitlementID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID1.contains("error"), "Can't add purchaseOptionsID1 to branch: " + purchaseOptionsID1);
			
		String entitlementName = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlementName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlementName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlementName));
		
		String purchaseOptionsName = "airlockEntitlement.purchaseOptions1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptionsName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptionsName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptionsName));
	}

	@Test(dependsOnMethods="addPurchaseOptionsToMaster", description ="Add new entitlement to branch")
	public void addEntitlementToBranch() throws Exception{
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject entitlementObj = new JSONObject(entitlementStr);
		entitlementObj.put("namespace", "airlockEntitlement");
		entitlementObj.put("name", "entitlement2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (entitlementID2.contains("error"), "Can't add entitlementID2 to branch: " + entitlementID2);
		
		String entitlement1Name = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement1Name));
		
		String entitlement2Name = "airlockEntitlement.entitlement2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement2Name));
		
		String purchaseOptionsName = "airlockEntitlement.purchaseOptions1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptionsName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptionsName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptionsName));		
	}
	
	@Test(dependsOnMethods="addEntitlementToBranch", description ="Add new purchaseOptions to branch")
	public void addPurchaseOptionsToBranch() throws Exception{
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		JSONObject entitlementObj = new JSONObject(entitlementStr);
		entitlementObj.put("namespace", "airlockEntitlement");
		entitlementObj.put("name", "purchaseOptions2");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementObj.toString(), entitlementID2, sessionToken);
		Assert.assertFalse (purchaseOptionsID2.contains("error"), "Can't add purchaseOptionsID2 to branch: " + purchaseOptionsID2);
		
		String entitlement1Name = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement1Name));
		
		String entitlement2Name = "airlockEntitlement.entitlement2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement2Name));
		
		String purchaseOptions1Name = "airlockEntitlement.purchaseOptions1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions1Name));
		
		String purchaseOptions2Name = "airlockEntitlement.purchaseOptions2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions2Name));
	}

	@Test(dependsOnMethods="addPurchaseOptionsToBranch", description ="Add new feature to branch")
	public void addFeatureToBranchDifferentCase() throws Exception{
		String feature3 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject feature3Obj = new JSONObject(feature3);
		feature3Obj.put("name", "feature3");
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, feature3Obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		
		String featureName = "ns1.feature3";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName));
		

		feature3Obj.put("name", "Feature3");
		
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID2, feature3Obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "Android"))==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "iOS")) ==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "c_sharp")) ==1);

		String entitlement1Name = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement1Name));
		
		String entitlement2Name = "airlockEntitlement.entitlement2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement2Name));
		
		String purchaseOptions1Name = "airlockEntitlement.purchaseOptions1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions1Name));
		
		String purchaseOptions2Name = "airlockEntitlement.purchaseOptions2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions2Name));
	}
	
	@Test(dependsOnMethods="addFeatureToBranchDifferentCase", description ="Add new feature to branch")
	public void addFeatureDifferentNamespace() throws Exception{
		String feature4 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject feature4Obj = new JSONObject(feature4);
		feature4Obj.put("name", "feature4");
		feature4Obj.put("namespace", "ns2");
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, feature4Obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		
		String featureName = "ns2.feature4";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName));
		

		String feature5 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject feature5Obj = new JSONObject(feature5);
		feature5Obj.put("name", "feature5");
		feature5Obj.put("namespace", "ns2");
		featureIDBranch = f.addFeature(seasonID, feature5Obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added: " + featureIDBranch);

	    featureName = "ns2.feature5";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName));
		
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "Android"))==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "iOS")) ==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "c_sharp")) ==1);

		String entitlement1Name = "airlockEntitlement.entitlement1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement1Name));
		
		String entitlement2Name = "airlockEntitlement.entitlement2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(entitlement2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(entitlement2Name));
		
		String purchaseOptions1Name = "airlockEntitlement.purchaseOptions1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions1Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions1Name));
		
		String purchaseOptions2Name = "airlockEntitlement.purchaseOptions2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(purchaseOptions2Name));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(purchaseOptions2Name));
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
