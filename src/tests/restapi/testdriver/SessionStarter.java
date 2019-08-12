package tests.restapi.testdriver;

import java.io.StringReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.RestClientUtils;

public class SessionStarter
{
	String airlockAdminUrl;
	String oktaUrl;
	String oktaApp;
	
	public SessionStarter(String airlockAdminUrl, String oktaUrl, String oktaApp)
	{
		this.airlockAdminUrl = airlockAdminUrl;
		this.oktaUrl = oktaUrl;
		this.oktaApp = oktaApp;
	}

	public String getJWT(String user, String password) throws Exception
	{
		String oktaToken =  getOktaSessionToken(user, password);

		// both direct/redirect methods are shown here for documentation purposes
		String samlResult = getSamlDirectly(oktaToken);
		//String samlResult = getSamlRedirected(oktaToken);

		// what was returned is an html directing the browser to POST to the server with a SAMLResponse form.
		// We extract the SAMLResponse and POST it to the server
		String samlForm = "SAMLResponse=" + ExtractSaml(samlResult);

		System.out.println("obtaining JWT");
		RestClientUtils.RestCallResults results = RestClientUtils.sendPost(airlockAdminUrl + "authentication/login", samlForm);
		if (results.code != 200)
			throw new Exception(results.message);

		String jwt = results.message; 
		System.out.println("JWT TOKEN " + jwt);
		return jwt;
	}

	// the okta session token causes the login screen to be skipped
	String getOktaSessionToken(String user, String password) throws Exception
	{
		System.out.println("obtaining OKTA session token");
		JSONObject credentials = new JSONObject();
		credentials.put("username", user);
		credentials.put("password", password);

		Map<String,String> headers = new TreeMap<String,String>();
		headers.put("Content-Type", "application/json");

		RestClientUtils.RestCallResults results = RestClientUtils.sendPost(oktaUrl, credentials.toString(), headers);
		if (results.code != 200)
			throw new Exception(results.message);
		JSONObject response = new JSONObject(results.message);
		return response.getString("sessionToken");
	}

	// get otka app url from airlock server and call it directly
	String getSamlDirectly(String oktaToken) throws Exception
	{
		RestClientUtils.RestCallResults results;

		System.out.println("obtaining OKTA app url");
		results = RestClientUtils.sendGet(airlockAdminUrl + "authentication/oktaurl/" + oktaApp);
		if (results.code != 200)
			throw new Exception(results.message);
		String oktaAppUrl = results.message;

		System.out.println("obtaining SAML");
		results = RestClientUtils.sendGet(oktaAppUrl + "&sessionToken=" + oktaToken);
		if (results.code != 200)
			throw new Exception(results.message);

		return results.message;
	}

	// tell airlock server to redirect the call to the okta app
	String getSamlRedirected(String oktaToken) throws Exception
	{
		System.out.println("obtaining SAML");
		Map<String,String> headers = new TreeMap<String,String>();
		headers.put("sessionToken", oktaToken);
		RestClientUtils.RestCallResults results = RestClientUtils.sendGetWithRedirect(airlockAdminUrl + "authentication/login/" + oktaApp, headers);
		if (results.code != 200)
			throw new Exception(results.message);

		return results.message;

	}

/*  temporary implementation extracting SAML from HTML
	String ExtractSaml(String html) throws Exception
	{
		String samlTag="name=\"SAMLResponse\" type=\"hidden\" value=\"";
		int start = html.indexOf(samlTag);
		if (start == -1)
			throw new Exception("no SAML in response");
		String saml = html.substring(start+samlTag.length());
		int stop = saml.indexOf("\"/>");
		saml = saml.substring(0,stop);
		saml = saml.replace("&#x2b;","%2B").replace("&#x3d;","%3D");
		return saml;
	}
*/

	String ExtractSaml(String html) throws Exception
	{
		HTMLEditorKit htmlKit = new HTMLEditorKit();
		HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
		htmlDoc.putProperty("IgnoreCharsetDirective", new Boolean(true));
		htmlKit.read(new StringReader(html), htmlDoc, 0);
		AttributeSet set = htmlDoc.getElement("appForm").getElement(0).getElement(0).getAttributes();

		String saml = "";
		Enumeration<?> attributes = set.getAttributeNames();
		while ((attributes.hasMoreElements()))
		{
		   Object attr = attributes.nextElement();
		   if (attr.toString().equals("value")) {
		      saml = set.getAttribute(attr).toString().replace("+","%2B").replace("=","%3D");
		      break;
		   };
		}
		return saml;
	}


}
