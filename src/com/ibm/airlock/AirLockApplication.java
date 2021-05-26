package com.ibm.airlock;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;


public class AirLockApplication extends Application {
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		classes.add(FeatureServices.class);
		classes.add(ProductServices.class);
		classes.add(InternalUserGroupServices.class);
		classes.add(AirlyticsServices.class);
		classes.add(AuthenticationServices.class);
		classes.add(OperationsServices.class);
		classes.add(TranslationServices.class);
		classes.add(AnalyticsServices.class);
		//classes.add(InAppPurchaseServices.class);
		
		classes.add(TestServices.class);
		
		
		classes.add(ApiDeclarationProvider.class);
		classes.add(ResourceListingProvider.class);
		classes.add(ApiListingResourceJSON.class);			
		
		return classes;
	}	
}
