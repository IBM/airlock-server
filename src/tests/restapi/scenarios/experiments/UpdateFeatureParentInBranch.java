package tests.restapi.scenarios.experiments;

import java.io.IOException;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateFeatureParentInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;

		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);
        f = new FeaturesRestApi();
        f.setURL(m_url);
        
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		

	}

	/*	- update unchecked feature in branch
	 * - add new items under unchecked feature in branch
	 * move:
	 * 	- unchecked under new
		- unchecked under unchecked
		- unchecked under checked
		- unchecked to root
	  	- checked under new
		- checked under unchecked
		- checked under checked	
		- checked to root	
		- new under unchecked
		- new under checked
		- new under new
		- new to branch root
		add:
		- new under unchecked
		- new under checked
		- new under new
		delete
		- unchecked from new
		- unchecked from unchecked
		- unchecked from checked
	  	- checked from new
		- checked from unchecked
		- checked from new		
		- new from unchecked
		- new from checked
		- new from new
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		//checkout F2
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		jsonF.put("name", "F3");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID3);
	
		jsonF.put("name", "F4");
		featureID4 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature4 was not added: " + featureID3);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update unchecked out feature") 
	public void updateUncheckedFeatureInBranch () throws IOException, JSONException {
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("description", "New description");
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated feature that is not checked out in branch");
	}
	
	@Test (dependsOnMethods="updateUncheckedFeatureInBranch", description ="Add children in branch to unchecked out feature") 
	public void addChildrenToUncheckedFeatureInBranch () throws IOException, JSONException {
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String response = f.addFeatureToBranch(seasonID, branchID, feature3, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature3 was added to unchecked feature in branch");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		response = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "MIX was added to unchecked feature in branch");

		String configMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		response = f.addFeatureToBranch(seasonID, branchID, configMix, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration MIX was added to unchecked feature in branch");
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		response = f.addFeatureToBranch(seasonID, branchID, configuration1, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration rule1 was added to unchecked feature in branch");


	}
	
	@Test (dependsOnMethods="addChildrenToUncheckedFeatureInBranch", description ="Move unchecked feature under checked out feature - ok") 
	public void moveUncheckedFeatureToChecked () throws IOException, JSONException {
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = feature2.getJSONArray("features");
		children.put(feature1);
		feature2.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't move unchecked feature under checked out feature");
		
		int res = uniqueness(getRoot(), feature1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature1.getString("name") + " was found " + res + " times in the tree");
	}
	

	@Test (dependsOnMethods="moveUncheckedFeatureToChecked", description ="Move unchecked feature under new feature - ok") 
	public void moveUncheckedFeatureToNewFeature () throws IOException, JSONException {
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = feature4.getJSONArray("features");
		children.put(feature1);
		feature4.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, feature4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move unchecked out feature under new feature");

		int res = uniqueness(getRoot(), feature1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveUncheckedFeatureToNewFeature", description ="Move unchecked feature under unchecked feature - fails") 
	public void moveUncheckedFeatureToUnchecked () throws IOException, JSONException {
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = feature3.getJSONArray("features");
		children.put(feature1);
		feature3.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, feature3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move unchecked out feature under unchecked feature");

		int res = uniqueness(getRoot(), feature1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");


	}
	
	@Test (dependsOnMethods="moveUncheckedFeatureToUnchecked", description ="Move unchecked feature under unchecked feature - ok") 
	public void moveUncheckedFeatureToRoot () throws IOException, JSONException {
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("features");
		children.put(feature1);
		root.put("features", children);
		
		//F1 previous parent is F4 and must be removed from there
		root = removeChildFromFeature( root, featureID4, featureID1);
		
		//root is also unchecked, can't moved under unchecked root
		String response = f.updateFeatureInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move unchecked out feature under root");

		int res = uniqueness(getRoot(), feature1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");


	}

	@Test (dependsOnMethods="moveUncheckedFeatureToRoot", description ="Move checked feature under unchecked out feature - fails") 
	public void moveCheckedFeatureToUnchecked () throws IOException, JSONException {
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = feature1.getJSONArray("features");
		children.put(feature2);
		feature1.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, feature1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked feature under unchecked out feature");
		
		int res = uniqueness(getRoot(), feature2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}
	

	@Test (dependsOnMethods="moveCheckedFeatureToUnchecked", description ="Move checked feature under new feature - ok") 
	public void moveCheckedFeatureToNewFeature () throws IOException, JSONException {
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = feature4.getJSONArray("features");
		children.put(feature2);
		feature4.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, feature4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move checked out feature under new feature");

		int res = uniqueness(getRoot(), feature2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}
	
	@Test (dependsOnMethods="moveCheckedFeatureToNewFeature", description ="Move checked feature under checked feature - ok") 
	public void moveCheckedFeatureToChecked () throws IOException, JSONException {
		//checkout F3
		String response = br.checkoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature3 was not checked out to branch");

		
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = feature3.getJSONArray("features");
		children.put(feature2);
		feature3.put("features", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID3, feature3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out feature under checked feature");

		int res = uniqueness(getRoot(), feature2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}
	
	@Test (dependsOnMethods="moveCheckedFeatureToChecked", description ="Move checked feature to root - ok") 
	public void moveCheckedFeatureToRoot () throws IOException, JSONException {
		
		//checkout root
		String rootId = f.getRootId(seasonID, sessionToken);
		String response = br.checkoutFeature(branchID, rootId, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't check out root");
		
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));		
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("features");
		children.put(feature2);
		root.put("features", children);
		
		//F2 previous parent is F3 and must be removed from there
		root = removeChildFromFeature( root, featureID3, featureID2);
		
		response = f.updateFeatureInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move checked out feature under root");

		int res = uniqueness(getRoot(), feature2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}



	@Test (dependsOnMethods="moveCheckedFeatureToRoot", description ="Move new feature under unchecked out feature - fails") 
	public void moveNewFeatureToUnchecked () throws IOException, JSONException {
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = feature1.getJSONArray("features");
		children.put(feature4);
		feature1.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, feature1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked feature under unchecked out feature");
		
		int res = uniqueness(getRoot(), feature4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}


	@Test (dependsOnMethods="moveNewFeatureToUnchecked", description ="Move new feature under new feature - ok") 
	public void moveNewFeatureToNewFeature () throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "FB1");
		String FB1Id = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(FB1Id.contains("error"), "New feature FB1 was not added to branch: " + FB1Id);
		
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONObject newInBranch = new JSONObject(f.getFeatureFromBranch(FB1Id, branchID, sessionToken));
		
		JSONArray children = newInBranch.getJSONArray("features");
		children.put(feature4);
		newInBranch.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, FB1Id, newInBranch.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move checked out feature under new feature");
	
		int res = uniqueness(getRoot(), feature4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}

	@Test (dependsOnMethods="moveNewFeatureToNewFeature", description ="Move new feature to checked feature - ok") 
	public void moveNewFeatureToChecked () throws IOException, JSONException {
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = feature2.getJSONArray("features");
		children.put(feature4);
		feature2.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new feature under checked feature");
		
		int res = uniqueness(getRoot(), feature4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}

	@Test (dependsOnMethods="moveNewFeatureToChecked", description ="Move new feature to root - ok") 
	public void moveNewFeatureToRoot () throws IOException, JSONException {
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("features");
		children.put(feature4);
		root.put("features", children);
		
		
		//F4 previous parent is F2 and must be removed from there
		root = removeChildFromFeature( root, featureID2, featureID4);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move new feature under root");
	
		int res = uniqueness(getRoot(), feature4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + feature4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "Feature " + key + " was found more than once in branchFeatureParentName");

	}
	
	@Test (dependsOnMethods="moveNewFeatureToRoot", description ="Add new feature under unchecked feature - ok") 
	public void addNewFeatureToUncheckedFeature () throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "new1");
		String newId1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertTrue(newId1.contains("error"), "New feature  was added to branch under unchecked feature: " + newId1);
	
	}
	
	@Test (dependsOnMethods="addNewFeatureToUncheckedFeature", description ="Add new feature under new feature - ok") 
	public void addNewFeatureToNewFeature () throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "new2");
		String newId1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), featureID4, sessionToken);
		Assert.assertFalse(newId1.contains("error"), "New feature  was not added to branch under new feature: " + newId1);
	
	}
	
	@Test (dependsOnMethods="addNewFeatureToNewFeature", description ="Add new feature under checked feature - ok") 
	public void addNewFeatureToCheckedFeature () throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "new3");
		String newId1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), featureID2, sessionToken);
		Assert.assertFalse(newId1.contains("error"), "New feature  was not added to branch under checked feature: " + newId1);
	
	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private  JSONObject removeChildFromFeature(JSONObject root,  String parentId, String childId) throws JSONException{
		
		JSONArray features = root.getJSONArray("features");
		for (int i=0; i<features.size(); i++){
			JSONObject feature = features.getJSONObject(i);
			if (feature.getString("uniqueId").equals(parentId)){	//find old parent
				JSONArray children = feature.getJSONArray("features");
				for (int j=0; j< children.size(); j++){
					if (children.getJSONObject(j).getString("uniqueId").equals(childId)){
						children.remove(j);
					}
				}
			}
	
		}
		return root;
		
	}
	
	
	private int uniqueness(JSONObject startFrom, String featureName, ArrayList<Integer>  featureFound) throws JSONException{
		int found = featureFound.get(0);
		for (int i=0; i<startFrom.getJSONArray("features").size(); i++){
			JSONObject feature = startFrom.getJSONArray("features").getJSONObject(i);
			//System.out.println(feature.getString("name"));
			if (feature.getString("name").equals(featureName)) {
				found++;
				featureFound.add(0, found);
			}	
			
			if (feature.getJSONArray("features").size()>0)
				uniqueness(feature, featureName, featureFound);
		}
		
		return featureFound.get(0);
	}
	
	private HashMap<String, Integer> uniqueChildren(JSONObject startFrom, HashMap<String, Integer> children) throws JSONException{		
		for (int i=0; i<startFrom.getJSONArray("features").size(); i++){
			JSONObject feature = startFrom.getJSONArray("features").getJSONObject(i);						
			if (feature.containsKey("branchFeaturesItems")) {
				for(int j=0; j<feature.getJSONArray("branchFeaturesItems").size(); j++){
					if (children.containsKey(feature.getJSONArray("branchFeaturesItems").getString(j)))		{
						children.put(feature.getJSONArray("branchFeaturesItems").getString(j), Integer.valueOf(2));
					}
						
					else
						children.put(feature.getJSONArray("branchFeaturesItems").getString(j), 1);
				}
			}	
			
			if (feature.getJSONArray("features").size()>0)
				uniqueChildren(feature, children);
		}

		return children;
	}
	
	private String checkMapUniqueness(HashMap<String, Integer> children){
		
		
		for (Map.Entry<String, Integer>entry : children.entrySet()) {
			if (entry.getValue()>1)
				return entry.getKey();
		}
		
		return "0";
	}
	
	private JSONObject getRoot() throws JSONException{
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);
		return new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));

	}
	
	private ArrayList<Integer> setCounter()
	{
		ArrayList<Integer> count = new ArrayList<Integer>();
		count.add(0);
		return count;
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
