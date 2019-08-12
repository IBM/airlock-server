
package com.ibm.airlock;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.authentication.Authentication;
import com.ibm.airlock.admin.authentication.AzurAD;
import com.ibm.airlock.admin.authentication.JwtData;
import com.ibm.airlock.admin.authentication.Okta;
import com.ibm.airlock.admin.authentication.Providers;
import com.ibm.airlock.admin.authentication.Providers.Flavor;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.authentication.UserRoles;
import com.ibm.airlock.admin.operations.AirlockAPIKeys;
import com.ibm.airlock.admin.serialize.AuditLogWriter;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path ("/admin/authentication")
@Produces(value="text/plain")
@Api(value = "/authentication", description = "Authentication Services API")
public class AuthenticationServices
{
	public static final Logger logger = Logger.getLogger(AuthenticationServices.class.getName());

	@Context
	private ServletContext context;

	@Context
	UriInfo uri;

	@POST
	@Produces(value="text/plain")
	@ApiOperation(value = "/startSession")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })

	//@SecurityRole(SecurityRole.Type.Editor) 

	// this method does not contain an OKTA applications, so we try all available ones
	// TODO: add authentication for a known application
	public Response startSession(InputStream samlAssertion)
	{
		try {
			String assertion = Utilities.streamToString(samlAssertion);

			Flavor auth = Providers.get(context, Providers.Type.OKTA);
			if (auth == null)
				return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

			UserInfo info = Okta.authenticateSet(auth, new Authentication.Parms(assertion, UserRoles.get(context)));

			if (info.getErrorJson() != null)
				return sendInfoError(Status.UNAUTHORIZED, info);

			String jwt = JwtData.createJWT(info);

			AuditLogWriter auditLogWriter = (AuditLogWriter)context.getAttribute(Constants.AUDIT_LOG_WRITER_PARAM_NAME);
			auditLogWriter.log("Start session", info); 				

			return sendJwt(jwt);
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	@GET
	@Path ("/extend")
	@Produces(value="text/plain")
	@ApiOperation(value = "/extendSession")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized") })
	public Response extendSession(@HeaderParam(Constants.AUTHENTICATION_HEADER) String jwt)
	{
		boolean skipAuthentication = (Boolean)context.getAttribute(Constants.SKIP_AUTHENTICATION_PARAM_NAME);
		if (skipAuthentication)
			return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);


		UserRoles roles = UserRoles.get(context);
		if (roles == null)
			return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

		AirlockAPIKeys keys = AirlockAPIKeys.get(context);

		try {
			String newJWT = JwtData.extendSession(jwt, roles, keys);
			return sendJwt(newJWT);
		}
		catch (Exception e)
		{
			return sendErrorWithJwt(Status.UNAUTHORIZED, "session cannot be extended: " + e.toString(), jwt); //TODO: temp for debugging
		}
	}

	@POST
	@Path ("/startSessionFromKey")
	@Produces(value="text/plain")
	@ApiOperation(value = "Start session from Api key")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 401, message = "Unauthorized"),
			@ApiResponse(code = 500, message = "Internal error") })

	public Response startSessionFromKey(String body)
	{
		JSONObject bodyJSON = null;
		try {
			bodyJSON = new JSONObject(body);  
		} catch (Exception je) {
			return sendError(Status.BAD_REQUEST, Strings.illegalInputJSON + je.getMessage());				
		}

		String apiKeyName =  bodyJSON.optString(Constants.JSON_FIELD_KEY);
		String password = bodyJSON.optString(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD);
		if (apiKeyName == null)
			return sendError(Status.UNAUTHORIZED, "missing key");
		if (password == null)
			return sendError(Status.UNAUTHORIZED, "missing keyPassword");

		try {
			AirlockAPIKeys apiKeys = AirlockAPIKeys.get(context);
			String jwt = JwtData.createApiKeyJWT(apiKeys, apiKeyName, password);
			return sendJwt(jwt);
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	Response sendJwt(String jwt)
	{
		// optionally create a cookie containing the JWT
		String airlockCookieDomain = AirLockContextListener.getEnv(Constants.ENV_AIRLOCK_COOKIE_DOMAIN);
		if (!airlockCookieDomain.isEmpty())
		{
			// scope the cookie by domain but not by path. delete it at end of browser session.
			final String path = "/";
			Cookie cookie = new Cookie(Constants.AIRLOCK_COOKIE, jwt, path, airlockCookieDomain); 
			return Response.ok(jwt).cookie(new NewCookie(cookie)).build();
		}
		else
			return Response.ok(jwt).build();
	}

	String getOktaRedirectUrl(String appName) throws Exception
	{
		if (appName == null)
			throw new Exception("misssing appName");

		Okta auth = (Okta) Providers.get(context, Providers.Type.OKTA, appName);
		if (auth == null)
			throw new Exception(Constants.SKIP_AUTHENTICATION);

		return auth.getRedirectUrl();
	}

	String getAzureTokenFromAuthCode(String code) throws Exception
	{
		AzurAD auth = (AzurAD) Providers.get(context, Providers.Type.AZURE, AzurAD.GENERAL_APP_NAME);
		if (auth == null)
			throw new Exception(Constants.SKIP_AUTHENTICATION);

		return auth.getTokenFromAuthCode(code, uri);
	}

	String getAzureADRedirectUrl(String state) throws Exception
	{
		AzurAD auth = (AzurAD) Providers.get(context, Providers.Type.AZURE, AzurAD.GENERAL_APP_NAME);
		if (auth == null)
			throw new Exception(Constants.SKIP_AUTHENTICATION);

		return auth.getRedirectUrl(uri, state);
	}

	// TODO: add a method that directs a user to the correct provider based on some internal database

	// methods for direct access to the OKTA login application.
	// the GET redirects to the OKTA login page
	@GET
	@Path ("/login/{app-name}")
	public Response login_in(@PathParam("app-name")String appName, @HeaderParam(Constants.AUTHENTICATION_HEADER) String oktaSession)
	{
		try {
			String url = getOktaRedirectUrl(appName);
			if (oktaSession != null)
				url += "&sessionToken=" + oktaSession;

			return Response.temporaryRedirect(new URI(url)).build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	// the POST gets a SAML assertion back from the OKTA login and returns a JWT to the browser
	// convert the SAML assertion to JWT & return the JWT
	@POST
	@Path ("/login")
	public Response login_out(@FormParam(Constants.AUTHENTICATION_SAML) String assertion)
	{
		try {

			if (assertion == null)
				return sendError(Status.BAD_REQUEST, Constants.AUTHENTICATION_SAML + " parameter missing"); 

			Flavor auth = Providers.get(context, Providers.Type.OKTA);
			if (auth == null)
				return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

			UserInfo info = Okta.authenticateSet(auth, new Authentication.Parms(assertion, UserRoles.get(context)));
			if (info.getErrorJson() != null)
				return sendInfoError(Status.BAD_REQUEST, info);

			String jwt = JwtData.createJWT(info);
			return sendJwt(jwt);
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	// methods for direct access to the Azure Active Directory login application.
	// the GET redirects to the a login page
	// state can be null - if not null, add it to the redirect url 
	@GET
	@Path ("/ad/authcode")
	public Response ad_login_in(@QueryParam("state") String state)
	{
		try {
			String url = getAzureADRedirectUrl(state);

			return Response.temporaryRedirect(new URI(url)).build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}
/*		
	@GET
	@Path ("/ad/test")
	public Response ad_test(@QueryParam("jwt") String jwt, @QueryParam("error") String err)
	{
		try {
			//String url = getAzureADRedirectUrl();

			return Response.ok().build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}
*/	 
	//turn the authentication code to Azure ad token and then convert the azure token to airlock token
	@GET
	@Path ("/ad/login")
	public Response ad_login_out(@QueryParam("code") String code, @QueryParam("state") String state) 
	{
		try {

			if (code == null)
				return sendError(Status.BAD_REQUEST, "'code' parameter missing"); 

			Authentication auth = Providers.get(context, Providers.Type.AZURE, AzurAD.GENERAL_APP_NAME);
			if (auth == null)
				return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

			String token = getAzureTokenFromAuthCode(code);

			UserInfo info = auth.authenticate(new Authentication.Parms(token, UserRoles.get(context)));
			if (info != null && info.getErrorJson() != null) {
				if (state == null || state.isEmpty()) {		
					return sendInfoErrorWithJwt(Status.UNAUTHORIZED, info, token);
				}
				else {
					if (token!=null)
						logger.severe(token);
					if (info!=null)
						logger.severe(info.getErrorJson());
					state = state+"?error=error.";
					return Response.temporaryRedirect(new URI(state)).build();
				}
			}
			String jwt = JwtData.createJWT(info);
			if (state == null || state.isEmpty())
				return sendJwt(jwt);
			else {
				state = state+"?jwt=" + jwt;
				return Response.temporaryRedirect(new URI(state))/*.header(Constants.AUTHENTICATION_HEADER, jwt)*/.build();
			}
		}
		catch (Exception e)
		{
			if (state == null || state.isEmpty()) {						
				return sendError(Status.BAD_REQUEST, e.toString());
			}
			else {
				logger.severe(e.getMessage());		
				state = state+"?error=error.";
				try {
					return Response.temporaryRedirect(new URI(state)).build();
				} catch (URISyntaxException e1) {
					return sendError(Status.INTERNAL_SERVER_ERROR, e1.toString());					
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			if (state == null || state.isEmpty()) {		
				return sendError(Status.BAD_REQUEST, e.toString());
			}
			else {
				logger.severe(e.getMessage());		
				state = state+"?error=error.";
				try {
					return Response.temporaryRedirect(new URI(state)).build();
				} catch (URISyntaxException e1) {
					return sendError(Status.INTERNAL_SERVER_ERROR, e1.toString());					
				}
			}
		}
	}

	// validate the Azure AD JWT and issue an Airlock JWT to replace it
	@GET
	@Path ("/ad/jwt")
	public Response ad_convert_jwt(@HeaderParam("token") String token, @QueryParam("state") String state)
	{
		try {

			if (token == null)
				return sendError(Status.BAD_REQUEST, "'token' parameter missing"); 

			Authentication auth = Providers.get(context, Providers.Type.AZURE, AzurAD.GENERAL_APP_NAME);
			if (auth == null)
				return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

			UserInfo info = auth.authenticate(new Authentication.Parms(token, UserRoles.get(context)));
			if (info != null && info.getErrorJson() != null)
				return sendInfoErrorWithJwt(Status.UNAUTHORIZED, info, token);
			
			String jwt = JwtData.createJWT(info);
			if (state == null || state.isEmpty())
				return sendJwt(jwt);
			else {
				state = state+"?jwt=" + jwt;
				return Response.temporaryRedirect(new URI(state))/*.header(Constants.AUTHENTICATION_HEADER, jwt)*/.build();
			}
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		} catch (Throwable e) {
			e.printStackTrace();
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	@GET
	@Path ("/oktaurl/{app-name}")
	public Response get_okta_url(@PathParam("app-name")String appName)
	{
		try {
			String url = getOktaRedirectUrl(appName);
			return Response.ok(url).build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	// check whether the user is authorized to view analytics. returns 200, 401, or BAD_REQUEST
	static final String ORIGINAL_METHOD = "X-Original-Method";  // inserted by NGINX when calling this method for validation
	@GET
	@Path ("/analyticsviewer")
	public Response authorizedForAnalytics(@CookieParam(Constants.AIRLOCK_COOKIE) String cookie,
			@HeaderParam(Constants.AUTHENTICATION_HEADER) String token,
			@HeaderParam(ORIGINAL_METHOD) String originalMethod)
	{
		try {
			if (token == null) // JWT is either a header token or a cookie
				token = cookie;

			// restriction has been removed - users with viewAnalytics role can do anything. editAnalytics role is unused.
			boolean requestView = true; // = (originalMethod == null || originalMethod.equals("GET") || originalMethod.equals("POST"));
			String signature = requestView ? "AuthenticationServices.viewAnalytics" : "AuthenticationServices.editAnalytics";

			UserInfo info = UserInfo.validate(signature, context, token, null); 
			if (info != null && info.getErrorJson() != null)
				return sendInfoErrorWithJwt(Status.UNAUTHORIZED, info, token); //TODO: temp for debugging

			return Response.ok().build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	private Response sendInfoErrorWithJwt(Status unauthorized, UserInfo info, String token) {
		logger.severe(token);
		return sendInfoError (unauthorized, info);		
	}

	// validate a JWT without checking roles
	@GET
	@Path ("/validate")
	public Response validateJWT(@CookieParam(Constants.AIRLOCK_COOKIE) String cookie, @HeaderParam(Constants.AUTHENTICATION_HEADER) String token)
	{
		try {
			if (token == null) // JWT is  either a header token or a cookie
				token = cookie;

			UserInfo info = UserInfo.validate(null, context, token, null);
			if (info != null && info.getErrorJson() != null)
				return sendInfoError(Status.UNAUTHORIZED, info);

			return Response.ok().build();
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	// validate a single-sign-on JWT and issue an Airlock JWT to replace it
	@GET
	@Path ("/sso")
	public Response processSsoJwt(@QueryParam("key") String key, @HeaderParam(Constants.AUTHENTICATION_HEADER) String token)
	{
		if (token == null)
			return sendError(Status.BAD_REQUEST, "missing single-sign-on token");
		if (key == null)
			return sendError(Status.BAD_REQUEST, "missing single-sign-on key parameter");

		try {
			Authentication auth = Providers.get(context, Providers.Type.BLUEID, key);
			if (auth == null)
				return sendError(Status.BAD_REQUEST, Constants.SKIP_AUTHENTICATION);

			UserInfo info = auth.authenticate(new Authentication.Parms(token, UserRoles.get(context)));
			String jwt = JwtData.createJWT(info);
			return sendJwt(jwt);
		}
		catch (Exception e)
		{
			return sendError(Status.BAD_REQUEST, e.toString());
		}
	}

	Response sendErrorWithJwt(Status status, String err, String jwt)
	{
		logger.severe(jwt);
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}

	Response sendError(Status status, String err)
	{
		logger.severe(err);
		return Response.status(status).entity(Utilities.errorMsgToErrorJSON(err)).build();
	}
	Response sendInfoError(Status status, UserInfo info)
	{
		logger.severe(info.getErrorJson());		
		return Response.status(status).entity(info.getErrorJson()).build();
	}
}

