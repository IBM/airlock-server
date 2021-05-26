package tests.restapi.scenarios.capabilities;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

public class TestAllApi {

	protected String config;
	protected FeaturesRestApi featureApi;
	protected ProductsRestApi productApi;
	protected SeasonsRestApi seasonApi;
	protected InputSchemaRestApi schemaApi ;
	protected OperationRestApi operationApi;
	protected StringsRestApi stringApi;
	protected UtilitiesRestApi utilitiesApi;
	protected TranslationsRestApi translationApi;
	protected AnalyticsRestApi analyticsApi;
	protected TestServicesRestApi testServicesApi;
	protected ExperimentsRestApi experimentsRestApi;
	protected BranchesRestApi branchesRestApi;
	protected StreamsRestApi streamsApi;
	protected InAppPurchasesRestApi purchasesApi;
	protected EntitiesRestApi entitiesApi;
	protected AirlocklNotificationRestApi notificationApi;
	protected UserGroupsRestApi userGroupsApi;
	protected AirlockUtils baseUtils;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	ArrayList<String> results;
	protected boolean isAuthenticated;


	public TestAllApi(String url,String c_operationsUrl,String t_url,String a_url, String configPath) throws IOException{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;


		productApi = new ProductsRestApi();
		seasonApi = new SeasonsRestApi();
		featureApi = new FeaturesRestApi();
		schemaApi = new InputSchemaRestApi();
		utilitiesApi = new UtilitiesRestApi();
		stringApi = new StringsRestApi();
		translationApi = new TranslationsRestApi();
		analyticsApi = new AnalyticsRestApi();
		testServicesApi = new TestServicesRestApi();
		experimentsRestApi = new ExperimentsRestApi();
		branchesRestApi = new BranchesRestApi();
		operationApi = new OperationRestApi();
		streamsApi = new StreamsRestApi();
		notificationApi = new AirlocklNotificationRestApi();
		userGroupsApi = new UserGroupsRestApi();
		purchasesApi = new InAppPurchasesRestApi();
		entitiesApi = new EntitiesRestApi();

		productApi.setURL(m_url);
		seasonApi.setURL(m_url);
		featureApi.setURL(m_url);
		schemaApi.setURL(m_url);
		utilitiesApi.setURL(m_url);
		branchesRestApi.setURL(m_url);
		stringApi.setURL(translationUrl);
		translationApi.setURL(translationUrl);
		analyticsApi.setURL(analyticsUrl);
		experimentsRestApi.setURL(analyticsUrl);
		operationApi.setURL(operationsUrl);
		streamsApi.setURL(m_url);
		notificationApi.setUrl(m_url);
		userGroupsApi.setURL(m_url);
		purchasesApi.setURL(m_url);
		entitiesApi.setURL(m_url);
	}

	//*********** auxiliary test methods ********************//
	public String setCapabilitiesInProduct(String productID, JSONArray capabilites, String sessionToken) throws JSONException{
		String product = productApi.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.remove("seasons");
		json.put("capabilities", capabilites);
		return productApi.updateProduct(productID, json.toString(), sessionToken);
	}

	public JSONArray getCapabilitiesInProduct(String productID, String sessionToken) throws JSONException{
		String product = productApi.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		return json.getJSONArray("capabilities");
	}

	public void resetServerCapabilities(String sessionToken) throws Exception{
		String file = FileUtils.fileToString(config + "capabilities.txt", "UTF-8", false);
		JSONObject inputCap = new JSONObject(file);

		String response = getCapabilities(sessionToken);
		JSONObject json = new JSONObject(response);
		json.put("capabilities", inputCap);
		setCapabilities(json.toString(), sessionToken);

	}

	private void validateTestResult(String response, String error, boolean expectedResult){
		if (response.contains("error") != expectedResult)
			results.add(error);
	}


	//************* PRODUCT FUNCTIONS*************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllProduct(String sessionToken, boolean expectedResult) throws JSONException, IOException{
		results = new ArrayList<String>();

		String productID = createProduct(sessionToken);
		validateTestResult(productID, "add product", expectedResult);

		String response = getProductList(sessionToken);
		validateTestResult(response, "getProductList", expectedResult);

		response = getProduct(productID, sessionToken);
		validateTestResult(response, "getProduct", expectedResult);

		response = updateProduct(productID, sessionToken);
		validateTestResult(response, "updateProduct", expectedResult);

		if (isAuthenticated){
			response = followProduct(productID, sessionToken);
			validateTestResult(response, "followProduct", expectedResult);

			response = getProductFollowers(productID, sessionToken);
			validateTestResult(response, "getProductFollowers", expectedResult);

			int responseCode = unfollowProduct(productID, sessionToken);
			boolean deleted = ((responseCode == 200) ? false : true);
			if (deleted != expectedResult)
				results.add("unfollow product");
		}

		int responseCode = deleteProduct(productID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete product");

		response = createProductWithSpecifiedId(UUID.randomUUID().toString(), sessionToken);
		validateTestResult(response, "createProductWithSpecifiedId", expectedResult);


		return results;
	}

	public String createProduct(String sessionToken) throws IOException{

		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		return productApi.addProduct(product, sessionToken);
	}

	public String getProductList(String sessionToken){

		return productApi.getAllProducts(sessionToken);

	}

	public String getProduct(String productID, String sessionToken){
		return productApi.getProduct(productID, sessionToken);
	}

	public String updateProduct(String productID, String sessionToken) throws JSONException{
		String product = productApi.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.put("description", "new product description");
		json.remove("seasons");
		return productApi.updateProduct(productID, json.toString(), sessionToken);
	}

	public int deleteProduct(String productID, String sessionToken){
		return productApi.deleteProduct(productID, sessionToken);
	}

	public String  createProductWithSpecifiedId(String productID, String sessionToken) throws IOException{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		return productApi.addProduct(product, productID, sessionToken);
	}



	public String getProductFollowers(String productID, String sessionToken) {
		return productApi.getProductFollowers(productID, sessionToken);
	}

	public String followProduct(String productID, String sessionToken) {
		return productApi.followProduct(productID, sessionToken);
	}

	public int unfollowProduct(String productID, String sessionToken) {
		return productApi.unfollowProduct(productID, sessionToken);
	}

	//************** SEASON FUNCTIONS **************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllSeason(String productID, String sessionToken, boolean expectedResult) throws Exception{
		results = new ArrayList<String>();

		String seasonID = createSeason(productID, sessionToken);
		validateTestResult(productID, "createSeason", expectedResult);

		String season = "{\"minVersion\":\"5.0\"}";
		String season2 = createSeasonWithSpecifiedId(UUID.randomUUID().toString(), productID, season, sessionToken);
		validateTestResult(season2, "createSeasonWithSpecifiedId", expectedResult);
		deleteSeason(season2, sessionToken);	//remove to continue test

		String response = getAllSeasons(sessionToken);
		validateTestResult(response, "getAllSeasons", expectedResult);

		//response = upgradeSeason(season2, "V4.5", sessionToken);
		//validateTestResult(response, "upgradeSeason", expectedResult);

		response = updateSeason(seasonID, productID, sessionToken);
		validateTestResult(response, "updateSeason", expectedResult);

		response = getSeasonConstants(seasonID, "iOS", sessionToken);
		validateTestResult(response, "getSeasonConstants", expectedResult);

		response = getSeasonDefaults(seasonID, sessionToken);
		validateTestResult(response, "getSeasonDefaults", expectedResult);

		response = getDocumentLinks(seasonID, sessionToken);
		validateTestResult(response, "getDocumentLinks", expectedResult);

		response = getServerVersion(seasonID, sessionToken);
		validateTestResult(response, "getServerVersion", expectedResult);


		int responseCode = deleteSeason(seasonID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete season");

		return results;
	}

	public String createSeason(String productID, String sessionToken) throws IOException{
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		return seasonApi.addSeason(productID, season, sessionToken);
	}

	public String getAllSeasons(String sessionToken){
		return seasonApi.getAllSeasons(sessionToken);
	}

	public String updateSeason(String seasonID, String productID, String sessionToken) throws Exception{
		String season = seasonApi.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "3.5");
		return seasonApi.updateSeason(seasonID, json.toString(), sessionToken);
	}

	public String upgradeSeason(String seasonID, String version, String sessionToken) throws Exception{
		return seasonApi.upgradeSeason(seasonID, version, sessionToken);

	}

	public int deleteSeason(String seasonID, String sessionToken){
		return seasonApi.deleteSeason(seasonID, sessionToken);

	}

	public String createSeasonWithSpecifiedId(String seasonID, String productID, String season, String sessionToken) throws IOException{
		return seasonApi.addSeason(productID, seasonID, season, sessionToken);	
	}

	public String getSeasonConstants(String seasonID, String platform, String sessionToken){

		return seasonApi.getConstants(seasonID, platform, sessionToken);
	}

	public String getSeasonDefaults(String seasonID, String sessionToken){
		return seasonApi.getDefaults(seasonID, sessionToken);
	}
	public String getDocumentLinks(String seasonID, String sessionToken) throws Exception{
		return seasonApi.getDocumentLinks(seasonID, sessionToken);
	}

	public String getServerVersion(String seasonID, String sessionToken) throws Exception{
		return seasonApi.getServerVersion(seasonID, sessionToken);
	}

	public String resetEncryptionKey(String seasonID, String sessionToken) {
		return seasonApi.resetEncryptionKey(seasonID, sessionToken);
	}

	public String getEncryptionKey(String seasonID, String sessionToken) {
		return seasonApi.getEncryptionKey(seasonID, sessionToken);
	}
	
	public JSONArray getSeasonsPerProduct(String productID, String sessionToken) throws Exception {
		return seasonApi.getSeasonsPerProduct(productID, sessionToken);
	}
	
	public String getBranchesUsage(String seasonID, String sessionToken) throws Exception {
		return seasonApi.getBranchesUsage(seasonID, sessionToken);
	}
	//************** SCHEMA FUNCTIONS **************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllInputSchema(String seasonID, String sessionToken, boolean expectedResult) throws Exception{
		results = new ArrayList<String>();

		String response = getInputSchema(seasonID, sessionToken);
		validateTestResult(response, "getInputSchema", expectedResult);

		response = validateInputSchema(seasonID, sessionToken);
		validateTestResult(response, "validateInputSchema", expectedResult);

		response = updateInputSchema(seasonID, sessionToken);
		validateTestResult(response, "updateInputSchema", expectedResult);

		return results;
	}



	public String getInputSchema(String seasonID, String sessionToken) throws Exception{
		return schemaApi.getInputSchema(seasonID, sessionToken);

	}

	public String getInputSample(String seasonID, String sessionToken) throws Exception{
		return schemaApi.getInputSample(seasonID,"DEVELOPMENT","90", sessionToken,"MAXIMAL",0.7);

	}

	public String updateInputSchema(String seasonID, String sessionToken) throws Exception{
		String schema = schemaApi.getInputSchema(seasonID, sessionToken);
		String file = FileUtils.fileToString(config + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
		JSONObject is = new JSONObject(file);
		JSONObject jsonSchema = new JSONObject(schema);
		jsonSchema.put("inputSchema", is);
		return schemaApi.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);

	}

	public String validateInputSchema(String seasonID, String sessionToken) throws Exception{
		String schemaBody = FileUtils.fileToString(config + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
		return schemaApi.validateSchema(seasonID, schemaBody.toString(), sessionToken);

	}

	//************** UTILITY FUNCTIONS **************//


	@SuppressWarnings("rawtypes")
	public ArrayList runAllUtilities(String seasonID, String sessionToken, boolean expectedResult) throws Exception{
		results = new ArrayList<String>();


		String utilityID = addUtility(seasonID, sessionToken);
		validateTestResult(utilityID, "addUtility", expectedResult);

		String response = getAllUtilities(seasonID, sessionToken);
		//validateTestResult(response, "getAllUtilities", expectedResult);
		if (!response.contains("lastModified")) //the response contains the "error" word even when the result is ok 
			results.add("getAllUtilities");

		response = getUtility(utilityID, sessionToken);
		validateTestResult(response, "getUtility", expectedResult);

		response = getUtilitiesInfo(seasonID, sessionToken);
		validateTestResult(response, "getUtilitiesInfo", expectedResult);

		response = updateUtility(utilityID, sessionToken);
		validateTestResult(response, "updateUtility", expectedResult);

		response = simulateUtility(seasonID, sessionToken);
		validateTestResult(response, "simulateUtility", expectedResult);


		int responseCode = deleteUtility(utilityID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete utility");

		return results;
	}


	public String addUtility(String seasonID, String sessionToken) throws IOException{
		String utility = FileUtils.fileToString(config + "/utilities/utility1.txt", "UTF-8", false);
		Properties utilProps1 = new Properties();
		utilProps1.load(new StringReader(utility));
		return utilitiesApi.addUtility(seasonID, utilProps1, sessionToken);

	}

	public String addStagedUtility(String seasonID, String sessionToken, boolean production) throws IOException{
		String utility = FileUtils.fileToString(config + "/utilities/utility1.txt", "UTF-8", false);
		Properties utilProps1 = new Properties();
		utilProps1.load(new StringReader(utility));
		utilProps1.setProperty("stage", production?"PRODUCTION":"DEVELOPMENT");
		return utilitiesApi.addUtility(seasonID, utilProps1, sessionToken);
	}

	public String addStagedStreamUtility(String seasonID, String sessionToken, boolean production) throws IOException{
		String utility = FileUtils.fileToString(config + "/utilities/utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("stage", production?"PRODUCTION":"DEVELOPMENT");

		return utilitiesApi.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);

	}

	public String getAllUtilities(String seasonID, String sessionToken){
		return utilitiesApi.getAllUtilites(seasonID,sessionToken,"DEVELOPMENT");

	}

	public String getUtility(String utilityID, String sessionToken){
		return utilitiesApi.getUtility(utilityID, sessionToken);

	}

	public String getUtilitiesInfo(String seasonID, String sessionToken){
		return utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");

	}

	public String updateUtility(String utilityID, String sessionToken) throws JSONException{
		String utility = utilitiesApi.getUtility(utilityID, sessionToken);
		JSONObject jsonUtil = new JSONObject(utility);
		jsonUtil.put("minAppVersion", "0.2");
		return utilitiesApi.updateUtility(utilityID, jsonUtil, sessionToken);

	}

	public String updateUtilityStage(String utilityID, String sessionToken, String stage) throws JSONException{
		String utility = utilitiesApi.getUtility(utilityID, sessionToken);
		JSONObject jsonUtil = new JSONObject(utility);
		jsonUtil.put("stage", stage);
		return utilitiesApi.updateUtility(utilityID, jsonUtil, sessionToken);
	}

	public int deleteUtility(String utilityID, String sessionToken){
		return utilitiesApi.deleteUtility(utilityID, sessionToken);

	}

	public String simulateUtility(String seasonID, String sessionToken){
		String body =" $#^StartOfRule^#$";
		return utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);

	}



	//************** FEATURE FUNCTIONS **************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllFeatures(String seasonID, String branchID, String sessionToken, boolean expectedResult) throws Exception{
		results = new ArrayList<String>();


		String featureID = createFeature(seasonID, branchID, sessionToken);
		validateTestResult(featureID, "createFeature", expectedResult);

		String response = getAllFeatures(sessionToken);
		validateTestResult(response, "getAllFeatures", expectedResult);

		response = getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		validateTestResult(response, "getFeaturesBySeasonFromBranch", expectedResult);

		response = getFeatureFromBranch(featureID, branchID, sessionToken);
		validateTestResult(response, "getFeatureFromBranch", expectedResult);


		response = updateFeatureInBranch(seasonID, branchID, featureID, sessionToken);
		validateTestResult(response, "updateFeatureInBranch", expectedResult);

		response = featuresInSeason(seasonID, sessionToken);
		validateTestResult(response, "featuresInSeason", expectedResult);

		response = featuresInSeason(seasonID, sessionToken);
		validateTestResult(response, "featuresInSeason", expectedResult);

		//response = findFeature(seasonID, sessionToken);
		//validateTestResult(response, "findFeature", expectedResult);

		response = getFeatureAttributes(featureID, branchID, sessionToken);
		validateTestResult(response, "getFeatureAttributes", expectedResult);

		response = simulateDeleteFeatureFromBranch(featureID, branchID, sessionToken);
		validateTestResult(response, "simulateDeleteFeatureFromBranch", expectedResult);

		response = simulateUpdateFeatureInBranch(seasonID, branchID, featureID, sessionToken);
		validateTestResult(response, "simulateUpdateFeatureInBranch", expectedResult);

		response = copyFeature(seasonID, branchID, featureID, sessionToken);
		validateTestResult(response, "copyFeature", expectedResult);


		if (isAuthenticated){

			response = followFeature(featureID, sessionToken);
			validateTestResult(response, "followFeature", expectedResult);

			response = getFeatureFollowers(featureID, sessionToken);
			validateTestResult(response, "getFeatureFollowers", expectedResult);

			int code = unfollowFeature(featureID, sessionToken);
			validateTestResult(response, "getFeatureFollowers", expectedResult);

			boolean unfollow = ((code == 200) ? false : true);		

			if (unfollow != expectedResult)
				results.add("unfollow feature");


		}
		int responseCode = deleteFeature(featureID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		

		if (deleted != expectedResult)
			results.add("delete feature");

		return results;
	}


	public String createFeature(String seasonID, String branchID, String sessionToken) throws IOException, JSONException{
		return createStagedFeature(seasonID, branchID, sessionToken, false);	
	}

	public String createStagedFeature(String seasonID, String branchID, String sessionToken, boolean production) throws IOException, JSONException{
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("internalUserGroups", new JSONArray());

		json.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		return featureApi.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
	}

	public String getAllFeatures(String sessionToken){
		return featureApi.getAllFeatures(sessionToken);

	}

	public String getFeaturesBySeasonFromBranch(String seasonID, String branchID, String sessionToken){
		return featureApi.getFeaturesBySeason(seasonID, branchID, sessionToken);
	}


	public String getFeatureFromBranch(String featureID, String branchID, String sessionToken){
		return featureApi.getFeatureFromBranch(featureID, branchID, sessionToken);

	}

	public String updateFeatureInBranch(String seasonID, String branchID, String featureID, String sessionToken) throws JSONException, IOException{
		String feature = featureApi.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "new description");
		return featureApi.updateFeatureInBranch(seasonID, branchID, featureID, json.toString(), sessionToken);

	}
	
	public String updateFeatureInBranch(String seasonID, String branchID, String featureID, String feature, String sessionToken) throws JSONException, IOException{
		return featureApi.updateFeatureInBranch(seasonID, branchID, featureID, feature, sessionToken);

	}

	public String updateFeatureStageInBranch(String seasonID, String branchID, String featureID, String sessionToken, String stage) throws JSONException, IOException{
		String feature = featureApi.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", stage);
		return featureApi.updateFeatureInBranch(seasonID, branchID, featureID, json.toString(), sessionToken);
	}

	public int deleteFeature(String featureID, String sessionToken){
		return featureApi.deleteFeature(featureID, sessionToken);
	}

	public String featuresInSeason(String seasonID, String sessionToken){
		return featureApi.featuresInSeason(seasonID, sessionToken);
	}

	public RestClientUtils.RestCallResults findFeature(String seasonId, String branchId, String pattern, Collection<String> searchAreas, Collection<String> options, String sessionToken){
		return featureApi.findFeatures(seasonId, branchId, pattern, searchAreas, options, sessionToken);
	}

	public String getFeatureFollowers(String featureID, String sessionToken) {
		return featureApi.getFeatureFollowers(featureID, sessionToken);

	}

	public String followFeature(String featureID, String sessionToken) {
		return featureApi.followFeature(featureID, sessionToken);
	}

	public int  unfollowFeature(String featureID, String sessionToken) {
		return featureApi.unfollowFeature(featureID, sessionToken);

	}

	public String  getFeatureAttributes(String featureID, String branchID, String sessionToken) {
		return featureApi.getFeatureAttributesFromBranch(featureID, branchID, sessionToken);

	}

	public String  simulateDeleteFeatureFromBranch(String featureID, String branchID, String sessionToken) {
		return featureApi.simulateDeleteFeatureFromBranch(featureID, branchID, sessionToken);

	}

	public String  simulateUpdateFeatureInBranch(String seasonID, String branchID, String  featureID, String sessionToken) throws JSONException, IOException {
		String feature = featureApi.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", "new description");
		return featureApi.simulateUpdateFeatureInBranch(seasonID, branchID, featureID, json.toString(), sessionToken);

	}

	public String importFeatures(String featureToImport, String targetSeasonID, String branchID, String sessionToken) throws IOException {
		String rootId = featureApi.getBranchRootId(targetSeasonID, branchID, sessionToken);					
		return featureApi.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suf2", true, sessionToken, branchID);
	}

	public String copyFeature(String seasonID, String branchID, String sourceFeatureId, String sessionToken) throws IOException {
		String rootId = featureApi.getBranchRootId(seasonID, branchID, sessionToken); 
		return featureApi.copyItemBetweenBranches(sourceFeatureId, rootId, "ACT", null, "suf1", sessionToken, BranchesRestApi.MASTER, BranchesRestApi.MASTER);

	}

	//************** BRANCHES ************************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllBranches(String seasonID, String sessionToken, boolean expectedResult) throws Exception{
		results = new ArrayList<String>();


		String branchID = createBranch(seasonID, sessionToken);
		validateTestResult(branchID, "createBranch", expectedResult);

		String response = getBranch(branchID, sessionToken);
		validateTestResult(response, "getBranch", expectedResult);

		response = updateBranch(branchID, sessionToken);
		validateTestResult(response, "updateBranch", expectedResult);

		response = getAllBranches(seasonID, sessionToken);
		validateTestResult(response, "getAllBranches", expectedResult);

		String featureID = createFeature(seasonID, BranchesRestApi.MASTER, sessionToken);
		response = checkOut(branchID, featureID, sessionToken);
		validateTestResult(response, "checkOut", expectedResult);

		response = cancelCheckOut(branchID, featureID, sessionToken);
		validateTestResult(response, "cancelCheckOut", expectedResult);

		int responseCode = deleteBranch(branchID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		

		if (deleted != expectedResult)
			results.add("delete branch");

		return results;
	}



	public String createBranch(String seasonID, String sessionToken) throws IOException, JSONException {
		String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(branch);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		return branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,sessionToken);

	}

	public String getBranch(String branchID, String sessionToken) throws Exception{
		return branchesRestApi.getBranch(branchID,sessionToken);
	}

	public String updateBranch(String branchID, String sessionToken) throws Exception {
		String branch = branchesRestApi.getBranch(branchID,sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("description", "new description");
		return branchesRestApi.updateBranch(branchID, json.toString(), sessionToken);
	}

	public int deleteBranch(String branchID, String sessionToken) throws Exception {
		return branchesRestApi.deleteBranch(branchID, sessionToken);

	}


	public String getAllBranches(String seasonID, String sessionToken) throws Exception {

		return branchesRestApi.getAllBranches(seasonID,sessionToken);

	}

	public String checkOut(String branchID, String featureID, String sessionToken) throws IOException{

		return branchesRestApi.checkoutFeature(branchID,featureID, sessionToken);

	}

	public String cancelCheckOut(String branchID, String featureID, String sessionToken) throws IOException{

		return branchesRestApi.cancelCheckoutFeature(branchID,featureID, sessionToken);
	}


	// *************** STREAMS *******************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllStreams(String seasonID, String sessionToken, boolean expectedResult) throws Exception{

		results = new ArrayList<String>();
		String streamID = createStream(seasonID, sessionToken);
		validateTestResult(streamID, "create stream", expectedResult);
		String response = getStream(streamID, sessionToken);
		validateTestResult(response, "get stream", expectedResult);
		response = updateStream(streamID, sessionToken);
		validateTestResult(response, "update stream", expectedResult);
		response = getAllStreams(seasonID, sessionToken);
		validateTestResult(response, "get all streams", expectedResult);
		response = getStreamEvents(seasonID, sessionToken);
		validateTestResult(response, "get stream events", expectedResult);		
		response = updateStreamEvents(seasonID, sessionToken);
		validateTestResult(response, "update stream events", expectedResult);		
		response = filterStreamEvents(seasonID, sessionToken);
		validateTestResult(response, "filter stream events", expectedResult);				

		int responseCode = deleteStream(streamID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete stream");

		return results;

	}

	public String createStream(String seasonID, String sessionToken) throws JSONException, IOException{
		return createStagedStream(seasonID, sessionToken, false);
	}

	public String createStagedStream(String seasonID, String sessionToken, boolean production) throws JSONException, IOException{

		String stream = FileUtils.fileToString(config + "streams/stream1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(stream) ;
		json.put("name", RandomStringUtils.randomAlphabetic(6));
		json.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		json.put("internalUserGroups", new JSONArray());

		return streamsApi.createStream(seasonID, json.toString(), sessionToken);
	}

	public String updateStream(String streamID, String sessionToken) throws Exception{
		String response = streamsApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(response);
		json.put("description", "new stream description");
		return streamsApi.updateStream(streamID, json.toString(), sessionToken);

	}

	public String updateStreamStage(String streamID, String sessionToken, String stage) throws Exception{
		String response = streamsApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(response);
		json.put("stage", stage);
		return streamsApi.updateStream(streamID, json.toString(), sessionToken);
	}

	public String getStream(String streamID, String sessionToken) throws Exception{
		return streamsApi.getStream(streamID, sessionToken);

	}

	public String getAllStreams(String seasonID, String sessionToken) throws Exception{
		return streamsApi.getAllStreams(seasonID, sessionToken);

	}

	public int deleteStream(String streamID, String sessionToken) throws Exception{
		return streamsApi.deleteStream(streamID, sessionToken);
	}



	public String getStreamEvents(String seasonID, String sessionToken) throws Exception{
		return streamsApi.getStreamEvent(seasonID, sessionToken);

	}

	public String updateStreamEvents(String seasonID, String sessionToken) throws Exception{

		String events = FileUtils.fileToString(config + "streams/global_events.json", "UTF-8", false);		
		JSONObject newEvents = new JSONObject(events);
		String obj = streamsApi.getStreamEvent(seasonID, sessionToken);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", newEvents.getJSONArray("events"));
		return streamsApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);

	}


	public String filterStreamEvents(String seasonID, String sessionToken) throws IOException{
		return streamsApi.getStreamEventFields(seasonID, "event.name==\"module-viewed\"", sessionToken);		
	}





	// *************** AIRLOCK NOTIFICATIONS *********************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllNotifications(String seasonID, String sessionToken, boolean expectedResult) throws JSONException, IOException{
		results = new ArrayList<String>();

		String notificationID = addNotification(seasonID, sessionToken);
		validateTestResult(notificationID, "add notification", expectedResult);

		String response = updateNotification(notificationID, sessionToken);
		validateTestResult(response, "update notification", expectedResult);

		response = getNotification(notificationID, sessionToken);
		validateTestResult(response, "get notification", expectedResult);


		response = getAllNotifications(seasonID, sessionToken);
		validateTestResult(response, "get all notification", expectedResult);

		response = updateAllNotifications(seasonID, sessionToken);
		validateTestResult(response, "get all notification", expectedResult);


		int responseCode = deleteNotification(notificationID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete notification");

		return results;
	}



	public String addNotification(String seasonID, String sessionToken) throws IOException, JSONException{
		return createStagedNotification(seasonID, sessionToken, false);
	}

	public String createStagedNotification(String seasonID, String sessionToken, boolean production) throws IOException, JSONException{
		String notification = FileUtils.fileToString(config + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		json.put("internalUserGroups", new JSONArray());

		return notificationApi.createNotification(seasonID, json.toString(), sessionToken);
	}

	public String updateNotification(String notificationID, String sessionToken) throws JSONException{
		String notification = notificationApi.getNotification(notificationID, sessionToken);
		JSONObject json = new JSONObject(notification);
		json.put("description", "new descr");
		return notificationApi.updateNotification(notificationID, json.toString(), sessionToken);

	}

	public String updateNotificationStage(String notificationID, String sessionToken, String stage) throws JSONException{
		String notification = notificationApi.getNotification(notificationID, sessionToken);
		JSONObject json = new JSONObject(notification);
		json.put("stage", stage);
		return notificationApi.updateNotification(notificationID, json.toString(), sessionToken);
	}

	public String getAllNotifications(String seasonID, String sessionToken){
		return notificationApi.getAllNotifications(seasonID, sessionToken);
	}

	public String updateAllNotifications(String seasonID, String sessionToken) throws JSONException{
		String allNotif = notificationApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);

		JSONObject limitation = new JSONObject();
		limitation.put("maxNotifications", 1);
		limitation.put("minInterval", 10);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);

		allNotifJson.put("notificationsLimitations", allLimitations);

		return  notificationApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);

	}

	public int deleteNotification(String notificationID, String sessionToken){
		return notificationApi.deleteNotification(notificationID, sessionToken);
	}

	public String getNotification(String notificationID, String sessionToken){

		return notificationApi.getNotification(notificationID, sessionToken);
	}



	/****** ANALYTICS FUNCTIONS 
	 * @throws JSONException *******/

	@SuppressWarnings("rawtypes")
	public ArrayList runAllAnalytics(String seasonID, String branchID, String featureID, String sessionToken, boolean expectedResult) throws JSONException{

		results = new ArrayList<String>();

		String response = addFeatureToAnalytics(featureID, branchID, sessionToken);
		validateTestResult(response, "addFeatureToAnalytics", expectedResult);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		response = updateFeatureAttributesForAnalytics(featureID, branchID, attributes.toString(), sessionToken);
		validateTestResult(response, "updateFeatureAttributesForAnalytics", expectedResult);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.weatherSummary.closestLightning.cardinalDirection");
		response = updateInputFieldsForAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		validateTestResult(response, "updateInputFieldsForAnalytics", expectedResult);

		response = removeFeatureFromAnalytics(featureID, branchID, sessionToken);
		validateTestResult(response, "removeFeatureFromAnalytics", expectedResult);

		String globalDataCollection = getGlobalDataCollection(seasonID, branchID, sessionToken);
		validateTestResult(globalDataCollection, "getGlobalDataCollection", expectedResult);

		String input = analyticsApi.addFeatureOnOff(globalDataCollection, featureID);
		response = updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		validateTestResult(response, "updateGlobalDataCollection", expectedResult);

		response = getAnalyticsQuota(seasonID, sessionToken);
		validateTestResult(response, "getAnalyticsQuota", expectedResult);

		response = setAnalyticsQuota(seasonID, sessionToken);
		validateTestResult(response, "setAnalyticsQuota", expectedResult);

		return results;
	}

	public String addFeatureToAnalytics(String featureID,String branchID,  String sessionToken) {
		return analyticsApi.addFeatureToAnalytics(featureID, branchID, sessionToken);
	}

	public String updateFeatureAttributesForAnalytics(String featureID, String branchID, String attributes, String sessionToken) {
		return analyticsApi.addAttributesToAnalytics(featureID, branchID, attributes, sessionToken);

	}

	public String updateInputFieldsForAnalytics(String seasonID, String branchID, String inputFields, String sessionToken) {
		return analyticsApi.updateInputFieldToAnalytics(seasonID, branchID, inputFields, sessionToken);
	}

	public String removeFeatureFromAnalytics(String featureID, String branchID, String sessionToken) {
		return analyticsApi.deleteFeatureFromAnalytics(featureID, branchID, sessionToken);
	}

	public String getGlobalDataCollection(String seasonID, String branchID, String sessionToken) {
		return analyticsApi.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
	}

	public String updateGlobalDataCollection(String seasonID, String branchID, String input, String sessionToken) {
		return analyticsApi.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);

	}

	public String getAnalyticsQuota(String seasonID, String sessionToken) {
		return analyticsApi.getQuota(seasonID,sessionToken);
	}

	public String setAnalyticsQuota(String seasonID, String sessionToken) {
		return analyticsApi.updateQuota(seasonID, 3, sessionToken);
	}

	/****** EXPERIMENTS/VARIANTS FUNCTIONS  
	 * @throws Exception *******/

	@SuppressWarnings("rawtypes")
	public ArrayList runAllExperiments(String productID,  String seasonID, String sessionToken, boolean expectedResult) throws Exception{

		results = new ArrayList<String>();

		String experimentID = createExperiment(productID, sessionToken);
		validateTestResult(experimentID, "createExperiment", expectedResult);

		String branchName = RandomStringUtils.randomAlphabetic(5);
		String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(branch);
		json.put("name", branchName);
		branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,sessionToken);

		String variantID = createVariant(experimentID, branchName, sessionToken);
		validateTestResult(variantID, "createVariant", expectedResult);

		String response = getVariant(variantID, sessionToken);
		validateTestResult(response, "getVariant", expectedResult);

		response = updateVariant(variantID, sessionToken);
		validateTestResult(response, "updateVariant", expectedResult);

		//	response = resetDashboard(experimentID, sessionToken);
		//	validateTestResult(response, "resetDashboard", expectedResult);

		response = getExperiment(experimentID, sessionToken);
		validateTestResult(response, "getExperiment", expectedResult);

		response = updateExperiment(experimentID, sessionToken);
		validateTestResult(response, "updateExperiment", expectedResult);

		String allExperiments = getAllExperiments(productID, sessionToken);
		validateTestResult(allExperiments, "getAllExperiments", expectedResult);

		response = updateAllExperiments(productID, allExperiments, sessionToken);
		validateTestResult(response, "updateAllExperiments", expectedResult);

		response = getExperimentInputSample(experimentID, sessionToken);
		validateTestResult(allExperiments, "getExperimentInputSample", expectedResult);

		response = getExperimentUtilitiesInfo(experimentID, sessionToken);
		validateTestResult(allExperiments, "getExperimentUtilitiesInfo", expectedResult);

		response = getBranchesInExperiment(experimentID, sessionToken);
		validateTestResult(allExperiments, "getBranchesInExperiment", expectedResult);

		response = getExperimentIndexingInfo(experimentID, sessionToken);
		validateTestResult(allExperiments, "getExperimentIndexingInfo", expectedResult);

		response = getExperimentGlobalDataCollection(experimentID, sessionToken);
		validateTestResult(allExperiments, "getExperimentGlobalDataCollection", expectedResult);

		int responseCode = deleteVariant(variantID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete variant");

		responseCode = deleteExperiment(experimentID, sessionToken);
		deleted = ((responseCode == 200) ? false : true);		

		if (deleted != expectedResult)
			results.add("delete variant");

		return results;
	}

	public String createExperiment(String productID, String sessionToken) throws IOException, JSONException {
		String experiment = FileUtils.fileToString(config + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", false);			
		return experimentsRestApi.createExperiment(productID, expJson.toString(),sessionToken);
	}

	public String resetDashboard(String experimentID, String sessionToken) throws IOException {
		return experimentsRestApi.resetDashboard(experimentID,sessionToken);
	}

	public String updateExperiment(String experimentID, String sessionToken) throws Exception{

		String experiment = experimentsRestApi.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("description", "new description");
		return experimentsRestApi.updateExperiment(experimentID, json.toString(), sessionToken);
	}

	public String updateAllExperiments(String productID, String experiment, String sessionToken) throws Exception{

		return experimentsRestApi.updateExperiments(productID, experiment, sessionToken);
	}

	public String updateExperiment(String experimentID, String experiment, String sessionToken) throws IOException{

		return experimentsRestApi.updateExperiment(experimentID, experiment, sessionToken);
	}
	

	public int deleteExperiment(String experimentID, String sessionToken) throws Exception{
		return experimentsRestApi.deleteExperiment(experimentID, sessionToken);
	}


	public String getExperiment(String experimentID, String sessionToken) throws Exception {
		return experimentsRestApi.getExperiment(experimentID,sessionToken);
	}
	public String getAllExperiments(String productID, String sessionToken) throws Exception {
		return experimentsRestApi.getAllExperiments(productID,sessionToken);
	}

	public String getExperimentInputSample(String experimentID, String sessionToken) throws Exception {
		return experimentsRestApi.getInputSample(experimentID, "DEVELOPMENT", "2.5", sessionToken, "MAXIMAL", 0.7);
	}

	public String getExperimentUtilitiesInfo(String experimentID, String sessionToken) throws Exception {
		return experimentsRestApi.getUtilitiesInfo(experimentID, "DEVELOPMENT", sessionToken);
	}

	public String getBranchesInExperiment(String experimentID, String sessionToken) throws Exception {
		return experimentsRestApi.getAvailableBranches(experimentID, sessionToken);
	}

	public String getExperimentIndexingInfo(String experimentID, String sessionToken) throws Exception {
		return experimentsRestApi.getIndexinginfo(experimentID, sessionToken);
	}

	public String getExperimentGlobalDataCollection(String experimentID ,String sessionToken) {

		return analyticsApi.getExperimentGlobalDataCollection(experimentID, sessionToken);
	}

	public String getExperimentAnalyticsQuota(String experimentID, String sessionToken) {
		return analyticsApi.getExperimentQuota(experimentID, sessionToken);
	}

	public String createVariant(String experimentID, String branchName, String sessionToken) throws JSONException, IOException {

		String variant = FileUtils.fileToString(config + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);			
		variantJson.put("branchName", branchName);
		return experimentsRestApi.createVariant(experimentID,variantJson.toString(),sessionToken);
	}

	public String updateVariant(String variantID, String sessionToken) throws Exception {
		String variant = experimentsRestApi.getVariant(variantID,sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("description", "new description");
		return experimentsRestApi.updateVariant(variantID,json.toString(),sessionToken);
	}
	
	public String updateVariant(String variantID, String variant, String sessionToken) throws Exception {
		return experimentsRestApi.updateVariant(variantID, variant, sessionToken);
	}

	public String getVariant(String variantID, String sessionToken) throws Exception{
		return experimentsRestApi.getVariant(variantID,sessionToken);
	}


	public int deleteVariant(String variantID, String sessionToken) throws Exception {

		return experimentsRestApi.deleteVariant(variantID, sessionToken);
	}



	//************** STRING/TRANSLATION FUNCTIONS **************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllStrings(String seasonID, String productID, String sessionToken, boolean expectedResult) throws Exception{

		results = new ArrayList<String>();
		String stringID = addString(seasonID, "/strings/string1.txt", sessionToken);
		validateTestResult(stringID, "create string", expectedResult);
		String response = getAllStrings(seasonID, sessionToken);
		validateTestResult(response, "get all strings", expectedResult);
		response = getString(stringID, sessionToken);
		validateTestResult(response, "get string", expectedResult);
		response = updateString(stringID, sessionToken);
		validateTestResult(response, "update string", expectedResult);
		response = getStringForTranslation(seasonID, sessionToken);
		validateTestResult(response, "getStringForTranslation", expectedResult);
		response = getNewStringsForTranslation(seasonID, sessionToken);
		validateTestResult(response, "getNewStringsForTranslation", expectedResult);

		String featureID = createFeature(seasonID, BranchesRestApi.MASTER, sessionToken);
		response = getStringsUsedInFeature(featureID, sessionToken);
		validateTestResult(response, "getNewStringsForTranslation", expectedResult);
		response = getStringStatuses(seasonID, sessionToken);
		validateTestResult(response, "getStringStatuses", expectedResult);
		response = getStringsByStatuses(seasonID, sessionToken);
		validateTestResult(response, "getStringsByStatuses", expectedResult);
		response = getSupportedLocales(seasonID, sessionToken);
		validateTestResult(response, "getSupportedLocales", expectedResult);
		response = addSupportedLocales(seasonID, sessionToken);
		validateTestResult(response, "addSupportedLocales", expectedResult);
		response = removeSupportedLocales(seasonID, sessionToken);
		validateTestResult(response, "removeSupportedLocales", expectedResult);

		response = markForTranslation(seasonID, stringID, sessionToken);
		validateTestResult(response, "markForTranslation", expectedResult);
		response = reviewTranslation(seasonID, stringID, sessionToken);
		validateTestResult(response, "reviewTranslation", expectedResult);
		response = sendToTranslation(seasonID, stringID, sessionToken);
		validateTestResult(response, "sendToTranslation", expectedResult);

		response = addTranslation(seasonID, "fr", sessionToken);
		validateTestResult(response, "addTranslation", expectedResult);
		response = getTranslation(seasonID, sessionToken);
		validateTestResult(response, "getTranslation", expectedResult);
		response = updateTranslation(seasonID, sessionToken);
		validateTestResult(response, "updateTranslation", expectedResult);

		response = overrideTranslate(stringID, sessionToken);
		validateTestResult(response, "overrideTranslate", expectedResult);
		response = cancelOverrideTranslate(stringID, sessionToken);
		validateTestResult(response, "cancelOverrideTranslate", expectedResult);
		response = getTranslationSummary(seasonID, stringID, sessionToken);
		validateTestResult(response, "getTranslationSummary", expectedResult);
		response = removeSupportedLocales(seasonID, sessionToken);
		validateTestResult(response, "removeSupportedLocales", expectedResult);

		String season = "{\"minVersion\":\"5.0\"}";
		String seasonID2 = seasonApi.addSeason(productID, season, sessionToken);
		response = copyStrings(seasonID2, stringID, sessionToken);
		validateTestResult(response, "copyStrings", expectedResult);
		response = importStrings(seasonID, seasonID2, sessionToken);
		validateTestResult(response, "importStrings", expectedResult);

		int responseCode = deleteString(stringID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete string");

		return results;

	}


	public String addString(String seasonID, String filePath, String sessionToken) throws Exception{
		String str = FileUtils.fileToString(config + filePath, "UTF-8", false);
		return stringApi.addString(seasonID, str, sessionToken);
	}

	public String addStagedString(String seasonID, String filePath, String sessionToken, boolean production) throws Exception{
		String str = FileUtils.fileToString(config + filePath, "UTF-8", false);
		JSONObject strObj = new JSONObject(str);
		strObj.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		return stringApi.addString(seasonID, strObj.toString(), sessionToken);
	}


	public String getAllStrings(String seasonID, String sessionToken) throws Exception{
		return stringApi.getAllStrings(seasonID,sessionToken);
	}

	public String getString(String stringID, String sessionToken) throws Exception{
		return stringApi.getString(stringID, sessionToken);
	}

	public String updateString(String stringID, String sessionToken) throws Exception{

		String str = stringApi.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("value", "hi");
		jsonstr.put("owner", "Bill");
		return stringApi.updateString(stringID, jsonstr.toString(), sessionToken);
	}


	public String updateStringStage(String stringID, String sessionToken, String stage) throws Exception{
		String str = stringApi.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("stage", stage);
		return stringApi.updateString(stringID, jsonstr.toString(), sessionToken);
	}

	public int deleteString(String stringID, String sessionToken) throws Exception{
		return stringApi.deleteString(stringID, sessionToken);
	}

	public String copyStrings(String targetSeasonID, String stringID, String sessionToken) throws Exception{
		return translationApi.copyStrings(targetSeasonID,new String[]{stringID},false, sessionToken);
	}


	public String importStrings(String sourceSeasonID, String destSeasonID, String sessionToken) throws Exception{
		String input = stringApi.getAllStrings(sourceSeasonID, "INCLUDE_TRANSLATIONS", sessionToken);
		return translationApi.importStrings(destSeasonID, input, false, sessionToken);

	}

	public String getStringForTranslation(String seasonID, String sessionToken) throws Exception{
		return translationApi.stringForTranslation(seasonID,sessionToken);
	}

	public String addTranslation(String seasonID, String locale, String sessionToken) throws Exception{
		String frTranslation = FileUtils.fileToString(config + "strings/translationFR6.txt", "UTF-8", false);
		return translationApi.addTranslation(seasonID,locale,frTranslation,sessionToken);
	}

	public String getTranslation(String seasonID, String sessionToken) throws Exception{
		return translationApi.getTranslation(seasonID,"fr","DEVELOPMENT",sessionToken);
	}

	public String updateTranslation(String seasonID, String sessionToken) throws Exception{
		String frTranslation = FileUtils.fileToString(config + "strings/translationFR6.txt", "UTF-8", false);
		frTranslation = frTranslation.replace("Bonjour","SALUT");
		return translationApi.updateTranslation(seasonID,"fr",frTranslation,sessionToken);
	}



	public String getSupportedLocales(String seasonID, String sessionToken) throws Exception {
		return translationApi.getSupportedLocales(seasonID, sessionToken);
	}

	public String addSupportedLocales(String seasonID, String sessionToken) throws Exception {
		return translationApi.addSupportedLocales(seasonID,"fr", sessionToken);
	}

	public String removeSupportedLocales(String seasonID, String sessionToken) throws Exception {
		return translationApi.removeSupportedLocales(seasonID,"fr", sessionToken);
	}

	public String overrideTranslate(String stringID, String sessionToken) throws Exception {
		return translationApi.overrideTranslate(stringID,"fr","", sessionToken);

	}

	public String cancelOverrideTranslate(String stringID, String sessionToken) throws Exception {
		return translationApi.cancelOverride(stringID,"fr", sessionToken);
	}


	public String getStringsUsedInFeature(String featureID, String sessionToken) throws Exception {
		return translationApi.stringInUse(featureID, sessionToken);
	}

	public String markForTranslation(String seasonID, String stringID, String sessionToken) throws Exception {
		return translationApi.markForTranslation(seasonID,new String[]{stringID}, sessionToken);
	}

	public String reviewTranslation(String seasonID, String stringID,String sessionToken) throws Exception {
		return translationApi.reviewForTranslation(seasonID,new String[]{stringID}, sessionToken);
	}

	public String getNewStringsForTranslation(String seasonID, String sessionToken) throws Exception {
		return translationApi.getNewStringsForTranslation(seasonID,new String[]{}, sessionToken);
	}


	public String sendToTranslation(String seasonID, String stringID, String sessionToken) throws Exception {
		return translationApi.sendToTranslation(seasonID,new String[]{stringID}, sessionToken);
	}

	public String getTranslationSummary(String seasonID, String stringID, String sessionToken) throws Exception {
		return translationApi.getTranslationSummary(seasonID,new String[]{stringID}, sessionToken);
	}

	public String getStringStatuses(String seasonID, String sessionToken) throws Exception {
		return translationApi.getStringStatuses(seasonID,sessionToken);
	}

	public String getStringsByStatuses(String seasonID, String sessionToken) throws Exception {
		return translationApi.getStringsByStatuses(seasonID, "NEW_STRING", "BASIC", sessionToken);
	}

	//TODO
	//GET /translations/seasons/{season-id}/stringstoformat
	//PUT /translations/seasons/{season-id}/importstringswithformat



	//************** OPERATIONS FUNCTIONS **************//

	public String getRoles(String sessionToken) throws Exception {
		return operationApi.getRoles(sessionToken);
	}
	public String setRoles(String input, String sessionToken) throws IOException{
		return operationApi.setRoles(input, sessionToken);

	}

	public String getAirlockUsers(String sessionToken) throws Exception {
		return operationApi.getAirlockUsers(sessionToken);
	}

	public String getProductAirlockUsers(String productID, String sessionToken) throws Exception {
		return operationApi.getProductAirlockUsers(sessionToken, productID);
	}
	/*public String setAirlockUsers(String input, String sessionToken) throws IOException {
			return operationApi.setAirlockUsers(input,sessionToken);
	}*/


	public String getAirlockServers(String sessionToken) throws Exception {
		return operationApi.getAirlockServers(sessionToken);
	}

	public String setAirlockServers(String input, String sessionToken) throws IOException {
		return operationApi.setAirlockServers(input,sessionToken);
	}

	public String getUserGroups(String productID, String sessionToken) throws Exception{
		return userGroupsApi.getUserGroups(productID, sessionToken);
	}

	public String setUserGroups(String productID, String input, String sessionToken) throws Exception {
		return userGroupsApi.setUserGroups(productID, input, sessionToken);
	}
	
	public String getUserGroupsUsage(String productID, String sessionToken) throws Exception {
		return userGroupsApi.getUserGroupsUsage(productID, sessionToken);
	}

	public int healthcheck(String sessionToken) throws Exception {
		return operationApi.healthcheck(sessionToken);
	}

	public String getCapabilities(String sessionToken) throws Exception {
		return operationApi.getCapabilities(sessionToken);
	}
	public String setCapabilities(String input, String sessionToken) throws Exception{
		return operationApi.setCapabilities(input, sessionToken);

	}

	//************** AIRLOCK KEY  ******************//

	public String generateAirlockKeyCompleteResponse(String input, String sessionToken) throws IOException{
		return operationApi.generateAirlockKeyCompleteResponse(input, sessionToken);
	}

	public String startSessionFromKey(String key, String keyPassword) throws Exception{
		return operationApi.startSessionFromKey(key, keyPassword);
	}

	public String getAllKeysPerUser(String owner, String sessionToken) throws Exception{
		return operationApi.getAllKeys(owner, sessionToken);
	}

	public String getKey(String keyID, String sessionToken) throws Exception{
		return operationApi.getKey(keyID, sessionToken);
	}

	public String updateKey(String keyID, String keyContent, String sessionToken) throws Exception{
		return operationApi.updateKey(keyID, keyContent, sessionToken);
	}

	public int deleteKey(String keyID, String sessionToken) throws Exception{
		return operationApi.deleteKey(keyID, sessionToken);
	}	

	public String getRolesPerUser(String user, String sessionToken) throws Exception{
		return operationApi.getRolesPerUser(user, sessionToken);
	}

	
	// *************** PURCHSES API *********************//

	@SuppressWarnings("rawtypes")
	public ArrayList runAllPurchases(String seasonID, String sessionToken, boolean expectedResult) throws JSONException, IOException{
		results = new ArrayList<String>();

		String purchaseID = addInAppPurchase(seasonID, sessionToken);
		validateTestResult(purchaseID, "add inAppPurcahse", expectedResult);

		String response = updatePurchase(seasonID, purchaseID, sessionToken);
		validateTestResult(response, "update inAppPurcahse", expectedResult);

		response = getPurcahse(purchaseID, sessionToken);
		validateTestResult(response, "get inAppPurcahse", expectedResult);

		response = getAllPurchases(seasonID, sessionToken);
		validateTestResult(response, "get all purchases", expectedResult);

		int responseCode = deletePurchase(purchaseID, sessionToken);
		boolean deleted = ((responseCode == 200) ? false : true);		//if responseCode!=200, it's an error, therefore success=false

		if (deleted != expectedResult)
			results.add("delete purcahse");

		return results;
	}

	public String addInAppPurchase(String seasonID, String sessionToken) throws IOException, JSONException{
		return createStagedInAppPurchase(seasonID, sessionToken, false);
	}

	public String createStagedInAppPurchase(String seasonID, String sessionToken, boolean production) throws IOException, JSONException{
		String unAppPurcahse = FileUtils.fileToString(config + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(unAppPurcahse);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		json.put("internalUserGroups", new JSONArray());

		return purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
	}
	
	public String addPurchaseOptions(String seasonID, String parentID, String sessionToken) throws IOException, JSONException{
		return createStagedPurchaseOptions(seasonID, parentID, sessionToken, false);
	}
	
	public String createStagedPurchaseOptions(String seasonID, String parentID, String sessionToken, boolean production) throws IOException, JSONException{
		String unAppPurcahse = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(unAppPurcahse);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", production?"PRODUCTION":"DEVELOPMENT");
		json.put("internalUserGroups", new JSONArray());

		return purchasesApi.addPurchaseItem(seasonID, json.toString(), parentID, sessionToken);
	}

	public String updatePurchase(String seasonID, String purchaseID, String sessionToken) throws JSONException, IOException{
		String purcahseItem = purchasesApi.getPurchaseItem(purchaseID, sessionToken);
		JSONObject json = new JSONObject(purcahseItem);
		json.put("description", "new descr");
		return purchasesApi.updatePurchaseItem(seasonID, purchaseID, json.toString(), sessionToken);
	}

	public String updatePurcahseStage(String seasonID, String purchaseID, String sessionToken, String stage) throws JSONException, IOException{
		String purcahseItem = purchasesApi.getPurchaseItem(purchaseID, sessionToken);
		JSONObject json = new JSONObject(purcahseItem);
		json.put("stage", stage);
		return purchasesApi.updatePurchaseItem(seasonID, purchaseID, json.toString(), sessionToken);
	}

	public String getAllPurchases(String seasonID, String sessionToken){
		return purchasesApi.getAllPurchaseItems(seasonID, sessionToken);
	}

	public int deletePurchase(String purchaseID, String sessionToken){
		return purchasesApi.deletePurchaseItem(purchaseID, sessionToken);
	}

	public String getPurcahse(String purchaseID, String sessionToken){
		return purchasesApi.getPurchaseItem(purchaseID, sessionToken);
	}
	
	public String addEntity(String productID, String filePath, String sessionToken) throws Exception{
		String entity = FileUtils.fileToString(config + filePath, "UTF-8", false);
		return entitiesApi.createEntity(productID, entity, sessionToken);
	}
	
	public String addAttributeType(String entityID, String filePath, String sessionToken) throws Exception{
		String at = FileUtils.fileToString(config + filePath, "UTF-8", false);
		return entitiesApi.createAttributeType(entityID, at, sessionToken);
	}
	
	public String addAttribute(String entityID, String filePath, String attributeTypeID, String sessionToken) throws Exception{
		String at = FileUtils.fileToString(config + filePath, "UTF-8", false);
		JSONObject attObj = new JSONObject(at);
		attObj.put("attributeTypeId", attributeTypeID);
		return entitiesApi.createAttribute(entityID, attObj.toString(), sessionToken);
	}
	
	public String getAttribute(String attributeID, String sessionToken) throws Exception{
		return entitiesApi.getAttribute(attributeID, sessionToken);
	}
	
	public String getAttributeType(String attributeTypeID, String sessionToken) throws Exception{
		return entitiesApi.getAttributeType(attributeTypeID, sessionToken);
	}
	
	public String getEntity(String entityID, String sessionToken) throws Exception{
		return entitiesApi.getEntity(entityID, sessionToken);
	}
	
	public String getProductEntities(String productID, String sessionToken) throws Exception{
		return entitiesApi.getProductEntities(productID, sessionToken);
	}
	
	public String getAttributes(String entityID, String sessionToken) throws Exception{
		return entitiesApi.getAttributes(entityID, sessionToken);
	}
	
	public String getAttributeTypes(String entityID, String sessionToken) throws Exception{
		return entitiesApi.getAttributeTypes(entityID, sessionToken);
	}
	
	public String getDbSchemas(String sessionToken) throws Exception{
		return entitiesApi.getDbSchemas(sessionToken);
	}
	
	public String getDbTablesInSchema(String dbSchema, String sessionToken) throws Exception{
		return entitiesApi.getDbTablesInSchema(dbSchema, sessionToken);
	}
	
	public String updateEntity(String entityID, String entity, String sessionToken) throws Exception{
		return entitiesApi.updateEntity(entityID, entity, sessionToken);
	}
	
	public String updateAttribute(String attributeID, String attribute, String sessionToken) throws Exception{
		return entitiesApi.updateAttribute(attributeID, attribute, sessionToken);
	}
	
	public String updateAttributeType(String attributeTypeID, String attributeType, String sessionToken) throws Exception{
		return entitiesApi.updateAttributeType(attributeTypeID, attributeType, sessionToken);
	}
	
	public int deleteAttribute(String attributeID, String sessionToken) throws Exception{
		return entitiesApi.deleteAttribute(attributeID, sessionToken);
	}
	
	public int deleteAttributeType(String attributeTypeID, String sessionToken) throws Exception{
		return entitiesApi.deleteAttributeType(attributeTypeID, sessionToken);
	}
	
	public int deleteEntity(String entityID, String sessionToken) throws Exception{
		return entitiesApi.deleteEntity(entityID, sessionToken);
	}
}
