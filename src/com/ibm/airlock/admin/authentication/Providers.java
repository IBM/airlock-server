package com.ibm.airlock.admin.authentication;

import java.util.TreeMap;

import javax.servlet.ServletContext;

import com.ibm.airlock.Constants;

public class Providers
{
	public enum Type { OKTA, BLUEID, AZURE };

	@SuppressWarnings("serial")
	public class Flavor extends TreeMap<String, Authentication> {}

	Flavor providerMaps[]; // for each type of provider, a map of applications

	public Providers()
	{
		providerMaps = new Flavor[ Type.values().length ];
		for (int i = 0; i < providerMaps.length; ++i)
			providerMaps[i] = new Flavor();
	}

	public void addProvider(String appKey, Authentication auth)
	{
		providerMaps[ auth.getProvider().ordinal() ].put(appKey, auth);
	}

	// get all applications of a provider type
	public Flavor getProviders(Type providerType)
	{
		return providerMaps[ providerType.ordinal() ];
	}

	// get one application of a specific provider type and appKey. returns null if missing
	public Authentication getProvider(Type providerType, String appKey)
	{
		return getProviders(providerType).get(appKey);
	}

	//-------------------------------------------------
	static public Providers get(ServletContext context)
	{
		return (Providers) context.getAttribute(Constants.PROVIDERS);
	}
	static public Authentication get(ServletContext context, Type providerType, String appKey) throws Exception
	{
		Providers all = get(context);
		if (all == null)
			return null; // server is not configured with authentication

		Authentication auth = all.getProvider(providerType, appKey);
		if (auth == null)
			throw new Exception("Provider is not configured: " + providerType + "/" + appKey);
		return auth;
	}
	static public Flavor get(ServletContext context, Type providerType)
	{
		Providers all = get(context);
		if (all == null)
			return null; // server is not configured with authentication

		return all.getProviders(providerType);

	}
}
