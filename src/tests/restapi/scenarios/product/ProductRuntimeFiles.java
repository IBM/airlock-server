package tests.restapi.scenarios.product;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class ProductRuntimeFiles {
	
	protected String productID;
	protected String seasonID1;
	protected String seasonID2;
	protected String seasonID3;
	protected String filePath;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	protected InputSchemaRestApi schema;
	protected UtilitiesRestApi utilitiesApi;
	protected StringsRestApi stringsApi;
	private SeasonsRestApi s;
	private FeaturesRestApi f;
	private String m_url;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		productID = "";
		filePath = configPath;
		p = new ProductsRestApi();
 		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		f = new FeaturesRestApi();
		f.setURL(url);
		m_url = url;
		s = new SeasonsRestApi();
		s.setURL(url);
		
		
        schema = new InputSchemaRestApi();
        schema.setURL(url);
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);

		productID = baseUtils.createProduct();
		
	}
	
	@Test (description = "validate productRuntime upon season creation")
	public void createSesaon() throws Exception {
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		seasonID1 = baseUtils.createSeason(productID, "0.1"); 
		Assert.assertFalse(seasonID1.contains("error"), "Fail craeting season");
		
		RuntimeDateUtilities.setSleep();
		
		RuntimeRestApi.DateModificationResults  response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		JSONObject prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 1, "wrong number of seasons");
		JSONObject seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
	}
	
	
	@Test (dependsOnMethods = "createSesaon", description = "validate productRuntime upon second season creation")
	public void addSecondSesaon() throws Exception {
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		seasonID2 = baseUtils.createSeason(productID, "1.1"); 
		Assert.assertFalse(seasonID2.contains("error"), "Fail craeting season");
		
		RuntimeDateUtilities.setSleep();
		
		//first season
		RuntimeRestApi.DateModificationResults  response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		JSONObject prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		JSONObject seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.1"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
		
		//second season
		response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.1"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
	}
	
	@Test (dependsOnMethods = "addSecondSesaon", description = "validate productRuntime upon season update")
	public void updateSesaon() throws Exception {
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "1.2");
		String res = s.updateSeason(seasonID1, json.toString(), sessionToken);
		Assert.assertTrue(!res.contains("error"), "fail updating season" );
		
		RuntimeDateUtilities.setSleep();
		
		//first season
		RuntimeRestApi.DateModificationResults  response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		JSONObject prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		JSONObject seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.2"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.2"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
		
		//second season
		response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.2"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.2"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
	}
	
	@Test (dependsOnMethods = "updateSesaon", description = "validate productRuntime upon product update")
	public void updateProduct() throws Exception {
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String prod = p.getProduct(productID, sessionToken);
		JSONObject prodJson = new JSONObject(prod);
		String ci = RandomStringUtils.randomAlphabetic(5);
		prodJson.put("codeIdentifier", ci);
		prodJson.remove("seasons");
		String res = p.updateProduct(productID, prodJson.toString(), sessionToken);
		Assert.assertTrue(!res.contains("error"), "fail updating product" );
		
		RuntimeDateUtilities.setSleep();
		
		//first season
		RuntimeRestApi.DateModificationResults  response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		JSONObject prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getString("codeIdentifier").equals(ci)," wrong codeIdentifier");
		
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		JSONObject seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.2"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.2"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
		
		//second season
		response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getString("codeIdentifier").equals(ci)," wrong codeIdentifier");
		
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 2, "wrong number of seasons");
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.getString("maxVersion").equals("1.2"), "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
		seasonObj = prodObj.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("1.2"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID2), "wrong season id");
	}
	
	@Test (dependsOnMethods = "updateProduct", description = "validate product Runtime upon season deletion")
	public void deleteSesaon() throws Exception {
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		int code = s.deleteSeason(seasonID2,sessionToken);
		Assert.assertTrue(code == 200, "fail deleting season" );
		
		RuntimeDateUtilities.setSleep();
		
		//first season
		RuntimeRestApi.DateModificationResults  response = RuntimeDateUtilities.getProductRuntimeDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==200, "product runtime file does not exists for season2 in product1");
		
		JSONObject prodObj = new JSONObject(response.message);
		Assert.assertTrue(prodObj.getString("uniqueId").equals(productID), "wrong product id");
		Assert.assertTrue(prodObj.getJSONArray("seasons").size() == 1, "wrong number of seasons");
		JSONObject seasonObj = prodObj.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(seasonObj.getString("minVersion").equals("0.1"), "wrong season min version");
		Assert.assertTrue(seasonObj.get("maxVersion") == null, "wrong season max version");
		Assert.assertTrue(seasonObj.getString("uniqueId").equals(seasonID1), "wrong season id");
		
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}


}
