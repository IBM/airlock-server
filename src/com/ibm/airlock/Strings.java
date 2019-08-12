package com.ibm.airlock;

/**
 * Created by amitaim on 13/07/2017.
 */
public class Strings
{
	public static String illegalProductUUID = "Illegal product-id GUID: ";
    public static String illegalSeasonUUID = "Illegal season-id GUID: ";
    public static String illegalFeatureUUID = "Illegal feature-id GUID: ";
    public static String illegalPurchaseUUID = "Illegal purchase-id GUID: ";
    public static String illegalNewParentUUID = "Illegal new-parent-id GUID: ";
    public static String illegalStringUUID = "Illegal string-id GUID: ";
    public static String illegalUtilityUUID = "Illegal utility-id GUID: ";
    public static String illegalStreamUUID = "Illegal stream-id GUID: ";
    public static String illegalNotificationUUID = "Illegal notification-id GUID: ";
    public static String illegalKeyUUID = "Illegal key-id GUID: ";
    public static String illegalParentUUID = "Illegal parent GUID: ";
    public static String illegalBranchUUID = "Illegal branch GUID: ";
    public static String illegalExperimentUUID = "Illegal experiment-id GUID: ";
    public static String illegalVariantUUID = "Illegal variant-id GUID: ";
    public static String illegalDestinationUUID = "Illegal dest-id GUID:";
    public static String illegalUserUUID = "Illegal user-id GUID: ";
    
    public static String illegalInputFieldJSONArray = "Illegal input fields JSON Array: ";
    public static String illegalInputJSON = "Illegal input JSON: ";
    public static String illegalStringJSON = "Invalid string in JSON";
    public static String missingKeyJSON = "The call body is missing a JSON key: ";
    public static String illegalFeatureAttibuteJSON = "Illegal feature attributes JSON Array:";

    public static String illegalMode = "Illegal mode '%s'. The legal values are : ";
    public static String illegalFormat = "Illegal format '%s'. The legal values are : ";
    public static String illegalStage = "Illegal stage '%s'. The legal values are : ";
    public static String illegalTypeLegalValuesAre = "Illegal type '%s'. The legal values are : ";
    public static String illegalSimulationType = "Illegal simulation type '%s'. The legal values are : ";
    public static String illegalGenerationMode = "Illegal generation mode '%s'. The legal values are : ";
    public static String illegalOutputMode = "Illegal output mode '%s'. The legal values are : ";
    public static String illegalStatus = "Illegal status '%s'. The legal statuses are :";
    public static String nonExistingSeason = "The season %s does not exist.";
    public static String nonExistingBranch = "Branch does not exists";
    public static String nonExistingFile = "The file does not exists";
    public static String failedUpgradingSeason = "Failed upgrading season %s : ";
    public static String failedUpgradingSeasonUtilities = "Failed upgrading season's %s utilities: ";
    public static String mustUpgradeSeason = "This season must be upgraded. Call upgrade and resubmit your request.";

    public static String productNotFound = "Product not found.";
    public static String seasonNotFound = "Season not found.";
    public static String AirlockItemNotFound = "Airlock item not found."; //for features/purchases/cr/or
    public static String utilityNotFound = "Utility not found.";
    public static String streamNotFound = "Stream not found.";
    public static String notificationNotFound = "Notification not found.";
    public static String apiKeyNotFound = "API key not found.";
    public static String parentNotFound = "Parent item not found.";
    public static String branchNotFound = "Branch not found.";
    public static String experimentNotFound = "Experiment not found.";
    public static String variantNotFound = "Variant not found.";
    public static String stringNotFound = "String not found.";
    public static String destinationNotFound = "Destination not found.";
    public static String parentNotFoundInSeason = "Parent feature is not in the specified season.";
    public static String typeNotFound = "No such feature type.";
    public static String platformNotFound = "Unknown platform: ";
    public static String attributesNotFound = " does not have attributes.";
    public static String airlockUserNotFound = "Airlock user not found.";
    public static String webhookNotFound = "Webhook not found.";
 
    
    

    public static String modeMissing = "The mode parameter is missing.";
    public static String stageMissing = "The stage parameter is missing.";
    public static String nameMissing = "The name parameter is missing.";
    public static String plateformMissing = "The platform parameter is missing.";
    public static String parentMissing = "The parent parameter is missing.";
    public static String typeMissing = "The type parameter is missing.";
    public static String lastModifiedMissing = "The lastmodified parameter is missing.";
    public static String generationModeMissing =  "The generationmode parameter is missing.";
    public static String minAppMissing = "The minAppVersion parameter is missing.";
    public static String simulationTypeMissing =  "The simulation type parameter is missing";
    public static String keyMissing = "The key '&s' is missing.";
    public static String statusMissing = "The status parameter is missing.";
    public static String idsMissing = "The ids parameter or ids body is missing";

    public static String prodAnalyticsUpdateError = "Unable to update analytics. Only a user with the Administrator or Product Lead role can update analytics for an item in the production stage.";
    public static String utilityNoValidationError = "Only a user with the Administrator role can create a utility without validating it.";
    public static String utilityUpdateNoValidationError = "Only a user with the Administrator role can update a utility without validating it.";
    public static String inputSchemaNoValidationError = "Only a user with the Administrator role can update an input schema without validating it.";
    public static String streamUpdateNoValidationError = "Only a user with the Administrator role can update a stream without validating it.";
    public static String streamNoValidationError = "Only a user with the Administrator role can create a stream without validating it.";
    public static String prodCheckoutError = "Unable to check out. Only a user with the Administrator or Product Lead role can check out a feature in the production stage to branches that are included in a production experiment.";
    public static String prodCancelCheckoutError = "Unable to cancel checkout. Only a user with the Administrator or Product Lead role can cancel the checkout of a feature in the production stage from branches that are included in a production experiment";
    public static String cancelCheckoutErrorNewSubItemsExist = "Unable to cancel checkout. The item has NEW sub-items in the branch."; 
    public static String featureNotVisibleInBranch = "Unable to checkout. The feature is not visible in the branch.";
    public static String prodFeatureAnalyticsError = "Unable to send the feature to analytics. Only a user with the Administrator or Product Lead role can send a feature in the production stage to analytics.";
    public static String prodFeatureStopAnalyticsError = "Unable to stop sending the feature to analytics. Only a user with the Administrator or Product Lead role can stop sending a feature the production stage to analytics.";
    public static String prodFeatureUpdateAnalyticsError = "Unable to update the feature attributes for analytics. Only a user with the Administrator or Product Lead role can update attributes for a feature in the production stage.";
    public static String prodOverrideError = "Unable to override the translation. Only a user with the Administrator or Product Lead role can override translation value for string in the production stage.";
    public static String prodCancelOverrideError = "Unable to cancel the translation override. Only a user with the Administrator or Product Lead role can cancel translation override for string in the production stage.";
    public static String prodReplaceTranslationError = "Unable to replace the translation. Only a user with the Administrator or Product Lead role can replace translations for a string in the production stage.";
    public static String prodCopyOverrideError = "Unable to override the given strings. Only a user with the Administrator or Product Lead role can perform actions on strings in the production stage";
    public static String prodActionStringsError = "Unable to perform the action on the given strings. Only a user with the Administrator or Product Lead role can perform actions on strings in the production stage";

    public static String skyppingEmptySeason = "skipping empty season ";
    public static String failedExport = "Failed to export the strings: ";
    public static String failedImport = "Failed to import strings: ";
    public static String failedWritingRole = "Failed writing the roles to S3: ";
    public static String failedWritingServer = "Failed writing the airlock servers to S3: ";
    public static String failedWritingUsers = "Failed writing the airlockUsers to S3: ";
    public static String failedWritingProduct = "Failed writing the product followers to S3: ";
    public static String failedGeneratingSample = "Failed to generate an input sample from the Input Schema ";
    public static String invalidJsonGeneratingSample = "Invalid JSON format when generating input sample from Input schema ";

    public static String failedGeneratingNotificationsOutputSample = "Unable to generate notification output sample from the notifications schema ";
    public static String invalidJsonGeneratingNotificationsOutputSample = "Invalid JSON format when generating output sample from notifications schema ";

    public static String failedDeletingProducts = "Failed deleting the product's folder from S3: ";
    public static String failedDeletingSeasons = "Failed deleting the season's files from S3: ";
    public static String failedDeletingBranch = "Failed deleting the branch %s from S3.: ";
    public static String failedDeletingString = "Failed deleting locale strings file from S3: ";

    public static String failedReadingWar = "Fail reading war manifest: ";
    public static String failedReadingFile = "Failed reading %s file from S3: ";
    public static String failedInitialization = "Failed initializing the Airlock service: ";
    public static String failedInitializationReadingFile = "Failed initializing the Airlock service. Unable to read the %s file from S3: ";
    public static String failedInitializationInvalidJson = "Failed initializing the Airlock service. The %s file is not in legal JSON format: ";
    public static String failedInitializationSeason = "Failed initializing the Airlock service. Season:";
    public static String unableToReadFile = " Initialization failed. Unable to read the %s files from S3: ";
    public static String failedInitializationSeasonUnexpectedFormat = " Initialization failed. One or more season files are not in the expected format: ";
    public static String failedInitializationFollowersUnexpectedFormat = "Failed initializing the Airlock service. Initialization failed. One or more followers files are not in the expected format: ";
    public static String failedInitializationFileUnexpectedFormat = " Initialization failed. The %s file is not in the expected format: ";

    public static String productWithId = "Cannot add a product that has a unique id.";
    public static String seasonWithId = "Cannot add a season that has a unique id.";
    public static String featureWithId = "Cannot add a feature that has a unique id.";
    public static String streamWithId = "Cannot add a stream that has a unique id.";
    public static String notificationWithId = "Cannot add a notification that has a unique id.";
    public static String utilityWithId = "Cannot add an utility that has a unique id.";
    public static String branchWithId = "Cannot add a branch that has a unique id.";
    public static String experimentWithId = "Cannot add an experiment that has a unique id.";
    public static String variantWithId = "Cannot add a variant that has a unique id.";
    public static String stringWithId = "Cannot add a string that has a unique id.";
    public static String apiKeyWithId = "Cannot add a api key that has a unique id.";
    public static String airlockUserWithId = "Cannot add a Airlock user that has a unique id.";
    public static String webhookWithId = "Cannot add Webhook that has a unique id.";

    public static String featureWithDifferentId = "The feature-id in the path is not the same as the uniqueId in the JSON.";
    public static String productWithDifferentId = "The product-id in the path is not the same as the uniqueId in the product JSON.";
    public static String experimentWithDifferentId = "The experiment-id in path is not the same as the uniqueId in experiment JSON.";
    public static String experimentProductWithDifferentId = "The product-id in the path is not the same as the productId in the experiment JSON.";
    public static String seasonWithDifferentId = "The season-id in the path is not the same as the uniqueId in season JSON.";
    public static String seasonProductWithDifferentId = "The product-id in the path is not the same as the productId in the season JSON.";
    public static String utilitySeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the utility JSON.";
    public static String featureSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the feature JSON.";
    public static String inputSchemaSeasonWithDifferentId = "The season-id in path is not the same as the seasonId in input schema JSON.";
    public static String branchSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the branch JSON.";
    public static String streamSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the stream JSON.";
    public static String notificationSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the notification JSON.";
    public static String airlockFeatureSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the AirlockFeatures JSON";
    public static String collectionSeasonWithDifferentId = "The season-id in path is not the same as the seasonId in global data collection JSON.";
    public static String branchFeatureWithDifferentId = "The feature-id in path is not the same as the uniqueId in feature JSON.";
    public static String streamWithDifferentId = "The stream-id in path is not the same as the uniqueId in stream JSON.";
    public static String notificationWithDifferentId = "The notification-id in path is not the same as the uniqueId in notification JSON.";
    public static String webhookWithDifferentId = "The webhook-id in path is not the same as the uniqueId in webhook JSON.";
    public static String apiKeyWithDifferentId = "The key-id in path is not the same as the uniqueId in Airlock API key JSON.";
    public static String branchWithDifferentId = "The branch-id in path is not the same as the uniqueId in branch JSON.";
    public static String branchFeatureWithDifferentSeason = "The version range of the feature is different than the version range of the branch";
    public static String variantExperimentWithDifferentId = "The experiment-id in the path is not the same as the experimentId in the variant JSON.";
    public static String variantWithDifferentId = "The variant-id in path is not the same as the uniqueId in variant JSON.";
    public static String stringSeasonWithDifferentId = "The season-id in the path is not the same as the seasonId in the string JSON";
    public static String stringWithDifferentId = "The string-id in path is not the same as the uniqueId in string JSON.";
    public static String userProductWithDifferentId = "The product-id in the path is not the same as the product Id in the Airlock user JSON.";
    public static String userWithDifferentId = "The user-id in path is not the same as the uniqueId in Airlock user JSON.";
    
    public static String productWithFeatureProd = "The product cannot be deleted. One or more of the items in the product's version range are in the production stage."; 
    public static String productWithStreamProd = "The product cannot be deleted. One or more of the streams in the product's version range are in the production stage.";
    public static String productWithNotificationProd = "The product cannot be deleted. One or more of the notifications in the product's version range are in the production stage.";
    public static String productWithExpProd = "The product cannot be deleted. One or more of the experiments in the product are in the production stage.";
    public static String seasonWithFeatureProd = "The version range cannot be deleted. One or more of the items in the version range are in the production stage."; 
    public static String seasonWithStreamProd = "The version range cannot be deleted. One or more of the streams in the version range are in the production stage.";
    public static String seasonWithNotificationProd = "The version range cannot be deleted. One or more of the notifications in the version range are in the production stage.";
    public static String seasonWithExpProd = "The version range cannot be deleted. One or more of the branches in the version range are used in an experiment.";
    public static String featureWithSubfeatureProd = "The item cannot be deleted. The item or one or more of its subitems are in the production stage.";
    public static String featureWithSubfeatureProdWithId = "The feature %s cannot be deleted. The feature or one or more of its subfeatures are in the production stage.";
    
    public static String featureAppearsInOrderingRule = "The feature cannot be deleted. The feature or one of its sub features appears in the '%s' ordering rule";
    public static String deleteFeatureRoot = "Cannot delete ROOT feature.";
    public static String copyFeatureRoot = "Cannot copy ROOT feature.";
    public static String copyOrderingRule = "Cannot copy ordering rule.";
    public static String importFeatureRoot = "Cannot import root feature.";
    public static String pastedNotCheckout = "Cannot paste an item under an item that is not checked out. First check out the parent item.";
    public static String importNotCheckout = "Cannot import a feature under an item that is not checked out. First check out the parent item.";
    public static String featureWithSubfeatures = "Cannot add a feature with subfeatures. Add the feature and its subfeatures one by one.";
    public static String featureWithConfigurations = "Cannot add a feature with configurations. Add the feature and its configurations one by one.";
    public static String featureWithOrderingRules = "Cannot add a feature with ordering rules. Add the feature and its ordering rules one by one.";
    public static String featureWithPurchaseOptions = "Cannot add a feature with purchase options.";
    public static String featureWithInAppPurchases = "Cannot add a feature with entitlemens.";

    public static String utilityProd = "Cannot delete utility in the production stage.";
    public static String streamProd = "Cannot delete stream in the production stage.";
    public static String notificationProd = "Cannot delete notification in the production stage.";
    public static String branchInUse = "Cannot delete the branch. It is part of ";
    public static String expInProd = "Cannot delete experiment in the production stage.";
    public static String variantInProd = "Cannot delete variant in the production stage.";
    public static String stringInProd = "Cannot delete string in the production stage.";

    public static String branchNotSupported = "The current version range does not support branches.";
    public static String streamsNotSupported = "The current version range does not support streams.";
    public static String notificationsNotSupported = "The current version range does not support notifications.";
    public static String experimentNotSupported = "The current version range does not support experiments.";
    public static String analyticsNotSupported = "The current version range does not support analytics.";
    public static String localNotSupported = "The given locale is not supported";
    public static String translationStatusNotSupported =  "The current version range does not support translation statuses.";
    public static String encryptionNotSupported = "The current version range does not support encryption.";
    public static String cSharpConstantsNotSupported = "The current version range does not support c sharp constants file.";
    public static String ServerDoesNotSupportEncryption = "The current server installation does not support runtime files encrypion.";

    
    public static String failPublishExps = "Fail publishing experiments list to Analytics server: ";
    public static String failAddingExperiment = "Failed to add experiment to the analytics server: ";
    public static String failDeletingExperiment = "Failed to delete the experiment from the analytics server: ";
    public static String failUpdatingExperiment = "Failed to update the experiment on the analytics server: ";
    public static String failUpdateAnalytics = "Fail to update analytics: ";
    public static String missingSeparator = "The request body does not contain the separator to distinguish between the utility and the ";
    public static String userNotLoggedIn = "User must be logged in to an authenticated server.";
    public static String cannotBeFollowed = " cannot be followed";
    public static String branchNotInSeason = "The current version range does not contain the given branch.";
    public static String mergeException = "An error occurred during branch building: MergeException: ";
    public static String missingAssetException = "An error occurred during assets validation: ";
    public static String typeCheckoutError = "Only FEATURE, MUTUAL_EXCLUSION_GROUP, ENTITLEMENT, ENTITLEMENT_MUTUAL_EXCLUSION_GROUP, PURCHASE_OPTIONS, PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP or ROOT types can be checked out.";
    public static String typeCancelCheckoutError = "Only checkouts of FEATURE, MUTUAL_EXCLUSION_GROUP, ENTITLEMENT, ENTITLEMENT_MUTUAL_EXCLUSION_GROUP, PURCHASE_OPTIONS, PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP or ROOT types can be cancelled.";
    public static String alreadyCheckout = "The feature was already checked out by another user.";
    public static String featureNotInBranch = "Feature is not in the specified branch.";
    public static String cannotRemoveFromBranch = "You cannot remove a feature from a branch if the feature has a new subfeature or configuration that only exists in the branch.";
    public static String expNotSupportedInSeason = "Experiments are not supported by some of the product version ranges that are within the experiment range.";
    public static String pathMissingFile = "Input path does not contain features file: ";
    public static String onlyFeaturesInAppPurchasesAndPurcahseOptions =  "Only features, inAppPurchase and purchaseOptions are allowed.";
    public static String illegalBranchName = "Illegal branch name. The branch name must be either MASTER or a legal branch GUID";
    public static String illegalQuota =  "Illegal quota. Must be an integer greater than or equal to 0.";
    public static String illegalIssueType =  "Illegal issue type ";
    public static String illegalIssueSubType =  "Illegal issue subtype ";
    public static String illegalIssueTypeAndSubtype =  "Illegal issue type/subtype combination: ";
    public static String translationAlreadyExist =  "Locale translations already exist.";
    public static String translationDoesNotExist =  "Locale translations does not exist.";
    public static String englishNotNeeded =  "English strings should not be translated.";
    public static String stageNotNeeded =  "In Airlock 2.0 and earlier, the stage should not be specified";
    public static String stringNotInSeason =  "The following string is not part of this season: ";
    public static String onlyFeatureConfigUseStrings =  "Only features or configurations can use strings";
    public static String nonExistingOverride =  "The specified string does not have an override value for the specified locale.";
    public static String noPendingTranslation =  "the string does not have a pending translation in locale ";
    public static String haveOverride =  "the string has an override in locale %s and cannot be replaced";
    public static String cannotRemove =  " cannot be removed.";
    public static String JSONException =  "An Json exception occurred when adding strings";
    public static String cannotOverrideProdDev =  "Cannot override string in production stage with string in development stage";
    public static String stringNotInStatus =  "String %s, %s is not in %s status and therefore cannot be %s";
    public static String smartlingNotConfigured =  "Smartling project is not configured for season: ";
    public static String translationNotRunning =  "translation services are not running";
    public static String stringNotSubmitted =  "String has not been submitted to Smartling.";
    public static String localeNotMapped = "locale '%s' is not mapped to a Smartling locale ID";
    public static String cannotUpdateIssue = "Unable to update issue status in the translation table: ";
    public static String cannotAddIssue = "Unable to add new issue to translation table: ";
    public static String issueWithLocal = "SOURCE issue must not specify a locale";
    public static String assetsMissing = "One or more asset (utility,context field,stream...) is missing in the destination";


    public static String failGetEventsFields = "Fail get events fields: ";
	public static String failGetExperimentFromAnalyticsServer = "Failed to get experiment data from the analytics server: ";
	public static String failGetExperimentsFromAnalyticsServer = "Failed to get data for experiments from the analytics server: ";
	public static String analyticsServerIsNotConfigured = "Cannot include indexing data when analytics server url is not configured.";
	public static String failResetExperimentDashboardInAnalyticsServer = "Failed to reset the dashboard on the analytics server: ";
	public static String analyticsServerNotConfigured = "Analytics server is not configured on this airlock server";
	
	public static String authNotConfigured = "Authentication is not configured on this server";
	//capabilities
	public static String productDoesNotEnableCapability = "%s is not included in product %s.";
	public static String capabilityNotEnabled = "%s is not included.";
		
	// engine
	public static String RuleFail = "Rule returned false";
	public static String RuleErrorFallback = "Rule error; result obtained from fallback";
	public static String RuleAndConfigurationError = "Rule and configuration errors; both obtained from fallback";
	public static String RuleConfigurationFallback = "Rule was successful, but configuration failed and was taken from fallback";
	public static String RuleConfigurationTurnoff ="Rule was successful, but configuration failed and the feature's fallback is off";
	public static String RuleDisabled = "Rule disabled";
	public static String RuleVersioned = "Application version is outside of feature version range";
	public static String RuleUserGroup = "Feature is in development and the device is not associated with any of the feature's internal user groups";
	public static String RuleParentFailed = "Parent feature is off";
	public static String RuleReorderFailed = "Failure in rule reordering: ";
	public static String RuleSkipped = "Feature is off because another feature in its mutual exclusion group is on";
	public static String RulePercentage = "Feature is turned off due to rollout percentage";
	public static String RuleFeatureTurnoff = "Feature was on, but was turned off by configuration rule";
	
	public static String changeAirlockSerevrStateTo = "Changing Airlock service state to ";
	public static String fieldIsMissing = "The %s field is missing.";
	public static String nonBooleanField = "The %s field should be boolean (true/false).";
	public static String malformedUrl = "Malformed URL: %s";
	public static String lastModifiedIsMissing = "The lastModified field is missing. This field must be specified during update.";
	public static String fieldCannotBeChangedDuringUpdate = "%s cannot be changed during update.";
	public static String seasonNotInProduct = "The version range is not in the product.";
	public static String itemChangedByAnotherUser = "The %s was changed by another user. Refresh your browser and try again.";
	public static String illegalType = "Illegal type: ";
	
	public static String systemIsNotAuthenticated = "The system is not configured for authentication. API key management is not supported.";

	public static String OnlyAdminAndOwnerCanDeleteKey = "Only an Administrator or the key owner can delete an API key.";
    public static String OnlyAdminAndOwnerCanUpdateKey = "Only an Administrator or the key owner can update an API key.";
    public static String KeyCannotDeleteItself = "Session initiated with an API key cannot delete that key."; 

    public static String globalUserDoesNotContainProductId = "The product-id should be null in the Airlock user JSON for global user.";
    public static String productUserDoesNotContainNullProductId = "The product-id should not be null in the Airlock user JSON for product user.";
	public static String notGlobalUser = "Fail to add user %s to product because the user does not exist in the global users list.";
	public static String userInProducts = "Fail to delete user because the user has permissions in the following products: %s";

	public static String illegalRuntimeEncryption = "The version range cannot be configured to encrypt runtime files while the product does not include the RUNTIME_ENCRYPTION capability.";
	public static String failReduceRuntimeEncryptionCapability = "Cannot remove the RUNTIME_ENCRYPTION capability while some version ranges are configured to encrypt runtime files. The following products have version ranges that are configured to encrypt runtime files: %s";
	public static String failReduceRuntimeEncryptionCapabilityFromProduct = "Cannot remove the RUNTIME_ENCRYPTION capability while some version ranges are configured to encrypt runtime files.";
	public static String seasonNotConfiguredForEncryption = "The version range is not configured with runtime encryption.";
			
	public static String noPurchaseWithGivenId = "The item with the given id '%s' does not exist";
	public static String notEntitlementId = "The included entitlement with the given id '%s' is not an entitlement";
	public static String duplicateIncludedEntitlementId = "The included entitlement id '%s' appears more than once.";
	public static String illegalEntitlementMTXParent = "Illegal parent. An entitlement mutual exclusion group can only reside under the root, entitlement or entitlement mutual exclusion.";
	public static String entitlementNotSupportedInVersion = "The current version range does not support entitlements.";
	public static String illegalPurchaseOptionsMTXParent = "Illegal parent. A purchase options mutual exclusion group can only reside under the entitlement or another purchase options mutual exclusion group.";
	public static String typeNotSupportedInFeaturesFunction = "The type '%s' is not supported in the features API."; //cannot call purchases items in features api
	public static String typeNotSupportedInPurchasesFunction = "The type '%s' is not supported in the purchases API."; //cannot call features/orderingRules ...  items in purchases api
	
	public static String duplicateStoreType = "The store type '%s' appears more than once.";
	
	public static String purchaseItemWithSubfeatures = "Cannot add a purchase item with subfeatures.";
    public static String purchaseItemWithConfigurations = "Cannot add a purchase item with configurations";
    public static String purchaseItemWithOrderingRules = "Cannot add a purchase item with ordering rules.";
    public static String purchaseItemWithPurchaseOptions = "Cannot add a purchase item with purchase options. Add the purchase and its purchase options one by one.";
    public static String purchaseItemWithEntitlements = "Cannot add a purchase item with entitlements. Add the purchase item and its sub purchase items one by one.";

    public static String purchaseItemNotFound = "Purchase item not found.";
    public static String failedInitializationSeasonPurchasesUnexpectedFormat = " Initialization failed. Entitlementד file is not in the expected format: ";
    public static String noEntitlementItem = "The specified entitlement does not exist.";
    public static String itemIsNotEntitlement = "The specified item is not an entitlement item.";
    public static String premiumFeatureWithoutEntitlement = "Cannot mark a feature as premium without specifiying the entitlement.";
    public static String premiumFeatureWithoutPremiumRule = "Cannot mark a feature as premium without specifiying the premiumRule.";
    public static String deletePurchaseRoot = "The ROOT purchase cannot be deleted.";
    public static String purchaseWithSubItemsProdWithId = "The purchase item %s cannot be deleted. The purchase item or one of its subitems are in the production stage.";
	public static String purchaseAttachedToFeature = "The entitlement cannot be deleted. It is attached to the premium faeture %s in branch %s.";
	public static String entitlementIncludedInOtherEntitlements = "The entitlement cannot be deleted. It is included in entitlement %s in branch %s.";
	public static String featureInProdEntitlementInDev = "Cannot attach an entitlement in the development stage to a feature in the production stage";
	public static String entitlementAttachedToProdFeature = "Cannot revert the entitlement to development. It is attached to the faeture %s in the production stage, in branch %s.";
	public static String illegalEntitlementParent = "Illegal parent. An entitlement can only be added under the ROOT, an entitlement or an entitlements mutual exclusion group.";
	public static String parentInPurchasesTree = "To add items to the purchases list please use the purchases API.";
	public static String parentInFeaturesTree = "To add items to the faetures list please use the features API.";
	public static String IncludedEntitlementCannotIncludeIteself = "Cannot update the entitlement. Entitlement cannot include itself.";
	public static String IncludedEntitlementCyclic = "Cannot update the entitlement. Tthe entitlement %s causes a cyclic dependency.";
	public static String IllegalStoreType = "Illegal store type. Store type can be either %s or %s";
	public static String notUniqueNameReservedEntitlementNamespace = "An item with the specified namespace and name already exists in the current version range and branch. Please nota that %s is a reserved namespase for entitlements items.";
	public static String notUniqueName = "An item with the specified namespace and name already exists in the current version range and branch. Periods and spaces are considered the same.";
	public static String entitlementWithNoStoreProductId = "Cannot attach the feature to an entitlement since the entitlement has no purchase options with a store product id.";
	public static String cannotDeletePurchaseOptionsLeavesEntitlementWithoutStoreProdId = "Cannot delete purcahse items. The parent entitlement that is attached to a premium feature will remain without a purchase option that has a store product id.";
	public static String cannotUpdatePurchaseOptionsLeavesEntitlementWithoutStoreProdId = "Cannot remove all store products from purchase options. The parent entitlement that is attached to a premium feature will remain without a purchase option that has a store product id.";
	public static String cannotUpdatePurchaseOptionsToDevelopment = "Cannot revert the purchase options to development. The parent entitlement that is attached to a premium feature will remain without a purchase option that has a store product id.";
	public static String allreadyFollowed = "%s is already being followed by user.";
	public static String cannotCopyMissingEntitlement = "Cannot copy feature. The entitlement %s is missing in the feature %s.";
	public static String cannotImportMissingEntitlement = "Cannot import feature. The entitlement %s is missing in feature %s.";
	public static String cannotImportMissingIncludedEntitlement = "Cannot import entitlement. The entitlement %s is missing in the entitlement bundle %s.";
	public static String cannotCopyMissingIncludedEntitlement = "Cannot copy entitlement. The entitlement %s is missing in entitlement bundle %s.";
	public static String entitlementsNotSupported = "The current version range does not support entitlements.";
	public static String cannotAddFeatureUnderEntitlementsRoot = "Cannot add a feature under the entitlements root.";
	public static String cannotAddFeatureUnderPurchaseOptions = "Cannot add a feature under a purchase options item.";
	public static String cannotAddFeatureUnderEntitlement = "Cannot add a feature under an entitlement.";
	public static String cannotAddFeatureUnderEntitlementMtx = "Cannot add a feature under an entitlements mutual exclusion group.";
	public static String cannotAddFeatureUnderPurchaseOptionsMtx = "Cannot add a feature under a purchase options mutual exclusion group.";
	public static String cannotAddFeatureMtxUnderEntitlementsRoot = "Cannot add a mutual exclusion group under the entitlements root.";
	public static String cannotAddEntitlementUnderFeaturesRoot = "Cannot add an entitlement under the features root.";
	public static String cannotAddEntitlementMtxUnderFeaturesRoot = "Cannot add an entitlement mutual exclusion group under the features root.";
	public static String entitlementInDifferentSeason = "The entitlement does not belong to the features's version range.";
	public static String noPermissionToUpdateEntitlemen = "Unable to update the entitlement. Only a user with an Administrator or Product Lead role can change a subitem that is in the production stage.";
	public static String illegalEntitlemenJson = "Illegal entitlements mutual exclusion JSON: ";
	public static String illagePurchaseOptionsParent = "Illegal parent. A purchase option can only be added under an entitlement or purchase options mutual exclusion group.";
	public static String noPermissionToUpdatePurchaseOptions = "Unable to update the purchase options. Only a user with an Administrator or Product Lead role can change a subitem that is in the production stage.";
	public static String illegalPurchaseOptionsJson = "Illegal purchase options mutual exclusion JSON: ";
    public static String entitlementForNonPremiumFeature = "An entitlement cannot be specified for a non premium feature.";
	
    
    public static String cannotCopyStringWithinTheSameSeason = "Cannot copy strings within the same version range. Use import/export instead.";
}
