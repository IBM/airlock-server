package tests.restapi.scenarios.strings;


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

public class DeleteStringInUseByFeature {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	
	@Test (description = "Add string in production stage")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	@Test (dependsOnMethods="addString",  description = "Add feature with configuration")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fObj = new JSONObject(feature);
		String ruleString =  "  translate(\"app.hello\", \"testing string\");  true;	" ;
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		fObj.put("rule", ruleObj);
		String featureID = f.addFeature(seasonID, fObj.toString(), "ROOT", sessionToken );
		Assert.assertFalse(featureID.contains("error"), "Cannot create feature: " + featureID );
	}
	
	@Test (dependsOnMethods="addFeature",  description = "Delete string in use by configuration")
	public void deleteString() throws Exception{
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertNotEquals(responseCode, 200, "String in use by feature was deleted");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
