package com.ibm.airlock;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.opensaml.ws.security.SecurityPolicyException;

import com.okta.saml.Application;
import com.okta.saml.Configuration;
import com.okta.saml.SAMLRequest;
import com.okta.saml.SAMLResponse;
import com.okta.saml.SAMLValidator;

public class SamlSet
{
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
	//Saml[] items;
	TreeMap<String, Saml> allSamlApps = new TreeMap<String, Saml>();
	TreeMap<String, Saml> supportedSamlApps = null; //used when oktaApplications env param is given
	
	SamlSet(TreeMap<String,String> config, String oktaApplications) throws SecurityPolicyException
	{
		for (Map.Entry<String,String> e : config.entrySet())
		{
			String appName = e.getKey();
			Saml saml = new Saml(e.getValue());
			allSamlApps.put(appName, saml);
		}
		
		if (oktaApplications != null && !oktaApplications.isEmpty()) { //if okta_applications env param is specified - build list of supported apps
			String[] oktaApps =  oktaApplications.split(";");
			supportedSamlApps = new TreeMap<String, Saml>();
			for (int i=0; i< oktaApps.length; i++) { 
				Saml samlApplication = getApplication(oktaApps[i]);
				if (samlApplication==null) //the given okta application does not exist
					throw new SecurityPolicyException("invalid OKTA_APPLICATION: " + oktaApps[i]);
				
				supportedSamlApps.put(oktaApps[i], samlApplication);
			}			
		}		
	}

	Saml getApplication(String appName) { return allSamlApps.get(appName); }

	boolean isSupportedApplication(String appName) {
		if (supportedSamlApps == null) //all apps are allowed
			return true;
		
		if (supportedSamlApps.get(appName) == null) 
				return false;
		
		return true;
	}
	
	SAMLResponse validate(String assertion) throws SecurityPolicyException
	{
		if (assertion == null)
			throw new SecurityPolicyException("missing assertion token");

		try {
			assertion = new String(Base64.decodeBase64(assertion.getBytes("UTF-8")), Charset.forName("UTF-8"));
		}
		catch (Exception e)
		{
			throw new SecurityPolicyException("invalid assertion token");
		}

		// if okta applications are specified check only supported samls. throw error if none validated
		TreeMap<String,Saml> source = (supportedSamlApps != null) ? supportedSamlApps : allSamlApps;
		SecurityPolicyException last_error = null;

		for (Saml saml : source.values())
		{
			try {
				return saml.validate(assertion);
			}
			catch (SecurityPolicyException err) {
				last_error = err;
			}
		}
		throw last_error;
	}
}
