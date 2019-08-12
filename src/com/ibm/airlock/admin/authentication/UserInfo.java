package com.ibm.airlock.admin.authentication;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;

public class UserInfo
{
	public static final String USER_ROLES = "userRoles";
	public static final String USER_ATTR = "userAttributes";
	public static final String API_KEY = "apiKey";
	public static final String EMAIL = "Email";

	String id, effectiveId;
	Set<RoleType> roles;
	Map<String, List<String>> attributes;
	String errorJson;

	public UserInfo(String userId, Set<RoleType> roles, Map<String, List<String>> attributes)
	{
		this.id = userId; this.roles = roles; this.attributes = attributes;

		// when a user changes email, the OKTA id stays the same but we prefer the new id.
		// always using the first email in the email list.
		String newId = getOneAttribute(EMAIL);
		this.effectiveId = (newId != null) ? newId : id;
	}
	public UserInfo(String error)
	{
		errorJson = Utilities.errorMsgToErrorJSON(error);
	}

	public static String getOneAttribute(String key, Map<String, List<String>> attr)
	{
		if (attr != null)
		{
			List<String> values = attr.get(key);
			if (values != null && !values.isEmpty())
				return values.get(0);
		}
		return null;
	}
	public String getOneAttribute(String key)
	{
		return getOneAttribute(key, attributes);
	}
	public String getApiKey()
	{
		return getOneAttribute(API_KEY);
	}
	public String getId() { return effectiveId; }
	public Set<RoleType> getRoles() { return roles; }
	public Map<String, List<String>> getAttributes() { return attributes; }
	public String getErrorJson() { return errorJson; }

	
	// generate UserInfo from JWT and optionally verify its roles against a method signature
	public static UserInfo validate(String methodSignature, ServletContext context, String jwt, Product product)
	{
		boolean skipAuthentication = (Boolean)context.getAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME);
		if (skipAuthentication)
			return null; // authentication not configured on this server
		
		try {
			UserRoles userRoles;
			if (product == null) {
				userRoles = UserRoles.get(context);
				if (userRoles == null)
					return null; // should not happen - validating thea authentication is configured on this server above
			}
			else {
				//take user roles from product
				@SuppressWarnings("unchecked")
				Map<String,UserRoles> userRolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
				
				userRoles = userRolesPerProductMap.get(product.getUniqueId().toString());
				if (userRoles == null) //no userRoles for this product (can be only for old products before server upgrade)
					userRoles = UserRoles.get(context);				
				
				if (userRoles == null)
					return null; // should not happen - validating thea authentication is configured on this server above

			}
			AirlockAPIKeys keys = AirlockAPIKeys.get(context);
			UserInfo info = JwtData.jwt2UserInfo(jwt, userRoles, keys, product);
			if (info.getErrorJson() != null)
				return info;

			if (methodSignature != null)
			{
				// check whether the method signature matches the user's permitted roles.
				// (revoked users and apiKeys generate empty role sets and will fail here)
				userRoles.checkPermittedRoles(methodSignature, info.roles);
			}

			return info;
		}
		catch (Exception e)
		{
			return new UserInfo(e.toString());
		}
	}
}
