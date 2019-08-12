package tests.restapi.validations.utilities;

import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityUpdateValidateMissingFields {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	protected String utility;
	private String sessionToken = "";

	/*
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		if (sToken != null)
			sessionToken = sToken;
		AirlockUtils baseUtils = new AirlockUtils(m_url, configPath, sessionToken, productsToDeleteFile);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);

	}

/*
	@Test 
	public void emptyUtility() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("utility", json);
		updateUtility(json);
	}
	
	@Test 
	public void emptyStage() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("stage", json);
		updateUtility(json);
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("uniqueId", json);
		String response =  u.updateUtility(utilityID, json, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void emptyLastModified() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("lastModified", json);
		updateUtility(json);
	}

	@Test 
	public void emptyMinAppVersion() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("minAppVersion", json);
		updateUtility(json);
	}
	
	@Test 
	public void emptySeasonId() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		removeKey("seasonId", json);
		updateUtility(json);
	}
			
	private void updateUtility(JSONObject json) throws IOException, JSONException{
		String response =  u.updateUtility(utilityID, json, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	private void removeKey(String key, JSONObject json){
		json.remove(key);
	}

	

	@AfterTest
	private void reset(){
		p.deleteProduct(productID, sessionToken);
	}
	
	*/
}
