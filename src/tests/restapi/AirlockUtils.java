package tests.restapi;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import com.ibm.airlock.admin.Utilities;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.testdriver.ConnectToSso;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class AirlockUtils {

	String productID;
	String seasonID;
	String featureID;
	String inAppPurchaseID;
	String m_url;
	String m_analyticsUrl;
	String m_translationsUrl;
	String m_configPath;
	public String sessionToken = "";
	String productsToDelete;
	ProductsRestApi p;
	FeaturesRestApi f;
	BranchesRestApi br ;
	UtilitiesRestApi u;
	SeasonsRestApi s;
	InputSchemaRestApi schema;
	ExperimentsRestApi exp;
	StringsRestApi stringsApi;
	OperationRestApi operationsApi;
	TranslationsRestApi translationsApi;
	InAppPurchasesRestApi purchasesApi;
	String m_operUrl;

	public AirlockUtils(String url){
		m_url = url;
	}
	
	public AirlockUtils(String url, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile){
		m_url = url;
		m_configPath = configPath;
		m_operUrl = m_url.replace("airlock/api/admin", "airlock/api/ops");

		productsToDelete = productsToDeleteFile;
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		operationsApi = new OperationRestApi();
		operationsApi.setURL(m_operUrl);

		sessionToken = getJWTToken(userName, userPassword, appName);
		
	}
	
	public AirlockUtils(String url, String analyticsUrl, String translationUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		m_translationsUrl = translationUrl;
		m_operUrl = m_url.replace("airlock/api/admin", "airlock/api/ops");
		m_configPath = configPath;
		
		productsToDelete = productsToDeleteFile;
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		f = new FeaturesRestApi();
		f.setURL(url);
		br = new BranchesRestApi();
		br.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		translationsApi = new TranslationsRestApi();
		translationsApi.setURL(m_translationsUrl);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(m_translationsUrl);
		operationsApi = new OperationRestApi();
		operationsApi.setURL(m_operUrl);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
		
		sessionToken = getJWTToken(userName, userPassword, appName);

	}
	
	public void setSessionToken(String token){
		sessionToken = token;
	}
	
	public void reset(String productID, String sessionToken){
		if (productID != null)

			p.deleteProduct(productID, sessionToken);
	}
	
		
	public String createProduct() throws IOException{
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(m_url);
		String product = FileUtils.fileToString(m_configPath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProduct(product, sessionToken);
			
		return productID;
	}
	
	public String createProductCopyGlobalAdmins() throws IOException{
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(m_url);
		String product = FileUtils.fileToString(m_configPath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProductCopyGlobalAdmins(product, sessionToken);
			
		return productID;
	}
	
	public String createProductWithSpecifiedName(String name) throws IOException, JSONException{
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(m_url);
		String product = FileUtils.fileToString(m_configPath + "product1.txt", "UTF-8", false);
		JSONObject prodObj = new JSONObject(product);
		prodObj.put("name", name);
		prodObj.put("codeIdentifier", name);
		productID = p.addProduct(prodObj.toString(), sessionToken);
			
		return productID;
	}
	
	public String createProductWithoutAddingUserGroups() throws IOException{
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(m_url);
		String product = FileUtils.fileToString(m_configPath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProductWithoutAddingUserGroups(product, sessionToken);
		return productID;
	}
	
	public String createSeason(String product) throws IOException{
		productID = product;
		SeasonsRestApi s = new SeasonsRestApi();
		s.setURL(m_url);		
		String season = FileUtils.fileToString(m_configPath + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		return seasonID;
	}
	
	public String createSeason(String product, String minVersion) throws IOException, JSONException{
		productID = product;
		SeasonsRestApi s = new SeasonsRestApi();
		s.setURL(m_url);		
		String season = FileUtils.fileToString(m_configPath + "season1.txt", "UTF-8", false);
		JSONObject seasonObj = new JSONObject(season);
		seasonObj.put("minVersion", minVersion);
		seasonID = s.addSeason(productID, seasonObj.toString(), sessionToken);
		return seasonID;
	}

	public String createBranchInExperiment(String analyticsUrl) throws Exception{
		return createBranchInEnabledExperiment(analyticsUrl, false);
		/*String experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5),analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String variantID = addVariant(experimentID,"variant1","branch1",analyticsUrl, false);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

		return branchID;*/
	}
	
	public String createBranchInEnabledExperiment(String analyticsUrl, boolean enabled) throws Exception{
		String experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5),analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String variantID = addVariant(experimentID,"variant1","branch1",analyticsUrl, false);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

		if (enabled) {
			JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
			experiment.put("enabled", true);
			String response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment was not updated");
		}
		return branchID;
	}
	public JSONObject createBranchInProdExperiment(String analyticsUrl) throws Exception{
		return createBranchInProdEnabledExperiment(analyticsUrl, false);
		/*String experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5),analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String variantID = addVariant(experimentID,"variantProd","branch1",analyticsUrl, true);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

		JSONObject exp = new JSONObject();
		exp.put("expId",experimentID);
		exp.put("varId",variantID);
		exp.put("brId",branchID);
		return exp;*/
	}
	
	public JSONObject createBranchInProdEnabledExperiment(String analyticsUrl, boolean enabled) throws Exception{
		String experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5),analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String variantID = addVariant(experimentID,"variantProd","branch1",analyticsUrl, true);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

		if (enabled) {
			JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
			experiment.put("enabled", true);
			String response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment was not updated");

		}
		JSONObject exp = new JSONObject();
		exp.put("expId",experimentID);
		exp.put("varId",variantID);
		exp.put("brId",branchID);
		return exp;
	}

	public String addExperiment(String experimentName, String analyticsUrl, boolean production, boolean enabled) throws IOException, JSONException {
		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", enabled);
		
		if(production)
			expJson.put("stage", "PRODUCTION");
		return exp.createExperiment(productID, expJson.toString(), sessionToken);

	}
	
	public String addExperiment(String analyticsUrl, boolean production, boolean enabled) throws IOException, JSONException {
		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		
		if(production) {
			expJson.put("stage", "PRODUCTION");
		}
		
		if (enabled)
			expJson.put("enabled", true);
		else
			expJson.put("enabled", false);
		
		return exp.createExperiment(productID, expJson.toString(), sessionToken);

	}

	public String addBranch(String branchName) throws JSONException, IOException{
		return addBranchFromBranch(branchName, BranchesRestApi.MASTER,seasonID);
		/*
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);
		String branch = FileUtils.fileToString(m_configPath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
*/
	}
	
	public String addBranchFromBranch(String branchName, String branchId ,String seasonID) throws JSONException, IOException{
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);
		String branch = FileUtils.fileToString(m_configPath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), branchId, sessionToken);
	}

	public String addVariant(String experimentID, String variantName,String branchName, String analyticsUrl, boolean production) throws IOException, JSONException{
		ExperimentsRestApi exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		String variant = FileUtils.fileToString(m_configPath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		if(production)
			variantJson.put("stage", "PRODUCTION");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);
	}
	
	public String createFeature(String season) throws IOException {
		seasonID = season;
		FeaturesRestApi f = new FeaturesRestApi();
		f.setURL(m_url);
		String feature = FileUtils.fileToString(m_configPath + "feature1.txt", "UTF-8", false);
		feature = JSONUtils.generateUniqueString(feature, 8, "name");
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		return featureID;

	}

	public String createInAppPurchase(String season) throws IOException {
		seasonID = season;
		String pi = FileUtils.fileToString(m_configPath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		pi = JSONUtils.generateUniqueString(pi, 8, "name");
		inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, pi, "ROOT", sessionToken);
		return inAppPurchaseID;
	}

	
	public void createSchema(String seasonID) throws Exception {			
			String file = FileUtils.fileToString(m_configPath + "inputSchema.txt", "UTF-8", false);
			JSONObject is = new JSONObject(file);
			
			InputSchemaRestApi schema = new InputSchemaRestApi();
			schema.setURL(m_url);
			
			String schemaResponse = schema.getInputSchema(seasonID, sessionToken);
			JSONObject jsonSchema = new JSONObject(schemaResponse);
			jsonSchema.put("inputSchema", is);
			schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	}
	
	public String createBranch(String seasonID) throws IOException {
		String branch = FileUtils.fileToString(m_configPath + "experiments/branch1.txt", "UTF-8", false);
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);

		String branchID = br.createBranch(seasonID, branch, "MASTER", sessionToken);
		return branchID;

	}
	
	
	public String getDefaults(String seasonID) {
		String defaults = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/defaults", sessionToken);
			defaults = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return defaults;
	}
	
	public String getConstants(String seasonID, String platform) {
		String constants = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/constants?platform="+platform, sessionToken);
			constants = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return constants;
	}
	
	public void createUtility(String seasonID) throws Exception {			
		String utility = FileUtils.fileToString(m_configPath + "javascriptUtilities_Q1_2017.txt", "UTF-8", false);

		Properties utilProps = new Properties();
		utilProps.setProperty("stage", "PRODUCTION");
		utilProps.setProperty("minAppVersion", "1.0");
		utilProps.setProperty("utility", utility);
		UtilitiesRestApi u = new UtilitiesRestApi();
		u.addUtility(seasonID, utilProps, sessionToken);

}
	
	public void createUserGroups() throws Exception{
		
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/usergroups", sessionToken);
		String userGroups = res.message;
		JSONObject jsonGroups = new JSONObject(userGroups);
		JSONArray groups = jsonGroups.getJSONArray("internalUserGroups");
		boolean qa = false;
		boolean dev = false;
		for (int i = 0; i < groups.size(); i++){
			if (groups.get(i).equals("QA"))
				qa = true;
			else if (groups.get(i).equals("DEV"))
				dev = true;
		}
		
		if (qa && dev)
			return;
		
		if (!qa)
			groups.put("QA");
		if (!dev)
			groups.put("DEV");
		
		JSONObject json = new JSONObject(userGroups);
		json.remove("internalUserGroups");
		json.put("internalUserGroups", groups);
		RestClientUtils.sendPut(m_url + "/usergroups", json.toString(), sessionToken);
	}
	
	public void printProductToFile(String productID){
			try {
					FileWriter fw = new FileWriter(productsToDelete, true);
				    BufferedWriter bw = new BufferedWriter(fw);
				    PrintWriter out = new PrintWriter(bw);
				
				    out.println(productID);
				    out.close();

				} catch (IOException e) {
				    System.out.println("cannot write productID to file: " + e.getLocalizedMessage());
				}
		}

	public String getOktaToken(String userName,String password){
		try{
			JSONObject credential = new JSONObject();
			credential.put("username",userName);
			credential.put("password",password);
			RestClientUtils.RestCallResults results = RestClientUtils.sendPostJson("https://weather.oktapreview.com/api/v1/authn",credential.toString(),null);
			JSONObject jsonResult = new JSONObject(results.message);
			String oktaToken = jsonResult.getString("sessionToken");
			return oktaToken;
		}catch (Exception e){
			return null;
		}
	}
	
	public String getAzureToken(String userName,String password) throws IOException{
	
		final String AUTHORITY_URL = "https://login.microsoftonline.com/";
		//final String REDIRECT_URL_POSTFIX = "admin/authentication/ad/login/";
		
		String azureClientId = "";
		String azureTenant = "";


		//URI myUri = uri.getBaseUri();
		//String redirectUrl = "http://localhost:9090/airloc/" + REDIRECT_URL_POSTFIX;
		
		String urlString = AUTHORITY_URL + azureTenant + "/oauth2/token?";
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
	
		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
	
		// Send post request
		con.setDoOutput(true);
		HashMap<String, String> reqBodyParams = new HashMap<String, String>();
		reqBodyParams.put("grant_type", "password");
		reqBodyParams.put("client_id", azureClientId);
		reqBodyParams.put("resource", azureClientId);
		reqBodyParams.put("username", userName); 
		reqBodyParams.put("password", "");
		reqBodyParams.put("scope", "openid");
		
				
		String reqBody = getDataString(reqBodyParams);
		con.getOutputStream().write(reqBody.getBytes("UTF-8"));
		
		int responseCode = con.getResponseCode();

		InputStream inp;
		boolean err = false;
		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null) {
			inp = con.getInputStream(); 
		} 
		else {
			inp = con.getErrorStream();
			err = true;
		}
		
		if (err) {
			String errMsg;
			if (inp == null)
				errMsg = "Response Code : " + responseCode;
			else
				errMsg = Utilities.streamToString(inp);
			
			throw new IOException("Failed getting Azure token from Azure authentication code:" + errMsg); 
		}
		
		String out;
		if (inp == null)
			throw new IOException("Failed getting Azure token from Azure authentication code. No token returned.");
		else
		{
			out = Utilities.streamToString(inp);
			
			try {
				JSONObject tokenJSON = new JSONObject(out);
				return tokenJSON.getString("access_token");
			} catch (JSONException e) {
				throw new IOException("Failed getting Azure token from Azure authentication code. Illegal token structure:" + e.getMessage());
			}
			
		}
	}

	public String setNewJWTTokenUsingBluemix(String userName,String password,String ssoConfigPath,String stage){
		try {
			ConnectToSso sso = new ConnectToSso(ssoConfigPath);
			String ssoJwt = sso.getJWT(userName, password);

			String url = m_url + "/authentication/sso?stage=" + stage;
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, ssoJwt);
			if (res.code != 200)
				throw new Exception(res.message);
			String jwt = res.message;
			return jwt;
		}catch (Exception e){
			return null;
		}
	}
	
	//if appName is null or empty - get token from azure, otherwise - get token from okta
	public String setNewJWTToken(String userName,String password,String appName){
		try {
			if (appName != null && !appName.isEmpty()) {
				String OktaToken = getOktaToken(userName, password);
				RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/authentication/oktaurl/"+appName);
				String OktaUrl = res.message;
				res = RestClientUtils.sendGet(OktaUrl + "&sessionToken=" + OktaToken);
				Reader stringReader = new StringReader(res.message);
				HTMLEditorKit htmlKit = new HTMLEditorKit();
				HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
				htmlDoc.putProperty("IgnoreCharsetDirective", new Boolean(true));
				htmlKit.read(stringReader, htmlDoc, 0);
				AttributeSet set = htmlDoc.getElement("appForm").getElement(0).getElement(0).getAttributes();
				Enumeration attributes = set.getAttributeNames();
				String saml = "";
				while ((attributes.hasMoreElements())) {
					Object attr = attributes.nextElement();
					if (attr.toString().equals("value")) {
						saml = set.getAttribute(attr).toString().replace("+", "%2B").replace("=", "%3D");
						break;
					}
					;
				}
				String samlForm = "SAMLResponse=" + saml;
				res = RestClientUtils.sendPost(m_url+"/authentication/login", samlForm);
				if (res.code != 200)
					throw new Exception(res.message);
				String jwt = res.message;
				return jwt;
			}
			else {
				//TODO - get token from azure AD
				String AzureToken = getAzureToken(userName, password);
				return null;
			}
		}
		catch (Exception e){
			return null;
		}
	}
	
	//if appName is null or empty - get token from azure, otherwise - get token from okta
	public String getJWTToken (String userName,String password,String appName)  {
		String token = "";
		try {
			if (System.getProperty("sessionToken") == null || System.getProperty("sessionToken").equals("")) {
				token = setNewJWTToken(userName, password, appName);
			} else {
				token = System.getProperty("sessionToken");
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/authentication/extend", token);
				
				if (res.code!=200) {
					token = setNewJWTToken(userName, password, appName);					
				}else{
					token = res.message;
				}
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Fatal error in AirlockUtils when generating JWT token");
		}	
		if (token != null)
			System.setProperty("sessionToken", token);
	
		return token;
		

	}
	
	public EmailNotification setNotification(String notify, String url, String sessionToken) throws IOException{
		
		boolean isOn = "true".equals(notify);
		EmailNotification notification = new EmailNotification(isOn, url, sessionToken);
		notification.startTest();
		notification.clearMailFile();
		return notification;
	}
	
	public JSONObject getNotificationResult(EmailNotification notification){
		JSONObject notificationOutput = new JSONObject();
		if (notification.isOn())
		{
			notificationOutput = notification.getMailFile();
			notification.stopTest();
		}
		return notificationOutput;
	}
	public void setSeasonId(String seasonID) {
		this.seasonID = seasonID;
		
	}
	
	public void deleteKeys(String user) throws Exception{

		String allKeys = operationsApi.getAllKeys(user, sessionToken);
		JSONArray airlockAPIKeys = new JSONObject(allKeys).getJSONArray("airlockAPIKeys");
		int response;
		for (int i=0; i<airlockAPIKeys.size(); i++ ){
			JSONObject apikey = airlockAPIKeys.getJSONObject(i);
			response = operationsApi.deleteKey(apikey.getString("uniqueId"), sessionToken);
			if (response != 200)
				System.out.println("Can't delete key: " + apikey.getString("key") + " id: " + apikey.getString("uniqueId"));
		}
		
	}

	private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;
	    for(Map.Entry<String, String> entry : params.entrySet()){
	        if (first)
	            first = false;
	        else
	            result.append("&");    
	        result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
	    }    
	    return result.toString();
	}
}
