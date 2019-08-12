package com.ibm.airlock.admin.authentication;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;

import com.ibm.airlock.Constants.RoleType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

// Blue ID SSO provider returns a JWT. validate it with a public key and convert to the Airlock standard UserInfo.

// openssl rsa -in key.pem -pubout -outform DER -out key.der
// openssl x509 -in cert.pem -noout -pubkey > pubkey.pem

public class BlueId extends Authentication
{
	String appName;
	PublicKey key;

	/*
	Map<String, PublicKey> publicKeys = new TreeMap<String, PublicKey>();
	public BlueId(Map<String,String> keys) throws Exception
	{
		provider = Providers.Type.BLUEID;
		for (Map.Entry<String,String> ent : keys.entrySet())
		{
			String fileName = ent.getKey();
			String pemFile = ent.getValue();

			pemFile = pemFile.replace("-----BEGIN PUBLIC KEY-----", "");
			pemFile = pemFile.replace("-----END PUBLIC KEY-----", "");
			byte[] derFile = Base64.decodeBase64(pemFile); // another option is to prepare derFile in advance

			X509EncodedKeySpec spec = new X509EncodedKeySpec(derFile);
			KeyFactory kf = KeyFactory.getInstance("RSA"); //RS256
			PublicKey key = kf.generatePublic(spec);

			publicKeys.put(fileName, key);
		}
	}*/
	public BlueId(String appName, String pemFile) throws Exception
	{
		provider = Providers.Type.BLUEID;
		this.appName = appName;

		pemFile = pemFile.replace("-----BEGIN PUBLIC KEY-----", "");
		pemFile = pemFile.replace("-----END PUBLIC KEY-----", "");
		byte[] derFile = Base64.decodeBase64(pemFile); // another option is to prepare derFile in advance

		X509EncodedKeySpec spec = new X509EncodedKeySpec(derFile);
		KeyFactory kf = KeyFactory.getInstance("RSA"); //RS256
		this.key = kf.generatePublic(spec);
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
		Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(sso_jwt).getBody();

		String userId = getItem(claims, "email");
		String firstName = getItem(claims, "given_name");
		String lasttName = getItem(claims, "family_name");

		Set<RoleType> roles = ur.getUserRoles(userId);

		Map<String, List<String>> attr = new TreeMap<String, List<String>>();
		attr.put(UserInfo.EMAIL, new ArrayList<>(Arrays.asList(userId)));
		attr.put("FirstName", new ArrayList<>(Arrays.asList(firstName)));
		attr.put("LastName", new ArrayList<>(Arrays.asList(lasttName)));

		return new UserInfo(userId, roles, attr);
	}
	String getItem(Claims claims, String key) throws Exception
	{
		Object obj = claims.get(key);
		if (!(obj instanceof String))
			throw new Exception("missing " + key + " in JWT token");
		return (String) obj;
	}
}
