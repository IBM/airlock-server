package com.ibm.airlock.admin.authentication;

public abstract class Authentication
{
	Providers.Type provider;

	public static class Parms
	{
		String assertion;
//		String key;
		UserRoles roles;
		String additionalInfo; // TBD

		public Parms(String assertion, UserRoles roles)
		{
			this.assertion = assertion; this.roles = roles;
		}
/*		public Parms(String assertion, String key, UserRoles roles)
		{
			this.assertion = assertion; this.key = key; this.roles = roles;
		}*/
	}

	public Providers.Type getProvider() { return provider; }
	public abstract UserInfo authenticate(Parms parms);

}
