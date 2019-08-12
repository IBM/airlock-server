package com.ibm.airlock.admin.authentication;

import java.security.GeneralSecurityException;
import java.security.Key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;

//import javax.crypto.spec.SecretKeySpec;
//import org.apache.commons.codec.binary.Base64;

import org.apache.wink.json4j.JSONException;

import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.operations.AirlockAPIKey;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.operations.PasswordHash;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtData
{
	static Key signingKey;
	static int jwtExpirationMin = 60; // default is 60 minutes

	public static int getjExpirationMin() { return jwtExpirationMin; }
	public static void setExpirationMin(int min) { jwtExpirationMin = min; }
	static TreeSet<RoleType> emptyRoles = new TreeSet<RoleType>();

	// signingKey initialization
	static {
		signingKey = io.jsonwebtoken.impl.crypto.MacProvider.generateKey(SignatureAlgorithm.HS256);

		// optional: keep a key on S3 to allow JWT token continuity when airlock restarts
		// String seed = "bWUiOlsiRGF5YW4iXX0sInVzZXJSb2xlcyI6";
		// signingKey = new SecretKeySpec(Base64.decodeBase64(seed), SignatureAlgorithm.HS256.getJcaName());
	}

	//-----------------------------------------------------
	// create a standard JWT from airlock UserInfo
	public static String createJWT(UserInfo info) throws JSONException
	{
		Map<String,Object> claims = new TreeMap<String,Object>();
		ArrayList<RoleType> arr = new ArrayList<RoleType>(info.roles);
		claims.put(UserInfo.USER_ROLES, arr);
		claims.put(UserInfo.USER_ATTR, info.attributes);

		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		Date expire = new Date(nowMillis + jwtExpirationMin*60*1000);

//		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
//		logger.info("CREATE NEW JWT, issued at: " + df.format(now) + ", expiring at: "+ df.format(expire));

		String compactJws = Jwts.builder()
                .setClaims(claims) // must be added first since it clears existing keys
                .setId(info.getId())
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(SignatureAlgorithm.HS256, signingKey)
                .compact();

		return compactJws;
	}

	//-----------------------------------------------------
	// create airlock UserInfo from a standard JWT. roles are taken as-is from the JWT
	@SuppressWarnings("unchecked")
	public static UserInfo jwt2UserInfo(String jwt) throws GeneralSecurityException
	{
		if (jwt == null)
			throw new GeneralSecurityException("Missing session token");

		Claims claims = getClaims(jwt);
		String id = claims.getId();

		Map<String, List<String>> attr = (Map<String, List<String>>) claims.get(UserInfo.USER_ATTR);
		List<String> arr = (List<String>) claims.get(UserInfo.USER_ROLES);
		Set<RoleType> roles = new TreeSet<RoleType>();
		for (String item : arr)
		{
			RoleType type = Utilities.strToRoleType(item);
			if (type != null)
				roles.add(type);
		}

		return new UserInfo(id, roles, attr);
	}

	//return null if this jwt was not created from an api key
	public static AirlockAPIKey getApiKeyFromJWT(String jwt, ServletContext context) throws GeneralSecurityException {
		if (jwt == null)
			return  null;
		Claims claims = getClaims(jwt);
		
		@SuppressWarnings("unchecked")
		Map<String, List<String>> attr = (Map<String, List<String>>) claims.get(UserInfo.USER_ATTR);
		String apiKey = UserInfo.getOneAttribute(UserInfo.API_KEY, attr);

		if (apiKey == null) 
			return null;
		
		AirlockAPIKeys keys = AirlockAPIKeys.get(context);
		AirlockAPIKey keyData = keys.getAPIKeyByKeyname(apiKey);
		return keyData;
	}
	
	// create airlock UserInfo from a standard JWT but ignore the JWT roles and use in-memory roles instead
	// (in case the user permissions had changed)
	@SuppressWarnings("unchecked")
	public static UserInfo jwt2UserInfo(String jwt, UserRoles ur, AirlockAPIKeys keys, Product product) throws GeneralSecurityException
	{
		if (jwt == null)
			throw new GeneralSecurityException("Missing session token");

		Claims claims = getClaims(jwt);
		String id = claims.getId();

		Map<String, List<String>> attr = (Map<String, List<String>>) claims.get(UserInfo.USER_ATTR);
		String apiKey = UserInfo.getOneAttribute(UserInfo.API_KEY, attr);
		
		Set<RoleType> roles;
		if (apiKey == null) // regular JWT, get current roles from UserRoles table
		{
			roles = ur.getUserRoles(id);
		}
		else // apiKey JWT, get current roles from AirlockAPIKeys table
		{
			AirlockAPIKey keyData = keys.getAPIKeyByKeyname(apiKey);
			if (product == null) { //use global roles in teh key
				roles = (keyData == null) ? emptyRoles : keyData.getRoles();
			}
			else {
				roles = (keyData.getProductRoles(product) == null) ? emptyRoles : keyData.getProductRoles(product);
			}
		}
		return new UserInfo(id, roles, attr);
	}
	static Claims getClaims(String jwt) throws GeneralSecurityException
	{
		Claims claims = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(jwt).getBody();

		Date from = claims.getIssuedAt();
		Date to = claims.getExpiration();
		Date now = new Date(System.currentTimeMillis());

/*		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		String sfrom = (from == null) ? "null" : df.format(from);
		String sto   = (to   == null) ? "null" : df.format(to);
		String snow = df.format(now);
		logger.info("CHECK JWT DATE at: " + snow + ", issued at: " + sfrom + ", expiring at: "+ sto);
*/
		// this is checked during JWT parsing, but we want to be sure
		if (from == null || to == null || now.before(from) || now.after(to))
		{
			//logger.info("IT IS EXPIRED");
			throw new GeneralSecurityException("Expired");
		}

		return claims;
	}
	//-----------------------------------------------------
	// extend a standard JWT
	public static String extendSession(String jwt) throws Exception
	{
		//logger.info("EXTENDING JWT");
		UserInfo info = jwt2UserInfo(jwt);
		return createJWT(info) ;
	}
	// extend a standard JWT using current in-memory roles
	public static String extendSession(String jwt, UserRoles userRoles, AirlockAPIKeys apiKeys) throws Exception
	{
		//logger.info("EXTENDING JWT");
		UserInfo info = jwt2UserInfo(jwt, userRoles, apiKeys, null); //here the product is not relevant since used only for keys
		return createJWT(info) ;
	}
	//-----------------------------------------------------
	// create a special dummy JWT used by analytics
	public static String createAnalyticsJWT() throws JSONException
	{ 
		//The roles of the airlockAnalyticsServer are on the jwt. This user is not in the airlockUsers maps but the jwt is legal as viewer as
		//long as user * or *.weather.com is viewer.
		//In the analytics side the roles are taken from the jwt and not from the maps therefore this user is admin on the analytics server.
		final String dummyUser = "aaa@weather.com";
		TreeSet<RoleType> roles = new TreeSet<RoleType>(Arrays.asList(RoleType.Administrator, RoleType.ProductLead, RoleType.Viewer, RoleType.Editor));

		Map<String, List<String>> attr = new TreeMap<String, List<String>>();
		attr.put(UserInfo.EMAIL, new ArrayList<>(Arrays.asList(dummyUser)));

		UserInfo info = new UserInfo(dummyUser, roles, attr);
		return createJWT(info);
	}
	//-----------------------------------------------------
	// create a JWT from a user-generated API key
	public static String createApiKeyJWT(AirlockAPIKeys apiKeys, String keyname, String password) throws GeneralSecurityException, JSONException
	{
		AirlockAPIKey key = apiKeys.getAPIKeyByKeyname(keyname);
		if (key == null)
			throw new GeneralSecurityException("invalid API key " + keyname);
		//if (!key.getPassword().equals(password))
		if (!PasswordHash.validatePassword(password, key.getPassword()))
			throw new GeneralSecurityException("invalid API password");

		Map<String, List<String>> attributes = new TreeMap<String, List<String>>();
		attributes.put(UserInfo.API_KEY, Arrays.asList(keyname));

		Set<RoleType> roles = new TreeSet<RoleType>(key.getRoles());
		UserInfo info = new UserInfo(key.getOwner(), roles, attributes);

		return createJWT(info);
	}
}
