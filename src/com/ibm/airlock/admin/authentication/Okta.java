package com.ibm.airlock.admin.authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import org.apache.commons.codec.binary.Base64;
import org.opensaml.ws.security.SecurityPolicyException;

import com.okta.saml.Application;
import com.okta.saml.Configuration;
import com.okta.saml.SAMLRequest;
import com.okta.saml.SAMLResponse;
import com.okta.saml.SAMLValidator;

public class Okta extends Authentication
{
	//----------------------------------------------------------------------------
	static class Saml
	{
		Configuration configuration;
		SAMLValidator validator;
		Application app;
		
		Saml(String config) throws SecurityPolicyException
		{
        	validator = new SAMLValidator();
        	configuration = validator.getConfiguration(config);
        	app = configuration.getDefaultApplication();

			if (configuration.getDefaultEntityID() == null)
				throw new SecurityPolicyException("Default application has not been configured in SAML xml");
		}

		SAMLResponse validate(String assertion) throws SecurityPolicyException
		{
			return validator.getSAMLResponse(assertion, configuration);
		}
		
		String getRedirectUrl() throws UnsupportedEncodingException
		{
            SAMLRequest samlRequest = validator.getSAMLRequest(app);

            // Base64 encode and then URL encode the SAMLRequest
            String encodedSaml = Base64.encodeBase64String(samlRequest.toString().getBytes());

            String url = app.getAuthenticationURL();
            url += "?" + "SAMLRequest" + "=" + URLEncoder.encode(encodedSaml, "UTF-8");
            return url;
		}
	}
	//----------------------------------------------------------------------------
/*
	TreeMap<String, Saml> supportedSamlApps = null;
	public Okta(TreeMap<String,String> config, String oktaApplications) throws SecurityPolicyException
	{
		provider = Providers.Type.OKTA;
		TreeMap<String, Saml> allSamlApps = new TreeMap<String, Saml>();
		for (Map.Entry<String,String> e : config.entrySet())
		{
			String appName = e.getKey();
			Saml saml = new Saml(e.getValue());
			allSamlApps.put(appName, saml);
		}

		 // if OKTA_APPLICATIONS environment variable is specified, obtain a subset
		if (oktaApplications == null || oktaApplications.isEmpty())
			supportedSamlApps = allSamlApps;
		else
		{
			supportedSamlApps = new TreeMap<String, Saml>();
			String[] oktaApps =  oktaApplications.split(";");

			for (String app : oktaApps)
			{
				Saml samlApplication = allSamlApps.get(app);
				if (samlApplication == null)
					throw new SecurityPolicyException("invalid OKTA application: " + app);

				supportedSamlApps.put(app, samlApplication);
			}
			if (supportedSamlApps.isEmpty())
				throw new SecurityPolicyException("No data found in OKTA_APPLICATIONS" );
		}		
	}
*/

	String appName;
	Saml saml;

	public Okta(String appName, String config) throws SecurityPolicyException
	{
		provider = Providers.Type.OKTA;
		this.appName = appName;
		this.saml = new Saml(config);
	}

	@Override
	public UserInfo authenticate(Parms parms)
	{
		try {
			String assertion = decodeAssertion(parms.assertion);
			SAMLResponse response = saml.validate(assertion);
			return response2UserInfo(response, parms.roles);
		}
		catch (Exception e)
		{
			return new UserInfo(e.toString());
		}
	}

	public String getRedirectUrl() throws Exception
	{
	    return saml.getRedirectUrl();
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
	//-------------------------------------------------------
	// operations on a set of OKTA applications
	/*
	public static Saml getApplication(Providers providers, String appName)
	{
		Okta okta = (Okta) providers.getProvider(Providers.Type.OKTA, appName);
		return (okta == null) ? null : okta.saml;
	}
	public static boolean isSupportedApplication(Providers providers, String appName)
	{
		return getApplication(providers, appName) != null;
	}
	*/

	// scan OKTA application set looking for a matching SAML
	public static UserInfo authenticateSet(Providers.Flavor set, Parms parms)
	{
		try {
			String assertion = decodeAssertion(parms.assertion);
			SAMLResponse response = validateSet(set, assertion);
			return response2UserInfo(response, parms.roles);
		}
		catch (Exception e)
		{
			return new UserInfo(e.toString());
		}
	}
	static SAMLResponse validateSet(Providers.Flavor set, String assertion) throws SecurityPolicyException
	{
		SecurityPolicyException last_error = new SecurityPolicyException("no providers found");
		for (Authentication auth : set.values())
		{
			try {
				Okta okta = (Okta) auth;
				return okta.saml.validate(assertion);
			}
			catch (SecurityPolicyException err) {
				last_error = err;
			}
		}
		throw last_error;
	}
}
