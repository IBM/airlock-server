package tests.restapi.scenarios.feature;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.*;

import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;

import static com.ibm.airlock.utilities.FeatureFilter.SearchArea.*;
import static com.ibm.airlock.utilities.FeatureFilter.SearchOption.*;

public class FindFeature {
    protected String seasonID;
    protected String productID;
    protected String featureID;
    protected String filePath;
    protected FeaturesRestApi f;
    protected ProductsRestApi p;
    protected InputSchemaRestApi schema;
    private String sessionToken = "";
    protected AirlockUtils baseUtils;

    // Search areas
    private static final List<String> SEARCH_AREA_ALL = Arrays.asList();
    private static final List<String> SEARCH_AREA_INVALID = Arrays.asList("invalidsearcharea");

    private static final List<String> SEARCH_AREA_NAME = Arrays.asList(NAME.toString());
    private static final List<String> SEARCH_AREA_NAMESPACE = Arrays.asList(NAMESPACE.toString());
    private static final List<String> SEARCH_AREA_DESCRIPTION = Arrays.asList(DESCRIPTION.toString());
    private static final List<String> SEARCH_AREA_DISPLAY_NAME = Arrays.asList(DISPLAY_NAME.toString());
    private static final List<String> SEARCH_AREA_RULE = Arrays.asList(RULE.toString());
    private static final List<String> SEARCH_AREA_CONFIGURATION = Arrays.asList(CONFIGURATION.toString());

    private static final List<String> SEARCH_AREA_RULE_AND_DESC = Arrays.asList(RULE.toString(), DESCRIPTION.toString());
    private static final List<String> SEARCH_AREA_RULE_AND_DISP_NAME = Arrays.asList(RULE.toString(), DISPLAY_NAME.toString());
    private static final List<String> SEARCH_AREA_NAME_AND_DISP_NAME = Arrays.asList(NAME.toString(), DISPLAY_NAME.toString());

    // Search options
    private static final List<String> SEARCH_OPTIONS_DEFAULT = Arrays.asList();
    private static final List<String> SEARCH_OPTIONS_CASE_SENSITIVE = Arrays.asList(CASE_SENSITIVE.toString());
    private static final List<String> SEARCH_OPTIONS_EXACT_MATCH = Arrays.asList(EXACT_MATCH.toString());
    private static final List<String> SEARCH_OPTIONS_REGEX = Arrays.asList(REG_EXP.toString());
    private static final List<String> SEARCH_OPTIONS_IVALID = Arrays.asList("invalidsearchoption");


    @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
        schema = new InputSchemaRestApi();
        schema.setURL(url);

	}

    @Test(description = "Add features")
    public void addFeatures() throws Exception {
        String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);

        String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
        String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
        String feature3 = FileUtils.fileToString(filePath + "featureWithRule.txt", "UTF-8", false);
        String feature4 = FileUtils.fileToString(filePath + "feature5.txt", "UTF-8", false);

        String fId;
        fId = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
        Assert.assertFalse(fId.contains("error"), "Error occurred while adding feature:" + fId);

        fId = f.addFeature(seasonID, feature2, "ROOT", sessionToken);
        Assert.assertFalse(fId.contains("error"), "Error occurred while adding feature:" + fId);

        fId = f.addFeature(seasonID, feature3, "ROOT", sessionToken);
        Assert.assertFalse(fId.contains("error"), "Error occurred while adding feature:" + fId);

        fId = f.addFeature(seasonID, feature4, "ROOT", sessionToken);
        Assert.assertFalse(fId.contains("error"), "Error occurred while adding feature:" + fId);
    }

    /**
     * Find feature
     */
    @Test(dependsOnMethods = "addFeatures", description = "Find feature by name")
    public void testFindFeatureByName() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "Feature2", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by invalid name")
    public void testFindFeatureByInvalidName() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "InvalidFeatureName", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);

        res = f.findFeaturesParsed(seasonID, "kkkk", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Namespace")
    public void testFindFeatureByNamespace() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "ns1", SEARCH_AREA_NAMESPACE, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 3);

        res = f.findFeaturesParsed(seasonID, "ns2", SEARCH_AREA_NAMESPACE, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "ns3", SEARCH_AREA_NAMESPACE, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Description")
    public void testFindFeatureByDescription() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "F21", SEARCH_AREA_DESCRIPTION, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 3);

        res = f.findFeaturesParsed(seasonID, "F34", SEARCH_AREA_DESCRIPTION, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Display Name")
    public void testFindFeatureByDisplayName() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "five", SEARCH_AREA_DISPLAY_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "six", SEARCH_AREA_DISPLAY_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Rule")
    public void testFindFeatureByRule() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "hello", SEARCH_AREA_RULE, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "goodbye", SEARCH_AREA_RULE, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Configuration")
    public void testFindFeatureByConfigurration() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "red", SEARCH_AREA_CONFIGURATION, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "blue", SEARCH_AREA_CONFIGURATION, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by all areas")
    public void testFindFeatureByAllAreas() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "hello", SEARCH_AREA_ALL, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 2);

        res = f.findFeaturesParsed(seasonID, "ns2", SEARCH_AREA_ALL, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "red", SEARCH_AREA_ALL, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "notexists", SEARCH_AREA_ALL, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by areas")
    public void testFindFeatureByAreas() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "hello", SEARCH_AREA_RULE_AND_DESC, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 2);

        res = f.findFeaturesParsed(seasonID, "hello", SEARCH_AREA_RULE_AND_DISP_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "hello", SEARCH_AREA_NAME_AND_DISP_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by name with exact match")
    public void testFindFeatureExactMatch() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_EXACT_MATCH, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "Feat", SEARCH_AREA_NAME, SEARCH_OPTIONS_EXACT_MATCH, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature case sensitive")
    public void testFindFeatureCaseSensitive() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_CASE_SENSITIVE, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_CASE_SENSITIVE, sessionToken);
        Assert.assertEquals(res.size(), 0);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature case insensitive")
    public void testFindFeatureCaseInsensitive() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "feature1", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Wildcard name")
    public void testFindFeatureByWildcard() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Fea*1", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "Feat???1", SEARCH_AREA_NAME, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.size(), 1);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Find feature by Regexp name")
    public void testFindFeatureByRegexp() throws JSONException {
        JSONArray res = f.findFeaturesParsed(seasonID, "Fea.*1", SEARCH_AREA_NAME, SEARCH_OPTIONS_REGEX, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "Feat...1", SEARCH_AREA_NAME, SEARCH_OPTIONS_REGEX, sessionToken);
        Assert.assertEquals(res.size(), 1);

        res = f.findFeaturesParsed(seasonID, "F[a-z]*\\d", SEARCH_AREA_NAME, SEARCH_OPTIONS_REGEX, sessionToken);
        Assert.assertEquals(res.size(), 3);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Test invalid areas")
    public void testInvalidAreas() throws JSONException {
        RestClientUtils.RestCallResults res = f.findFeatures(seasonID,"MASTER", "hello", SEARCH_AREA_INVALID, SEARCH_OPTIONS_DEFAULT, sessionToken);
        Assert.assertEquals(res.code, Response.Status.BAD_REQUEST.getStatusCode(), "Wrong status code: " + res.code);
        Assert.assertTrue(res.message.contains("error"), "Message should contain error. Message: " + res.message);
    }

    @Test(dependsOnMethods = "addFeatures", description = "Test invalid options")
    public void testInvalidOptions() throws JSONException {
        RestClientUtils.RestCallResults res = f.findFeatures(seasonID,"MASTER", "hello", SEARCH_AREA_NAME, SEARCH_OPTIONS_IVALID, sessionToken);
        Assert.assertEquals(res.code, Response.Status.BAD_REQUEST.getStatusCode(), "Wrong status code: " + res.code);
        Assert.assertTrue(res.message.contains("error"), "Message should contain error. Message: " + res.message);
    }

    @AfterTest
    private void reset() {
        baseUtils.reset(productID, sessionToken);
    }
}
