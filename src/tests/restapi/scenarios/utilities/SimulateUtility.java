package tests.restapi.scenarios.utilities;

import com.ibm.airlock.Strings;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;


public class SimulateUtility {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String utilityID1;
	protected String deepFreezeID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected InputSchemaRestApi schema;
	protected UtilitiesRestApi u;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;

	@SuppressWarnings("unused")
	private int numOfOriginalFuntions = 0; 
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		schema = new InputSchemaRestApi();
		schema.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject utilitiesInfoJson = new JSONObject(response);
		
		numOfOriginalFuntions = utilitiesInfoJson.keySet().size();
	}



	@Test(description = "invalid params")
	private void invalidCalls(){
		//missing body
		String result = utilitiesApi.simulateUtility(seasonID,"","DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("The request body does not contain the separator"));//typo
		//missing stage
		String body ="$#^StartOfRule^#$";
		result = utilitiesApi.simulateUtility(seasonID,body,"","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains(Strings.stageMissing));
		/*
		//missing minAppVersion
		result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT",null,"RULE",sessionToken);
		Assert.assertTrue(result.contains("The minAppVersion parameter is missing"));
		//invalid minAppVersion
		result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","","RULE",sessionToken);
		Assert.assertTrue(result.contains("The minAppVersion parameter is missing"));
		*/
		//missing simulation type
		result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","",sessionToken);
		Assert.assertTrue(result.contains("The simulation type parameter is missing"));
		//invalid simulation type
		result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","FEATURE",sessionToken);
		Assert.assertTrue(result.contains("Illegal simulation type"));

	}


	//RULE TESTS
	@Test(dependsOnMethods = "invalidCalls",  description = "use unexisting util")
	private void useNonexestingUtilInRule(){
		String body =" $#^StartOfRule^#$ nonexisting()";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("\\\"nonexisting\\\" is not defined"));
	}
	@Test(dependsOnMethods = "useNonexestingUtilInRule",  description = "use unexisting util in function")
	private void useNonexestingUtilInRuleNested(){
		String body =" $#^StartOfRule^#$ function testNonexisting(){nonexisting()}; testNonexisting() == true;";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("\\\"nonexisting\\\" is not defined."));
	}
	@Test(dependsOnMethods = "useNonexestingUtilInRuleNested",  description = "return non boolean")
	private void nonBooleanRule(){
		String body ="$#^StartOfRule^#$ 1";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains(" Script result is not boolean"));
	}
	@Test(dependsOnMethods = "nonBooleanRule",  description = "invalid JS")
	private void invalidRule(){
		String body =" $#^StartOfRule^#$ %";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("Javascript error: syntax error"));
	}
	@Test(dependsOnMethods = "invalidRule",  description = "non existing field in rule")
	private void missingFieldInRule(){
		String body =" $#^StartOfRule^#$ context.missing == 1";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("Undefined field: context.missing"));
	}


	//CONFIG TESTS
	@Test(dependsOnMethods = "missingFieldInRule",  description = "use unexisting util")
	private void useNonexestingUtilInConfig(){
		String body =" $#^StartOfRule^#$ nonexisting()";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","CONFIGURATION",sessionToken);
		Assert.assertTrue(result.contains("\\\"nonexisting\\\" is not defined"));
	}
	@Test(dependsOnMethods = "useNonexestingUtilInConfig",  description = "invalid JS")
	private void invalidConfig(){
		String body =" $#^StartOfRule^#$ %";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","CONFIGURATION",sessionToken);
		Assert.assertTrue(result.contains("Javascript configuration error: syntax error"));
	}
	@Test(dependsOnMethods = "invalidConfig",  description = "non existing field in rule")
	private void missingFieldInConfig(){
		String body =" $#^StartOfRule^#${\"text\":context.missing}";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","CONFIGURATION",sessionToken);
		Assert.assertTrue(result.contains("Undefined field: context.missing"));
	}



	@Test(dependsOnMethods = "missingFieldInConfig",  description = "duplicate utilities")
	private void missingFieldInUtility(){
		String body ="var a = context.missing $#^StartOfRule^#$ true";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("is not defined"));
	}
	@Test(dependsOnMethods = "missingFieldInUtility",  description = "duplicate utilities")
	private void duplicateInSameUtility(){
		String body ="function a(){}; function a(){}; $#^StartOfRule^#$ true";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("duplicate functions: a"));
	}
	@Test(dependsOnMethods = "duplicateInSameUtility",  description = "duplicate utilities")
	private void duplicateUtility(){
		String body ="function calcDistance(){};$#^StartOfRule^#$ true";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("duplicate functions: calcDistance"));
	}
	@Test(dependsOnMethods = "duplicateUtility",  description = "duplicate utilities")
	private void missingUtilityNested(){
		String body ="nonexistingUtil();function newUtil(){ nonexistingUtil()};newUtil();$#^StartOfRule^#$ true";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("\\\"nonexistingUtil\\\" is not defined."));
	}
	@Test(dependsOnMethods = "missingUtilityNested",  description = "duplicate utilities")
	private void invalidUtil(){
		String body ="nonexistingUtil();function newUtil(){ %};newUtil();$#^StartOfRule^#$ true";
		String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
		Assert.assertTrue(result.contains("syntax error"));
	}



	@Test(dependsOnMethods = "invalidUtil",  description = "duplicate utilities")
	private void prodDevFieldInRule(){
		try {
			String sch = schema.getInputSchema(seasonID, sessionToken);
			JSONObject jsonSchema = new JSONObject(sch);
			String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
			jsonSchema.put("inputSchema", new JSONObject(schemaBody));
			schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			String body ="$#^StartOfRule^#$ context.testData.dsx.eventName==true";
			String result = utilitiesApi.simulateUtility(seasonID,body,"PRODUCTION","1.1.1","RULE",sessionToken);
			Assert.assertTrue(result.contains("Undefined field: context.testData.dsx.eventName"));
			body ="$#^StartOfRule^#$ context.testData.dsx.teaserTitle==true";
			result = utilitiesApi.simulateUtility(seasonID,body,"PRODUCTION","1.1.1","RULE",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			body ="$#^StartOfRule^#$ context.testData.dsx.eventName==true";
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","RULE",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			body ="$#^StartOfRule^#$ context.testData.dsx.teaserTitle==true";
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","RULE",sessionToken);
			Assert.assertTrue(result.contains("{}"));
		}catch (Exception e){
			Assert.fail("exception");
		}
	}
	@Test(dependsOnMethods = "prodDevFieldInRule",  description = "duplicate utilities")
	private void prodDevFieldInConfig(){
		try {
			String body ="$#^StartOfRule^#$ {\"test\":context.testData.dsx.eventName}";
			String result = utilitiesApi.simulateUtility(seasonID,body,"PRODUCTION","1.1.1","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("Undefined field: context.testData.dsx.eventName"));
			body ="$#^StartOfRule^#$ {\"test\":context.testData.dsx.teaserTitle}";
			result = utilitiesApi.simulateUtility(seasonID,body,"PRODUCTION","1.1.1","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			body ="$#^StartOfRule^#$ {\"test\":context.testData.dsx.eventName}";
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			body ="$#^StartOfRule^#$ {\"test\":context.testData.dsx.teaserTitle}";
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("{}"));
		}catch (Exception e){
			Assert.fail("exception");
		}
	}
	@Test(dependsOnMethods = "prodDevFieldInConfig",  description = "duplicate utilities")
	private void minAppFieldInRule(){
		try {
			String body ="$#^StartOfRule^#$ context.testData.dsx.mode==true";
			String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","RULE",sessionToken);
			Assert.assertTrue(result.contains("Undefined field: context.testData.dsx.mode"));
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.2","RULE",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.3","RULE",sessionToken);
			Assert.assertTrue(result.contains("{}"));
		}catch (Exception e){
			Assert.fail("exception");
		}
	}
	@Test(dependsOnMethods = "minAppFieldInRule",  description = "duplicate utilities")
	private void minAppFieldInConfig(){
		try {
			String body ="$#^StartOfRule^#$ {\"test\":context.testData.dsx.mode}";
			String result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.1","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("Undefined field: context.testData.dsx.mode"));
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.2","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("{}"));
			result = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","1.1.3","CONFIGURATION",sessionToken);
			Assert.assertTrue(result.contains("{}"));
		}catch (Exception e){
			Assert.fail("exception");
		}
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
