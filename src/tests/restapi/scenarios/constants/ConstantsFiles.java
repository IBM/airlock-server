package tests.restapi.scenarios.constants;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.StringUtils;
import tests.restapi.*;

import java.io.IOException;

public class ConstantsFiles {
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
	
	@Test (dependsOnMethods="addBranch", description ="Add feature to master") 
	public void addFeatureToMaster () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureIDMaster = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureIDMaster.contains("error"), "Feature was not added to master: " + featureIDMaster);

		String featureName = "ns1.Feature1";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName));

	}
	
	@Test(dependsOnMethods="addFeatureToMaster", description ="Add new feature to branch")
	public void addFeatureToBranch() throws Exception{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		String featureName = "ns1.Feature2";
		Assert.assertTrue(baseUtils.getConstants(seasonID, "Android").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "iOS").contains(featureName));
		Assert.assertTrue(baseUtils.getConstants(seasonID, "c_sharp").contains(featureName));

		featureIDBranch = f.addFeatureToBranch(seasonID, branchID2, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);

		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "Android"))==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "iOS")) ==1);
		Assert.assertTrue(StringUtils.howManyOccurrences(featureName,baseUtils.getConstants(seasonID, "c_sharp")) ==1);

	}

	@Test(dependsOnMethods="addFeatureToBranch", description ="Add new feature to branch")
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

	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
