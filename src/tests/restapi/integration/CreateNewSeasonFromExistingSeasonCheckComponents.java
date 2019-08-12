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

public class CreateNewSeasonFromExistingSeasonCheckComponents {
	protected String seasonID1;
	protected String seasonID2;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	protected SeasonsRestApi s;
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
	@Test (description = "Add product, season and features")
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
		featureID1 = f.addFeature(seasonID1, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Test should pass, but instead failed: " + featureID1 );
		
		String feature2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID1, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Test should pass, but instead failed: " + featureID2 );
		
		String feature3 = FileUtils.fileToString(config + "feature3.txt", "UTF-8", false);
		featureID3 = f.addFeature(seasonID1, feature3, featureID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Test should pass, but instead failed: " + featureID3 );
	}
	
	
	
		
	/* create a new season - it should copy the previous season
	 * validate that 3 features exist in the new season and featureID3 is a child of featureID2, but with new uniqueId
	 * can add a feature to season1
	 * minVer of season2 = maxVer of season1, maxVer season2=null
	 * 
	 */
	@Test (dependsOnMethods="addComponents", description = "Create new season and validate its parameters and features")
	public void createNewSeason() throws IOException{
		String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Test should pass, but instead failed: " + seasonID2 );
		
		try {
			JSONArray seasons = s.getSeasonsPerProduct(productID, sessionToken);
			Assert.assertEquals(seasons.size(), 2, "New season was not added to the product.");
			if (seasons.size() == 2) {
				JSONObject season1 = (JSONObject)seasons.get(0);
				JSONObject season2 = (JSONObject)seasons.get(1);
				
				Assert.assertNotEquals(season1.getString("uniqueId"), season2.getString("uniqueId"), "The new season has the same uniqueId as the old season");
				validateSeasonVersionNumbers(season1, season2);
				
				JSONArray oldFeatures = f.getFeaturesBySeason(seasonID1, sessionToken);
				JSONArray newFeatures = f.getFeaturesBySeason(seasonID2, sessionToken);
				
				for(int i=0; i<oldFeatures.size(); i++){
					
					boolean compare = jsonObjsAreEqual(oldFeatures.getJSONObject(i), newFeatures.getJSONObject(i));
					Assert.assertTrue(compare, "Features objects are not equal");
				}

			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void validateSeasonVersionNumbers(JSONObject season1, JSONObject season2) throws JSONException{
		
		Assert.assertNotNull(season1.getString("maxVersion"), "maxVersion for season1 was not set.");
		String maxVer = season1.getString("maxVersion");
		String minVer = season2.getString("minVersion");
		if (!maxVer.equals(minVer)){
			Assert.fail("minVersion of season2 is not equal to maxVersion of season1");
		} 
		

		if ( !season2.isNull("maxVersion")) {
			Assert.fail("maxVersion of season2 should be set to null");
		} 
		//Assert.assertEquals(maxVer.equals(minVer), "minVersion of season2 is not equal to maxVersion of season1");
		//Assert.assertNull(season2.getString("maxVersion"), "maxVersion of season2 should be set to null");
		
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
		    	if (key.equals("features")){
		    		JSONArray array1 = js1.getJSONArray(key);
		    		JSONArray array2 = js2.getJSONArray(key);
		    		if (array1.size() != array2.size())
		    			return false;
		    		continue;
		    	}
		        Object val1 = js1.get(key);
		        Object val2 = js2.get(key);
		        if (val1 instanceof JSONObject) {
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
