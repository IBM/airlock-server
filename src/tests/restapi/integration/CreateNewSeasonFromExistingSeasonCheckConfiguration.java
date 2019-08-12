package tests.restapi.integration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import tests.restapi.SeasonsRestApi;

public class CreateNewSeasonFromExistingSeasonCheckConfiguration {
	protected String seasonID1;
	protected String seasonID2;
	protected String featureID;
	protected String configRuleID1;
	protected String configRuleID2;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;

		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();

		p.setURL(url);
		s.setURL(url);
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}
	
	//
	@Test (description = "Add product, season, feature and 2 configuraton rules in hierarchy")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);	
		baseUtils.printProductToFile(productID);
		
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID1.contains("error"), "Test should pass, but instead failed: " + seasonID1 );
		
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID1, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		
		String config1 = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		configRuleID1 = f.addFeature(seasonID1, config1, featureID, sessionToken);
		Assert.assertFalse(configRuleID1.contains("error"), "Test should pass, but instead failed: " + configRuleID1 );
		
		String config2 = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
		configRuleID2 = f.addFeature(seasonID1, config2, configRuleID1, sessionToken);
		Assert.assertFalse(configRuleID2.contains("error"), "Test should pass, but instead failed: " + configRuleID2 );
		
		
	}
	
	
	
		
	/* create a new season - it should copy the previous season
	 * validate that a hierarchy of configuration rules exist in the new season and  with new uniqueIds and they are children of feature1
	 * minVer of season2 = maxVer of season1, maxVer season2=null
	 * 
	 */
	@Test (dependsOnMethods="addComponents", description = "Create new season and validate its parameters and features")
	public void createNewSeason() throws IOException, JSONException{
		String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Test should pass, but instead failed: " + seasonID2 );
		
		//get configurationRules from season1
		String oldFeature = f.getFeature(featureID, sessionToken);
		JSONObject json1 = new JSONObject(oldFeature);
		JSONArray oldConfigRules = json1.getJSONArray("configurationRules");
		
		//get configurationRules from season1
		JSONArray newFeatures = f.getFeaturesBySeason(seasonID2, sessionToken);
		Assert.assertTrue(newFeatures.size()==1, "Incorrect number of features in the new season");
		JSONObject json2 = newFeatures.getJSONObject(0);
		JSONArray newConfigRules = json2.getJSONArray("configurationRules");
		Assert.assertTrue(oldConfigRules.size()==newConfigRules.size(), "Incorrect number of configuration rules in the new season");
		
		//compare configurationRules
		boolean compare1 = jsonObjsAreEqual(oldConfigRules.getJSONObject(0), newConfigRules.getJSONObject(0));
		Assert.assertTrue(compare1, "Incorrect configuration rule 1");
		
	}
	

	


		private  boolean jsonObjsAreEqual (JSONObject js1, JSONObject js2) throws JSONException {
		    if (js1 == null || js2 == null) {
		        return (js1 == js2);
		    }

		    List<String> l1 =  Arrays.asList(JSONObject.getNames(js1));
		    Collections.sort(l1);
		    List<String> l2 =  Arrays.asList(JSONObject.getNames(js2));
		    Collections.sort(l2);
		    if (!l1.equals(l2)) {
		        return false;
		    }
		    for (String key : l1) {
		    	if (key.equals("uniqueId") || key.equals("lastModified") || key.equals("minAppVersion")  || key.equals("creationDate") || key.equals("seasonId"))
		    		continue;
		    	if (key.equals("features") || key.equals("configurationRules")){
		    		JSONArray array1 = js1.getJSONArray(key);
		    		JSONArray array2 = js2.getJSONArray(key);
		    		if (array1.size() != array2.size())
		    			return false;
		    		continue;
		    	}
		    	
		        Object val1 = js1.get(key);
		        Object val2 = js2.get(key);
		        
		        if (val1 instanceof JSONObject && ((JSONObject) val1).length()!= 0 && ((JSONObject) val2).length()!= 0) {
		            if (!(val2 instanceof JSONObject)) {
		                return false;
		            }
		            if (!jsonObjsAreEqual((JSONObject)val1, (JSONObject)val2)) {
		                return false;
		            }
		        }

		        if (val1 == null) {
		            if (val2 != null) {
		                return false;
		            }
		        }  else if (!val1.equals(val2)) {
		            return false;
		        }
		    }
		    
		    return true;
		}
		
	
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
