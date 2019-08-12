package com.ibm.airlock;

//  This code was replaced by a new SecurityFilter. It is kept here for future reference

// The original implementation used a ContainerRequestFilter which inspects a method's custom annotation for allowed roles,
// and decides whether the call is allowed to proceed.
// It is currently on hold since ContainerRequestFilter is not implemented in wink and importing it from another
// package failed to load the provider.

// As a substitute, we let each method start by referencing a global map containing the annotations.

/*

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.opensaml.ws.security.SecurityPolicyException;

public class AuthorizingFilter implements Filter
{
	static final String PERMIT_GET = "permitGet";
	static final String PERMIT_OTHER = "permitOther";
	static final String SPLITTER = ";" ;

	ServletContext context = null;
	boolean skipAuthentication = false;
	String[] permitGet, permitOther;

	@Override
	public void init(FilterConfig conf) throws ServletException
	{
		context = conf.getServletContext();
		SamlSet saml = (SamlSet)context.getAttribute(Constants.AUTHENTICATION_CONFIG);
		skipAuthentication = (saml == null);

		permitGet = initPermitList(conf, PERMIT_GET);
		permitOther = initPermitList(conf, PERMIT_OTHER);
	}

	String[] initPermitList(FilterConfig conf, String key)
	{
		String urls = conf.getInitParameter(key);
		if (urls == null)
			return new String[0];
		
		ArrayList<String> temp = new ArrayList<String>();
		for (String str : urls.split(SPLITTER))
		{
			str = str.trim();
			if (!str.isEmpty())
				temp.add(str);
		}
		return temp.toArray(new String[temp.size()]);
	}

	boolean isPermitted(String path, String type)
	{
		String[] permit = type.equals("GET") ? permitGet : permitOther;
		for (String prefix : permit)
		{
			if (path.startsWith(prefix))
				return true;
		}
		return false;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (skipAuthentication)
		{
			chain.doFilter(request, response);
			return;
		}

		if (! (request instanceof HttpServletRequest && response instanceof HttpServletResponse) )
			throw new ServletException("Not an HTTP request");

		HttpServletRequest httpReq = (HttpServletRequest)request;

		if (isPermitted(httpReq.getRequestURI(), httpReq.getMethod()))
		{
			chain.doFilter(request, response);
			return;
		}

		try {

			String assertion = httpReq.getHeader(Constants.AUTHENTICATION_HEADER);
			AuthenticationServices.validateSAML(context, assertion);
		}
		catch (SecurityPolicyException e)
		{
			HttpServletResponse httpResp = (HttpServletResponse)response;
			httpResp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authorized: " + e.toString());
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy()
	{}
}
*/


//------------------------------------------------------------------------------------------------------
// The original SecurityFilter
//------------------------------------------------------------------------------------------------------
/*
import javax.ws.rs.ext.Provider;
import javax.ws.rs.Priorities;
import javax.annotation.Priority;
import javax.servlet.ServletContext;

import com.ibm.airlock.SamlSet;
import com.okta.saml.SAMLResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;


@Provider
@Priority(Priorities.AUTHENTICATION) // if this filter clashes with CORS filter, maybe use Priorities.USER
public class SecurityFilter implements ContainerRequestFilter
{
 @Context
 private ResourceInfo resourceInfo;

	@Context
	private ServletContext context;

 SamlSet saml; // SAML class for token validation
 Map<String, SecurityRole.Type> userRole; // UserId mapped to a user role

 SecurityFilter()
 {
		saml = (SamlSet)context.getAttribute(Constants.AUTHENTICATION_CONFIG);
 	userRole = new TreeMap<String, SecurityRole.Type>();
 	userRole.put("yigal@ilairlock-dev.com", SecurityRole.Type.Viewer);
 }

 // we assume this is not a @PreMatching, so the method annotation is known

 @Override
 public void filter(ContainerRequestContext requestContext) throws IOException
 {
 	if (saml == null)
 		return;

 	SecurityRole.Type methodRole = getMethodRole();
     if (methodRole == null)
     	 throw new WebApplicationException(403);

     String assertion = requestContext.getHeaderString(Constants.AUTHENTICATION_HEADER);
     SAMLResponse response = null;
     try {
     	response = saml.validate(assertion);
     }
     catch (Exception e)
     {
     	throw new WebApplicationException(403);
     }

     String userId = response.getUserID();
     SecurityRole.Type runtimeRole = userRole.get(userId);

     // we assume an unmapped user with a valid assertion token has a Viewer role
     if (runtimeRole == null)
     	runtimeRole = SecurityRole.Type.Viewer;

     // check that the runtime user has permission to use this method
     if (runtimeRole.ordinal() < methodRole.ordinal())
     	throw new WebApplicationException(403);

     // later the method may perform more stringent checks with the user role
     requestContext.getHeaders().remove("UserRole"); // just in case someone tries to sneak in
     requestContext.getHeaders().add("UserRole", runtimeRole.toString());
 }

 SecurityRole.Type getMethodRole()
 {
 	if (resourceInfo == null)
 		return null;

     Method resourceMethod = resourceInfo.getResourceMethod();
     if (resourceMethod == null)
     	return null;

 	SecurityRole annotation = resourceMethod.getAnnotation(SecurityRole.class);
 	return  (annotation == null) ? null : annotation.value();
 }
}
//------------------------------------------------------------------------------------------------------
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class SecurityBinder implements DynamicFeature {

 @Override
 public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		String auth = getEnv(Constants.ENV_SKIP_AUTHENTICATION);
		Boolean skipAuth = new Boolean(auth);
		if (!skipAuth)
			context.register(SecurityFilter.class);
 }

 public static String getEnv(String key)
	{
		String out = System.getenv(key);
		if (out == null)
			out = System.getProperty(key);

		return (out == null) ? "" : out;
	}
}
//------------------------------------------------------------------------------------------------------
package com.ibm.airlock;

//import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityRole
{
	public enum Type { Viewer, Editor, ProductLead, Administrator }
	Type value();
}
*/