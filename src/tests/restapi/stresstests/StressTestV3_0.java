package tests.restapi.stresstests;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;

public class StressTestV3_0 {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
	private int count=1;
	private JSONObject body = new JSONObject();
	private int loops1;
	private int loops2;
	private String featureToCheckoutProdId;
	private String featureToCheckoutDevId;
	private JSONObject feature;
	private ArrayList<String> branches = new ArrayList<String>();
	private String stringId;
	protected StringsRestApi stringsApi;
	
	@BeforeClass
	@Parameters({"url", "testUrl",  "analyticsUrl", "translationsUrl", "configPath", "userGroups", "firstIteration", "secondIteration", "featureToCheckoutProd", "featureToCheckoutDev", "stringToUpdate", "sessionToken", "productsToDeleteFile", "userName", "userPassword", "appName"})
	public void init(String url, String testUrl, String analyticsUrl, String translationsUrl, String configPath, String userGroups, String firstIteration, String secondIteration, String featureToCheckoutProd, String featureToCheckoutDev, String stringToUpdate, @Optional String sToken, String productsToDeleteFile, String userName, String userPassword, String appName) throws Exception{
		filePath = configPath;
		m_url = testUrl;

		loops1 = Integer.parseInt(firstIteration);
		loops2 = Integer.parseInt(secondIteration);
		featureToCheckoutProdId = featureToCheckoutProd;
		featureToCheckoutDevId = featureToCheckoutDev;
		stringId = stringToUpdate;
		
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		f = new FeaturesRestApi();
		f.setURL(url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		body.put("path", "vicky/PRODUCTION_DATA/35660b4f-4f79-4f8a-a9ea-ffb93bf91dc7/5244393e-9ad5-4383-9f85-8e52aa8b02ca");
		
		body.put("productName", "AndroidPerformanceProduct");
		body.put("productId", "35660b4f-4f79-4f8a-a9ea-ffb93bf91dc7");
		body.put("seasonId", "5244393e-9ad5-4383-9f85-8e52aa8b02ca");
		body.put("minVersion", "9.0");
		
		productID = "35660b4f-4f79-4f8a-a9ea-ffb93bf91dc7";
		seasonID = "5244393e-9ad5-4383-9f85-8e52aa8b02ca";

		feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		//add required usergroups
		String userGroupsList = FileUtils.fileToString(userGroups, "UTF-8", false);
		String[] userList = userGroupsList.split(",");
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url + "/usergroups", sessionToken);
		String response = res.message;
		JSONObject json = new JSONObject(response);
		

		JSONArray currentGroups = new JSONObject(response).getJSONArray("internalUserGroups");
		List<String> newGroupsList = new ArrayList<String>();
		//add all existing groups
		for (int i=0; i<currentGroups.length(); i++){
			newGroupsList.add(currentGroups.getString(i));
		}
		//add new groups
		for (String group : userList){
			if (!currentGroups.contains(group)) {
				newGroupsList.add(group);
			} 
		}
		json.put("internalUserGroups", newGroupsList);
		try {
			res = RestClientUtils.sendPut( url + "/usergroups", json.toString(), sessionToken);
			if (res.code != 200)
				System.out.println("Could not update user groups. Some feature may not be created " + res.message);
		} catch (Exception e){
			System.out.println("Could not update user groups. Some feature may not be created " + e.getLocalizedMessage());
		}
	
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Test (description="Run performance test")
	public void run() throws Exception{
		//copy product
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url, body.toString(), sessionToken);
		Assert.assertTrue(res.code==200, "Product was not copied");
	
		for (int i=0; i<loops1; i++) {
			experimentID = addExperiment("experiment"+count);
			Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
			
			if (i==0)	//first branch is created from master
				branchID = addBranch("branch"+count, BranchesRestApi.MASTER);
			else	//all other branches are created from the previous branch 
				branchID = addBranch("branch"+count, branchID);
			Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
			branches.add(branchID);
			
			System.out.println("In 1st iteration. Added branch #" + count);

			variantID = addVariant("variant"+count, "branch"+count);
			Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
			
			//checkout 2 features only in the first branch
			if (i==0){
				String response = br.checkoutFeature(branchID, featureToCheckoutProdId, sessionToken);
				Assert.assertFalse(response.contains("error"), "Can't checkout prod feature: " + response);
				response = br.checkoutFeature(branchID, featureToCheckoutDevId, sessionToken);
				Assert.assertFalse(response.contains("error"), "Can't checkout dev feature: " + response);
			}
			
			//add feature
			feature.put("name", "NewFeatureIteration1 "+count);
			String featureID = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
			
			//report new feature to analytics
			an.addFeatureToAnalytics(featureID, branchID, sessionToken);

			//update checked out development feature in MASTER
			JSONObject devFeature = new JSONObject(f.getFeature(featureToCheckoutDevId, sessionToken));
			devFeature.put("name", "HeadsUpDev"+count);
			String response = f.updateFeature(seasonID, featureToCheckoutDevId, devFeature.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't update feature: " + response);
			count++;			
		}
		
		
		count = 1;
		//iterate branches
		for (int outLoop=0; outLoop<loops2; outLoop++){
			System.out.println("In 2nd iteration. Loop #" + outLoop);
			for (int i=0; i<branches.size(); i++){
				System.out.println("In 2nd iteration. Working on branch #" + i);
				
				String currentBranch = (String)branches.get(i);
				
				//add feature to master and check it out to branch
				feature.put("name", "NewFeatureIteration2 "+count);
				String featureID1 = f.addFeature(seasonID, feature.toString(), "ROOT", sessionToken);
				br.checkoutFeature(currentBranch, featureID1, sessionToken);
				
				//report checked out feature to analytics in branch
				an.addFeatureToAnalytics(featureID1, currentBranch, sessionToken);
				
				//add sub-feature
				feature.put("name", "NewSubFeatureIteration2 "+count);
				String subfeatureID = f.addFeatureToBranch(seasonID, currentBranch, feature.toString(), featureID1, sessionToken);
	
				//move sub-feature to root
				JSONObject subfeature = new JSONObject(f.getFeatureFromBranch(subfeatureID, currentBranch, sessionToken));
				
				String rootId = f.getBranchRootId(seasonID, currentBranch, sessionToken);
				JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, currentBranch, sessionToken));
				JSONArray children = root.getJSONArray("features");
				for (int j=0; j<children.size(); j++){
					JSONObject curFeature = children.getJSONObject(j);
					if (curFeature.getString("uniqueId").equals(featureID1)){  //remove subfeature from previous parent
						curFeature.put("features", new JSONArray());
					}
				}
				children.add(subfeature);
				root.put("features", children);
				
				String response = f.updateFeatureInBranch(seasonID, currentBranch, rootId, root.toString(), sessionToken);
				Assert.assertFalse(response.contains("error"), "Can't update root in branch: " + response);
				
				count++;
			}
			
			//move string from production to development
			JSONObject stringToUpdate = new JSONObject(stringsApi.getString(stringId, sessionToken));
			if (stringToUpdate.getString("stage").equals("PRODUCTION"))
				stringToUpdate.put("stage", "DEVELOPMENT");
			else
				stringToUpdate.put("stage", "PRODUCTION");
			String response = stringsApi.updateString(stringId, stringToUpdate.toString(), sessionToken);
			

		}	
		
	}

	private String addExperiment(String experimentName) throws IOException, JSONException{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", experimentName);
		expJson.put("minVersion", "9.0");
		expJson.put("maxVersion", "10.0");
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("stage", "PRODUCTION");
		return exp.createExperiment(productID, expJson.toString(), sessionToken);

	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName, String branchSource) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), branchSource, sessionToken);

	}
	

	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
