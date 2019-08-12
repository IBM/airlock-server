package tests.restapi.scenarios.translation;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.File;
import java.io.StringReader;
import java.util.Properties;

/**
 * Created by amitaim on 29/03/2017.
 */
public class StringInUse {

    protected String seasonID;
    protected String featureID;
    protected String featureID2;
    protected String subFeatureID;
    protected String featureIDProd;
    protected String configID;
    protected String configID2;
    protected String configIDNested;
    protected String configIDProd;
    protected String utilityID;
    protected String utilityIDProd;
    protected String utilityIDNested;
    protected String stringID;
    protected String stringID2;
    protected String stringID3;
    protected String stringID4;
    protected String filePath;
    protected String str;
    protected String str2;
    protected String str3;
    protected String str4;
    protected StringsRestApi stringsApi;
    protected TranslationsRestApi translationsApi;
    protected FeaturesRestApi featureApi;
    protected UtilitiesRestApi utilitiesApi;
    protected ProductsRestApi p;
    private AirlockUtils baseUtils;
    protected String productID;
    protected String m_url;
    private String sessionToken = "";
  //  private String translationsUrl;

    @BeforeClass

	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        filePath = configPath;
        stringsApi = new StringsRestApi();
        stringsApi.setURL(translationsUrl);
        translationsApi = new TranslationsRestApi();
        translationsApi.setURL(translationsUrl);
        utilitiesApi = new UtilitiesRestApi();
        utilitiesApi.setURL(m_url);
		featureApi = new FeaturesRestApi();
		featureApi.setURL(url);
        p = new ProductsRestApi();
        p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
      productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
    }

    @Test(description = "Add string value")
    public void addString() throws Exception {
        str = FileUtils.fileToString(filePath + "/strings/string5.txt", "UTF-8", false);
        stringID = stringsApi.addString(seasonID, str, sessionToken);
        str2 = FileUtils.fileToString(filePath + "/strings/string4.txt", "UTF-8", false);
        stringID2 = stringsApi.addString(seasonID, str2, sessionToken);
    }

    @Test(dependsOnMethods = "addString", description = "Add config dev")
    public void addConfigurationDev() throws Exception {
        String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
        featureID = featureApi.addFeature(seasonID, feature, "ROOT", sessionToken);
        Assert.assertTrue(!featureID.contains("error"), "createFeature failed: " + featureID);
        String config = FileUtils.fileToString(filePath + "configuration_rule_translation.txt", "UTF-8", false);
        configID = featureApi.addFeature(seasonID, config, featureID, sessionToken);
        Assert.assertTrue(!configID.contains("error"), "createConfig failed: " + configID);
        String response = translationsApi.stringInUse(featureID, sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByConfiguration").getJSONObject(0).getString("key").equals("app.hello"), "missing string");
    }

    @Test(dependsOnMethods = "addConfigurationDev", description = "Add config dev and prod")
    public void addConfigurationDevAndProd() throws Exception {
        String feature = FileUtils.fileToString(filePath + "feature_production.txt", "UTF-8", false);
        featureIDProd = featureApi.addFeature(seasonID, feature, "ROOT", sessionToken);
        Assert.assertTrue(!featureIDProd.contains("error"), "createFeature failed: " + featureIDProd);
        String config = FileUtils.fileToString(filePath + "configuration_rule_translation.txt", "UTF-8", false);
        config = config.replace("CR3", "CR4");
        configID2 = featureApi.addFeature(seasonID, config, featureIDProd, sessionToken);
        Assert.assertTrue(!configID2.contains("error"), "createConfig failed: " + configID2);
        String response = translationsApi.stringInUse(featureIDProd, sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByConfiguration").getJSONObject(0).getString("key").equals("app.hello"));
        String configProd = FileUtils.fileToString(filePath + "configuration_rule_translation_prod.txt", "UTF-8", false);
        configIDProd = featureApi.addFeature(seasonID, configProd, featureIDProd, sessionToken);
        Assert.assertTrue(!configIDProd.contains("error"), "createConfig failed: " + configIDProd);
        response = translationsApi.stringInUse(featureIDProd, sessionToken);
        jsonResponse = new JSONObject(response);
        JSONArray stringsInUseByConfiguration = jsonResponse.getJSONArray("stringsInUseByConfiguration");
        Assert.assertTrue(stringsInUseByConfiguration.size() == 2);
        Assert.assertTrue(stringsInUseByConfiguration.getJSONObject(0).getString("key").equals("app.fallback") || stringsInUseByConfiguration.getJSONObject(0).getString("key").equals("app.hello"));
        Assert.assertTrue(stringsInUseByConfiguration.getJSONObject(1).getString("key").equals("app.fallback") || stringsInUseByConfiguration.getJSONObject(1).getString("key").equals("app.hello"));        
    }

    @Test(dependsOnMethods = "addConfigurationDevAndProd", description = "Add in nested feature")
    public void nestedFeature() throws Exception {
        String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
        featureID2 = featureApi.addFeature(seasonID, feature, "ROOT", sessionToken);
        Assert.assertTrue(!featureIDProd.contains("error"), "createFeature failed: " + featureID2);
        String subFeature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
        subFeatureID = featureApi.addFeature(seasonID, subFeature, featureID2, sessionToken);
        Assert.assertTrue(!subFeatureID.contains("error"), "createFeature failed: " + subFeatureID);
        String config = FileUtils.fileToString(filePath + "configuration_rule_translation.txt", "UTF-8", false);
        config = config.replace("CR3", "CR5");
        configIDNested = featureApi.addFeature(seasonID, config, subFeatureID, sessionToken);
        Assert.assertTrue(!configIDNested.contains("error"), "createConfig failed: " + configIDNested);
        String response = translationsApi.stringInUse(featureID2, sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByConfiguration").size() == 0);
    }

    @Test(dependsOnMethods = "nestedFeature", description = "Add dev utility")
    public void addDevUtility() throws Exception {
        String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1_translate.txt", "UTF-8", false);
        Properties utilProps = new Properties();
        utilProps.load(new StringReader(utility));
        utilityID = utilitiesApi.addUtility(seasonID, utilProps, sessionToken);
        Assert.assertFalse(utilityID.contains("error"), "could not add utility: " + utilityID );             
        String response = translationsApi.stringInUse(featureID, sessionToken);
        JSONObject jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").getJSONObject(0).getString("key").equals("app.hello"), "missing string");
        
       	String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJSON = new JSONObject(str);
		strJSON.put("stage", "PRODUCTION");
		response = stringsApi.updateString(stringID, strJSON.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Could not update stirng to PRODUCTION: " + response );
        
		str = stringsApi.getString(stringID2, sessionToken);
		strJSON = new JSONObject(str);
		strJSON.put("stage", "PRODUCTION");
		response = stringsApi.updateString(stringID2, strJSON.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Could not update stirng to PRODUCTION: " + response );
       
		String configRule = featureApi.getFeature(configID2, sessionToken);
        JSONObject configRuleJSON = new JSONObject(configRule);
        configRuleJSON.put("stage", "PRODUCTION");
        response = featureApi.updateFeature(seasonID, configID2, configRuleJSON.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Could not update config to PRODUCTION: " + response );
        	
        response = translationsApi.stringInUse(featureIDProd, sessionToken);
        jsonResponse = new JSONObject(response);
        //should not see dev utility
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").size() == 0, "should not see dev utility");
    }

    @Test(dependsOnMethods = "addDevUtility", description = "Add prod utility")
    public void addProdUtility() throws Exception {
        String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utilityProd_translate.txt", "UTF-8", false);
        Properties utilProps = new Properties();
        utilProps.load(new StringReader(utility));
        utilityIDProd = utilitiesApi.addUtility(seasonID, utilProps, sessionToken);
        Assert.assertFalse(utilityIDProd.contains("error"), "could not add utility: " + utilityIDProd );
        String response = translationsApi.stringInUse(featureID, sessionToken);
        
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray stringsInUseByUtilities = jsonResponse.getJSONArray("stringsInUseByUtilities");
        Assert.assertTrue(stringsInUseByUtilities.size() == 2);
        Assert.assertTrue(stringsInUseByUtilities.getJSONObject(0).getString("key").equals("app.fallback") || stringsInUseByUtilities.getJSONObject(0).getString("key").equals("app.hello"));
        Assert.assertTrue(stringsInUseByUtilities.getJSONObject(1).getString("key").equals("app.fallback") || stringsInUseByUtilities.getJSONObject(1).getString("key").equals("app.hello"));
        
        //Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").getJSONObject(0).getString("key").equals("app.fallback"), "missing string");
        //Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").getJSONObject(1).getString("key").equals("app.hello"), "missing string");
                
        response = translationsApi.stringInUse(featureIDProd, sessionToken);
        jsonResponse = new JSONObject(response);
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").size() == 1, "missing string");
        Assert.assertTrue(jsonResponse.getJSONArray("stringsInUseByUtilities").getJSONObject(0).getString("key").equals("app.fallback"), "missing string");
    }
    
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
