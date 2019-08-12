package com.ibm.airlock.admin.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opensaml.ws.security.SecurityPolicyException;

import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Utilities;
import com.okta.saml.SAMLResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

public class AzurAD extends Authentication
{
	public static final Logger logger = Logger.getLogger(AzurAD.class.getName());

	
	final static String AUTHORITY_URL = "https://login.microsoftonline.com/";
	public static final String GENERAL_APP_NAME = "general.pem"; //there is only one key for azure ad but we are still keeping the multi key structure as in ibmId
	final static String REDIRECT_URL_POSTFIX = "admin/authentication/ad/login/";
	
	String appName;
	PublicKey key;
	String azureClientId; 
	String azureClientSecret;
	String azureTenant;

	public AzurAD(String appName, String pemFile, String azureClientId, String azureClientSecret, String azureTenant) throws Exception
	{
		provider = Providers.Type.AZURE;
		this.appName = appName;
		this.azureClientId = azureClientId; 
		this.azureClientSecret = azureClientSecret;
		this.azureTenant = azureTenant;

		setKeyFromPem (new JSONObject(pemFile));		
	}
	
	private void setKeyFromPem (JSONObject jsonKey) throws Exception {
		BigInteger modulus = new BigInteger(1, Base64.decodeBase64(jsonKey.getString("n")));
		BigInteger exponent = new BigInteger(1, Base64.decodeBase64(jsonKey.getString("e")));

		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		this.key = factory.generatePublic(spec);
	}

	@Override
	public UserInfo authenticate(Parms parms)
	{
		try {
			return Sso2AirlockInfo(parms.assertion, parms.roles);
		}
		catch (Exception e)
		{
			return new UserInfo(e.toString());
		}
	}

	UserInfo Sso2AirlockInfo(String sso_jwt, UserRoles ur) throws Exception
	{
		Claims claims = null;
		try {
			claims = Jwts.parser().setSigningKey(key).parseClaimsJws(sso_jwt).getBody();
		} catch (SignatureException se) {
			logger.warning("Signature exception: " + se.getMessage() + ". Looking for updated key.");
			//this can be caused by public key switch by the active directory - try finding the new key
			findNewKey(sso_jwt);
			claims = Jwts.parser().setSigningKey(key).parseClaimsJws(sso_jwt).getBody();
		}

		//String userId = getItem(claims, "email");
		String userId = getItem(claims, "unique_name");
		
		//the name field contains both first and last name
		String firstName = getItem(claims, "name");
		String lastName = "";//getItem(claims, "name"); 

		Set<RoleType> roles = ur.getUserRoles(userId);

		Map<String, List<String>> attr = new TreeMap<String, List<String>>();
		attr.put(UserInfo.EMAIL, new ArrayList<>(Arrays.asList(userId)));
		attr.put("FirstName", new ArrayList<>(Arrays.asList(firstName)));
		attr.put("LastName", new ArrayList<>(Arrays.asList(lastName)));

		return new UserInfo(userId, roles, attr);
	}
	
	private void findNewKey(String sso_jwt) throws Exception {
		URL url = new URL("https://login.microsoftonline.com/common/discovery/keys");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();

		InputStream inp;
		if ((responseCode >= 200 && responseCode <= 299) || con.getErrorStream() == null) {
			inp = con.getInputStream(); 
		} 
		else {
			inp = con.getErrorStream();	
			String errMsg;
			if (inp == null)
				errMsg = "Failed getting active directory public keys. Response Code : " + responseCode;
			else
				errMsg = "Failed getting active directory public keys" + Utilities.streamToString(inp);
			
			logger.severe(errMsg);
			throw new IOException(errMsg); 
		}
		
		String keysStr = Utilities.streamToString(inp);
		JSONObject keysObj = new JSONObject(keysStr);
		JSONArray keysArray = keysObj.getJSONArray("keys");
		
		PublicKey origKey = this.key;
		
		for (int i=0; i<keysArray.size(); i++) {
			//try the keys one by one
			JSONObject keyJSON = keysArray.getJSONObject(i);
			try {
				logger.info("Trying key: " + keyJSON.toString());
				setKeyFromPem(keyJSON);
				Jwts.parser().setSigningKey(this.key).parseClaimsJws(sso_jwt).getBody();
				//no exception thrown - the key was found
				logger.info("Found key: " + keyJSON.toString());
				return;
			} catch (Exception se) {
				//move on to the next key
				logger.info("Wrong key: " + keyJSON.toString() + ": " + se.getMessage());
			}
		}
		//key was not found - return to original key
		this.key = origKey;
	}

	String getItem(Claims claims, String key) throws Exception
	{
		Object obj = claims.get(key);
		if (!(obj instanceof String))
			throw new Exception("missing " + key + " in JWT token");
		return (String) obj;
	}
	

	public String getRedirectUrl(UriInfo uri, String state) throws Exception
	{
		URI myUri = uri.getBaseUri();
		String redirectUrl = myUri + "admin/authentication/ad/login/";
		
		/*
		 //for retrieving id_token instead of code
		  //nonce - 	required - 	A value included in the request, generated by the app, that is included in the resulting id_token as a claim. The app can then verify this value to mitigate token replay attacks. The value is typically a randomized, unique string or GUID that can be used to identify the origin of the request.
		 String url = AUTHORITY_URL + azureTenant + "/oauth2/authorize?" + 
					  "client_id=" + azureClientId + 
					  "&response_type=id_token" +
					  "&nonce=7362CAEA-9CA5-4B43-9BA3-34D7C303EBA7"+
					  "&response_mode=form_post" +
				      "&redirect_uri=" + redirectUrl;*/
		

		String url = AUTHORITY_URL + azureTenant + "/oauth2/authorize?" + 
					  "client_id=" + azureClientId + 
					  "&response_type=code" +
					  "&response_mode=query" +
					  "&redirect_uri=" + redirectUrl;
		if (state!=null && !state.isEmpty()) {
			url = url + "&state=" + state;
		}
		//+  "&state=" + myUri + "admin/authentication/ad/test/";
					
		
	    return url;
	}

	static String decodeAssertion(String assertion) throws SecurityPolicyException
	{
		if (assertion == null)
			throw new SecurityPolicyException("missing assertion token");

		try {
			return new String(Base64.decodeBase64(assertion.getBytes("UTF-8")), Charset.forName("UTF-8"));
		}
		catch (Exception e)
		{
			throw new SecurityPolicyException("invalid assertion token");
		}
	}

	static UserInfo response2UserInfo(SAMLResponse response, UserRoles roles)
	{
		// the OKTA SAMLResponse userId may be an old email; replace it with the latest email
		String userId = response.getUserID();

		// disregard OKTA roles, obtain roles from the role table
		UserInfo info = new UserInfo(userId, roles.getUserRoles(userId), response.getAttributes());

		// get an up-to-date email id and reselect the roles according to it
		String effectiveId = info.getId(); 
		if ( ! userId.equals(effectiveId) )
			info.roles = roles.getUserRoles(effectiveId);

		return info;
	}

	public String getTokenFromAuthCode(String authCode, UriInfo uri) throws IOException {
		URI myUri = uri.getBaseUri();
		String redirectUrl = myUri + REDIRECT_URL_POSTFIX;
		
		String urlString = AUTHORITY_URL + azureTenant + "/oauth2/token?";
		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
	
		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
	
		// Send post request
		con.setDoOutput(true);
		HashMap<String, String> reqBodyParams = new HashMap<String, String>();
		reqBodyParams.put("grant_type", "authorization_code");
		reqBodyParams.put("client_id", azureClientId);
		reqBodyParams.put("resource", azureClientId);
		reqBodyParams.put("client_secret", azureClientSecret);
		reqBodyParams.put("code", authCode);
		reqBodyParams.put("redirect_uri", redirectUrl);
		
		// "&resource=https://graph.windows.net";
				
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
