package tests.restapi.scenarios.constants;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.StringUtils;
import tests.restapi.*;

import java.io.IOException;

public class ConstantsFilesNamespaceInDifferentCase {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String featureIDMaster1;
	private String featureIDMaster2;
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

	
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
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
	Add new feature to branch
	Add same feature to branch twice
	Add feature to master and checkout in branch
	Add feature to branch with name like in master
	Add all feature types
	- checkout from master and add new feature under it to branch
	-add new feature to branch with master feature as parent
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
	
	@Test (dependsOnMethods="addBranch", description ="Add 2 feature to master with different name and same ns in different case") 
	public void addFeaturesToMaster () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		featureJson.put("name", "feature1");
		featureJson.put("namespace", "AaA");
		featureIDMaster1 = f.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDMaster1.contains("error"), "Feature was not added to master: " + featureIDMaster1);

		
		featureJson.put("name", "feature2");
		featureJson.put("namespace", "aAa");
		featureIDMaster2 = f.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDMaster2.contains("error"), "Feature was not added to master: " + featureIDMaster2);

		String featureName1 = "AaA.feature1";
		String featureName2 = "aAa.feature2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName1));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName1));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName1));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName2));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName2));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName2));

		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains("public class AaA {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "Android").contains("public class aAa {"));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains("@objc class AaA_impl : NSObject {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "iOS").contains("@objc class aAa_impl : NSObject {"));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains("public class AaA{"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "c_sharp").contains("public class aAa{"));
	}
	
	@Test(dependsOnMethods="addFeaturesToMaster", description ="Add new feature to branch with the same namespace with different case")
	public void addFeatureToBranch() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		featureJson.put("name", "feature3");
		featureJson.put("namespace", "aaa");
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, featureJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		String featureName1 = "AaA.feature1";
		String featureName2 = "aAa.feature2";
		String featureName3 = "aaa.feature3";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName1));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName1));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName1));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName2));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName2));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName2));

		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName3));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName3));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName3));

		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains("public class AaA {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "Android").contains("public class aAa {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "Android").contains("public class aaa {"));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains("@objc class AaA_impl : NSObject {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "iOS").contains("@objc class aAa_impl : NSObject {"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "iOS").contains("@objc class aaa_impl : NSObject {"));
		
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains("public class AaA{"));
		Assert.assertFalse(baseUtils.getConstants(seasonID, "c_sharp").contains("public class aAa{"));	
		Assert.assertFalse(baseUtils.getConstants(seasonID, "c_sharp").contains("public class aaa{"));	
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
