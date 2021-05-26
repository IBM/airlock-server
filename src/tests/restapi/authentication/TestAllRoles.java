package tests.restapi.authentication;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;


import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.EntitiesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TestServicesRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;

public class TestAllRoles {
	protected String seasonID;
	protected String seasonID4Test;
	protected String seasonToCopyTo;
	protected String featureID;
	protected String featureID4Test;
	protected String featureID4TestProd;
	protected String inAppPurchaseID;
	protected String inAppPurchaseID4Test;
	protected String inAppPurchaseID4TestProd;
	protected String purchaseOptionsID;
	protected String purchaseOptionsID4Test;
	protected String purchaseOptionsID4TestProd;
	protected String userID;
	protected String orderingRuleID;
	protected String orderingRuleID4Test;
	protected String webhookID;
	protected String webhookID4Test;
	protected String productID;
	protected String productID4Test;
	protected String utilityID;
	protected String utilityID4Test;
	protected String stringID;
	protected String stringID4Test;
	protected String stringID4TestProd;
	protected String experimentID;
	protected String experimentID2;
	protected String experimentIDProd;
	protected String streamID;
	protected String streamIDProd;
	protected String streamID4TestProd;
	protected String notificationID;
	protected String entityID;
	protected String attributeID;
	protected String attributeTypeID;
	protected String notificationID4TestProd;
	protected String branchID;
	protected String branchIDDevInProd;
	protected String branchIDProd;
	protected String variantID;
	protected String variantDevInProd;
	protected String variantProdInProd;
	protected String emptyDataCollection;
	protected String config;
	protected String serverVersion = "2.6";
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
	protected AirlocklNotificationRestApi notificationApi;
	protected InAppPurchasesRestApi purchasesApi;
	private EntitiesRestApi entitiesApi;

	protected AirlockUtils baseUtils;
	protected String sessionToken;
	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	protected String testServicesUrl;
	protected String adminUser;
	protected String productLeadUser;
	protected String editorUser;
	protected String translatorUser;
	protected String viewerUser;
	protected String adminPassword;
	protected String productLeadPassword;
	protected String editorPassword;
	protected String translatorPassword;
	protected String viewerPassword;
	protected String m_appName = "backend_dev";
	protected String m_ssoConfigPath;
	protected String m_stage;
	protected List<String> rolesToRun = new ArrayList<String>();
	private SoftAssert softAssert = new SoftAssert();

	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","testServicesUrl","configPath", "operationsUrl","admin","productLead","editor","translator","viewer","adminPass","productLeadPass","editorPass","translatorPass","viewerPass","appName","ssoConfigPath","stage", "expectedServerVersion", "productsToDeleteFile", "runRoles"})
	public void init(String url,String t_url,String a_url,String ts_url, String configPath, String c_operationsUrl,String admin,String productLead, String editor,String translator,String viewer,String adminPass,String productleadPass,String editorPass,String translatorPass,String viewerPass,@Optional String appName,@Optional String ssoConfigPath,@Optional String stage, @Optional String expectedServerVersion, String productsToDeleteFile, String runRoles) throws IOException{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		testServicesUrl =ts_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		productLeadUser = productLead;
		productLeadPassword = productleadPass;
		editorUser = editor;
		editorPassword = editorPass;
		translatorUser = translator;
		translatorPassword = translatorPass;
		viewerUser = viewer;
		viewerPassword = viewerPass;
		m_ssoConfigPath = ssoConfigPath;
		m_stage = stage;
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
		purchasesApi = new InAppPurchasesRestApi();

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
		testServicesApi.setURL(testServicesUrl);
		operationApi.setURL(operationsUrl);
		streamsApi.setURL(m_url);
		notificationApi.setUrl(m_url);
		purchasesApi.setURL(m_url);
		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		if(m_ssoConfigPath != null && m_stage != null){
			adminToken = baseUtils.setNewJWTTokenUsingBluemix(adminUser, adminPassword, m_ssoConfigPath,m_stage);
		} else {
			adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		}

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}


		if (expectedServerVersion != null)
			serverVersion = expectedServerVersion;

		rolesToRun = Arrays.asList(runRoles.split(","));


	}

	@BeforeMethod
	public void prepareTests(){
		sessionToken = adminToken;
		try {
			int respCode = operationApi.healthcheck(adminToken);
			Assert.assertEquals(respCode, 200, "healthcheck failed with code: " + respCode);
		} catch (Exception e1) {
			Assert.fail("get healthcheck failed:\n" +e1.getLocalizedMessage());
		}

		createProduct(false);
		productID = productID4Test;
		try {
			String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
			seasonID = seasonApi.addSeason(productID, season, sessionToken);
			emptyDataCollection = analyticsApi.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER,"BASIC", sessionToken);
			String schema = schemaApi.getInputSchema(seasonID, sessionToken);
			String file = FileUtils.fileToString(config + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
			JSONObject is = new JSONObject(file);
			JSONObject jsonSchema = new JSONObject(schema);
			jsonSchema.put("inputSchema", is);
			schemaApi.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			String str = FileUtils.fileToString(config + "/strings/string2.txt", "UTF-8", false);
			stringID = stringApi.addString(seasonID, str, sessionToken);
			String utility = FileUtils.fileToString(config + "/utilities/utility3.txt", "UTF-8", false);
			Properties utilProps1 = new Properties();
			utilProps1.load(new StringReader(utility));
			utilityID = utilitiesApi.addUtility(seasonID, utilProps1, sessionToken);

			String feature = FileUtils.fileToString(config + "feature3.txt", "UTF-8", false);
			featureID = featureApi.addFeature(seasonID, feature, "ROOT", sessionToken);

			String inAppPurchase = FileUtils.fileToString(config + "purchases/inAppPurchase3.txt", "UTF-8", false);
			inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchase, "ROOT", sessionToken);

			String purchaseOptions = FileUtils.fileToString(config + "purchases/purchaseOptions3.txt", "UTF-8", false);
			purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, inAppPurchaseID, sessionToken);

			String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonConfig = new JSONObject(configuration);
			JSONObject newConfiguration = new JSONObject();
			newConfiguration.put("color", "red");
			jsonConfig.put("configuration", newConfiguration);
			featureApi.addFeature(seasonID, jsonConfig.toString(), featureID, adminToken);

			String orderingRule = FileUtils.fileToString(config + "orderingRule/orderingRule1.txt", "UTF-8", false);
			JSONObject jsonOR = new JSONObject(orderingRule);
			jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
			orderingRuleID = featureApi.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);

			String webhook = FileUtils.fileToString(config + "webhooks/webhook1.txt", "UTF-8", false);
			JSONObject jsonHook = new JSONObject(webhook);
			jsonHook.put("name", RandomStringUtils.randomAlphabetic(5));
			webhookID = operationApi.addWebhook(jsonHook.toString(), sessionToken);

		}catch (Exception e){}
	}

	@AfterMethod
	public void deleteData(){
		try {
			productApi.deleteProduct(productID, adminToken);
		}catch (Exception e){
			e.printStackTrace();
		}
	}


	@Test
	public void testGeneralRoles(){
		if (rolesToRun.contains("all")) {
			testProductLead(false);
			testEditor(false);
			testTranslator(false);
			testViewer(false);
			testAdmin(false);		
			return;
		}

		if (rolesToRun.contains("admin"))
			testAdmin(false);

		if (rolesToRun.contains("productLead"))
			testProductLead(false);

		if (rolesToRun.contains("editor"))
			testEditor(false);

		if (rolesToRun.contains("translator"))
			testTranslator(false);

		if (rolesToRun.contains("viewer"))
			testViewer(false);
	}

	private void updateProductsRoles() throws Exception {
		String response = operationApi.resetUsersFromListForProduct(productID, config + "authentication/product_users.txt", adminToken);
		Assert.assertFalse(response.contains("error"), "Fail to update product's users");
		String prevAdminUser = adminUser;
		String prevAdminPassword = adminPassword;
		String prevProductLeadUser = productLeadUser;
		String prevProductLeadPassword = productLeadPassword;
		String prevEditorUser = editorUser;
		String prevEditorPassword = editorPassword;
		String prevTranslatorUser = translatorUser;
		String prevTranslatorPassword = translatorPassword;
		//String prevViewerUser = viewerUser; 
		//String prevViewerPassword = viewerPassword;


		adminUser = prevEditorUser;
		adminPassword = prevEditorPassword;
		productLeadUser = prevTranslatorUser;
		productLeadPassword = prevTranslatorPassword;
		translatorUser = prevAdminUser;
		translatorPassword = prevAdminPassword;
		editorUser = prevProductLeadUser;
		editorPassword = prevProductLeadPassword;

		if(m_ssoConfigPath != null && m_stage != null){
			adminToken = baseUtils.setNewJWTTokenUsingBluemix(adminUser, adminPassword, m_ssoConfigPath,m_stage);
		} else {
			adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		}

	}

	@Test(dependsOnMethods="testGeneralRoles")
	public void testProductRoles() throws Exception {
		updateProductsRoles();
		if (rolesToRun.contains("all")) {

			testProductLead(true);
			testEditor(true);
			testTranslator(true);
			testViewer(true);
			testAdmin(true);
			return;
		}


		if (rolesToRun.contains("productLead"))
			testProductLead(true);

		if (rolesToRun.contains("editor"))
			testEditor(true);

		if (rolesToRun.contains("translator"))
			testTranslator(true);

		if (rolesToRun.contains("viewer"))
			testViewer(true);

		if (rolesToRun.contains("admin"))
			testAdmin(true);

	}




	public void testAdmin(boolean fromProduct){
		System.out.println("Starting role: admin");
		//sessionToken = adminToken;
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(adminUser,adminPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(adminUser,adminPassword,m_appName);
		}

		//products
		//createProduct(fromProduct?true:false); // admin of product cannot create product. Only general admin can
		createProduct(false); // editor, prodLead and admin can create product
		getProductList(false);
		getProduct(false);
		updateProduct(false);
		if (!fromProduct)
			createProductWithSpecifiedId(fromProduct?true:false);

		//seasons
		createSeason(false);
		getServerVersion(false);
		getAllSeasons(false);
		updateSeason(false);
		upgradeSeason(false);
		deleteSeason(false);
		createSeasonWithSpecifiedId(false);
		getSeasonConstants(false);
		getSeasonDefaults(false);
		getDocumentlinks(false);
		//capabilities
		getCapabilities(false);
		getSeasonCapabilities(false);
		setCapabilities(false);

		//encryption key
		getEncryptionKey(false);
		resetEncryptionKey(false);

		//input schema
		getInputSchema(false);
		getInputSample(false);
		updateInputSchema(false);
		validateInputSchema(false);
		//utilities
		addUtility(false);
		getAllUtilities(false);
		getUtility(false);
		getUtilitiesInfo(false);
		updateUtility(false);
		deleteUtility(false);
		simulateUtility(false);
		//notifications
		addNotification(false);
		updateNotification(false);
		addNotificationInProd(false, false);
		updateNotificationInProd(false);
		changeNotificationsOrder(false);
		addNotificationsLimitations(false);
		updateNotificationsLimitations(false);
		deleteNotification(false);
		//experiment
		createExperiment(false);
		updateExperiment(false);
		getExperiment(false);
		getAllExperiments(false);
		getExperimentInputSample(false);
		getExperimentUtilitiesInfo(false);
		getExperimentGlobalDataCollection(false);
		getExperimentAnalyticsQuota(false);
		createExperimentInProd(false);
		updateExperimentInProduction(false);
		reorderDevExp(false);
		updateExperimentToProd(false);
		resetDashboardExperimentInDev(false);
		resetDashboardExperimentInProd(false);
		reorderProdExp(false);
		createBranch(false);
		updateBranch(false);
		getBranch(false);
		getAllBranches(false);
		deleteBranch(false);
		createDevVariantInDevExp(false);
		createDevVariantInProdExp(false);
		createProdVariantInProdExp(false);
		updateDevVariantInDevExp(false);
		updateDevVariantInProdExp(false);
		updateProdVariantInProdExp(false);
		//feature
		createFeature(false);
		getAllFeatures(false);
		getFeaturesForSeason(false);
		getAllFeaturesForSeason(false);
		getFeature(false);
		updateFeature(false);
		createFeatureInProduction(false,false);
		updateFeatureInProduction(false);
		createOrderingRule(false);
		updateOrderingRuleToProd(false);
		checkOutStandAlone(false);
		checkOutProdStandAlone(false);
		updateCheckoutStandAlone(false);
		updateCheckoutProdStandAlone(false);
		downgradeCheckoutProdStandAlone(false);
		cancelCheckOutStandAlone(false);
		cancelCheckOutProdStandAlone(false);
		checkOutDevInProd(false);
		checkOutProdDevInProd(false);
		updateCheckoutDevInProd(false);
		updateCheckoutProdDevInProd(false);
		downgradeCheckoutProdDevInProd(false);
		cancelCheckOutDevInProd(false);
		cancelCheckOutProdDevInProd(false);
		checkOutProdInProd(false);
		checkOutProdProdInProd(false);
		updateCheckoutProdInProd(false);
		updateCheckoutProdProdInProd(false);
		downgradeCheckoutProdProdInProd(false);
		cancelCheckOutProdInProd(false);
		cancelCheckOutProdProdInProd(false);
		updateGlobalDataCollection(false);
		updateGlobalDataCollectionFeatureProd(false);
		updateGlobalDataCollectionAttributeProd(false);
		updateGlobalDataCollectionInputFieldProd(false);
		updateGlobalDataCollectionMix(false);
		addFeatureInProdToAnalytics(false);
		updateFeatureInProdAttributesForAnalytics(false);
		updateInputFieldsInProdForAnalytics(false);
		updateInputFieldsForAnalyticsDevAndProd(false);
		deleteFeature(false);
		deleteFeatureInProduction(true);
		deleteDevVariantInDevExp(false);
		deleteDevVariantInProdExp(false);
		deleteProdVariantInProdExp(true);
		deleteExperiment(false);
		deleteExperimentInProduction(true);
		updateFeatureToProd(false);
		getGlobalDataCollection(false);
		addFeatureToAnalytics(false);
		updateFeatureAttributesForAnalytics(false);
		getFeatureAttributes(false);
		updateInputFieldsForAnalytics(false);
		removeFeatureFromAnalytics(false);
		getAnalyticsQuota(false);
		setAnalyticsQuota(false);
		getProductFollowers(false);
		followProduct(false);
		unfollowProduct(false);
		getFeatureFollowers(false);
		followFeature(false);
		unfollowFeature(false);
		//strings
		addString(false);
		addStringInProd(false);
		copyStrings(false);
		copyStringsOverrideProd(false);
		importStrings(false);
		getAllStrings(false);
		getString(false);
		getStringForTranslation(false);
		addTranslation(false);
		getTranslation(false);
		updateTranslation(false);
		getTranslationSummary(false);
		addSupportedLocales(false);
		getStringStatuses(false);
		markForTranslation(false);
		markForTranslationInProd(false);
		reviewTranslation(false);
		getNewStringsForTranslation(false);
		sendToTranslation(false);
		overrideTranslate(false);
		cancelOverrideTranslate(false);
		overrideTranslateInProd(false);
		cancelOverrideTranslateInProd(false);
		getStringsUsedInFeature(false);
		getSupportedLocales(false);
		updateString(false);
		updateStringInProd(false);
		updateStringValueInProd(false);
		deleteString(false);
		removeSupportedLocales(false);
		//general administration
		getRoles(false);
		setRoles(false);
		getAirlockUsers(false);
		addAirlockUser(false);
		getAirlockUser(false);
		getUserRoleSets(false);
		updateAirlockUser(false);
		deleteAirlockUser(false);
		setAirlockUserPerProduct(false);
		getAirlockUsersPerProduct(false);
		getUserRolesPerProduct(false);
		getAirlockServers(false);
		setAirlockServers(false);
		getUserGroups(false);
		getUserGroupsUsage(false);
		setUserGroups(false);
		copyFeature(false);
		importFeatures(false);
		setTestMails(false);
		getTestMails(false);
		deleteTestMails(false);
		setTestTranslation(false);
		getTestTranslation(false);
		wakeUpTranslation(false);
		importSeason(false);
		//streams
		createStream(false);
		createStreamInProduction(true, false);
		updateStream(false);
		updateStreamInProduction(false);
		renameStreamInProduction(false);
		getStream(false);
		getAllStreams(false);
		deleteStream(false);
		deleteStreamInProduction(false);
		getStreamEvents(false);
		updateStreamEvents(false);
		filterStreamEvents(false);
		//webhooks
		createWebhook(false);
		getWebhooks(false);
		updateWebhook(false);
		deleteWebhook(false);

		//purchases
		createInAppPurchase(false);
		getAllPurchaseItemsForSeason(false);
		getInAppPurchase(false);
		updateInAppPurchase(false);
		createInAppPurchaseInProduction(false,false);
		updateInAppPurchaseInProduction(false);
		
		createPurchaseOptions(false);
		updatePurchaseOptions(false);
		createPurchaseOptionsInProduction(false,false);
		updatePurchaseOptionsInProduction(false);
		
		deletePurchaseOptions(false);
		deletePurchaseOptionsInProduction(true);
		deleteInAppPurchase(false);
		deleteInAppPurchaseInProduction(true);
		
		//updateEntities(false);
		addEntity(false);
		addAttribute(false);
		addAttributeType(false);
		getEntity(false);
		getProductEntities(false);
		getAttribute(false);
		getAttributes(false);
		getAttributeType(false);
		getAttributeTypes(false);
		updateEntity(false);
		updateAttribute(false);
		updateAttributeType(false);
		deleteEntity(false);
		deleteAttribute(false);
		deleteAttributeType(false);
		
		deleteProduct(false); //Should be last - from product cannot create but can delete - this causes asymmetric state in the test.  
	}

	public void testProductLead(boolean fromProduct){
		System.out.println("Starting role: product lead");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(productLeadUser,productLeadPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(productLeadUser,productLeadPassword,m_appName);
		}

		//encryption key
		getEncryptionKey(true);
		resetEncryptionKey(true);

		createProduct(fromProduct?true:false); //global prodLead can create product
		getProductList(false);
		getProduct(false);
		updateProduct(false);
		deleteProduct(true);
		if (!fromProduct)
			createProductWithSpecifiedId(true);
		createSeason(false);
		getServerVersion(false);
		getAllSeasons(false);
		updateSeason(false);
		upgradeSeason(true);
		deleteSeason(false);
		createSeasonWithSpecifiedId(true);
		getSeasonConstants(false);
		getSeasonDefaults(false);
		getDocumentlinks(false);
		//capabilities
		getCapabilities(false);
		getSeasonCapabilities(false);
		setCapabilities(true);

		getInputSchema(false);
		getInputSample(false);
		updateInputSchema(false);
		validateInputSchema(false);
		addUtility(false);
		getAllUtilities(false);
		getUtility(false);
		getUtilitiesInfo(false);
		updateUtility(false);
		deleteUtility(false);
		simulateUtility(false);
		addNotification(false);
		getNotification(false);
		updateNotification(false);
		addNotificationInProd(false, false);
		updateNotificationInProd(false);
		changeNotificationsOrder(false);
		addNotificationsLimitations(false);
		updateNotificationsLimitations(false);
		deleteNotification(false);
		createExperiment(false);
		updateExperiment(false);
		getExperiment(false);
		getAllExperiments(false);
		getExperimentInputSample(false);
		getExperimentUtilitiesInfo(false);
		getExperimentGlobalDataCollection(false);
		getExperimentAnalyticsQuota(false);
		createExperimentInProd(false);
		updateExperimentInProduction(false);
		resetDashboardExperimentInDev(false);
		resetDashboardExperimentInProd(false);
		reorderDevExp(false);
		updateExperimentToProd(false);
		reorderProdExp(false);
		createBranch(false);
		updateBranch(false);
		getBranch(false);
		getAllBranches(false);
		deleteBranch(false);
		createDevVariantInDevExp(false);
		createDevVariantInProdExp(false);
		createProdVariantInProdExp(false);
		updateDevVariantInDevExp(false);
		updateDevVariantInProdExp(false);
		updateProdVariantInProdExp(false);
		createFeature(false);
		getAllFeatures(false);
		getFeaturesForSeason(false);
		getAllFeaturesForSeason(false);
		getFeature(false);
		updateFeature(false);
		createFeatureInProduction(false,false);
		updateFeatureInProduction(false);
		createOrderingRule(false);
		updateOrderingRuleToProd(false);
		checkOutStandAlone(false);
		checkOutProdStandAlone(false);
		updateCheckoutStandAlone(false);
		updateCheckoutProdStandAlone(false);
		downgradeCheckoutProdStandAlone(false);
		cancelCheckOutStandAlone(false);
		cancelCheckOutProdStandAlone(false);
		checkOutDevInProd(false);
		checkOutProdDevInProd(false);
		updateCheckoutDevInProd(false);
		updateCheckoutProdDevInProd(false);
		downgradeCheckoutProdDevInProd(false);
		cancelCheckOutDevInProd(false);
		cancelCheckOutProdDevInProd(false);
		checkOutProdInProd(false);
		checkOutProdProdInProd(false);
		updateCheckoutProdInProd(false);
		updateCheckoutProdProdInProd(false);
		downgradeCheckoutProdProdInProd(false);
		cancelCheckOutProdInProd(false);
		cancelCheckOutProdProdInProd(false);
		updateGlobalDataCollection(false);
		updateGlobalDataCollectionFeatureProd(false);
		updateGlobalDataCollectionAttributeProd(false);
		updateGlobalDataCollectionInputFieldProd(false);
		updateGlobalDataCollectionMix(false);
		addFeatureInProdToAnalytics(false);
		updateFeatureInProdAttributesForAnalytics(false);
		updateInputFieldsInProdForAnalytics(false);
		updateInputFieldsForAnalyticsDevAndProd(false);
		deleteFeature(false);
		deleteFeatureInProduction(true);
		deleteDevVariantInDevExp(false);
		deleteDevVariantInProdExp(false);
		deleteProdVariantInProdExp(true);
		deleteExperiment(false);
		deleteExperimentInProduction(true);
		updateFeatureToProd(false);
		getGlobalDataCollection(false);
		addFeatureToAnalytics(false);
		updateFeatureAttributesForAnalytics(false);
		getFeatureAttributes(false);
		updateInputFieldsForAnalytics(false);
		removeFeatureFromAnalytics(false);
		getAnalyticsQuota(false);
		setAnalyticsQuota(true);
		getProductFollowers(false);
		followProduct(false);
		unfollowProduct(false);
		getFeatureFollowers(false);
		followFeature(false);
		unfollowFeature(false);
		addString(false);
		addStringInProd(false);
		copyStrings(false);
		copyStringsOverrideProd(false);
		importStrings(false);
		getAllStrings(false);
		getString(false);
		getStringForTranslation(false);
		addTranslation(false);
		getTranslation(false);
		updateTranslation(false);
		getTranslationSummary(false);
		addSupportedLocales(true);
		getStringStatuses(false);
		markForTranslation(false);
		markForTranslationInProd(false);
		reviewTranslation(false);
		getNewStringsForTranslation(false);
		sendToTranslation(false);
		overrideTranslate(false);
		cancelOverrideTranslate(false);
		overrideTranslateInProd(false);
		cancelOverrideTranslateInProd(false);
		getStringsUsedInFeature(false);
		getSupportedLocales(false);
		updateString(false);
		updateStringInProd(false);
		updateStringValueInProd(false);
		deleteString(false);
		removeSupportedLocales(true);
		getRoles(false);
		setRoles(true);
		getAirlockUsers(false);
		addAirlockUser(true);
		getAirlockUser(false);
		getUserRoleSets(false);
		updateAirlockUser(true);
		deleteAirlockUser(true);
		setAirlockUserPerProduct(true);
		getAirlockUsersPerProduct(false);
		getUserRolesPerProduct(false);
		getAirlockServers(false);
		setAirlockServers(true);
		getUserGroups(false);
		getUserGroupsUsage(false);
		setUserGroups(false);
		copyFeature(false);
		importFeatures(false);
		setTestMails(true);
		getTestMails(true);
		deleteTestMails(true);
		setTestTranslation(true);
		getTestTranslation(true);
		wakeUpTranslation(true);
		importSeason(true);
		createStream(false);
		createStreamInProduction(false, false);
		updateStream(false);
		updateStreamInProduction(false);
		renameStreamInProduction(true);
		getStream(false);
		getAllStreams(false);
		deleteStream(false);
		deleteStreamInProduction(false);
		getStreamEvents(false);
		updateStreamEvents(true);
		filterStreamEvents(false);
		//webhooks
		createWebhook(true);
		getWebhooks(true);
		updateWebhook(true);
		deleteWebhook(true);
		//purchases
		createInAppPurchase(false);
		getAllFeatures(false);
		getAllPurchaseItemsForSeason(false);
		getInAppPurchase(false);
		updateInAppPurchase(false);
		createInAppPurchaseInProduction(false,false);
		updateInAppPurchaseInProduction(false);
		createPurchaseOptions(false);
		updatePurchaseOptions(false);
		createPurchaseOptionsInProduction(false,false);
		updatePurchaseOptionsInProduction(false);
		
		deletePurchaseOptions(false);
		deleteInAppPurchase(false);
		deleteInAppPurchaseInProduction(true);
		
		addEntity(true);
		addAttribute(true);
		addAttributeType(true);
		getEntity(true);
		getProductEntities(true);
		getAttribute(true);
		getAttributeType(true);
		getAttributes(true);
		getAttributeTypes(true);
		updateEntity(true);
		updateAttribute(true);
		updateAttributeType(true);
		//updateEntities(true);
		deleteEntity(true);
		deleteAttribute(true);
		deleteAttributeType(true);
	}

	public void testEditor(boolean fromProduct){
		System.out.println("Starting role: editor");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(editorUser,editorPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(editorUser,editorPassword,m_appName);
		}
		createProduct(false); //editor, prodLead and admin can create product 
		getProductList(false);

		//encryption key
		getEncryptionKey(true);
		resetEncryptionKey(true);

		getProduct(false);
		updateProduct(true);
		deleteProduct(true);
		if (!fromProduct)
			createProductWithSpecifiedId(true);
		createSeason(true);
		getServerVersion(false);
		getAllSeasons(false);
		updateSeason(true);
		upgradeSeason(true);
		deleteSeason(true);
		createSeasonWithSpecifiedId(true);
		getSeasonConstants(false);
		getSeasonDefaults(false);
		getDocumentlinks(false);
		//capabilities
		getCapabilities(false);
		getSeasonCapabilities(false);
		setCapabilities(true);

		getInputSchema(false);
		getInputSample(false);
		updateInputSchema(true);
		validateInputSchema(false);
		addUtility(false);
		getAllUtilities(false);
		getUtility(false);
		getUtilitiesInfo(false);
		updateUtility(false);
		deleteUtility(false);
		simulateUtility(false);
		addNotification(false);
		getNotification(false);
		updateNotification(false);
		addNotificationInProd(false, true);
		updateNotificationInProd(true);
		changeNotificationsOrder(true);
		addNotificationsLimitations(true);
		updateNotificationsLimitations(true);
		deleteNotification(false);
		createExperiment(false);
		updateExperiment(false);
		getExperiment(false);
		getAllExperiments(false);
		getExperimentInputSample(false);
		getExperimentUtilitiesInfo(false);
		getExperimentGlobalDataCollection(false);
		getExperimentAnalyticsQuota(false);
		createExperimentInProd(true);
		updateExperimentInProduction(true);
		reorderDevExp(false);
		updateExperimentToProd(true);
		resetDashboardExperimentInDev(false);
		resetDashboardExperimentInProd(true);
		reorderProdExp(true);
		createBranch(false);
		updateBranch(false);
		getBranch(false);
		getAllBranches(false);
		deleteBranch(false);
		createDevVariantInDevExp(false);
		createDevVariantInProdExp(false);
		createProdVariantInProdExp(true);
		updateDevVariantInDevExp(false);
		updateDevVariantInProdExp(false);
		updateProdVariantInProdExp(true);
		createFeature(false);
		getAllFeatures(false);
		getFeaturesForSeason(false);
		getAllFeaturesForSeason(false);
		getFeature(false);
		updateFeature(false);
		createFeatureInProduction(false,true);
		updateFeatureInProduction(true);
		createOrderingRule(false);
		updateOrderingRuleToProd(true);
		checkOutStandAlone(false);
		checkOutProdStandAlone(false);
		updateCheckoutStandAlone(false);
		updateCheckoutProdStandAlone(true);
		downgradeCheckoutProdStandAlone(false);
		cancelCheckOutStandAlone(false);
		cancelCheckOutProdStandAlone(false);
		checkOutDevInProd(false);
		checkOutProdDevInProd(false);
		updateCheckoutDevInProd(false);
		updateCheckoutProdDevInProd(true);
		downgradeCheckoutProdDevInProd(false);
		cancelCheckOutDevInProd(false);
		cancelCheckOutProdDevInProd(false);
		checkOutProdInProd(false);
		checkOutProdProdInProd(true);//BUG!!!
		updateCheckoutProdInProd(false);
		updateCheckoutProdProdInProd(true);
		downgradeCheckoutProdProdInProd(true);
		cancelCheckOutProdInProd(false);
		cancelCheckOutProdProdInProd(true);//BUG!!!
		updateGlobalDataCollection(false);
		updateGlobalDataCollectionFeatureProd(true);
		updateGlobalDataCollectionAttributeProd(true);
		updateGlobalDataCollectionInputFieldProd(true);
		updateGlobalDataCollectionMix(false);
		addFeatureInProdToAnalytics(true);
		updateFeatureInProdAttributesForAnalytics(true);
		updateInputFieldsInProdForAnalytics(true);
		updateInputFieldsForAnalyticsDevAndProd(false);
		deleteFeature(false);
		deleteFeatureInProduction(true);
		deleteDevVariantInDevExp(false);
		deleteDevVariantInProdExp(false);
		deleteProdVariantInProdExp(true);
		deleteExperiment(false);
		deleteExperimentInProduction(true);
		updateFeatureToProd(true);
		getGlobalDataCollection(false);
		addFeatureToAnalytics(false);
		updateFeatureAttributesForAnalytics(false);
		getFeatureAttributes(false);
		updateInputFieldsForAnalytics(false);
		removeFeatureFromAnalytics(false);
		getAnalyticsQuota(false);
		setAnalyticsQuota(true);
		getProductFollowers(false);
		followProduct(false);
		unfollowProduct(false);
		getFeatureFollowers(false);
		followFeature(false);
		unfollowFeature(false);
		addString(false);
		addStringInProd(true);
		copyStrings(false);
		copyStringsOverrideProd(true);
		importStrings(false);
		getAllStrings(false);
		getString(false);
		getStringForTranslation(false);
		addTranslation(true);
		getTranslation(false);
		updateTranslation(true);
		getTranslationSummary(false);
		addSupportedLocales(true);
		getStringStatuses(false);
		markForTranslation(false);
		markForTranslationInProd(true);
		reviewTranslation(true);
		getNewStringsForTranslation(false);
		sendToTranslation(true);
		overrideTranslate(false);
		cancelOverrideTranslate(false);
		overrideTranslateInProd(true);
		cancelOverrideTranslateInProd(true);
		getStringsUsedInFeature(false);
		getSupportedLocales(false);
		updateString(false);
		updateStringInProd(true);
		updateStringValueInProd(true);
		deleteString(false);
		removeSupportedLocales(true);
		getRoles(false);
		setRoles(true);
		getAirlockUsers(false);
		addAirlockUser(true);
		getAirlockUser(false);
		getUserRoleSets(false);
		updateAirlockUser(true);
		deleteAirlockUser(true);
		getAirlockServers(false);
		setAirlockUserPerProduct(true);
		getAirlockUsersPerProduct(false);
		getUserRolesPerProduct(false);
		setAirlockServers(true);
		getUserGroups(false);
		getUserGroupsUsage(false);
		setUserGroups(false);
		copyFeature(false);
		importFeatures(false);
		setTestMails(true);
		getTestMails(true);
		deleteTestMails(true);
		setTestTranslation(true);
		getTestTranslation(true);
		wakeUpTranslation(true);
		importSeason(true);
		createStream(false);
		createStreamInProduction(false, true);
		updateStream(false);
		updateStreamInProduction(true);
		renameStreamInProduction(true);
		getStream(false);
		getAllStreams(false);
		deleteStream(false);
		deleteStreamInProduction(true);
		getStreamEvents(false);
		updateStreamEvents(true);
		filterStreamEvents(false);
		//webhooks
		createWebhook(true);
		getWebhooks(true);
		updateWebhook(true);
		deleteWebhook(true);

		//purchases
		createInAppPurchase(false);
		getAllPurchaseItemsForSeason(false);
		getInAppPurchase(false);
		updateInAppPurchase(false);
		createInAppPurchaseInProduction(false,true);
		updateInAppPurchaseInProduction(true);
		
		createPurchaseOptions(false);
		updatePurchaseOptions(false);
		createPurchaseOptionsInProduction(false,true);
		updatePurchaseOptionsInProduction(true);
		
		deletePurchaseOptions(false);
		deleteInAppPurchase(false);
		deleteInAppPurchaseInProduction(true);
		
		addEntity(true);
		addAttribute(true);
		addAttributeType(true);
		getEntity(true);
		getProductEntities(true);
		getAttribute(true);
		getAttributes(true);
		getAttributeType(true);
		getAttributeTypes(true);
		updateEntity(true);
		updateAttribute(true);
		updateAttributeType(true);
		//updateEntities(true);
		deleteEntity(true);
		deleteAttribute(true);
		deleteAttributeType(true);
	}

	public void testTranslator(boolean fromProduct){
		System.out.println("Starting role: translator");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(translatorUser,translatorPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(translatorUser, translatorPassword, m_appName);
		}

		createProduct(fromProduct?false:true); // in product the translator is the general admin user hence the create product will succeed

		//encryption key
		getEncryptionKey(true);
		resetEncryptionKey(true);

		getProductList(false);
		getProduct(false);
		updateProduct(fromProduct?false:true); // in product the translator is the general admin user hence the create product will succeed
		deleteProduct(fromProduct?false:true); // in product the translator is the general admin user hence the create product will succeed
		if (!fromProduct)
			createProductWithSpecifiedId(fromProduct?false:true); // in product the translator is the general admin user hence the create product will succeed
		createSeason(true);
		getServerVersion(false);
		getAllSeasons(false);
		updateSeason(true);
		upgradeSeason(true);
		deleteSeason(true);
		if (!fromProduct)
			createSeasonWithSpecifiedId(true);
		getSeasonConstants(false);
		getSeasonDefaults(false);
		getDocumentlinks(false);
		//capabilities
		getCapabilities(false);
		getSeasonCapabilities(false);
		setCapabilities(true);

		getInputSchema(false);
		getInputSample(false);
		updateInputSchema(true);
		validateInputSchema(false);
		addUtility(true);
		getAllUtilities(false);
		getUtility(false);
		getUtilitiesInfo(false);
		updateUtility(true);
		deleteUtility(true);
		simulateUtility(false);
		addNotification(true);
		getNotification(false);
		updateNotification(true);
		addNotificationInProd(false, true);
		updateNotificationInProd(true);
		changeNotificationsOrder(true);
		addNotificationsLimitations(true);
		updateNotificationsLimitations(true);
		deleteNotification(true);
		createExperiment(true);
		updateExperiment(true);
		getExperiment(false);
		getAllExperiments(false);
		getExperimentInputSample(false);
		getExperimentUtilitiesInfo(false);
		getExperimentGlobalDataCollection(false);
		getExperimentAnalyticsQuota(false);
		createExperimentInProd(true);
		updateExperimentInProduction(true);
		resetDashboardExperimentInDev(true);
		resetDashboardExperimentInProd(true);
		reorderDevExp(true);
		updateExperimentToProd(true);
		reorderProdExp(true);
		createBranch(true);
		updateBranch(true);
		getBranch(false);
		getAllBranches(false);
		deleteBranch(true);
		createDevVariantInDevExp(true);
		createDevVariantInProdExp(true);
		createProdVariantInProdExp(true);
		updateDevVariantInDevExp(true);
		updateDevVariantInProdExp(true);
		updateProdVariantInProdExp(true);
		createFeature(true);
		getAllFeatures(false);
		getFeaturesForSeason(false);
		getAllFeaturesForSeason(false);
		getFeature(false);
		updateFeature(true);
		createFeatureInProduction(false,true);
		updateFeatureInProduction(true);
		createOrderingRule(true);
		updateOrderingRuleToProd(true);
		checkOutStandAlone(true);
		checkOutProdStandAlone(true);
		updateCheckoutStandAlone(true);
		updateCheckoutProdStandAlone(true);
		downgradeCheckoutProdStandAlone(true);
		cancelCheckOutStandAlone(true);
		cancelCheckOutProdStandAlone(true);
		checkOutDevInProd(true);
		checkOutProdDevInProd(true);
		updateCheckoutDevInProd(true);
		updateCheckoutProdDevInProd(true);
		downgradeCheckoutProdDevInProd(true);
		cancelCheckOutDevInProd(true);
		cancelCheckOutProdDevInProd(true);
		checkOutProdInProd(true);
		checkOutProdProdInProd(true);
		updateCheckoutProdInProd(true);
		updateCheckoutProdProdInProd(true);
		downgradeCheckoutProdProdInProd(true);
		cancelCheckOutProdInProd(true);
		cancelCheckOutProdProdInProd(true);
		updateGlobalDataCollection(true);
		updateGlobalDataCollectionFeatureProd(true);
		updateGlobalDataCollectionAttributeProd(true);
		updateGlobalDataCollectionInputFieldProd(true);
		updateGlobalDataCollectionMix(true);
		addFeatureInProdToAnalytics(true);
		updateFeatureInProdAttributesForAnalytics(true);
		updateInputFieldsInProdForAnalytics(true);
		updateInputFieldsForAnalyticsDevAndProd(true);
		deleteFeature(true);
		deleteFeatureInProduction(true);
		deleteDevVariantInDevExp(true);
		deleteDevVariantInProdExp(true);
		deleteProdVariantInProdExp(true);
		deleteExperiment(true);
		deleteExperimentInProduction(true);
		updateFeatureToProd(true);
		getGlobalDataCollection(false);
		addFeatureToAnalytics(true);
		updateFeatureAttributesForAnalytics(true);
		getFeatureAttributes(false);
		updateInputFieldsForAnalytics(true);
		removeFeatureFromAnalytics(true);
		getAnalyticsQuota(false);
		setAnalyticsQuota(true);
		getProductFollowers(false);
		followProduct(false);
		unfollowProduct(false);
		getFeatureFollowers(false);
		followFeature(false);
		unfollowFeature(false);
		addString(false);
		addStringInProd(false);
		copyStrings(false);
		copyStringsOverrideProd(false);
		importStrings(false);
		getAllStrings(false);
		getString(false);
		getStringForTranslation(false);
		addTranslation(false);//??????
		getTranslation(false);
		updateTranslation(false);//??????
		getTranslationSummary(false);
		addSupportedLocales(true);
		getStringStatuses(false);
		markForTranslation(false);
		markForTranslationInProd(false);
		reviewTranslation(false);
		getNewStringsForTranslation(false);
		sendToTranslation(false);//??????
		overrideTranslate(false);
		cancelOverrideTranslate(false);
		overrideTranslateInProd(false);
		cancelOverrideTranslateInProd(false);
		getStringsUsedInFeature(false);
		getSupportedLocales(false);
		updateString(false);
		updateStringInProd(false);
		updateStringValueInProd(false);
		deleteString(false);
		removeSupportedLocales(true);
		getRoles(false);
		setRoles(true);
		getAirlockUsers(false);
		addAirlockUser(true);
		getAirlockUser(false);
		getUserRoleSets(false);
		updateAirlockUser(true);
		deleteAirlockUser(true);
		setAirlockUserPerProduct(true);
		getAirlockUsersPerProduct(false);
		getUserRolesPerProduct(false);
		getAirlockServers(false);
		setAirlockServers(true);
		getUserGroups(false);
		getUserGroupsUsage(false);
		setUserGroups(true);
		copyFeature(true);
		importFeatures(true);
		setTestMails(true);
		getTestMails(true);
		deleteTestMails(true);
		setTestTranslation(true);
		getTestTranslation(true);
		wakeUpTranslation(true);
		importSeason(true);
		createStream(true);
		createStreamInProduction(false, true);
		updateStream(true);
		updateStreamInProduction(true);
		renameStreamInProduction(true);
		getStream(false);
		getAllStreams(false);
		deleteStream(true);
		deleteStreamInProduction(true);
		getStreamEvents(false);
		updateStreamEvents(true);
		filterStreamEvents(false);
		createWebhook(true);
		getWebhooks(true);
		updateWebhook(true);
		deleteWebhook(true);

		//purchases
		createInAppPurchase(true);
		getAllPurchaseItemsForSeason(false);
		getInAppPurchase(false);
		updateInAppPurchase(true);
		createInAppPurchaseInProduction(false,true);
		updateInAppPurchaseInProduction(true);
		
		createPurchaseOptions(true);
		updatePurchaseOptions(true);
		createPurchaseOptionsInProduction(false,true);
		updatePurchaseOptionsInProduction(true);
		
		deletePurchaseOptions(true);
		deleteInAppPurchase(true);
		deleteInAppPurchaseInProduction(true);

	}


	public void testViewer(boolean fromProduct){
		System.out.println("Starting role: viewer");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(viewerUser,viewerPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(viewerUser, viewerPassword, m_appName);
		}
		createProduct(true);

		//encryption key
		getEncryptionKey(true);
		resetEncryptionKey(true);

		getProductList(false);
		getProduct(false);
		updateProduct(true);
		deleteProduct(true);
		if (!fromProduct)
			createProductWithSpecifiedId(true);
		createSeason(true);
		getServerVersion(false);
		getAllSeasons(false);
		updateSeason(true);
		upgradeSeason(true);
		deleteSeason(true);
		createSeasonWithSpecifiedId(true);
		getSeasonConstants(false);
		getSeasonDefaults(false);
		getDocumentlinks(false);
		//capabilities
		getCapabilities(false);
		getSeasonCapabilities(false);
		setCapabilities(true);

		getInputSchema(false);
		getInputSample(false);
		updateInputSchema(true);
		validateInputSchema(false);
		addUtility(true);
		getAllUtilities(false);
		getUtility(false);
		getUtilitiesInfo(false);
		updateUtility(true);
		deleteUtility(true);
		simulateUtility(false);
		addNotification(true);
		getNotification(false);
		updateNotification(true);
		addNotificationInProd(false, true);
		updateNotificationInProd(true);
		changeNotificationsOrder(true);
		addNotificationsLimitations(true);
		updateNotificationsLimitations(true);
		deleteNotification(true);
		createExperiment(true);
		updateExperiment(true);
		getExperiment(false);
		getAllExperiments(false);
		getExperimentInputSample(false);
		getExperimentUtilitiesInfo(false);
		getExperimentGlobalDataCollection(false);
		getExperimentAnalyticsQuota(false);
		createExperimentInProd(true);
		updateExperimentInProduction(true);
		resetDashboardExperimentInDev(true);
		resetDashboardExperimentInProd(true);
		reorderDevExp(true);
		updateExperimentToProd(true);
		reorderProdExp(true);
		createBranch(true);
		updateBranch(true);
		getBranch(false);
		getAllBranches(false);
		deleteBranch(true);
		createDevVariantInDevExp(true);
		createDevVariantInProdExp(true);
		createProdVariantInProdExp(true);
		updateDevVariantInDevExp(true);
		updateDevVariantInProdExp(true);
		updateProdVariantInProdExp(true);
		createFeature(true);
		getAllFeatures(false);
		getFeaturesForSeason(false);
		getAllFeaturesForSeason(false);
		getFeature(false);
		updateFeature(true);
		createFeatureInProduction(false,true);
		updateFeatureInProduction(true);
		createOrderingRule(true);
		updateOrderingRuleToProd(true);
		checkOutStandAlone(true);
		checkOutProdStandAlone(true);
		updateCheckoutStandAlone(true);
		updateCheckoutProdStandAlone(true);
		downgradeCheckoutProdStandAlone(true);
		cancelCheckOutStandAlone(true);
		cancelCheckOutProdStandAlone(true);
		checkOutDevInProd(true);
		checkOutProdDevInProd(true);
		updateCheckoutDevInProd(true);
		updateCheckoutProdDevInProd(true);
		downgradeCheckoutProdDevInProd(true);
		cancelCheckOutDevInProd(true);
		cancelCheckOutProdDevInProd(true);
		checkOutProdInProd(true);
		checkOutProdProdInProd(true);
		updateCheckoutProdInProd(true);
		updateCheckoutProdProdInProd(true);
		downgradeCheckoutProdProdInProd(true);
		cancelCheckOutProdInProd(true);
		cancelCheckOutProdProdInProd(true);
		updateGlobalDataCollection(true);
		updateGlobalDataCollectionFeatureProd(true);
		updateGlobalDataCollectionAttributeProd(true);
		updateGlobalDataCollectionInputFieldProd(true);
		updateGlobalDataCollectionMix(true);
		addFeatureInProdToAnalytics(true);
		updateFeatureInProdAttributesForAnalytics(true);
		updateInputFieldsInProdForAnalytics(true);
		updateInputFieldsForAnalyticsDevAndProd(true);
		deleteFeature(true);
		deleteFeatureInProduction(true);
		deleteDevVariantInDevExp(true);
		deleteDevVariantInProdExp(true);
		deleteProdVariantInProdExp(true);
		deleteExperiment(true);
		deleteExperimentInProduction(true);
		updateFeatureToProd(true);
		getGlobalDataCollection(false);
		addFeatureToAnalytics(true);
		updateFeatureAttributesForAnalytics(true);
		getFeatureAttributes(false);
		updateInputFieldsForAnalytics(true);
		removeFeatureFromAnalytics(true);
		getAnalyticsQuota(false);
		setAnalyticsQuota(true);
		getProductFollowers(false);
		followProduct(false);
		unfollowProduct(false);
		getFeatureFollowers(false);
		followFeature(false);
		unfollowFeature(false);
		addString(true);
		addStringInProd(true);
		copyStrings(true);
		copyStringsOverrideProd(true);
		importStrings(true);
		getAllStrings(false);
		getString(false);
		getStringForTranslation(false);
		addTranslation(true);
		getTranslation(false);
		updateTranslation(true);
		getTranslationSummary(false);
		addSupportedLocales(true);
		getStringStatuses(false);
		markForTranslation(true);
		markForTranslationInProd(true);
		reviewTranslation(true);
		getNewStringsForTranslation(false);
		sendToTranslation(true);
		overrideTranslate(true);
		cancelOverrideTranslate(true);
		overrideTranslateInProd(true);
		cancelOverrideTranslateInProd(true);
		getStringsUsedInFeature(false);
		getSupportedLocales(false);
		updateString(true);
		updateStringInProd(true);
		updateStringValueInProd(true);
		deleteString(true);
		removeSupportedLocales(true);
		getRoles(false);
		setRoles(true);
		getAirlockUsers(false);
		addAirlockUser(true);
		getAirlockUser(false);
		getUserRoleSets(false);
		updateAirlockUser(true);
		deleteAirlockUser(true);
		setAirlockUserPerProduct(true);
		getAirlockUsersPerProduct(false);
		getUserRolesPerProduct(false);
		getAirlockServers(false);
		setAirlockServers(true);
		getUserGroups(false);
		getUserGroupsUsage(false);
		setUserGroups(true);
		copyFeature(true);
		importFeatures(true);
		setTestMails(true);
		getTestMails(true);
		deleteTestMails(true);
		setTestTranslation(true);
		getTestTranslation(true);
		wakeUpTranslation(true);
		importSeason(true);
		createStream(true);
		createStreamInProduction(false, true);
		updateStream(true);
		updateStreamInProduction(true);
		renameStreamInProduction(true);
		getStream(false);
		getAllStreams(false);
		deleteStream(true);
		deleteStreamInProduction(true);
		getStreamEvents(false);
		updateStreamEvents(true);
		filterStreamEvents(false);
		createWebhook(true);
		getWebhooks(true);
		updateWebhook(true);
		deleteWebhook(true);
		//purchases
		createInAppPurchase(true);
		getAllPurchaseItemsForSeason(false);
		getInAppPurchase(false);
		updateInAppPurchase(true);
		createInAppPurchaseInProduction(false,true);
		updateInAppPurchaseInProduction(true);
		
		createPurchaseOptions(true);
		updatePurchaseOptions(true);
		createPurchaseOptionsInProduction(false,true);
		updatePurchaseOptionsInProduction(true);
		
		deletePurchaseOptions(true);
		deleteInAppPurchase(true);
		deleteInAppPurchaseInProduction(true);

		addEntity(true);
		addAttribute(true);
		addAttributeType(true);
		getEntity(true);
		getProductEntities(true);
		getAttribute(true);
		getAttributeType(true);
		getAttributes(true);
		getAttributeTypes(true);
		updateEntity(true);
		updateAttribute(true);
		updateAttributeType(true);
		//updateEntities(true);
		deleteEntity(true);
		deleteAttribute(true);
		deleteAttributeType(true);
	}



	//************* PRODUCT FUNCTIONS*************//

	private boolean isSecurityError(String results) {
		return results.contains("securityPolicyException") || results.contains("GeneralSecurityException");
	}
	public void createProduct(boolean expectedFailure){
		System.out.println("Running createProduct");
		try {
			String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
			product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
			product = JSONUtils.generateUniqueString(product, 8, "name");
			productID4Test = productApi.addProduct(product, sessionToken);
			Assert.assertEquals(isSecurityError(productID4Test), expectedFailure, "createProduct failed: " + productID4Test);
			if(expectedFailure == true){
				productID4Test = productID;
			}
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("createProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getProductList(boolean expectedFailure){
		System.out.println("Running getProductList");
		try {
			String response = productApi.getAllProducts(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getProductList failed ");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getProductList failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getProduct(boolean expectedFailure){
		System.out.println("Running getProduct");
		try {
			String response = productApi.getProduct(productID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getProduct failed: "+productID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateProduct(boolean expectedFailure){
		System.out.println("Running updateProduct");
		try {
			String product = productApi.getProduct(productID4Test, sessionToken);
			JSONObject json = new JSONObject(product);
			json.put("description", "new product description");
			json.remove("seasons");
			String response = productApi.updateProduct(productID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateProduct failed: "+productID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteProduct(boolean expectedFailure){
		System.out.println("Running deleteProduct");
		try {
			int response = productApi.deleteProduct(productID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteProduct failed: code " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("deleteProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void  createProductWithSpecifiedId(boolean expectedFailure){
		System.out.println("Running createProductWithSpecifiedId");
		try {
			//First create a new product and season
			String tr_product = FileUtils.fileToString(config + "tr_product.txt", "UTF-8", false);
			JSONObject prodObj = new JSONObject(tr_product);
			prodObj.put("name", prodObj.getString("name") + RandomStringUtils.randomAlphabetic(5));
			prodObj.put("codeIdentifier", prodObj.getString("codeIdentifier") + RandomStringUtils.randomAlphabetic(5));
			String tr_productId = productApi.addProduct(prodObj.toString(), adminToken);
			//Now we have an id to recreate
			//Delete
			productApi.deleteProduct(tr_productId, adminToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url + "/products/" + tr_productId, tr_product, sessionToken);
			productApi.deleteProduct(tr_productId, adminToken);
			softAssert.assertEquals(res.code != 200, expectedFailure, "createProductWithSpecifiedId failed: " + res.code + "response code was returned. Message: " + res.message);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("createProductWithSpecifiedId failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	//************** SEASON FUNCTIONS **************//

	public void createSeason(boolean expectedFailure){
		System.out.println("Running createSeason");
		try{
			String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
			seasonID4Test = seasonApi.addSeason(productID, season, sessionToken);
			Assert.assertEquals(isSecurityError(seasonID4Test), expectedFailure, "createSeason failed: " + seasonID4Test);
			if(expectedFailure == true){
				seasonID4Test = seasonID;
			}
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("createSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}

	}

	public void getAllSeasons(boolean expectedFailure){
		System.out.println("Running getAllSeasons");
		try{
			String response = seasonApi.getAllSeasons(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAllSeasons failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllSeasons failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateSeason(boolean expectedFailure){
		System.out.println("Running updateSeason");
		try{
			String season = seasonApi.getSeason(productID, seasonID4Test, sessionToken);
			JSONObject json = new JSONObject(season);
			json.put("minVersion", "1.5");
			String response = seasonApi.updateSeason(seasonID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateSeason failed: "+ seasonID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void upgradeSeason(boolean expectedFailure){
		System.out.println("Running upgradeSeason");
		try{
			String response = seasonApi.upgradeSeason(seasonID4Test, "V2", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "upgradeSeason failed: "+ seasonID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("upgradeSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteSeason(boolean expectedFailure){
		System.out.println("Running deleteSeason");
		try{
			int response = seasonApi.deleteSeason(seasonID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteSeason failed: code " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("deleteSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createSeasonWithSpecifiedId(boolean expectedFailure){
		System.out.println("Running createSeasonWithSpecifiedId");
		try{
			//First create a new product and season
			String tr_product = FileUtils.fileToString(config + "tr_product.txt", "UTF-8", false) ;
			String tr_productId = productApi.addProduct(tr_product, sessionToken);
			String tr_season = "{\"minVersion\": \"0.8\",\"productId\": \""+tr_productId+"\"}";
			String tr_seasonId = seasonApi.addSeason(tr_productId, tr_season,adminToken);
			//Now we have an id to recreate
			seasonApi.deleteSeason(tr_seasonId, adminToken);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/"+tr_productId+"/seasons/"+tr_seasonId,tr_season, sessionToken);
			productApi.deleteProduct(tr_productId, adminToken);
			softAssert.assertEquals(res.code != 200, expectedFailure, "createSeasonWithSpecifiedId failed: " + res.code + "response code was returned. Message: " + res.message);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("createSeasonWithSpecifiedId failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getSeasonConstants(boolean expectedFailure){
		System.out.println("Running getSeasonConstants");
		try{
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/constants?platform="+"iOS", sessionToken);
			String constants = res.message;
			Assert.assertEquals(isSecurityError(constants), expectedFailure, "getSeasonConstants failed: "+ seasonID);

			res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/constants?platform="+"Android", sessionToken);
			constants = res.message;
			softAssert.assertEquals(isSecurityError(constants), expectedFailure, "getSeasonConstants failed: "+ seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getSeasonConstants failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getSeasonDefaults(boolean expectedFailure){
		System.out.println("Running getSeasonDefaults");
		try{
			String response = seasonApi.getDefaults(seasonID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getSeasonDefaults failed: "+ seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getSeasonDefaults failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void getDocumentlinks(boolean expectedFailure){
		System.out.println("Running getDocumentlinks");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/documentlinks", sessionToken);
			String response = res.message;
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getDocumentlinks failed: " + seasonID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getDocumentlinks failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	//************** SCHEMA FUNCTIONS **************//
	public void getInputSchema(boolean expectedFailure){
		System.out.println("Running getInputSchema");
		try {
			String schema = schemaApi.getInputSchema(seasonID, sessionToken);
			softAssert.assertEquals(schema.contains("SecurityPolicyException"), expectedFailure, "getInputSchema failed: " + utilityID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getInputSchema failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getInputSample(boolean expectedFailure){
		System.out.println("Running getInputSample");
		try {
			String schema = schemaApi.getInputSample(seasonID,"DEVELOPMENT","90", sessionToken,"MAXIMAL",0.7);
			softAssert.assertEquals(schema.contains("SecurityPolicyException"), expectedFailure, "getInputSample failed: " + utilityID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getInputSample failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInputSchema(boolean expectedFailure){
		System.out.println("Running updateInputSchema");
		try {
			String schema = schemaApi.getInputSchema(seasonID, sessionToken);
			String file = FileUtils.fileToString(config + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
			JSONObject is = new JSONObject(file);
			JSONObject jsonSchema = new JSONObject(schema);
			jsonSchema.put("inputSchema", is);
			String response = schemaApi.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateInputSchema failed: " + utilityID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateInputSchema failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void validateInputSchema(boolean expectedFailure){
		System.out.println("Running validateInputSchema");
		try {
			String schemaBody = FileUtils.fileToString(config + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
			String response = schemaApi.validateSchema(seasonID, schemaBody.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "validateInputSchema failed: " + utilityID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateInputSchema failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	//************** UTILITY FUNCTIONS **************//

	public void addUtility(boolean expectedFailure){
		System.out.println("Running addUtility");
		try{
			String utility = FileUtils.fileToString(config + "/utilities/utility1.txt", "UTF-8", false);
			Properties utilProps1 = new Properties();
			utilProps1.load(new StringReader(utility));
			utilityID4Test = utilitiesApi.addUtility(seasonID, utilProps1, sessionToken);
			softAssert.assertEquals(utilityID4Test.contains("SecurityPolicyException"), expectedFailure, "addUtility failed: "+ utilityID4Test);
			if(expectedFailure){
				utilityID4Test = utilityID;
			}
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("addUtility failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllUtilities(boolean expectedFailure){
		System.out.println("Running getAllUtilities");
		try{
			String utility = utilitiesApi.getAllUtilites(seasonID,sessionToken,"DEVELOPMENT");
			softAssert.assertEquals(utility.contains("SecurityPolicyException"), expectedFailure, "getAllUtilities failed: "+ seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllUtilities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getUtility(boolean expectedFailure){
		System.out.println("Running getUtility");
		try {
			String utility = utilitiesApi.getUtility(utilityID4Test, sessionToken);
			softAssert.assertEquals(utility.contains("SecurityPolicyException"), expectedFailure, "getUtility failed: " + utilityID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getUtility failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getUtilitiesInfo(boolean expectedFailure){
		System.out.println("Running getUtilitiesInfo");
		try {
			String utility = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
			softAssert.assertEquals(utility.contains("SecurityPolicyException"), expectedFailure, "getUtilitiesInfo failed: " + seasonID);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getUtilitiesInfo failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateUtility(boolean expectedFailure){
		System.out.println("Running updateUtility");
		try {
			String utility = utilitiesApi.getUtility(utilityID4Test, sessionToken);
			JSONObject jsonUtil = new JSONObject(utility);
			jsonUtil.put("minAppVersion", "0.2");
			String response = utilitiesApi.updateUtility(utilityID4Test, jsonUtil, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateUtility failed: " + utilityID4Test);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateUtility failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteUtility(boolean expectedFailure){
		System.out.println("Running deleteUtility");
		try {
			int response = utilitiesApi.deleteUtility(utilityID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteUtility failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteUtility failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void simulateUtility(boolean expectedFailure){
		System.out.println("Running simulateUtility");
		try {
			String body =" $#^StartOfRule^#$";
			String response = utilitiesApi.simulateUtility(seasonID,body,"DEVELOPMENT","0.1","RULE",sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "simulateUtility failed");
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("simulateUtility failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	//************** FEATURE FUNCTIONS **************//
	public void createFeature(boolean expectedFailure){
		System.out.println("Running createFeature");
		try {
			String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
			JSONObject featureJson = new JSONObject(feature);
			featureJson.put("name", "feature"+RandomStringUtils.randomAlphabetic(5));

			featureID4Test = featureApi.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);
			Assert.assertEquals(isSecurityError(featureID4Test), expectedFailure, "createFeature failed: "+ featureID4Test);
			if(expectedFailure){
				featureID4Test = featureID;
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void createFeatureInProduction(boolean asAdmin,boolean expectedFailure){
		System.out.println("Running createFeatureInProduction");
		try {
			String feature = FileUtils.fileToString(config + "feature_production.txt", "UTF-8", false);
			JSONObject featureJson = new JSONObject(feature);
			featureJson.put("name", "Feature in production."+RandomStringUtils.randomAlphabetic(5));
			if(asAdmin) {
				featureID4TestProd = featureApi.addFeature(seasonID, featureJson.toString(), "ROOT", adminToken);
			}else {
				featureID4TestProd = featureApi.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);
			}
			Assert.assertEquals(featureID4TestProd.contains("Administrator or Product Lead role can add an item in the production stage") || isSecurityError(featureID4TestProd), expectedFailure, "createFeatureInProduction failed: "+ featureID4TestProd);
			if(expectedFailure == false){
				String configuration = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
				JSONObject jsonConfig = new JSONObject(configuration);
				JSONObject newConfiguration = new JSONObject();
				newConfiguration.put("color", "red");
				jsonConfig.put("configuration", newConfiguration);
				featureApi.addFeature(seasonID, jsonConfig.toString(), featureID4TestProd, adminToken);
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createFeatureInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllFeatures(boolean expectedFailure){
		System.out.println("Running getAllFeatures");
		try{
			String features = featureApi.getAllFeatures(sessionToken);
			softAssert.assertEquals(isSecurityError(features), expectedFailure, "getAllFeatures failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllFeatures failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getFeaturesForSeason(boolean expectedFailure){
		System.out.println("Running getFeaturesForSeason");
		try{
			JSONArray features = featureApi.getFeaturesBySeason(seasonID, sessionToken);
			softAssert.assertEquals(features.contains("SecurityPolicyException"), expectedFailure, "getFeaturesForSeason failed: " + seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getFeaturesForSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllFeaturesForSeason(boolean expectedFailure){
		System.out.println("Running getAllFeaturesForSeason");
		try{
			String features = featureApi.featuresInSeason(seasonID, sessionToken);
			softAssert.assertEquals(isSecurityError(features), expectedFailure, "getAllFeaturesForSeason failed: " + seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllFeaturesForSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getFeature(boolean expectedFailure){
		System.out.println("Running getFeature");
		try {
			String feature = featureApi.getFeature(featureID4Test, sessionToken);
			Assert.assertEquals(isSecurityError(feature), expectedFailure, "getFeature failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateFeature(boolean expectedFailure){
		System.out.println("Running updateFeature");
		try{
			String feature = featureApi.getFeature(featureID4Test, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeature(seasonID, featureID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateFeature failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void updateFeatureInProduction(boolean expectedFailure){
		System.out.println("Running updateFeatureInProduction");
		try{
			if(featureID4TestProd == null || isSecurityError(featureID4TestProd) || featureID4TestProd.contains("Unable to add the feature")){
				createFeatureInProduction(true,false);
			}
			String feature = featureApi.getFeature(featureID4TestProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeature(seasonID, featureID4TestProd, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") || isSecurityError(response), expectedFailure, "updateFeatureInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateFeatureInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void deleteFeature(boolean expectedFailure){
		System.out.println("Running deleteFeature");
		try {
			int response = featureApi.deleteFeature(featureID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteFeature failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void deleteFeatureInProduction(boolean expectedFailure){
		System.out.println("Running deleteFeatureInProduction");
		try {
			String feature = featureApi.getFeature(featureID4TestProd, sessionToken);
			int response = featureApi.deleteFeature(featureID4TestProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteFeatureInProduction failed: code " + response);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			featureID4TestProd = featureApi.updateFeature(seasonID,featureID4TestProd, json.toString(), adminToken);
			featureApi.deleteFeature(featureID4TestProd, adminToken);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteFeatureInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateFeatureToProd(boolean expectedFailure) {
		System.out.println("Running updateFeatureToProd");
		try {
			String featureText = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
			String currFeatureID = featureApi.addFeature(seasonID, featureText, "ROOT", adminToken);
			String feature = featureApi.getFeature(currFeatureID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");
			String response = featureApi.updateFeature(seasonID, currFeatureID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead can change a subfeature from the development to the production") || isSecurityError(response), expectedFailure, "updateFeatureToProd failed: " + featureID);
			feature = featureApi.getFeature(currFeatureID, sessionToken);
			json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			currFeatureID = featureApi.updateFeature(seasonID, currFeatureID, json.toString(), adminToken);
			featureApi.deleteFeature(currFeatureID, adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateFeatureToProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	//************** CHECKOUT FUNCTIONS **************//

	public void checkOutStandAlone(boolean expectedFailure){
		System.out.println("Running checkOutStandAlone");
		try {
			String response = branchesRestApi.checkoutFeature(branchID,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "checkOutStandAlone failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void checkOutProdStandAlone(boolean expectedFailure){
		System.out.println("Running checkOutProdStandAlone");
		try {
			String response = branchesRestApi.checkoutFeature(branchID,featureID4TestProd, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "checkOutProdStandAlone failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutProdStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateCheckoutStandAlone(boolean expectedFailure){
		System.out.println("Running updateCheckoutStandAlone");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4Test,branchID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchID,featureID4Test,json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateCheckoutStandAlone failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateCheckoutProdStandAlone(boolean expectedFailure){
		System.out.println("Running updateCheckoutProdStandAlone");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchID,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") ||isSecurityError(response), expectedFailure, "updateCheckoutProdStandAlone failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void downgradeCheckoutProdStandAlone(boolean expectedFailure){
		System.out.println("Running downgradeCheckoutProdStandAlone");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID,branchID,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateCheckoutProdStandAlone failed: " + featureID4TestProd);
			//restore
			json.put("stage", "PRODUCTION");
			featureApi.updateFeatureInBranch(seasonID,branchID,featureID4TestProd,json.toString(), adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutStandAlone(boolean expectedFailure){
		System.out.println("Running cancelCheckOutStandAlone");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchID,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelCheckOutStandAlone failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutProdStandAlone(boolean expectedFailure){
		System.out.println("Running cancelCheckOutProdStandAlone");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchID,featureID4TestProd, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelCheckOutProdStandAlone failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutProdStandAlone failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void checkOutDevInProd(boolean expectedFailure){
		System.out.println("Running checkOutDevInProd");
		try {
			String response = branchesRestApi.checkoutFeature(branchIDDevInProd,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "checkOutDevInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void checkOutProdDevInProd(boolean expectedFailure){
		System.out.println("Running checkOutProdDevInProd");
		try {
			String response = branchesRestApi.checkoutFeature(branchIDDevInProd,featureID4TestProd, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "checkOutProdDevInProd failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutProdDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateCheckoutDevInProd(boolean expectedFailure){
		System.out.println("Running updateCheckoutDevInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4Test,branchIDDevInProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDDevInProd,featureID4Test,json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateCheckoutDevInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateCheckoutProdDevInProd(boolean expectedFailure){
		System.out.println("Running updateCheckoutProdDevInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchIDDevInProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDDevInProd,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") || isSecurityError(response), expectedFailure, "updateCheckoutProdDevInProd failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void downgradeCheckoutProdDevInProd(boolean expectedFailure){
		System.out.println("Running downgradeCheckoutProdDevInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchIDDevInProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDDevInProd,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateCheckoutProdDevInProd failed: " + featureID4TestProd);
			//restore
			json.put("stage", "PRODUCTION");
			featureApi.updateFeatureInBranch(seasonID,branchIDDevInProd,featureID4TestProd,json.toString(), adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutDevInProd(boolean expectedFailure){
		System.out.println("Running cancelCheckOutDevInProd");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchIDDevInProd,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelCheckOutDevInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutProdDevInProd(boolean expectedFailure){
		System.out.println("Running cancelCheckOutProdDevInProd");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchIDDevInProd,featureID4TestProd, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelCheckOutProdDevInProd failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutProdDevInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void checkOutProdInProd(boolean expectedFailure){
		System.out.println("Running checkOutProdInProd");
		try {
			String response = branchesRestApi.checkoutFeature(branchIDProd,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "checkOutProdInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void checkOutProdProdInProd(boolean expectedFailure){
		System.out.println("Running checkOutProdProdInProd");
		try {
			String response = branchesRestApi.checkoutFeature(branchIDProd,featureID4TestProd, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can check out a feature in the production stage to branches that are included in a production experiment") || isSecurityError(response), expectedFailure, "checkOutProdProdInProd failed: " + featureID4TestProd);
			branchesRestApi.checkoutFeature(branchIDProd,featureID4TestProd, adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("checkOutProdProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void updateCheckoutProdInProd(boolean expectedFailure){
		System.out.println("Running updateCheckoutProdInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4Test,branchIDProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDProd,featureID4Test,json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateCheckoutProdInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateCheckoutProdProdInProd(boolean expectedFailure){
		System.out.println("Running updateCheckoutProdProdInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchIDProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("description", "new description");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDProd,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") || isSecurityError(response), expectedFailure, "updateCheckoutProdProdInProd failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void downgradeCheckoutProdProdInProd(boolean expectedFailure){
		System.out.println("Running downgradeCheckoutProdProdInProd");
		try {
			String feature = featureApi.getFeatureFromBranch(featureID4TestProd,branchIDProd, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID,branchIDProd,featureID4TestProd,json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead can change a subfeature from the production to the development stage") || isSecurityError(response), expectedFailure, "updateCheckoutProdProdInProd failed: " + featureID4TestProd);
			//restore
			json.put("stage", "PRODUCTION");
			featureApi.updateFeatureInBranch(seasonID,branchIDProd,featureID4TestProd,json.toString(), adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateCheckoutProdProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutProdInProd(boolean expectedFailure){
		System.out.println("Running cancelCheckOutProdInProd");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchIDProd,featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelCheckOutProdInProd failed: " + featureID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelCheckOutProdProdInProd(boolean expectedFailure){
		System.out.println("Running cancelCheckOutProdProdInProd");
		try {
			String response = branchesRestApi.cancelCheckoutFeature(branchIDProd,featureID4TestProd, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can cancel the checkout of a feature in the production stage from branches that are included in a production experiment") || isSecurityError(response), expectedFailure, "cancelCheckOutProdProdInProd failed: " + featureID4TestProd);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("cancelCheckOutProdProdInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	//************** STRING/TRANSLATION FUNCTIONS **************//

	public void addString(boolean expectedFailure){
		System.out.println("Running addString");
		try{
			String str = FileUtils.fileToString(config + "/strings/string1.txt", "UTF-8", false);
			stringID4Test = stringApi.addString(seasonID, str, sessionToken);
			Assert.assertEquals(isSecurityError(stringID4Test), expectedFailure, "addString failed: "+ stringID4Test);
			if(expectedFailure)
				stringID4Test = stringID;
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("addString failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void addStringInProd(boolean expectedFailure){
		System.out.println("Running addStringInProd");
		try{
			String str = FileUtils.fileToString(config + "/strings/string3.txt", "UTF-8", false);
			stringID4TestProd = stringApi.addString(seasonID, str, sessionToken);
			softAssert.assertEquals(stringID4TestProd.contains("Administrator or Product Lead role can add string in the production stage") || isSecurityError(stringID4TestProd), expectedFailure, "addStringInProd failed: "+ stringID4Test);
			if(expectedFailure) {
				stringID4TestProd = stringApi.addString(seasonID, str, adminToken);
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("addStringInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void getAllStrings(boolean expectedFailure){
		System.out.println("Running getAllStrings");
		try{
			String strings = stringApi.getAllStrings(seasonID,sessionToken);
			softAssert.assertEquals(isSecurityError(strings), expectedFailure, "getAllStrings failed: "+ seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllStrings failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getString(boolean expectedFailure){
		System.out.println("Running getString");
		try {
			String str = stringApi.getString(stringID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(str), expectedFailure, "getString failed: " + stringID4Test);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getString failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateString(boolean expectedFailure){
		System.out.println("Running updateString");
		try{
			String str = stringApi.getString(stringID4Test, sessionToken);
			JSONObject jsonstr = new JSONObject(str);
			jsonstr.put("value", "hi");
			jsonstr.put("owner", "Bill");
			String response = stringApi.updateString(stringID4Test, jsonstr.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateString failed: " + stringID4Test);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateString failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateStringInProd(boolean expectedFailure){
		System.out.println("Running updateStringInProd");
		try{
			String str = stringApi.getString(stringID4TestProd, sessionToken);
			JSONObject jsonstr = new JSONObject(str);
			jsonstr.put("owner", "Bill");
			String response = stringApi.updateString(stringID4TestProd, jsonstr.toString(), sessionToken);
			softAssert.assertEquals(response.contains(" Administrator, Product Lead or Translation Specialist role can update string in the production stage") || isSecurityError(response), expectedFailure, "updateStringInProd failed: " + stringID4TestProd);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateStringInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateStringValueInProd(boolean expectedFailure){
		System.out.println("Running updateStringValueInProd");
		try{
			String str = stringApi.getString(stringID4TestProd, sessionToken);
			JSONObject jsonstr = new JSONObject(str);
			jsonstr.put("value", "hi2");
			String response = stringApi.updateString(stringID4TestProd, jsonstr.toString(), sessionToken);
			softAssert.assertEquals(response.contains(" Administrator, Product Lead or Translation Specialist role can update string in the production stage") || isSecurityError(response), expectedFailure, "updateStringInProd failed: " + stringID4TestProd);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateStringValueInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void copyStrings(boolean expectedFailure){
		System.out.println("Running copyStrings");
		try{
			String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
			seasonToCopyTo = seasonApi.addSeason(productID, season, adminToken);
			String response = translationApi.copyStrings(seasonToCopyTo,new String[]{stringID4TestProd},false, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "copyStrings failed");
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("copyStrings failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void copyStringsOverrideProd(boolean expectedFailure){
		System.out.println("Running copyStringsOverrideProd");
		try{
			String allStrings = stringApi.getAllStrings(seasonToCopyTo,"BASIC",adminToken);
			JSONObject jsonStrings = new JSONObject(allStrings);
			JSONArray allStringArray = jsonStrings.getJSONArray("strings");
			String id = allStringArray.getJSONObject(allStringArray.size()-1).getString("uniqueId");
			String str = stringApi.getString(id, adminToken);
			JSONObject jsonstr = new JSONObject(str);
			jsonstr.put("value", "Bill");
			String response = stringApi.updateString(id, jsonstr.toString(), adminToken);
			response = translationApi.copyStrings(seasonToCopyTo,new String[]{stringID4Test,stringID4TestProd},true, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can perform actions on strings in the production stage") || isSecurityError(response), expectedFailure, "copyStrings failed");
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("copyStrings failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void importStrings(boolean expectedFailure){
		System.out.println("Running importStrings");
		try{
			String stringToImport = stringApi.getString(stringID4TestProd, sessionToken);
			String response = translationApi.copyStrings(seasonToCopyTo,new String[]{stringID4Test},false, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "copyStrings failed: " + stringID4Test);
			seasonApi.deleteSeason(seasonToCopyTo, adminToken);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("copyStrings failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getStringForTranslation(boolean expectedFailure){
		System.out.println("Running getStringForTranslation");
		try{
			String response = translationApi.stringForTranslation(seasonID,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getStringForTranslation failed: " + seasonID);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getStringForTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addTranslation(boolean expectedFailure){
		System.out.println("Running addTranslation");
		try{
			String frTranslation = FileUtils.fileToString(config + "strings/translationFR.txt", "UTF-8", false);
			String response = translationApi.addTranslation(seasonID,"fr",frTranslation,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "addTranslation failed: " + seasonID);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("addTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getTranslation(boolean expectedFailure){
		System.out.println("Running getTranslation");
		try{
			String response = translationApi.getTranslation(seasonID,"fr","DEVELOPMENT",sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getTranslation failed: " + seasonID);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateTranslation(boolean expectedFailure){
		System.out.println("Running updateTranslation");
		try{
			String frTranslation = FileUtils.fileToString(config + "strings/translationFR.txt", "UTF-8", false);
			frTranslation = frTranslation.replace("Bonjour","SALUT");
			String response = translationApi.updateTranslation(seasonID,"fr",frTranslation,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateTranslation failed: " + seasonID);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void deleteString(boolean expectedFailure){
		System.out.println("Running deleteString");
		try{
			int response = stringApi.deleteString(stringID4Test,sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteString failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteString failed with exception:\n" +e.getLocalizedMessage());
			}
		}

	}

	public void getSupportedLocales(boolean expectedFailure) {
		System.out.println("Running getSupportedLocales");
		try {
			String response = translationApi.getSupportedLocales(seasonID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getSupportedLocales failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getSupportedLocales failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addSupportedLocales(boolean expectedFailure) {
		System.out.println("Running addSupportedLocales");
		try {
			String response = translationApi.addSupportedLocales(seasonID,"fr", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "addSupportedLocales failed: " + response);
			translationApi.addSupportedLocales(seasonID,"fr", adminToken);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("addSupportedLocales failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void removeSupportedLocales(boolean expectedFailure) {
		System.out.println("Running removeSupportedLocales");
		try {
			String response = translationApi.removeSupportedLocales(seasonID,"fr", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "removeSupportedLocales failed: " + response);
			translationApi.removeSupportedLocales(seasonID,"fr", adminToken);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("removeSupportedLocales failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void overrideTranslate(boolean expectedFailure) {
		System.out.println("Running overrideTranslate");
		try {
			String response = translationApi.overrideTranslate(stringID,"fr","", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "overrideTranslate failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("overrideTranslate failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void cancelOverrideTranslate(boolean expectedFailure) {
		System.out.println("Running cancelOverrideTranslate");
		try {
			String response = translationApi.cancelOverride(stringID,"fr", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "cancelOverrideTranslate failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("cancelOverrideTranslate failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void overrideTranslateInProd(boolean expectedFailure) {
		System.out.println("Running overrideTranslateInProd");
		try {
			String response = translationApi.overrideTranslate(stringID4TestProd,"fr","", sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can override translation value for string in the production stage") || isSecurityError(response), expectedFailure, "overrideTranslateInProd failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("overrideTranslateInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void cancelOverrideTranslateInProd(boolean expectedFailure) {
		System.out.println("Running cancelOverrideTranslateInProd");
		try {
			String response = translationApi.cancelOverride(stringID4TestProd,"fr", sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can cancel translation override for string in the production stage") || isSecurityError(response), expectedFailure, "cancelOverrideTranslateInProd failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("cancelOverrideTranslateInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getStringsUsedInFeature(boolean expectedFailure) {
		System.out.println("Running getStringsUsedInFeature");
		try {
			String response = translationApi.stringInUse(featureID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getStringsUsedInFeature failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getStringsUsedInFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void markForTranslation(boolean expectedFailure) {
		System.out.println("Running markForTranslation");
		try {
			String response = translationApi.markForTranslation(seasonID,new String[]{stringID}, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "markForTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("markForTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void markForTranslationInProd(boolean expectedFailure) {
		System.out.println("Running markForTranslationInProd");
		try {
			String response = translationApi.markForTranslation(seasonID,new String[]{stringID4TestProd}, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can perform actions on strings in the production stage") || isSecurityError(response), expectedFailure, "markForTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("markForTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void reviewTranslation(boolean expectedFailure) {
		System.out.println("Running reviewTranslation");
		try {
			String response = translationApi.reviewForTranslation(seasonID,new String[]{stringID}, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "reviewTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("reviewTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getNewStringsForTranslation(boolean expectedFailure) {
		System.out.println("Running getNewStringsForTranslation");
		try {
			String response = translationApi.getNewStringsForTranslation(seasonID,new String[]{}, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getNewStringsForTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getNewStringsForTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void sendToTranslation(boolean expectedFailure) {
		System.out.println("Running sendToTranslation");
		try {
			String response = translationApi.sendToTranslation(seasonID,new String[]{stringID}, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "sendToTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("sendToTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getTranslationSummary(boolean expectedFailure) {
		System.out.println("Running getTranslationSummary");
		try {
			String response = translationApi.getTranslationSummary(seasonID,new String[]{stringID}, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getTranslationSummary failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getTranslationSummary failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getStringStatuses(boolean expectedFailure) {
		System.out.println("Running getStringStatuses");
		try {
			String response = translationApi.getStringStatuses(seasonID,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getStringStatuses failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getStringStatuses failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	//************** OPERATIONS FUNCTIONS **************//

	public void getRoles(boolean expectedFailure) {
		System.out.println("Running getRoles");
		try {
			String response = operationApi.getRoles(sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getRoles failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getRoles failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void setRoles(boolean expectedFailure){
		System.out.println("Running setRoles");
		try {
			String response = operationApi.getRoles(sessionToken);
			response = operationApi.setRoles(response, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setRoles failed: "+response);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("setRoles failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAirlockUsers(boolean expectedFailure) {
		System.out.println("Running getAirlockUsers");
		try {
			String response = operationApi.getAirlockUsers(sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getAirlockUsers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getAirlockUsers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getUserRoleSets(boolean expectedFailure) {
		System.out.println("Running getUserRoleSets");
		try {
			String user = operationApi.getUserRoleSets(adminUser, sessionToken);
			Assert.assertEquals(isSecurityError(user), expectedFailure, "getUserRoleSets failed: " + userID);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getUserRoleSets failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addAirlockUser(boolean expectedFailure) {
		System.out.println("Running addAirlockUser");
		try {
			String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);

			JSONObject json = new JSONObject(user);
			json.put("identifier", RandomStringUtils.randomAlphabetic(5)+"@il.ibm.com");

			userID = operationApi.addGlobalAirlockUser(json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(userID), expectedFailure, "add global user group failed: " + userID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setAirlockUsers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAirlockUser(boolean expectedFailure) {
		System.out.println("Running getAirlockUser");
		try {
			String user = operationApi.getAirlockUser(userID, sessionToken);
			Assert.assertEquals(isSecurityError(user), expectedFailure, "getAirlockUser failed: " + userID);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getAirlockUser failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteAirlockUser(boolean expectedFailure){
		System.out.println("Running deleteAirlockUser");
		try {
			int response = operationApi.deleteAirlockUser(userID, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteAirlockUser failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteAirlockUser failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateAirlockUser(boolean expectedFailure){
		System.out.println("Running updateAirlockUser");
		try{
			String user = operationApi.getAirlockUser(userID, sessionToken);
			JSONObject json = new JSONObject(user);
			String response = operationApi.updateAirlockUser(userID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateAirlockUser failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateAirlockUser failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAirlockUsersPerProduct(boolean expectedFailure) {
		System.out.println("Running getAirlockUsersPerProduct");
		try {
			String response = operationApi.getProductAirlockUsers(sessionToken, productID);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getAirlockUsers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getAirlockUsers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void getUserRolesPerProduct(boolean expectedFailure) {
		System.out.println("Running getAirlockUsersPerProduct");
		try {
			String response = operationApi.getUserRolesPerProduct(sessionToken, productID, viewerUser);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getAirlockUsers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getAirlockUsers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void setAirlockUserPerProduct(boolean expectedFailure) {
		System.out.println("Running setAirlockUserPerProduct");
		try {
			String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);

			JSONObject json = new JSONObject(user);
			json.put("identifier", RandomStringUtils.randomAlphabetic(5)+"@il.ibm.com");

			String response = operationApi.addProductAirlockUser(json.toString(), productID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "add product user failed: " + response);

		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setProductAirlockUsers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void getAirlockServers(boolean expectedFailure) {
		System.out.println("Running getAirlockServers");
		try {
			String response = operationApi.getAirlockServers(sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getAirlockServers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getAirlockServers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void setAirlockServers(boolean expectedFailure) {
		System.out.println("Running setAirlockServers");
		try {
			String response = operationApi.getAirlockServers(sessionToken);
			response = operationApi.setAirlockServers(response,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAirlockServers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getAirlockServers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getUserGroups(boolean expectedFailure){
		System.out.println("Running getUserGroups");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID +"/usergroups", sessionToken);
			String response = res.message;
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getUserGroups failed: " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getUserGroups failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getUserGroupsUsage(boolean expectedFailure){
		System.out.println("Running getUserGroupsUsage");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID +"/usergroups/usage", sessionToken);
			String response = res.message;
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getUserGroupsUsage failed: " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getUserGroups failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void setUserGroups(boolean expectedFailure) {
		System.out.println("Running setUserGroups");
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/" + productID +"/usergroups", sessionToken);
			String response = res.message;
			res = RestClientUtils.sendPut(m_url +"/usergroups",response, sessionToken);
			response = res.message;
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setUserGroups failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setUserGroups failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void importFeatures(boolean expectedFailure) {
		System.out.println("Running importFeatures");
		try {
			String featureToImport = featureApi.getFeature(featureID,sessionToken);
			String response = featureApi.importFeature(featureToImport, featureID, "ACT", null, "suffix1", true, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "importFeatures failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("importFeatures failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void copyFeature(boolean expectedFailure) {
		System.out.println("Running copyFeature");
		try {
			String response = featureApi.copyFeature(featureID, featureID, "ACT", null, "suffix2", sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "copyFeature failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("copyFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	/****** FOLLOW FUNCTIONS *******/

	public void getProductFollowers(boolean expectedFailure) {
		System.out.println("Running getProductFollowers");
		try {
			String response = productApi.getProductFollowers(productID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getProductFollowers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getProductFollowers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void followProduct(boolean expectedFailure) {
		System.out.println("Running followProduct");
		try {
			String response = productApi.followProduct(productID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "followProduct failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("followProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void unfollowProduct(boolean expectedFailure) {
		System.out.println("Running unfollowProduct");
		try {
			int response = productApi.unfollowProduct(productID, sessionToken);
			//Assert.assertEquals(isSecurityError(response), expectedFailure, "unfollowProduct failed: " + response);
			softAssert.assertEquals(response != 200, expectedFailure,"unfollowProduct failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("unfollowProduct failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getFeatureFollowers(boolean expectedFailure) {
		System.out.println("Running getFeatureFollowers");
		try {
			String response = featureApi.getFeatureFollowers(featureID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getFeatureFollowers failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getFeatureFollowers failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void followFeature(boolean expectedFailure) {
		System.out.println("Running followFeature");
		try {
			String response = featureApi.followFeature(featureID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "followFeature failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("followFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void unfollowFeature(boolean expectedFailure) {
		System.out.println("Running unfollowFeature");
		try {
			int response = featureApi.unfollowFeature(featureID, sessionToken);
			//Assert.assertEquals(isSecurityError(response), expectedFailure, "unfollowFeature failed: " + response);
			softAssert.assertEquals(response!=200, expectedFailure,"unfollowFeature failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("unfollowFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	/****** ANALYTICS FUNCTIONS *******/

	public void addFeatureToAnalytics(boolean expectedFailure) {
		System.out.println("Running addFeatureToAnalytics");
		try {
			String response = analyticsApi.addFeatureToAnalytics(featureID, BranchesRestApi.MASTER, sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "addFeatureToAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("addFeatureToAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addFeatureInProdToAnalytics(boolean expectedFailure) {
		System.out.println("Running addFeatureInProdToAnalytics");
		try {
			String response = analyticsApi.addFeatureToAnalytics(featureID4TestProd, BranchesRestApi.MASTER, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can send a feature in the production stage to analytics") || isSecurityError(response), expectedFailure, "addFeatureInProdToAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("addFeatureInProdToAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void updateFeatureAttributesForAnalytics(boolean expectedFailure) {
		System.out.println("Running updateFeatureAttributesForAnalytics");
		try {
			String response = analyticsApi.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER,"[]", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateFeatureAttributesForAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateFeatureAttributesForAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateFeatureInProdAttributesForAnalytics(boolean expectedFailure) {
		System.out.println("Running updateFeatureInProdAttributesForAnalytics");
		try {
			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);
			String response = analyticsApi.addAttributesToAnalytics(featureID4TestProd,BranchesRestApi.MASTER,attributes.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update attributes for a feature in the production stage")|| isSecurityError(response), expectedFailure, "updateFeatureAttributesForAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateFeatureAttributesForAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInputFieldsForAnalytics(boolean expectedFailure) {
		System.out.println("Running updateInputFieldsForAnalytics");
		try {
			JSONArray inputFields = new JSONArray();
			//field in DEVELOPMENT stage
			inputFields.put("context.device.locale");
			String response = analyticsApi.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateInputFieldsForAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateInputFieldsForAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInputFieldsInProdForAnalytics(boolean expectedFailure) {
		System.out.println("Running updateInputFieldsInProdForAnalytics");
		try {
			JSONArray inputFields = new JSONArray();
			//field in PRODCUTION stage
			inputFields.put("context.viewedLocation.country");
			String response = analyticsApi.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update context fields in the production stage for analytics") || isSecurityError(response), expectedFailure, "updateInputFieldsInProdForAnalytics failed: " + featureID);

		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateInputFieldsInProdForAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void updateInputFieldsForAnalyticsDevAndProd(boolean expectedFailure) {
		System.out.println("Running updateInputFieldsForAnalyticsDevAndProd");
		try {
			JSONArray inputFields = new JSONArray();
			//field in PRODUCTION stage
			inputFields.put("context.viewedLocation.country");
			analyticsApi.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), adminToken);
			//field in DEV stage
			inputFields.put("context.device.locale");
			String response = analyticsApi.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update analytics for an item in the production stage") || isSecurityError(response), expectedFailure, "updateInputFieldsForAnalyticsDevAndProd failed: " + featureID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateInputFieldsForAnalyticsDevAndProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}



	public void getFeatureAttributes(boolean expectedFailure) {
		System.out.println("Running getFeatureAttributes");
		try {
			String response = featureApi.getFeatureAttributes(featureID, sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getFeatureAttributes failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getFeatureAttributes failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void removeFeatureFromAnalytics(boolean expectedFailure) {
		System.out.println("Running removeFeatureFromAnalytics");
		try {
			String response = analyticsApi.deleteFeatureFromAnalytics(featureID, BranchesRestApi.MASTER, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "removeFeatureFromAnalytics failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("removeFeatureFromAnalytics failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getGlobalDataCollection(boolean expectedFailure) {
		System.out.println("Running getGlobalDataCollection");
		try {
			String response = analyticsApi.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER,"BASIC", sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getGlobalDataCollection failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getGlobalDataCollection failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	private String getEmptyDataCollection(){
		System.out.println("Running getEmptyDataCollection");
		try {
			JSONObject emptyDataJson = new JSONObject(emptyDataCollection);
			String response = analyticsApi.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER,"BASIC", adminToken);
			JSONObject responseJson = new JSONObject(response);
			emptyDataJson.put("lastModified",responseJson.get("lastModified"));
			return emptyDataJson.toString();

		}catch (Exception e){
			return "";
		}
	}

	public void updateGlobalDataCollection(boolean expectedFailure) {
		System.out.println("Running updateGlobalDataCollection");
		try {
			String response = getEmptyDataCollection();
			String input = analyticsApi.addFeatureOnOff(response, featureID);
			response = analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "updateGlobalDataCollection failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("updateGlobalDataCollection failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateGlobalDataCollectionFeatureProd(boolean expectedFailure) {
		System.out.println("Running updateGlobalDataCollectionFeatureProd");
		try {
			String response = getEmptyDataCollection();
			String input = analyticsApi.addFeatureOnOff(response, featureID4TestProd);
			response = analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update analytics for an item in the production stage") || isSecurityError(response), expectedFailure, "updateGlobalDataCollectionFeatureProd failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateGlobalDataCollectionFeatureProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void updateGlobalDataCollectionAttributeProd(boolean expectedFailure) {
		System.out.println("Running updateGlobalDataCollectionAttributeProd");
		try {
			String response = getEmptyDataCollection();
			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);
			String input = analyticsApi.addFeaturesAttributesToAnalytics(response, featureID4TestProd,attributes);
			response = analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update analytics for an item in the production stage") || isSecurityError(response), expectedFailure, "updateGlobalDataCollectionAttributeProd failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateGlobalDataCollectionAttributeProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateGlobalDataCollectionInputFieldProd(boolean expectedFailure) {
		System.out.println("Running updateGlobalDataCollectionInputFieldProd");
		try {
			String response = getEmptyDataCollection();
			String input = analyticsApi.addInputFieldsToAnalytics(response, "context.viewedLocation.country");
			response = analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update context fields in the production stage for analytics") || isSecurityError(response), expectedFailure, "updateGlobalDataCollectionInputFieldProd failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateGlobalDataCollectionInputFieldProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateGlobalDataCollectionMix(boolean expectedFailure) {
		System.out.println("Running updateGlobalDataCollectionMix");
		try {
			String input = getEmptyDataCollection();
			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);
			input = analyticsApi.addFeatureOnOff(input, featureID4TestProd);
			input = analyticsApi.addFeaturesAttributesToAnalytics(input, featureID4TestProd,attributes);
			input = analyticsApi.addInputFieldsToAnalytics(input, "context.viewedLocation.country");
			String response = analyticsApi.updateGlobalDataCollection(seasonID,BranchesRestApi.MASTER, input, adminToken);
			response = analyticsApi.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC",adminToken);
			input = analyticsApi.addFeatureOnOff(response, featureID);
			input = analyticsApi.addFeaturesAttributesToAnalytics(input, featureID,attributes);
			input = analyticsApi.addInputFieldsToAnalytics(input, "context.device.locale");
			response = analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateGlobalDataCollectionMix failed: " + response);
			input = getEmptyDataCollection();
			analyticsApi.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, adminToken);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateGlobalDataCollectionMix failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAnalyticsQuota(boolean expectedFailure) {
		System.out.println("Running getAnalyticsQuota");
		try {
			String response = analyticsApi.getQuota(seasonID,sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getAnalyticsQuota failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getAnalyticsQuota failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void setAnalyticsQuota(boolean expectedFailure) {
		System.out.println("Running setAnalyticsQuota");
		try {
			String response = analyticsApi.updateQuota(seasonID,2,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setUserGroups failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setUserGroups failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	/****** EXPERIMENTS/VARIANTS/BRANCHES FUNCTIONS *******/

	public void createExperiment(boolean expectedFailure) {
		System.out.println("Running createExperiment");
		try {
			String experiment = FileUtils.fileToString(config + "experiments/experiment1.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experiment);
			expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
			expJson.put("enabled", false);

			experimentID = experimentsRestApi.createExperiment(productID,expJson.toString(),sessionToken);
			Assert.assertEquals(isSecurityError(experimentID), expectedFailure, "createExperiment failed: " + experimentID);

			if(expectedFailure){
				experimentID = experimentsRestApi.createExperiment(productID,expJson.toString(),adminToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createExperimentInProd(boolean expectedFailure) {
		System.out.println("Running createExperimentInProd");
		try {
			String experiment = FileUtils.fileToString(config + "experiments/experimentProd.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experiment);
			expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
			expJson.put("enabled", false);
			experimentIDProd = experimentsRestApi.createExperiment(productID, expJson.toString(), sessionToken);
			softAssert.assertEquals(experimentIDProd.contains("Administrator or Product Lead role can add experiments and variants in the production stage") || isSecurityError(experimentIDProd), expectedFailure, "createExperimentInProd failed: " + experimentIDProd);
			if(expectedFailure){
				expJson = new JSONObject(experiment);
				expJson.put("enabled", false);

				experimentIDProd = experimentsRestApi.createExperiment(productID, expJson.toString(), adminToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("createExperimentInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void resetDashboardExperimentInProd(boolean expectedFailure) {
		System.out.println("Running resetDashboardExperimentInProd");
		try {
			String response = experimentsRestApi.resetDashboard(experimentIDProd,sessionToken);
			softAssert.assertEquals( isSecurityError(response), expectedFailure, "resetDashboard failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("resetDashboardExperimentInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void resetDashboardExperimentInDev(boolean expectedFailure) {
		System.out.println("Running resetDashboardExperimentInDev");
		try {
			String response = experimentsRestApi.resetDashboard(experimentID,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "resetDashboardExperimentInDev failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("resetDashboardExperimentInDev failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateExperiment(boolean expectedFailure){
		System.out.println("Running updateExperiment");
		try{
			String experiment = experimentsRestApi.getExperiment(experimentID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("description", "new description");
			String response = experimentsRestApi.updateExperiment(experimentID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateExperiment failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateExperimentInProduction(boolean expectedFailure){
		System.out.println("Running updateExperimentInProduction");
		try{
			String experiment = experimentsRestApi.getExperiment(experimentIDProd, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("description", "new description");
			String response = experimentsRestApi.updateExperiment(experimentIDProd, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update an experiment or variant in the production stage") || isSecurityError(response), expectedFailure, "updateExperimentInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateExperimentInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void reorderDevExp(boolean expectedFailure){
		System.out.println("Running reorderDevExp");
		try{
			String experimentText = FileUtils.fileToString(config + "experiments/experiment2.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experimentText);
			expJson.put("enabled", false);

			experimentID2 = experimentsRestApi.createExperiment(productID, expJson.toString(), adminToken);
			String experiment = experimentsRestApi.getAllExperiments(productID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			JSONArray experiments = json.getJSONArray("experiments");
			JSONObject exp0 = experiments.getJSONObject(0);
			experiments.set(0,experiments.getJSONObject(2));
			experiments.set(2,exp0);
			String response = experimentsRestApi.updateExperiments(productID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "reorderDevExp failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("reorderDevExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateExperimentToProd(boolean expectedFailure) {
		System.out.println("Running updateExperimentToProd");
		try {
			String experiment = experimentsRestApi.getExperiment(experimentID2, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("stage", "PRODUCTION");
			String id = experimentID2;
			experimentID2 = experimentsRestApi.updateExperiment(experimentID2, json.toString(), sessionToken);
			softAssert.assertEquals(experimentID2.contains("Only a user with the Administrator or Product Lead role can update an experiment or variant in the production stage") || experimentID2.contains("SecurityPolicyException"), expectedFailure, "updateExperimentToProd failed: " + experimentID2);
			if(expectedFailure) {
				experimentID2 = experimentsRestApi.updateExperiment(id, json.toString(), adminToken);
			}
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateExperimentToProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void reorderProdExp(boolean expectedFailure){
		System.out.println("Running reorderProdExp");
		try{
			String experiment = experimentsRestApi.getAllExperiments(productID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			JSONArray experiments = json.getJSONArray("experiments");
			JSONObject exp0 = experiments.getJSONObject(0);
			experiments.set(0,experiments.getJSONObject(1));
			experiments.set(1,exp0);
			String response = experimentsRestApi.updateExperiments(productID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change an experiments that are in the production stage") ||isSecurityError(response), expectedFailure, "reorderProdExp failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("reorderProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void deleteExperiment(boolean expectedFailure){
		System.out.println("Running deleteExperiment");
		try {
			int response = experimentsRestApi.deleteExperiment(experimentID, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteExperiment failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void deleteExperimentInProduction(boolean expectedFailure){
		System.out.println("Running deleteExperimentInProduction");
		try {
			int response = experimentsRestApi.deleteExperiment(experimentIDProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteExperimentInProduction failed: code " + response);
			String experiment = experimentsRestApi.getExperiment(experimentIDProd, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("stage", "DEVELOPMENT");
			experimentIDProd = experimentsRestApi.updateExperiment(experimentIDProd, json.toString(), sessionToken);
			experimentsRestApi.deleteExperiment(experimentIDProd, adminToken);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteExperimentInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getExperiment(boolean expectedFailure) {
		System.out.println("Running getExperiment");
		try {
			String response = experimentsRestApi.getExperiment(experimentID,sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getExperiment failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void getAllExperiments(boolean expectedFailure) {
		System.out.println("Running getAllExperiments");
		try {
			String response = experimentsRestApi.getAllExperiments(productID,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAllExperiments failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getAllExperiments failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getExperimentInputSample(boolean expectedFailure) {
		System.out.println("Running getExperimentInputSample");
		try {
			String response = experimentsRestApi.getInputSample(experimentID, "DEVELOPMENT", "2.5", sessionToken, "MAXIMAL", 0.7);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getExperimentInputSample failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getExperimentInputSample failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}



	public void getExperimentUtilitiesInfo(boolean expectedFailure) {
		System.out.println("Running getExperimentUtilitiesInfo");
		try {
			String response = experimentsRestApi.getUtilitiesInfo(experimentID, "DEVELOPMENT", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getExperimentUtilitiesInfo failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getExperimentUtilitiesInfo failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getExperimentGlobalDataCollection(boolean expectedFailure) {
		System.out.println("Running getExperimentGlobalDataCollection");
		try {
			String response = analyticsApi.getExperimentGlobalDataCollection(experimentID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getExperimentGlobalDataCollection failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getExperimentGlobalDataCollection failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void getExperimentAnalyticsQuota(boolean expectedFailure) {
		System.out.println("Running getExperimentAnalyticsQuota");
		try {
			String response = analyticsApi.getExperimentQuota(experimentID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getExperimentAnalyticsQuota failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getExperimentAnalyticsQuota failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void createBranch(boolean expectedFailure) {
		System.out.println("Running createBranch");
		try {
			String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(branch);
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			branchID = branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,sessionToken);
			Assert.assertEquals(isSecurityError(branchID), expectedFailure, "createBranch failed: " + experimentID);
			if(expectedFailure){
				branchID =  branchesRestApi.createBranch(seasonID,branch,BranchesRestApi.MASTER,adminToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createBranch failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateBranch(boolean expectedFailure) {
		System.out.println("Running updateBranch");
		try {
			String branch = branchesRestApi.getBranch(branchID,sessionToken);
			JSONObject json = new JSONObject(branch);
			json.put("description", "new description");
			String response = branchesRestApi.updateBranch(branchID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateBranch failed");
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateBranch failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteBranch(boolean expectedFailure) {
		System.out.println("Running deleteBranch");
		try {
			int response = branchesRestApi.deleteBranch(branchID, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteBranch failed: code " + response);
			if(expectedFailure){
				branchesRestApi.deleteBranch(branchID, adminToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteBranch failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getBranch(boolean expectedFailure) {
		System.out.println("Running getBranch");
		try {
			String response = branchesRestApi.getBranch(branchID,sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getBranch failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getBranch failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllBranches(boolean expectedFailure) {
		System.out.println("Running getAllBranches");
		try {
			String response = branchesRestApi.getAllBranches(seasonID,sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAllBranches failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getAllBranches failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createDevVariantInDevExp(boolean expectedFailure) {
		System.out.println("Running createDevVariantInDevExp");
		try {
			String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(branch);
			String branchName = RandomStringUtils.randomAlphabetic(5);
			json.put("name", branchName);
			branchID = branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,adminToken);
			String variant = FileUtils.fileToString(config + "experiments/variant1.txt", "UTF-8", false);
			JSONObject variantJson = new JSONObject(variant);
			variantJson.put("branchName", branchName);
			variantID = experimentsRestApi.createVariant(experimentID,variantJson.toString(),sessionToken);
			softAssert.assertEquals(isSecurityError(variantID), expectedFailure, "createDevVariantInDevExp failed: " + experimentID);
			if(expectedFailure){
				variantID = experimentsRestApi.createVariant(experimentID,variantJson.toString(),sessionToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("createDevVariantInDevExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createDevVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running createDevVariantInProdExp");
		try {
			String branch = FileUtils.fileToString(config + "experiments/branch3.txt", "UTF-8", false);
			JSONObject json = new JSONObject(branch);
			String branchName = RandomStringUtils.randomAlphabetic(5);
			json.put("name", branchName);
			branchIDDevInProd = branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,adminToken);
			String variant = FileUtils.fileToString(config + "experiments/variant3.txt", "UTF-8", false);
			JSONObject variantJson = new JSONObject(variant);
			variantJson.put("branchName", branchName);
			variantDevInProd = experimentsRestApi.createVariant(experimentIDProd,variantJson.toString(),sessionToken);
			softAssert.assertEquals(isSecurityError(variantDevInProd), expectedFailure, "createDevVariantInProdExp failed: " + experimentID);
			if(expectedFailure){
				variantDevInProd = experimentsRestApi.createVariant(experimentIDProd,variantJson.toString(),sessionToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("createDevVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createProdVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running createProdVariantInProdExp");
		try {
			String branch = FileUtils.fileToString(config + "experiments/branch2.txt", "UTF-8", false);
			JSONObject json = new JSONObject(branch);
			String branchName = RandomStringUtils.randomAlphabetic(5);
			json.put("name", branchName);

			branchIDProd = branchesRestApi.createBranch(seasonID,json.toString(),BranchesRestApi.MASTER,adminToken);
			String variant = FileUtils.fileToString(config + "experiments/variantProd.txt", "UTF-8", false);
			JSONObject variantJson = new JSONObject(variant);
			variantJson.put("branchName", branchName);
			variantProdInProd = experimentsRestApi.createVariant(experimentIDProd,variantJson.toString(),sessionToken);
			softAssert.assertEquals(variantProdInProd.contains("Administrator or Product Lead role can add experiments and variants in the production stage") || isSecurityError(variantProdInProd), expectedFailure, "createProdVariantInProdExp failed: " + experimentID);
			if(expectedFailure){
				variantProdInProd = experimentsRestApi.createVariant(experimentIDProd,variantJson.toString(),adminToken);
			}
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("createProdVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateDevVariantInDevExp(boolean expectedFailure) {
		System.out.println("Running updateDevVariantInDevExp");
		try {
			String variant = experimentsRestApi.getVariant(variantID,sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("description", "new description");
			String response = experimentsRestApi.updateVariant(variantID,json.toString(),sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateDevVariantInDevExp failed: " + variantID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateDevVariantInDevExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateDevVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running updateDevVariantInProdExp");
		try {
			String variant = experimentsRestApi.getVariant(variantDevInProd,sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("description", "new description");
			String response = experimentsRestApi.updateVariant(variantDevInProd,json.toString(),sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateDevVariantInProdExp failed: " + variantID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateDevVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateProdVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running updateProdVariantInProdExp");
		try {
			String variant = experimentsRestApi.getVariant(variantProdInProd,sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("description", "new description");
			String response = experimentsRestApi.updateVariant(variantProdInProd,json.toString(),sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update an experiment or variant in the production stage") || isSecurityError(response), expectedFailure, "updateProdVariantInProdExp failed: " + variantID);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateProdVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void deleteDevVariantInDevExp(boolean expectedFailure) {
		System.out.println("Running deleteDevVariantInDevExp");
		try {
			int response = experimentsRestApi.deleteVariant(variantID, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteDevVariantInDevExp failed: code " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteDevVariantInDevExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteDevVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running deleteDevVariantInProdExp");
		try {
			int response = experimentsRestApi.deleteVariant(variantDevInProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteDevVariantInProdExp failed: code " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteDevVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteProdVariantInProdExp(boolean expectedFailure) {
		System.out.println("Running deleteProdVariantInProdExp");
		try {
			int response = experimentsRestApi.deleteVariant(variantProdInProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteProdVariantInProdExp failed: code " + response);
			String variant = experimentsRestApi.getVariant(variantProdInProd,sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("stage", "DEVELOPMENT");
			variantProdInProd = experimentsRestApi.updateVariant(variantProdInProd,json.toString(),adminToken);
			experimentsRestApi.deleteVariant(variantProdInProd, adminToken);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteProdVariantInProdExp failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getVariant(boolean expectedFailure) {
		System.out.println("Running getVariant");
		try {
			String response = experimentsRestApi.getVariant(variantID,sessionToken);
			Assert.assertEquals(isSecurityError(response), expectedFailure, "getVariant failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getVariant failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	/****** TEST FUNCTIONS *******/

	public void setTestMails(boolean expectedFailure) {
		System.out.println("Running setTestMails");
		try {
			String response = testServicesApi.setTestMails(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setTestMails failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setTestMails failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getTestMails(boolean expectedFailure) {
		System.out.println("Running getTestMails");
		try {
			String response = testServicesApi.getTestMails(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getTestMails failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getTestMails failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteTestMails(boolean expectedFailure) {
		System.out.println("Running deleteTestMails");
		try {
			String response = testServicesApi.deleteTestMails(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "deleteTestMails failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setTestMails failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void setTestTranslation(boolean expectedFailure) {
		System.out.println("Running setTestTranslation");
		try {
			String response = testServicesApi.setTestTranslation(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setTestTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("setTestTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getTestTranslation(boolean expectedFailure) {
		System.out.println("Running getTestTranslation");
		try {
			String response = testServicesApi.getTestTranslation(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getTestTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getTestTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void wakeUpTranslation(boolean expectedFailure) {
		System.out.println("Running wakeUpTranslation");
		try {
			String response = testServicesApi.wakeUpTranslation(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "wakeUpTranslation failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("wakeUpTranslation failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void importSeason(boolean expectedFailure) {
		System.out.println("Running importSeason");
		try {
			String response = testServicesApi.importSeason(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "importSeason failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("importSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getServerVersion(boolean expectedFailure) {
		System.out.println("Running getServerVersion");
		try {
			String response = seasonApi.getServerVersion(seasonID, sessionToken);
			JSONObject json = new JSONObject(response);
			softAssert.assertEquals(isSecurityError(response) || !json.getString("serverVersion").equals(serverVersion), expectedFailure, "Incorrect server version: " + json.getString("serverVersion") );
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getServerVersion failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createStream(boolean expectedFailure){
		System.out.println("Running createStream");
		try {
			String stream = FileUtils.fileToString(config + "streams/stream1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(stream) ;
			json.put("name", RandomStringUtils.randomAlphabetic(6));
			streamID = streamsApi.createStream(seasonID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(streamID), expectedFailure, "createStream failed: "+ streamID);

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createStreamInProduction(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running createStreamInProduction");
		try {
			String stream = FileUtils.fileToString(config + "streams/stream1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(stream) ;
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(6));
			if(asAdmin) {
				streamID4TestProd = streamsApi.createStream(seasonID, json.toString(), adminToken);
			}else {
				streamID4TestProd = streamsApi.createStream(seasonID, json.toString(), sessionToken);
			}
			softAssert.assertEquals(streamID4TestProd.contains("Administrator or Product Lead role can add an item in the production stage") || isSecurityError(featureID4TestProd), expectedFailure, "createStreamInProduction failed: "+ streamID4TestProd);
			if(expectedFailure){
				streamID4TestProd = streamID;
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createStreamInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateStream(boolean expectedFailure){
		System.out.println("Running updateStream");
		try {

			String response = streamsApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(response);
			json.put("description", "new stream description");
			response = streamsApi.updateStream(streamID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateStream failed: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateStreamInProduction(boolean expectedFailure){
		System.out.println("Running updateStreamInProduction");
		try{
			if(streamID4TestProd == null || isSecurityError(streamID4TestProd) || streamID4TestProd.contains("Unable to add the feature")){
				createStreamInProduction(true,false);
			}
			String response = streamsApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(response);
			json.put("description", "new stream description");
			response = streamsApi.updateStream(streamID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update a stream that is in the production stage") || isSecurityError(response), expectedFailure, "updateStreamInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateFeatureInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void renameStreamInProduction(boolean expectedFailure){
		System.out.println("Running renameStreamInProduction");
		try{
			if(streamID4TestProd == null || isSecurityError(streamID4TestProd) || streamID4TestProd.contains("Unable to add the feature")){
				createStreamInProduction(true,false);
			}
			String response = streamsApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(response);
			json.put("name", "new stream name");
			response = streamsApi.updateStream(streamID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can update a stream that is in the production stage") || isSecurityError(response), expectedFailure, "updateStreamInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("renameStreamInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getStream(boolean expectedFailure){
		System.out.println("Running getStream");
		try {

			String response = streamsApi.getStream(streamID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getStream failed: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllStreams(boolean expectedFailure){
		System.out.println("Running getStream");
		try {

			String response = streamsApi.getAllStreams(seasonID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAllStreams failed: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getAllStreams failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteStream(boolean expectedFailure){
		System.out.println("Running deleteStream");
		try {

			int response = streamsApi.deleteStream(streamID, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteStream failed: code " + response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteStream failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	public void deleteStreamInProduction(boolean expectedFailure){
		System.out.println("Running deleteStreamInProduction");
		try {
			String stream = streamsApi.getStream(streamID4TestProd, sessionToken);
			int response = streamsApi.deleteStream(streamID4TestProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteStreamInProduction failed: code " + response);
			JSONObject json = new JSONObject(stream);
			json.put("stage", "DEVELOPMENT");
			streamID4TestProd = streamsApi.updateStream(streamID4TestProd, json.toString(), sessionToken);
			response = streamsApi.deleteStream(streamID4TestProd, sessionToken);
			softAssert.assertEquals(response == 200, expectedFailure, "deleteStreamInProduction failed after moved to development: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteStreamInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getStreamEvents(boolean expectedFailure){


		try {
			String response = streamsApi.getStreamEvent(seasonID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getStreamEvents failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getStreamEvents failed with exception:\n" +e.getLocalizedMessage());
			}
		}

	}

	public void updateStreamEvents(boolean expectedFailure){
		try {
			String events = FileUtils.fileToString(config + "streams/global_events.json", "UTF-8", false);		
			JSONObject newEvents = new JSONObject(events);
			String obj = streamsApi.getStreamEvent(seasonID, sessionToken);
			JSONObject streamEvent = new JSONObject(obj);
			streamEvent.put("events", newEvents.getJSONArray("events"));
			String response = streamsApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateStreamEvents failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("updateStreamEvents failed with exception:\n" +e.getLocalizedMessage());
			}
		}	
	}


	public void filterStreamEvents(boolean expectedFailure){
		try {
			String response = streamsApi.getStreamEventFields(seasonID, "event.name==\"module-viewed\"", sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "filterStreamEvents failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("filterStreamEvents failed with exception:\n" +e.getLocalizedMessage());
			}
		}		
	}



	public void addNotification(boolean expectedFailure){
		System.out.println("Running addNotification");
		try {

			String notification = FileUtils.fileToString(config + "notifications/notification1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(notification);
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			notificationID = notificationApi.createNotification(seasonID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(notificationID), expectedFailure, "addNotification failed: "+ notificationID);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addNotification failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateNotification(boolean expectedFailure){
		System.out.println("Running updateNotification");
		try {

			String notification = notificationApi.getNotification(notificationID, sessionToken);
			JSONObject json = new JSONObject(notification);
			json.put("description", "new descr");
			String response = notificationApi.updateNotification(notificationID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateNotification failed: "+ response);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateNotification failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateNotificationInProd(boolean expectedFailure){
		System.out.println("Running updateNotificationInProd");
		try {

			String notification = notificationApi.getNotification(notificationID4TestProd, sessionToken);
			JSONObject json = new JSONObject(notification);
			json.put("description", "new descr");
			String response = notificationApi.updateNotification(notificationID4TestProd, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateNotificationInProd failed: "+ response);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateNotificationInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addNotificationInProd(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running addNotificationInProd");
		try {

			String notification = FileUtils.fileToString(config + "notifications/notification1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(notification);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			if(asAdmin) {
				notificationID4TestProd = notificationApi.createNotification(seasonID, json.toString(), adminToken);
			}else {
				notificationID4TestProd = notificationApi.createNotification(seasonID, json.toString(), sessionToken);
			}

			softAssert.assertEquals(isSecurityError(notificationID4TestProd), expectedFailure, "addNotificationInProd failed: "+ notificationID4TestProd);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addNotificationInProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void changeNotificationsOrder(boolean expectedFailure){
		System.out.println("Running changeNotificationsOrder");
		try {

			//add second production notification
			String notification = FileUtils.fileToString(config + "notifications/notification1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(notification);
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String notifId = notificationApi.createNotification(seasonID, json.toString(), adminToken);
			softAssert.assertEquals(isSecurityError(notifId), expectedFailure, "changeNotificationsOrder failed adding second notification: "+ notifId);

			String allNotif = notificationApi.getAllNotifications(seasonID, sessionToken);
			JSONObject allNotifJson = new JSONObject(allNotif);
			JSONArray newNotifications = new JSONArray();
			newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(2));
			newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(1));
			newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(0));
			allNotifJson.put("notifications", newNotifications);		

			String response = notificationApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response)|| response.contains("error"), expectedFailure, "changeNotificationsOrder failed: " + response);


		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("changeNotificationsOrder failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addNotificationsLimitations(boolean expectedFailure){
		System.out.println("Running addNotificationsLimitations");
		try {

			//add notifications limitations

			String allNotif = notificationApi.getAllNotifications(seasonID, sessionToken);
			JSONObject allNotifJson = new JSONObject(allNotif);

			JSONObject limitation = new JSONObject();
			limitation.put("maxNotifications", 1);
			limitation.put("minInterval", 10);
			JSONArray allLimitations = new JSONArray();
			allLimitations.add(limitation);

			allNotifJson.put("notificationsLimitations", allLimitations);

			String response = notificationApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response)|| response.contains("error"), expectedFailure, "addNotificationsLimitations failed: " + response);


		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addNotificationsLimitations failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateNotificationsLimitations(boolean expectedFailure){
		System.out.println("Running updateNotificationsLimitations");
		try {

			//add notifications limitations

			String allNotif = notificationApi.getAllNotifications(seasonID, sessionToken);
			JSONObject allNotifJson = new JSONObject(allNotif);
			allNotifJson.getJSONArray("notificationsLimitations").getJSONObject(0).put("minInterval", 5);

			String response = notificationApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response)|| response.contains("error"), expectedFailure, "updateNotificationsLimitations failed: " + response);


		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateNotificationsLimitations failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}



	public void deleteNotification(boolean expectedFailure){
		System.out.println("Running deleteNotification");
		try {

			int response = notificationApi.deleteNotification(notificationID, sessionToken);
			softAssert.assertEquals(response, 200, "deleteNotification failed: "+ response);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("deleteNotification failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getNotification(boolean expectedFailure){
		System.out.println("Running getNotification");
		try {

			String response = notificationApi.getNotification(notificationID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getNotification failed: "+ response);

		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getNotification failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	//ordering rules
	public void createOrderingRule(boolean expectedFailure){
		System.out.println("Running createOrderingRule");
		try {

			String orderingRule = FileUtils.fileToString(config + "orderingRule/orderingRule1.txt", "UTF-8", false);
			JSONObject orderingRuleJson = new JSONObject(orderingRule);
			orderingRuleJson.put("name", "or"+RandomStringUtils.randomAlphabetic(5));
			orderingRuleJson.put("minAppVersion", "0.1");
			orderingRuleID4Test = featureApi.addFeature(seasonID, orderingRuleJson.toString(), featureID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(orderingRuleID4Test), expectedFailure, "createOrderingRule failed: "+ orderingRuleID4Test);
			if(expectedFailure){
				orderingRuleID4Test = orderingRuleID;
			}

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createOrderingRule failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateOrderingRuleToProd(boolean expectedFailure){
		System.out.println("Running updateOrderingRuleToProd");
		try {

			String orderingRule = featureApi.getFeature(orderingRuleID4Test, sessionToken);
			JSONObject json = new JSONObject(orderingRule);
			json.put("stage", "PRODUCTION");
			String response = featureApi.updateFeature(seasonID, orderingRuleID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateOrderingRuleToProd failed: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("updateOrderingRuleToProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	private void getCapabilities(boolean expectedFailure){
		try{
			String response = operationApi.getCapabilities(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getCapabilities failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getCapabilities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	private void getSeasonCapabilities(boolean expectedFailure){
		try{
			String response = operationApi.getSeasonCapabilities(seasonID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getSeasonCapabilities failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getCapabilities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	private void setCapabilities(boolean expectedFailure) {
		try{
			String capabilities = operationApi.getCapabilities(sessionToken);
			String response = operationApi.setCapabilities(capabilities, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "setCapabilities failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("setCapabilities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	private void getEncryptionKey(boolean expectedFailure){
		try{
			String response = seasonApi.getEncryptionKey(seasonID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getEncryptionKey failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("getEncryptionKey failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	private void resetEncryptionKey(boolean expectedFailure) {
		try{
			String response = seasonApi.resetEncryptionKey(seasonID4Test, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "resetEncryptionKey failed: "+ response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("resetEncryptionKey failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	// webhooks
	public void getWebhooks(boolean expectedFailure) {
		System.out.println("Running getWebhooks");
		try {
			String response = operationApi.getWebhooks(sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getWebhooks failed: " + response);
		} catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("getWebhooks failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void createWebhook(boolean expectedFailure){
		System.out.println("Running createWebhook");
		try {

			String webhook = FileUtils.fileToString(config + "webhooks/webhook1.txt", "UTF-8", false);
			JSONObject webhookJson = new JSONObject(webhook);
			webhookJson.put("name", "wh"+RandomStringUtils.randomAlphabetic(5));
			webhookID4Test = operationApi.addWebhook(webhookJson.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(webhookID4Test), expectedFailure, "createWebhook failed: "+ webhookID4Test);
			if(expectedFailure){
				webhookID4Test = webhookID;
			}

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createWebhook failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateWebhook(boolean expectedFailure){
		System.out.println("Running updateWebhook");
		try {

			String webhook = FileUtils.fileToString(config + "webhooks/webhook1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(webhook);
			json.put("minStage", "DEVELOPMENT");
			String response = operationApi.updateWebhook(webhookID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateWebhook failed: "+ response);

		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("updateWebhook failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteWebhook(boolean expectedFailure){
		System.out.println("Running deleteWebhook");
		try {
			int response = operationApi.deleteWebhook(webhookID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteSeason failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("deleteWebhook failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID4Test, sessionToken);
	}


	//************** PURCHASES FUNCTIONS **************//
	public void createInAppPurchase(boolean expectedFailure){
		System.out.println("Running createInAppPurchase");
		try {
			String inAppPurchase = FileUtils.fileToString(config + "purchases/inAppPurchase1.txt", "UTF-8", false);
			JSONObject inAppPurchaseJson = new JSONObject(inAppPurchase);
			inAppPurchaseJson.put("name", "inAppPurchase"+RandomStringUtils.randomAlphabetic(5));

			inAppPurchaseID4Test = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
			Assert.assertEquals(isSecurityError(inAppPurchaseID4Test), expectedFailure, "createInAppPurchase failed: "+ inAppPurchaseID4Test);
			if(expectedFailure){
				inAppPurchaseID4Test = inAppPurchaseID;
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createInAppPurchase failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	public void createInAppPurchaseInProduction(boolean asAdmin,boolean expectedFailure){
		System.out.println("Running createInAppPurchaseInProduction");
		try {
			String inAppPurchase = FileUtils.fileToString(config + "purchases/inAppPurchaseProduction.txt", "UTF-8", false);
			JSONObject inAppPurchaseJson = new JSONObject(inAppPurchase);
			inAppPurchaseJson.put("name", "inAppPurchase in production."+RandomStringUtils.randomAlphabetic(5));
			if(asAdmin) {
				inAppPurchaseID4TestProd = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), "ROOT", adminToken);
			}else {
				inAppPurchaseID4TestProd = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
			}
			Assert.assertEquals(inAppPurchaseID4TestProd.contains("Administrator or Product Lead role can add an item in the production stage") || isSecurityError(inAppPurchaseID4TestProd), expectedFailure, "createInAppPurchaseInProduction failed: "+ inAppPurchaseID4TestProd);
			if(expectedFailure == false){
				String configuration = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
				JSONObject jsonConfig = new JSONObject(configuration);
				JSONObject newConfiguration = new JSONObject();
				newConfiguration.put("color", "red");
				jsonConfig.put("configuration", newConfiguration);
				purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), inAppPurchaseID4TestProd, adminToken);
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createInAppPurchaseInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getAllPurchaseItemsForSeason(boolean expectedFailure){
		System.out.println("Running getAllPurchaseItemsForSeason");
		try{
			JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID, sessionToken);
			softAssert.assertEquals(inAppPurchases.contains("SecurityPolicyException"), expectedFailure, "getAllPurchaseItemsForSeason failed: " + seasonID);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("getAllPurchaseItemsForSeason failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void getInAppPurchase(boolean expectedFailure){
		System.out.println("Running getInAppPurchase");
		try {
			String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID4Test, sessionToken);
			Assert.assertEquals(isSecurityError(inAppPurchase), expectedFailure, "getInAppPurchase failed: " + inAppPurchase);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getInAppPurchase failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInAppPurchase(boolean expectedFailure){
		System.out.println("Running updateInAppPurchase");
		try{
			String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID4Test, sessionToken);
			JSONObject json = new JSONObject(inAppPurchase);
			json.put("description", "new description");
			String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateInAppPurchase failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateInAppPurchase failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInAppPurchaseInProduction(boolean expectedFailure){
		System.out.println("Running updateInAppPurchaseInProduction");
		try{
			if(inAppPurchaseID4TestProd == null || isSecurityError(inAppPurchaseID4TestProd) || inAppPurchaseID4TestProd.contains("Unable to add the inAppPurchase")){
				createInAppPurchaseInProduction(true,false);
			}
			String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID4TestProd, sessionToken);
			JSONObject json = new JSONObject(inAppPurchase);
			json.put("description", "new description");
			String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID4TestProd, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") || isSecurityError(response), expectedFailure, "updateInAppPurchaseInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateInAppPurchaseInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteInAppPurchase(boolean expectedFailure){
		System.out.println("Running deleteInAppPurchase");
		try {
			int response = purchasesApi.deletePurchaseItem(inAppPurchaseID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteInAppPurchase failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteInAppPurchase failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deleteInAppPurchaseInProduction(boolean expectedFailure){
		System.out.println("Running deleteInAppPurchaseInProduction");
		try {
			String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID4TestProd, sessionToken);
			int response = purchasesApi.deletePurchaseItem(inAppPurchaseID4TestProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deleteInAppPurchaseInProduction failed: code " + response);
			JSONObject json = new JSONObject(inAppPurchase);
			json.put("stage", "DEVELOPMENT");
			inAppPurchaseID4TestProd = purchasesApi.updatePurchaseItem(seasonID,inAppPurchaseID4TestProd, json.toString(), adminToken);
			purchasesApi.deletePurchaseItem(inAppPurchaseID4TestProd, adminToken);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deleteInAppPurchaseInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updateInAppPurcahseToProd(boolean expectedFailure) {
		System.out.println("Running updateInAppPurcahseToProd");
		try {
			String inAppPurchaseText = FileUtils.fileToString(config + "purchases/inAppPurchase2.txt", "UTF-8", false);
			String currInAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseText, "ROOT", adminToken);
			String inAppPurchase = purchasesApi.getPurchaseItem(currInAppPurchaseID, sessionToken);
			JSONObject json = new JSONObject(inAppPurchase);
			json.put("stage", "PRODUCTION");
			String response = purchasesApi.updatePurchaseItem(seasonID, currInAppPurchaseID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead can change an inAppPurchase from the development to the production") || isSecurityError(response), expectedFailure, "updateInAppPurchaseToProd failed: " + inAppPurchaseID);
			inAppPurchase = purchasesApi.getPurchaseItem(currInAppPurchaseID, sessionToken);
			json = new JSONObject(inAppPurchase);
			json.put("stage", "DEVELOPMENT");
			currInAppPurchaseID = purchasesApi.updatePurchaseItem(seasonID, currInAppPurchaseID, json.toString(), adminToken);
			purchasesApi.deletePurchaseItem(currInAppPurchaseID, adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updateInAppPurcahseToProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	
	//purchase options
	public void createPurchaseOptions(boolean expectedFailure){
		System.out.println("Running createPurchaseOptions");
		try {
			String purchaseOptions = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);
			JSONObject purchaseOptionsJson = new JSONObject(purchaseOptions);
			purchaseOptionsJson.put("name", "purchaseOptions"+RandomStringUtils.randomAlphabetic(5));

			purchaseOptionsID4Test = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsJson.toString(), inAppPurchaseID, sessionToken);
			Assert.assertEquals(isSecurityError(purchaseOptionsID4Test), expectedFailure, "createPurchaseOptions failed: "+ purchaseOptionsID4Test);
			if(expectedFailure){
				purchaseOptionsID4Test = purchaseOptionsID;
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createPurchaseOptions failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void createPurchaseOptionsInProduction(boolean asAdmin,boolean expectedFailure){
		System.out.println("Running createPurchaseOptionsInProduction");
		try {
			String purchaseOptions = FileUtils.fileToString(config + "purchases/purchaseOptionsProduction.txt", "UTF-8", false);
			JSONObject purchaseOptionsJson = new JSONObject(purchaseOptions);
			purchaseOptionsJson.put("name", "purchaseOptions in production."+RandomStringUtils.randomAlphabetic(5));
			if(asAdmin) {
				purchaseOptionsID4TestProd = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsJson.toString(), inAppPurchaseID4Test, adminToken);
			}else {
				purchaseOptionsID4TestProd = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsJson.toString(), inAppPurchaseID4Test, sessionToken);
			}
			Assert.assertEquals(purchaseOptionsID4TestProd.contains("Administrator or Product Lead role can add an item in the production stage") || isSecurityError(purchaseOptionsID4TestProd), expectedFailure, "createPurchaseOptionsInProduction failed: "+ purchaseOptionsID4TestProd);
			if(expectedFailure == false){
				String configuration = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
				JSONObject jsonConfig = new JSONObject(configuration);
				JSONObject newConfiguration = new JSONObject();
				newConfiguration.put("color", "red");
				jsonConfig.put("configuration", newConfiguration);
				purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), purchaseOptionsID4TestProd, adminToken);
			}
		}catch (Exception e) {
			if (expectedFailure == false) {
				Assert.fail("createPurchaseOptionsInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updatePurchaseOptions(boolean expectedFailure){
		System.out.println("Running updatePurchaseOptions");
		try{
			String purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID4Test, sessionToken);
			JSONObject json = new JSONObject(purchaseOptions);
			json.put("description", "new description");
			String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID4Test, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updatePurchaseOptions failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updatePurchaseOptions failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updatePurchaseOptionsInProduction(boolean expectedFailure){
		System.out.println("Running updatePurchaseOptionsInProduction");
		try{
			if(purchaseOptionsID4TestProd == null || isSecurityError(purchaseOptionsID4TestProd) || purchaseOptionsID4TestProd.contains("Unable to add the purchaseOptions")){
				createPurchaseOptionsInProduction(true,false);
			}
			String purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID4TestProd, sessionToken);
			JSONObject json = new JSONObject(purchaseOptions);
			json.put("description", "new description");
			String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID4TestProd, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead role can change a subitem that is in the production stage") || isSecurityError(response), expectedFailure, "updatePurchaseOptionsInProduction failed");
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updatePurchaseOptionsInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deletePurchaseOptions(boolean expectedFailure){
		System.out.println("Running deletePurchaseOptions");
		try {
			int response = purchasesApi.deletePurchaseItem(purchaseOptionsID4Test, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deletePurchaseOptionse failed: code " + response);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deletePurchaseOptions failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void deletePurchaseOptionsInProduction(boolean expectedFailure){
		System.out.println("Running deletePurchaseOptionsInProduction");
		try {
			String purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID4TestProd, sessionToken);
			int response = purchasesApi.deletePurchaseItem(purchaseOptionsID4TestProd, sessionToken);
			softAssert.assertEquals(response != 200, expectedFailure, "deletePurchaseOptionsInProduction failed: code " + response);
			JSONObject json = new JSONObject(purchaseOptions);
			json.put("stage", "DEVELOPMENT");
			purchaseOptionsID4TestProd = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID4TestProd, json.toString(), adminToken);
			purchasesApi.deletePurchaseItem(purchaseOptionsID4TestProd, adminToken);
		}catch (Exception e) {
			if (expectedFailure == false) {
				softAssert.fail("deletePurchaseOptionsInProduction failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void updatePurchaseOptionsToProd(boolean expectedFailure) {
		System.out.println("Running updatePurchaseOptionsToProd");
		try {
			String purchaseOptionsText = FileUtils.fileToString(config + "purchases/purchaseOptions2.txt", "UTF-8", false);
			String currPurchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsText, "ROOT", adminToken);
			String purchaseOptions = purchasesApi.getPurchaseItem(currPurchaseOptionsID, sessionToken);
			JSONObject json = new JSONObject(purchaseOptions);
			json.put("stage", "PRODUCTION");
			String response = purchasesApi.updatePurchaseItem(seasonID, currPurchaseOptionsID, json.toString(), sessionToken);
			softAssert.assertEquals(response.contains("Administrator or Product Lead can change an purchaseOptions from the development to the production") || isSecurityError(response), expectedFailure, "updatePurchaseOptionsToProd failed: " + purchaseOptionsID);
			purchaseOptions = purchasesApi.getPurchaseItem(currPurchaseOptionsID, sessionToken);
			json = new JSONObject(purchaseOptions);
			json.put("stage", "DEVELOPMENT");
			currPurchaseOptionsID = purchasesApi.updatePurchaseItem(seasonID, currPurchaseOptionsID, json.toString(), adminToken);
			purchasesApi.deletePurchaseItem(currPurchaseOptionsID, adminToken);
		}catch (Exception e){
			if(expectedFailure == false){
				softAssert.fail("updatePurchaseOptionsToProd failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//entities
	public void getEntity(boolean expectedFailure){
		System.out.println("Running getEntity");
		try {
			String response = entitiesApi.getEntity(entityID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getEntity failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getEntity failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getAttribute(boolean expectedFailure){
		System.out.println("Running getAttribute");
		try {
			String response = entitiesApi.getAttribute(attributeID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAttribute failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getAttribute failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getAttributes(boolean expectedFailure){
		System.out.println("Running getAttributes");
		try {
			String response = entitiesApi.getAttributes(entityID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAttributes failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getAttributes failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getAttributeType(boolean expectedFailure){
		System.out.println("Running getAttributeType");
		try {
			String response = entitiesApi.getAttribute(attributeTypeID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAttributeType failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getAttributeType failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getAttributeTypes(boolean expectedFailure){
		System.out.println("Running getAttributeTypes");
		try {
			String response = entitiesApi.getAttributes(entityID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getAttributeTypes failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getAttributeTypes failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getProductEntities(boolean expectedFailure){
		System.out.println("Running getProductEntities");
		try {
			String response = entitiesApi.getProductEntities(productID, sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "getEntity failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("getProductEntities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void deleteEntity(boolean expectedFailure){
		System.out.println("Running deleteEntity");
		try {
			int response = entitiesApi.deleteEntity(entityID, sessionToken);
			softAssert.assertEquals(response, 200, "deleteEntity failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("deleteEntity failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void deleteAttribute(boolean expectedFailure){
		System.out.println("Running deleteAttribute");
		try {
			int response = entitiesApi.deleteAttribute(attributeID, sessionToken);
			softAssert.assertEquals(response, 200, "deleteAttribute failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("deleteAttribute failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void deleteAttributeType(boolean expectedFailure){
		System.out.println("Running deleteAttribute");
		try {
			int response = entitiesApi.deleteAttributeType(attributeTypeID, sessionToken);
			softAssert.assertEquals(response, 200, "deleteAttributeType failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("deleteAttributeType failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void addEntity(boolean expectedFailure){
		System.out.println("Running addEntity");
		try {
			String entity = FileUtils.fileToString(config + "airlytics/entities/entity1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(entity);
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			entityID = entitiesApi.createEntity(productID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(entityID), expectedFailure, "addEntity failed: "+ entityID);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addEntity failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	public void addAttributeType(boolean expectedFailure){
		System.out.println("Running addAttributeType");
		try {
			String attType = FileUtils.fileToString(config + "airlytics/entities/attributeType1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(attType);
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			attributeTypeID = entitiesApi.createAttributeType(entityID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(attributeTypeID), expectedFailure, "addAttributeType failed: "+ attributeTypeID);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addAttributeType failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void addAttribute(boolean expectedFailure){
		System.out.println("Running addAttribute");
		try {
			String att = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(att);
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			json.put("attributeTypeID", attributeTypeID);
			attributeID = entitiesApi.createAttribute(entityID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(attributeID), expectedFailure, "addAttribute failed: "+ attributeID);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("addAttribute failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void updateEntity(boolean expectedFailure){
		System.out.println("Running updateEntity");
		try {
			String entity = entitiesApi.getEntity(entityID, sessionToken);
			JSONObject json = new JSONObject(entity);
			json.put("description", "new descr");
			String response = entitiesApi.updateEntity(entityID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateEntity failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateEntity failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	/*
	public void updateEntities(boolean expectedFailure){
		System.out.println("Running updateEntities");
		try {
			String entity = entitiesApi.getProductEntities(productID, sessionToken);
			JSONObject json = new JSONObject(entity);
			json.put("dbSchema", "airlock_test");
			String response = entitiesApi.updateEntities(productID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateEntities failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateEntities failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	*/
	public void updateAttribute(boolean expectedFailure){
		System.out.println("Running updateAttribute");
		try {
			String att = entitiesApi.getAttribute(attributeID, sessionToken);
			JSONObject json = new JSONObject(att);
			json.put("description", "new descr");
			String response = entitiesApi.updateAttribute(attributeID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateAttribute failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateAttribute failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void updateAttributeType(boolean expectedFailure){
		System.out.println("Running updateAttributeType");
		try {
			String attType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
			JSONObject json = new JSONObject(attType);
			json.put("description", "new descr");
			String response = entitiesApi.updateAttributeType(attributeTypeID, json.toString(), sessionToken);
			softAssert.assertEquals(isSecurityError(response), expectedFailure, "updateAttributeType failed: "+ response);
		}catch(Exception e){
			if (expectedFailure == false) {
				softAssert.fail("updateAttributeType failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

}
