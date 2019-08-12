package tests.restapi.integration;

import tests.restapi.AirlockUtils;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class InputSchemaVersionNumbers {
	protected String seasonID1;
	protected String seasonID2;
	protected String season;
	protected String productID;
	protected String config;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected InputSchemaRestApi is;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	/*
	@BeforeClass
	@Parameters({"url", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String configPath, @Optional String sToken, String productsToDeleteFile) throws IOException{
		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		is = new InputSchemaRestApi();
		baseUtils = new AirlockUtils(url, configPath, sessionToken, productsToDeleteFile);
		p.setURL(url);
		s.setURL(url);
		is.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);

	}
	

	@SuppressWarnings("static-access")
	@Test (description = "inputSchema version = season version")
	public void compareVersionStrings() throws Exception{
		season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		String schema = is.getAllInputSchemas(seasonID1, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject seasonJson = new JSONObject(season);
		Assert.assertEquals(schemaJson.getString("minVersion"), seasonJson.getString("minVersion"), "inputSchema minVersion is different from season minVerison");
		Assert.assertTrue(schemaJson.isNull("maxVersion"), "inputSchema maxVersion is not null");
	}
	

	
	@Test (dependsOnMethods = "compareVersionStrings", description = "inputSchema version = season version in the second season with updated maxVersion" )
	public void compareVersionStringsInNewSeason() throws Exception{
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(season2);
		seasonID2 = s.addSeason(productID, json.toString(), sessionToken);
		//season1 has a new maxVersion, its inputShema should be updated
		String schema = is.getAllInputSchemas(seasonID1, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject seasonJson = new JSONObject(season);
		Assert.assertEquals(schemaJson.getString("minVersion"), seasonJson.getString("minVersion"), "inputSchema minVersion is different from season minVerison");
		Assert.assertEquals(schemaJson.getString("maxVersion"), seasonJson.getString("maxVersion"), "inputSchema maxVersion is different from season maxVerison");

	}
	
	@Test (dependsOnMethods = "compareVersionStringsInNewSeason" )
	public void deleteSeason() throws Exception{
		s.deleteSeason(seasonID2, sessionToken);
		//season1 has a null maxVersion, its inputShema should be updated
		String schema = is.getAllInputSchemas(seasonID1, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);
		Assert.assertTrue(schemaJson.isNull("maxVersion"), "inputSchema maxVersion is not null");
		

	}

	@Test (dependsOnMethods = "deleteSeason" , description = "inputSchema version is updated when season's minVersion is updated")
	public void updateSeasonMinVersion() throws Exception{
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject seasonJson = new JSONObject(season);
		seasonJson.put("minVersion", "2.0");
		s.updateSeason(seasonID1, seasonJson.toString());
		String schema = is.getAllInputSchemas(seasonID1, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);
		
		Assert.assertEquals(schemaJson.getString("minVersion"), seasonJson.getString("minVersion"), "inputSchema minVersion is different from season minVerison");
	}
	
	@AfterTest
	public void reset(){
		p.deleteProduct(productID, sessionToken);
	}
	*/
}
