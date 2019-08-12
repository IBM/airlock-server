package tests.restapi.integration;

import tests.restapi.AirlockUtils;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class SeasonVsSchemaMinAppVersionNumber {
	protected String seasonID;
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
		p.setURL(url);
		s.setURL(url);
		is.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		
		baseUtils = new AirlockUtils(url, configPath, sessionToken, productsToDeleteFile);
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);	
		baseUtils.printProductToFile(productID);
		
		season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
	}

	

	//the first season's maxVersion is null, so schema's minAppVersion can be anything
	@Test (description = "The first season's maxVersion is null, so schema's minAppVersion can be anything")
	public void addSchemaWithLegalMinAppVer() throws Exception{
		String schema = FileUtils.fileToString(config + "inputSchema1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(schema);
		json.put("minAppVersion", "3.5");
		is.addInputSchema(seasonID, json.toString(), sessionToken);
	}
	
	//schema's minAppVersion must be less than season's maxVersion
	//create a second season to close the first season and create maxVersion="2.0"
	@Test (dependsOnMethods = "addSchemaWithLegalMinAppVer", expectedExceptions= AssertionError.class, description = "Schema's minAppVersion must be less than season's maxVersion")
	public void checkMaxVersion() throws Exception{
		
		season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		
		String schema = FileUtils.fileToString(config + "inputSchema2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(schema);
		json.put("minAppVersion", "9.0");
		is.addInputSchema(seasonID, json.toString(), sessionToken);
	}
	
	//create 3 seasons, update  min&max version in the second season. The first and third season version change, so the schema's versions should change
	@Test (dependsOnMethods = "checkMaxVersion", description = "Schema's minAppVersion must be updated when season's version is updated")
	public void checkMinVersionWhenSeasonChanges() throws Exception{
		
		season = FileUtils.fileToString(config + "season3.txt", "UTF-8", false);
		String seasonID3 = s.addSeason(productID, season, sessionToken);
		
		String season2 = s.getSeason(productID, seasonID2);
		JSONObject season2Json = new JSONObject(season2);
		
		season2Json.put("minVersion", "1.5");
		season2Json.put("maxVersion", "2.5");
		s.updateSeason(seasonID2, season2Json.toString(), sessionToken);
		
		String schema1 = is.getAllInputSchemas(seasonID, sessionToken);
		String schema3 = is.getAllInputSchemas(seasonID3, sessionToken);
		
		JSONObject season1SchemaJson = new JSONObject(schema1);
		JSONObject season3SchemaJson = new JSONObject(schema3);
		
		Assert.assertTrue(season1SchemaJson.getString("maxVersion").equals(season2Json.getString("minVersion")), "Schema's max version was not update");
		Assert.assertTrue(season3SchemaJson.getString("minVersion").equals(season2Json.getString("maxVersion")), "Schema's min version was not update");
	}
	
	
	@AfterTest 
	public void reset(){
		p.deleteProduct(productID, sessionToken);
	}
	
	*/
}
