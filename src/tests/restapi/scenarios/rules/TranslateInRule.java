package tests.restapi.scenarios.rules;

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class TranslateInRule {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String feature;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected StringsRestApi t;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);

	}
	
	@Test (description = "Use translate() in rule - not allowed")
	
	public void useTranslateInRule() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		t.addString(seasonID, str, sessionToken);
		
		//add feature with rule
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);				
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "translate(\"app.hello\", context.userPreferences.unitsOfMeasure)");	
		json.put("rule", rule);
		
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
