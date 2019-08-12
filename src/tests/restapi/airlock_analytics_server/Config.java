package tests.restapi.airlock_analytics_server;

import java.io.*;
import java.util.Enumeration;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class Config {


	String m_url;
	String m_configPath;
	ProductsRestApi p;
	SeasonsRestApi s;
	public String sessionToken = "";
	private ExperimentsRestApi expApi;
	private String productID;
	private String seasonID;
	private String productsToDelete;

	public Config(String airlockUrl, String configPath, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException{
		m_url = airlockUrl;
		m_configPath = configPath;
		sessionToken = getJWTToken(userName, userPassword, appName);
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_url);
		productsToDelete = productsToDeleteFile;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
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

	public String getSessionToken(){
		return sessionToken;
	}

	public String setNewJWTToken(String userName,String password,String appName){
		try {
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
		catch (Exception e){
			return null;
		}
	}
	
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

	public String createProduct() throws IOException{
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(m_url);
		String product = FileUtils.fileToString(m_configPath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProduct(product, sessionToken);
		printProductToFile(productID);
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
	
	public void reset(String productID, String sessionToken){
		if (productID != null)

			p.deleteProduct(productID, sessionToken);
	}
	
	public String setExperimentName(){
		return "experiment."+RandomStringUtils.randomAlphabetic(3);
	}
	
	public JSONObject getExperimentFromFile() throws IOException, JSONException {
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(experiment);
		json.put("name", setExperimentName());
		return json;
	}
	
	public String getAnalyticsUrl(){
		return m_url.replace("/api/admin", "/api/analytics");
	}


}
