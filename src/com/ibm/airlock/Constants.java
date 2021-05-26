package com.ibm.airlock;


public class Constants {
	public static final String FEATURES_DB_PARAM_NAME = "featuresDB";		
	public static final String PRODUCTS_DB_PARAM_NAME = "productsDB";
	public static final String FOLLOWERS_PRODUCTS_DB_PARAM_NAME = "followersProductsDB";
	public static final String FOLLOWERS_FEATURES_DB_PARAM_NAME = "followersFeaturesDB";
	public static final String SEASONS_DB_PARAM_NAME = "seasonsDB";
	public static final String ORIG_STRINGS_DB_PARAM_NAME = "originalStringsDB";
	public static final String EXPERIMENTS_DB_PARAM_NAME = "experimentsDB";
	public static final String VARIANTS_DB_PARAM_NAME = "variantsDB";
	public static final String UTILITIES_DB_PARAM_NAME = "utilitiesDB";
	public static final String STREAMS_DB_PARAM_NAME = "streamsDB";
	public static final String NOTIFICATIONS_DB_PARAM_NAME = "notificationsDB";
	public static final String BRANCHES_DB_PARAM_NAME = "branchesDB";
	public static final String COHORTS_DB_PARAM_NAME = "cohortsDB";
	public static final String DATA_IMPORT_DB_PARAM_NAME = "dataImportDB";
	public static final String ENTITIES_DB_PARAM_NAME = "entitiesDB";
	public static final String ATTRIBUTES_DB_PARAM_NAME = "attributesDB";
	public static final String ATTRIBUTE_TYPES_DB_PARAM_NAME = "attributeTypesDB";

	public static final String WEBHOOKS_PARAM_NAME = "webhooks";
	public static final String ROLES_PARAM_NAME = "roles";
	public static final String AIRLOCK_SERVERS_PARAM_NAME = "airlockServers";
	public static final String DATA_SERIALIZER_PARAM_NAME = "dataSerializer";
	public static final String INIT_FEATURES_DB_FILE_NAME = "initFeaturesDb.txt";
	public static final String INIT_PRODUCTS_DB_FILE_NAME = "initProductsDb.txt";
	public static final String GLOBAL_LOCK_PARAM_NAME = "globalLock";
	public static final String SKIP_AUTHENTICATION_PARAM_NAME = "skipAuth";
	public static final String RUNTIME_PUBLIC_FULL_PATH_PARAM_NAME = "runtimePublicFullPath";
	public static final String ANALYTICS_SERVER_URL_PARAM_NAME = "analyticsServerUrl";
	public static final String COHORTS_SERVER_URL_PARAM_NAME = "cohortsServerUrl";
	public static final String DATA_IMPORT_SERVER_URL_PARAM_NAME = "dataImportServerUrl";
	public static final String STORAGE_PUBLIC_PATH_PARAM_NAME = "storagePublicPath";
	public static final String API_KEYS_PARAM_NAME = "apiKeys";
	public static final String CAPABILITIES_PARAM_NAME = "capabilities";
	public static final String USER_ROLES_PER_PRODUCT_PARAM_NAME = "userRolesPerProduct";
	public static final String USER_GROUPS_PER_PRODUCT_PARAM_NAME = "userGroupsPerProduct";
	public static final String AIRLOCK_CHANGES_MAIL_ADDRESS_PARAM_NAME = "airlockChangesMailAddress";
    public static final String AIRLOCK_SERVER_DISPLAY_NAME_PARAM_NAME = "airlockServerDisplayName";
	public static final String AIRLOCK_API_KEYS_FILE_NAME = "AirlockAPIKeys.json";
	public static final String AIRLOCK_API_KEYS_PASSWORDS_FILE_NAME = "AirlockAPIKeysPasswords.json";
	public static final String AIRLOCK_CAPABILITIES_FILE_NAME = "AirlockCapabilities.json";
	public static final String AIRLOCK_USERS_DB_PARAM_NAME = "usersDB";
	public static final String AIRLOCK_GLOBAL_USERS_PARAM_NAME = "globalAirlockUsers";
	public static final String DB_HANDLER_PARAM_NAME = "dbHandler";
	public static final String ATHENA_HANDLER_PARAM_NAME = "athenaHandler";
	
	//public static final String RESET_ELASTIC_SCRIPT_FILE_NAME = "ops/resetElasticScript.json";
	
	public static final String IS_TEST_MODE = "isTestMode";
	public static final String CONSOLE_URL = "consoleUrl";
	public static final String SES_ENDPOINT = "sesEndpoint";
	
	public static final String JWT_EXPIRATION_MIN_PARAM_NAME = "jwtExpirationMin";
	public static final String AUDIT_LOG_WRITER_PARAM_NAME = "auditLogWriter";
	public static final String SERVICE_STATE_PARAM_NAME = "serviceState";
	//public static final String MINIMAL_USER_PROFILE = "minimalUserProfile";
	public static final String SCHEMA_VALIDATOR_SCRIPTS_FOLDER_NAME = "scripts";
	public static final String SCHEMA_VALIDATOR_AJV_PARAM_NAME = "ajv";
	public static final String SCHEMA_VALIDATOR_VALIDATOR_PARAM_NAME = "validator";
	public static final String SCHEMA_JSON_FAKER_PARAM_NAME = "jsonFaker";
	public static final String SCHEMA_JSON_PRUNE_PARAM_NAME = "jsonPrune";
	public static final String SCHEMA_JSON_GENERATOR_PARAM_NAME = "jsonGenerator";
	public static final String SCHEMA_VALIDATE_LEAVES_PARAM_NAME = "validateLeaves";
	public static final String JAVASCRIPT_UTILITIES_PARAM_NAME = "javascriptUtils";
	public static final String DEFAULT_NOTIFICATION_SCHEMA_PARAM_NAME = "defaultNotificationSchema";

	//common JSON fields
	public static final String JSON_FIELD_NAME = "name";
	public static final String JSON_FIELD_UNIQUE_ID = "uniqueId";
	public static final String JSON_FIELD_LAST_MODIFIED = "lastModified";
	public static final String JSON_FIELD_DESCRIPTION = "description";
	//public static final String JSON_FIELD_SEASON_NAME = "seasonName";
	public static final String JSON_FIELD_PRODUCT_NAME = "productName";
	public static final String JSON_FIELD_PRODUCTS = "products";
	public static final String JSON_FIELD_COHORTS = "cohorts";
	public static final String JSON_FIELD_COHORTS_DB_APP_NAME = "dbApplicationName";
	public static final String JSON_FIELD_INTERNAL_USER_GROUPS = "internalUserGroups";
	public static final String JSON_FIELD_INTERNAL_USER_GROUP = "internalUserGroup";	
	public static final String JSON_FIELD_STORAGE_PUBLIC_PATH = "s3Path";
	public static final String JSON_FIELD_DEV_STORAGE_PUBLIC_PATH = "devS3Path";
	public static final String JSON_FIELD_AIRLOCK_SERVERS = "airlockServers";
	public static final String JSON_FIELD_DEV_SERVER = "devServer";
	public static final String JSON_FIELD_STAGE_SERVER = "stageServer";
	public static final String JSON_FIELD_ROOT = "root";
	public static final String JSON_FIELD_DEFAULT_LANGUAGE = "defaultLanguage";
	public static final String JSON_FIELD_SUPPORTED_LANGUAGES = "supportedLanguages";
	public static final String JSON_FIELD_TRANSLATIONS = "translations";
	public static final String JSON_FIELD_JAVASCRIPT_UTILITIES = "javascriptUtilities";
	public static final String JSON_FIELD_GENERATED_ID = "id";
	public static final String JSON_FIELD_VALIDATION_MODE = "_validation_mode_";
	public static final String JSON_FIELD_STRING_IDS = "stringIds";
	public static final String JSON_FIELD_ENTITLEMENTS_ROOT = "entitlementsRoot";

	//data import
	public static final String JSON_FIELD_JOBS = "jobs";
	public static final String JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD = "pruneThreshold";

	
	//feature JSON fields
	public static final String JSON_FEATURE_FIELD_NAMESPACE = "namespace";
	public static final String JSON_FEATURE_FIELD_ADDITIONAL_INFO = "additionalInfo";
	public static final String JSON_FEATURE_FIELD_CONFIGURATION_SCHEMA = "configurationSchema";
	public static final String JSON_FEATURE_FIELD_CONFIGURATION = "configuration";
	public static final String JSON_FEATURE_FIELD_DEFAULT_CONFIG = "defaultConfiguration";
	public static final String JSON_FEATURE_FIELD_ENABLED = "enabled";
	public static final String JSON_FEATURE_FIELD_FEATURES = "features";
	public static final String JSON_FEATURE_FIELD_TYPE = "type";
	public static final String JSON_FEATURE_FIELD_SEASON_ID = "seasonId";	
	public static final String JSON_FEATURE_FIELD_STAGE = "stage";
	public static final String JSON_FEATURE_FIELD_PARENT = "parent";
	public static final String JSON_FEATURE_FIELD_CREATOR = "creator";
	public static final String JSON_FEATURE_FIELD_LEGACY_CREATOR = "_legacy";
	public static final String JSON_FEATURE_FIELD_OWNER = "owner";
	public static final String JSON_FEATURE_FIELD_CREATION_DATE = "creationDate";	
	public static final String JSON_FEATURE_FIELD_MIN_APP_VER = "minAppVersion";
	public static final String JSON_FEATURE_FIELD_RULE = "rule";
	public static final String JSON_FEATURE_FIELD_DEF_VAL = "defaultIfAirlockSystemIsDown";
	public static final String JSON_FEATURE_FIELD_PERCENTAGE = "rolloutPercentage";
	public static final String JSON_FEATURE_FIELD_PERCENTAGE_BITMAP = "rolloutPercentageBitmap";
	public static final String JSON_FEATURE_FIELD_MAX_FEATURES_ON = "maxFeaturesOn";
	public static final String JSON_FEATURE_FIELD_CONFIGURATION_RULES = "configurationRules";
	public static final String JSON_FEATURE_FIELD_ORDERING_RULES = "orderingRules";
	public static final String JSON_FEATURE_FIELD_ENTITLEMENTS = "entitlements";
	public static final String JSON_FEATURE_FIELD_PURCHASE_OPTIONS = "purchaseOptions";
	public static final String JSON_FEATURE_FIELD_PREMIUM_RULE = "premiumRule";
	
	public static final String JSON_FEATURE_FIELD_NO_CACHED_RES = "noCachedResults";
	public static final String JSON_FEATURE_FIELD_DISPLAY_NAME = "displayName";
	public static final String JSON_FEATURE_FIELD_FOLLOWERS = "followers";
	public static final String JSON_FEATURE_FIELD_IS_FOLLOWING = "isCurrentUserFollower";
	
	public static final String JSON_FIELD_BRANCH_STATUS = "branchStatus";
	public static final String JSON_FIELD_BRANCH_FEATURE_PARENT_NAME = "branchFeatureParentName";
	public static final String JSON_FIELD_BRANCH_CONFIGURATION_RULE_ITEMS = "branchConfigurationRuleItems";
	public static final String JSON_FIELD_BRANCH_ORDERING_RULE_ITEMS = "branchOrderingRuleItems";
	public static final String JSON_FIELD_BRANCH_PURCHASE_OPTIONS_ITEMS = "branchPurchaseOptionsItems";
	public static final String JSON_FIELD_BRANCH_ENTITLEMENT_ITEMS = "branchEntitlementItems";
	public static final String JSON_FIELD_BRANCH_FEATURES_ITEMS = "branchFeaturesItems";
	public static final String JSON_FIELD_BRANCH_ID = "branchId";
	
	//product JSON fields
	public static final String JSON_PRODUCT_FIELD_CODE_IDENTIFIER = "codeIdentifier";
	public static final String JSON_PRODUCT_FIELD_SEASONS = "seasons";
	public static final String JSON_PRODUCT_FIELD_FOLLOWERS = "followers";
	public static final String JSON_PRODUCT_FIELD_IS_FOLLOWING = "isCurrentUserFollower";
	public static final String JSON_FIELD_SMARTLING_PROJECT_ID = "smartlingProjectId";
	
	//season JSON fields
	public static final String JSON_SEASON_FIELD_MIN_VER = "minVersion";
	public static final String JSON_SEASON_FIELD_MAX_VER = "maxVersion";
	public static final String JSON_FIELD_PRODUCT_ID = "productId";
	public static final String JSON_FIELD_BRANCHES = "branches";
	public static final String JSON_FIELD_BRANCH = "branch";
	public static final String JSON_FIELD_SEASON = "season";
	public static final String JSON_FIELD_PLATRORMS = "platforms";
	public static final String JSON_FIELD_RUNTIME_ENCRYPTION = "runtimeEncryption";
	public static final String JSON_FIELD_IS_PART_OF_EXPERIMENT = "isPartOfExperiment";
	public static final String JSON_FIELD_BRANCH_MODIFICATION_DATE = "branchModificationDate";
	
	//followers
	public static final String JSON_FIELD_FOLLOWERS = "allFollowers";
	//Input schema
	public static final String JSON_FIELD_INPUT_SCHEMA = "inputSchema";	

	//Output schema
	public static final String ENABLE_FEATURE = "featureON";

	//utility
	public static final String JSON_FIELD_UTILITY = "utility";
	public static final String JSON_FIELD_UTILITIES = "utilities";	
	
	//rule JSON fields
	public static final String JSON_RULE_FIELD_RULE_STR = "ruleString";
	public static final String JSON_RULE_FIELD_FORCE = "force";
		
	//document links JSON fields	
	public static final String JSON_FIELD_DEFAULTS_FILE = "defaultsFile";
	
	public static final String JSON_FIELD_PLATFORM = "platform";
	public static final String JSON_FIELD_PLATFORMS = "platforms";
	public static final String JSON_FIELD_LINKS = "links";
	public static final String JSON_FIELD_LINK = "link";
	
	
	public static final String JSON_FIELD_IN_USE = "inUse";
	
	//entitlement
	public static final String JSON_FIELD_INCLUDED_ENTITLEMENTS = "includedEntitlements";
	public static final String JSON_FIELD_INCLUDED_ENTITLEMENTS_NAMES = "includedEntitlementsNames";
	public static final String JSON_FIELD_STORE_TYPE = "storeType";
	public static final String JSON_FIELD_STORE_PRODUCT_IDS = "storeProductIds";
	public static final String JSON_FIELD_PREMIUM = "premium";
	public static final String JSON_FIELD_ENTITLEMENT = "entitlement";
	public static final String RESERVED_ENTITLEMENTS_NAMESPACE = "airlockEntitlement";
	public static final String JSON_FIELD_ENTITLEMENT_NAME = "entitlementName";
		
	//roles
	public static final String JSON_FIELD_ROLES = "roles";
	public static final String JSON_FIELD_ROLE = "role";
	public static final String JSON_FIELD_DISPLAY_NAME = "displayName";
	public static final String JSON_FIELD_USERS = "users";
	public static final String JSON_FIELD_ACTIONS = "actions";	
	public static final String  JSON_FIELD_IS_GROUP_REPRESENTATION = "isGroupRepresentation";

	//about
	public static final String JSON_FIELD_BUILD_NUM = "buildNumber";
	public static final String JSON_FIELD_BUILD_DATE = "buildDate";
	public static final String JSON_FIELD_PRODUCT = "productName";
	
	//airlock user
	public static final String JSON_FIELD_IDENTIFIER = "identifier";
	public static final String JSON_FIELD_ROLE_SETS = "userRoleSets";

	//cohorts

	public static final String COHORT_EXPORT_TYPE_LOCALYTICS = "Localytics";
	public static final String COHORT_EXPORT_TYPE_DB_ONLY = "DB Only";
	public static final String JSON_FIELD_COHORT_QUERY = "queryCondition";
	public static final String JSON_FIELD_JOINED_TABLES = "joinedTables";
	public static final String JSON_FIELD_EXPORT_TYPE = "exportType";
	public static final String JSON_FIELD_COHORT_FREQUENCY = "updateFrequency";
	public static final String JSON_FIELD_COHORT_EXPORT = "export";
	public static final String JSON_FIELD_COHORT_EXPORT_KEY= "exportKey";
	public static final String JSON_FIELD_COHORT_EXPORTS = "exports";
	public static final String JSON_FIELD_COHORT_EXPORT_STATUSES = "statuses";
	public static final String JSON_FIELD_COHORT_STATUS = "status";
	public static final String JSON_FIELD_COHORT_RETRIES_NUMBER = "retriesNumber";
	public static final String JSON_FIELD_COHORT_CALCULATION_STATUS = "calculationStatus";
	public static final String JSON_FIELD_COHORT_CALCULATION_STATUS_MESSAGE = "calculationStatusMessage";
	public static final String JSON_FIELD_COHORT_VALUE_TYPE = "valueType";
	public static final String JSON_FIELD_COHORT_EXPORT_STATUS = "exportStatus";
	public static final String JSON_FIELD_COHORT_EXPORT_STATUS_DETAILS = "exportStatusDetails";
	public static final String JSON_FIELD_COHORT_EXPORT_STATUS_MESSAGE = "exportStatusMessage";

	public static final String JSON_FIELD_COHORT_STATUS_MESSAGE = "statusMessage";
	public static final String JSON_FIELD_COHORT_STATUS_DETAILS = "statusDetails";
	public static final String JSON_FIELD_COHORT_AIRLYTICS_STATUS_DETAILS = "airlyticsStatusDetails";
	public static final String JSON_FIELD_COHORT_THIRD_PARTY_STATUS_DETAILS = "thirdPartyStatusDetails";
	public static final String JSON_FIELD_COHORT_LAST_EXPORT_TIME = "lastExportTime";
	public static final String JSON_FIELD_COHORT_EXPORT_NAME = "exportName";
	public static final String JSON_FIELD_COHORT_USERS_NUMBER = "usersNumber";
	public static final String JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE = "queryAdditionalValue";
	//cohorts status details
	public static final String JSON_FIELD_COHORT_STATUS_ACTIVITY_ID = "activityId";
	public static final String JSON_FIELD_COHORT_STATUS_DETAILED_MESSAGE = "detailedMessage";
	public static final String JSON_FIELD_COHORT_STATUS_FAILED_IMPORTS = "failedImports";
	public static final String JSON_FIELD_COHORT_STATUS_PARSED_IMPORTS = "parsedImports";
	public static final String JSON_FIELD_COHORT_STATUS_SUCCESFUL_IMPORTS = "successfulImports";
	public static final String JSON_FIELD_COHORT_STATUS_TOTAL_IMPORTS = "totalImports";

	//data import
	public static final String JSON_FIELD_DATA_IMPORT_S3_FILE = "s3File";
	public static final String JSON_FIELD_DATA_IMPORT_OVERWRITE = "overwrite";
	public static final String JSON_FIELD_DATA_IMPORT_TARGET_TABLE = "targetTable";
	public static final String JSON_FIELD_DATA_IMPORT_STATUS = "status";
	public static final String JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE = "statusMessage";
	public static final String JSON_FIELD_DATA_IMPORT_STATUS_DETAILS = "detailedMessage";
	public static final String JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS = "successfulImports";
	public static final String JSON_FIELD_DATA_IMPORT_STATUS_AFFECTED_COLUMNS = "affectedColumns";

	//manifest attributes
	public static final String MANIFEST_ATT_BUILD_NUM = "Build-Number";
	public static final String MANIFEST_ATT_BUILD_DATE = "Build-Date";
	public static final String MANIFEST_ATT_PRODUCT_NAME = "Product-Name";
	
	
	public static final String OPERATIONS_FOLDER_NAME = "ops/";
	public static final String PRODUCTS_FILE_NAME = "products.json";
	public static final String PRODUCT_RUNTIME_FILE_NAME = "productRuntime.json";
	public static final String USER_GROUPS_FILE_NAME = "userGroups.json";
	public static final String USER_GROUPS_RUNTIME_FILE_NAME = "AirlockUserGroupsRuntime.json";
	public static final String ROLES_FILE_NAME = "ops/roles.json";
	public static final String WEBHOOKS_FILE_NAME = "ops/webhooks.json";
	public static final String AIRLOCK_SERVERS_FILE_NAME = "ops/airlockServers.json";
	public static final String AIRLOCK_USERS_FILE_NAME = "airlockUsers.json";
	public static final String SEASONS_FOLDER_NAME = "seasons";
	public static final String OKTA_CONFIGS_FOLDER_NAME = "oktaConfigurations";
	public static final String AIRLOCK_RUNTIME_PRODUCTION_FILE_NAME = "AirlockRuntimePRODUCTION.json";
	public static final String AIRLOCK_RUNTIME_FILE_NAME = "AirlockRuntime.json";
	public static final String AIRLOCK_RUNTIME_DEVELOPMENT_FILE_NAME = "AirlockRuntimeDEVELOPMENT.json";
	public static final String AIRLOCK_DEFAULTS_FILE_NAME = "AirlockDefaults.json";
	public static final String AIRLOCK_JAVA_FILE_NAME = "AirlockConstants.java";
	public static final String AIRLOCK_SWIFT_FILE_NAME = "AirlockConstants.swift";
	public static final String AIRLOCK_C_SHARP_FILE_NAME = "AirlockConstants.cs";
	public static final String AIRLOCK_FEATURES_FILE_NAME = "AirlockFeatures.json";
	public static final String AIRLOCK_STREAMS_FILE_NAME = "AirlockStreams.json";
	public static final String AIRLOCK_NOTIFICATIONS_FILE_NAME = "AirlockNotifications.json";
	public static final String AIRLOCK_STREAMS_EVENTS_FILE_NAME = "AirlockStreamsEvents.json";
	public static final String TRANSLATIONS_FOLDER_NAME = "translations"; 
	public static final String ORIGINAL_STRINGS_FILE_NAME = "original.json";
	//public static final String EN_STRINGS_FILE_NAME = "strings__en.json";
	public static final String JAVASCRIPT_UTILITIES_FILE_NAME = "javascriptUtilities.txt";
	public static final String DEFAULT_NOTIFICATION_SCHEMA_FILE_NAME = "defaultNotificationSchema.txt";
	public static final String AIRLOCK_ENTITLEMENTS_FILE_NAME = "AirlockEntitlements.json";


	public static final String AIRLOCK_SERVER_INFO_FILE_NAME = "AirlockServerInfo.json";
	public static final String AIRLOCK_ENCRYTION_KEY_FILE_NAME = "AirlockEncryptionKey.json";
	public static final String FOLLOWERS_PRODUCTS_FILE_NAME = "followersProducts.json";
	public static final String FOLLOWERS_FEATURES_FILE_NAME = "followersFeatures.json";
	public static final String RUNTIME_DEFAULTS_ZIP_FILE_NAME = "runtimeDefaults.zip";
	public static final String EMAILS_FILE_NAME = "emails.json";
	public static final String TESTS_PATH = "Tests";

	public static final String DEFAULT_LANGUAGE = "en";
	public static final String STRINGS_FILE_NAME_PREFIX = "strings__";
	public static final String STRINGS_FILE_NAME_EXTENSION = ".json";
	
	public static final String AIRLOCK_INPUT_SCHEMA_FILE_NAME = "AirlockInputShema.json";
	public static final String AIRLOCK_ANALYTICS_FILE_NAME = "AirlockAnalytics.json";
	public static final String AIRLOCK_EXPERIMENTS_FILE_NAME = "AirlockExperiments.json";

	public static final String AIRLOCK_COHORTS_FILE_NAME = "AirlockCohorts.json";
	public static final String AIRLYTICS_DATA_IMPORT_FILE_NAME = "AirlyticsDataImport.json";
	
	public static final String AIRLOCK_UTILITIES_DEVELOPMENT_FILE_NAME = "AirlockUtilitiesDEVELOPMENT.txt";
	public static final String AIRLOCK_STREAMS_UTILITIES_DEVELOPMENT_FILE_NAME = "AirlockStreamsUtilitiesDEVELOPMENT.txt";
	public static final String AIRLOCK_UTILITIES_PRODUCTION_FILE_NAME = "AirlockUtilitiesPRODUCTION.txt";
	public static final String AIRLOCK_STREAMS_UTILITIES_PRODUCTION_FILE_NAME = "AirlockStreamsUtilitiesPRODUCTION.txt";
	public static final String AIRLOCK_STREAMS_DEVELOPMENT_FILE_NAME = "AirlockStreamsDEVELOPMENT.json";
	public static final String AIRLOCK_STREAMS_PRODUCTION_FILE_NAME = "AirlockStreamsPRODUCTION.json";
	public static final String AIRLOCK_NOTIFICATIONS_DEVELOPMENT_FILE_NAME = "AirlockNotificationsDEVELOPMENT.json";
	public static final String AIRLOCK_NOTIFICATIONS_PRODUCTION_FILE_NAME = "AirlockNotificationsPRODUCTION.json";
	public static final String AIRLOCK_UTILITIES_FILE_NAME = "AirlockUtilities.json";
	public static final String AIRLOCK_BRANCHES_FILE_NAME = "AirlockBranches.json";
	public static final String AIRLOCK_RUNTIME_BRANCHES_FILE_NAME = "AirlockBranchesRuntime.json";	
	public static final String AIRLOCK_RUNTIME_BRANCH_DEVELOPMENT_FILE_NAME = "AirlockRuntimeBranchDEVELOPMENT.json";
	public static final String AIRLOCK_RUNTIME_BRANCH_PRODUCTION_FILE_NAME = "AirlockRuntimeBranchPRODUCTION.json";
	public static final String AIRLOCK_BRANCHES_FOLDER_NAME = "branches";
	public static final String AIRLOCK_BRANCH_FEATURES_FILE_NAME = "AirlockBranchFeatures.json";
	public static final String AIRLOCK_BRANCH_ANALYTICS_FILE_NAME = "AirlockBranchAnalytics.json";
	public static final String AIRLOCK_BRANCH_ENTITLEMENTS_FILE_NAME = "AirlockBranchEntitlements.json";
	
	public static final String JSON_FIELD_CONTEXT = "context";

	
	public static final String JSON_FIELD_VERSION = "version";
	
	//environment parameters
	public static final String ENV_STORAGE_PARAMS = "STORAGE_PARAMS";
	public static final String ENV_SERVER_NAME = "SERVER_NAME";
	public static final String ENV_SKIP_AUTHENTICATION = "SKIP_AUTHENTICATION";
	public static final String ENV_JWT_EXPIRATION_MIN = "JWT_EXPIRATION_MIN";
	public static final String ENV_OKTA_APPLICATIONS = "OKTA_APPLICATIONS"; //list of accepted okta applications - separated by ;
	public static final String ENV_SERVER_DISPLAY_NAME = "AIRLOCK_SERVER_DISPLAY_NAME";
	public static final String ENV_CONSOLE_URL = "CONSOLE_URL";
	public static final String ENV_SES_ENDPOINT = "SES_ENDPOINT";
	public static final String ENV_AIRLOCK_COOKIE_DOMAIN = "AIRLOCK_COOKIE_DOMAIN";
	public static final String ENV_AIRLOCK_CHANGES_MAIL_ADDRESS = "AIRLOCK_CHANGES_MAIL_ADDRESS";
	public static final String ENV_SEND_GRID_API_KEY = "SEND_GRID_API_KEY";
	public static final String ENV_AZURE_TENANT = "AZURE_TENANT";
	public static final String ENV_AZURE_CLIENT_ID = "AZURE_CLIENT_ID";
	public static final String ENV_AZURE_CLIENT_SECRET = "AZURE_CLIENT_SECRET";
	public static final String ENV_AUTHENTICATION_PROVIDER_TYPE = "AUTHENTICATION_PROVIDER_TYPE";
	public static final String ENV_EMAIL_PROVIDER_TYPE = "EMAIL_PROVIDER_TYPE";
	public static final String ENV_LOGS_FOLDER_PATH = "LOGS_FOLDER_PATH";
	public static final String ENV_ATHENA_REGION = "ATHENA_REGION";
	public static final String ENV_ATHENA_OUTPUT_BUCKET = "ATHENA_OUTPUT_BUCKET";
	public static final String ENV_ATHENA_CATALOG = "ATHENA_CATALOG";
	public static final String ENV_DB_SECRET_NAME = "DB_SECRET_NAME";
	public static final String ENV_SECRET_MANAGER_REGION = "SECRET_MANAGER_REGION";
	
	
	
	public static final String ENV_TRANSLATOR_MODE = "TRANSLATOR_MODE"; // OFF/SMARTLING
	public static final String ENV_SMARTLING_CONFIG_FILE = "SMARTLING_CONFIG_FILE";
	public static final String SMARTLING_CONFIG_FILE_DEFAULT = "smartlingConfig.json";
	public static final String SMARTLING_LOCALE_FILE_OVERRIDE = "smartlingLocales.txt";
	public static final String ENV_TRANSLATOR_NEW_STRING_PAUSE = "TRANSLATOR_NEW_STRING_PAUSE";
	public static final int    TRANSLATOR_NEW_STRING_DEFAULT = 30; 
	public static final String ENV_TRANSLATION_PAUSE = "TRANSLATION_PAUSE";
	public static final int    ENV_TRANSLATION_PAUSE_DEFAULT = 60; 
	public static final String ENV_RETRANSLATION_PAUSE = "RETRANSLATION_PAUSE";
	public static final int    ENV_RETRANSLATION_PAUSE_DEFAULT = 24 * 60 * 60; // (1 day)
	public static final int    TRANSLATOR_WAIT_AFTER_NEW_STRING = 30;

	//Analytics server
	public static final String ENV_ANALYTICS_SERVER_URL = "ANALYTICS_SERVER_URL";

	//AirCohorts server
	public static final String ENV_COHORTS_SERVER_URL = "COHORTS_SERVER_URL";

	//DataImport server
	public static final String ENV_DATA_IMPORT_SERVER_URL = "DATA_IMPORT_SERVER_URL";

	public static final String SERVER_DEFAULT_DISPLAY_NAME = "MAIN_SERVER";

	//AWS_ACCESS_KEY_ID
	//AWS_SECRET_ACCESS_KEY

	//authentication parameters
	public static final String AUTHENTICATION_HEADER = "sessionToken";
	public static final String AIRLOCK_COOKIE = "airlock_token";
	public static final String AUTHENTICATION_SAML = "SAMLResponse";
	public static final String AUTHENTICATION_CONFIG = "SAMLConfig";
	public static final String AUTHENTICATION_METHODS = "MethodPermissions";
	public static final String SKIP_AUTHENTICATION = "system is not configured with authentication";
	public static final String UNAUTHORIZED = "user is not authorized";
	public static final String AUTHENTICATION_SAML_FILES = "saml-files.txt";
	public static final String AUTHENTICATION_KEY_FILES = "key-files.txt";
	public static final String PROVIDERS = "providers";
	public static final String USER_ROLES = "userRoles";
	public static final String JSON_FIELD_USER = "user";
	
	public static final String BACKGROUND_TRANSLATOR = "BACKGROUND_TRANSLATOR"; // context listener key
	public static final String SMARTLING_PLACEHOLDER_FORMAT = "\\[\\[\\[(\\d+)\\]\\]\\]";
	public static final String NEW_VARIANT = "NEW";

	//encryption key
	public static final String JSON_FIELD_ENCRYPTION_KEY = "encryptionKey";
 	//String translations
	public static final String JSON_FIELD_VALUE = "value";
	public static final String JSON_FIELD_KEY = "key";
	public static final String JSON_FIELD_INTERNATIONAL_FALLBACK = "internationalFallback";
	public static final String JSON_FIELD_STRINGS = "strings";
	public static final String JSON_FIELD_MOST_RECENT_TRANSLATION = "mostRecentTranslation";
	public static final String JSON_FIELD_STRING_ID = "stringId";
	public static final String JSON_FIELD_TRANSLATION = "translation";
	public static final String JSON_FIELD_TRANSLATED_VALUE = "translatedValue";
	public static final String JSON_FIELD_SHADOW_VALUE = "shadowValue";
	//public static final String JSON_FIELD_ISSUE = "issue";
	//public static final String JSON_FIELD_ISSUE_ID = "issueId";
	public static final String JSON_FIELD_ISSUE_STATUS = "issueStatus";
	public static final String JSON_FIELD_SAME_STRINGS = "sameStrings";
	public static final String JSON_FIELD_SAME_STRINGS_AND_VARIANTS = "sameStringsAndVariants";
	public static final String JSON_FIELD_SAME_STRINGS_OTHER_VARIANTS = "sameStringsOtherVariants";
	//public static final String JSON_FIELD_OVERRIDE_VALUE = "overrideValue";
	public static final String JSON_FIELD_TRANSLATION_STATUS = "translationStatus";
	public static final String JSON_FIELD_LAST_SOURCE_MODIFICATION = "lastSourceModification";
	public static final String JSON_FIELD_TRANSLATOR_ID = "translatorId"; // hash supplied by Smartling or other 3rd party translators
	public static final String JSON_FIELD_STATUS = "status";
	public static final String JSON_FIELD_STRING_STATUSES = "stringsStatuses";
	public static final String JSON_FIELD_TRANSLATION_SUMMARY = "translationSummary";
	public static final String JSON_FIELD_lOCALE = "locale";
	public static final String JSON_FIELD_STRINGS_IN_USE_BY_UTIL = "stringsInUseByUtilities";
	public static final String JSON_FIELD_STRINGS_IN_USE_BY_CONFIG = "stringsInUseByConfiguration";
	public static final String JSON_FIELD_STRINGS_OLD_VARIANT = "variant";
	public static final String JSON_FIELD_STRINGS_VARIANT = "translationVariant";
	public static final String JSON_FIELD_SMARTLING_PROCESS = "smartlingCreationProcess";
	public static final String JSON_FIELD_TRANSLATION_INSTRUCTION = "translationInstruction";
	public static final String JSON_FIELD_MAX_STRING_SIZE = "maxStringSize";
	public static final String JSON_FIELD_NEW_TRANSLATION_AVAILABLE = "newTranslationAvailable";

	//Airlock servers
	public static final String JSON_FIELD_URL = "url";
	public static final String JSON_FIELD_CDN_OVERRIDE = "cdnOverride";
	public static final String JSON_FIELD_DEFAULT_SERVER = "defaultServer";
	public static final String JSON_FIELD_SERVERS = "servers";
	
	//Airlock server info
	public static final String JSON_FIELD_SERVER_VERSION = "serverVersion";
	
	//Copy 
	public static final String JSON_FIELD_ILLEGAL_GIVEN_MIN_APP_VER = "illegalGivenMinAppVersion";
	public static final String JSON_FIELD_ILLEGAL_MIN_APP_VER = "illegalMinAppVersion";
	public static final String JSON_FIELD_ILLEGAL_NAME = "illegalName";
	public static final String JSON_FIELD_MISSING_ASSETS = "missingAssets";
	public static final String JSON_FIELD_ILLEGAL_ID = "illegalId";
	public static final String JSON_FIELD_UPDATED_SEASONS_FEATURES = "updatedSeasonsFeatures";
	public static final String JSON_FIELD_UPDATED_SEASONS_ENTITLEMENTS = "updatedSeasonsEntitlements";
	public static final String JSON_FIELD_NEW_SUBTREE_ID = "newSubTreeId";
	public static final String JSON_FIELD_STRING_CONFLICTS = "stringsInConflict";
	public static final String JSON_FIELD_STRING_CONFLICTS_SIZE = "stringsInConflictSize";
	public static final String JSON_FIELD_STRING_OVERRIDE = "stringsOverride";
	public static final String JSON_FIELD_STRING_OVERRIDE_SIZE = "stringsOverrideSize";
	public static final String JSON_FIELD_STRING_ADDED = "addedStrings";
	public static final String JSON_FIELD_STRING_ADDED_SIZE = "addedStringsSize";
	public static final String JSON_FIELD_STRING_NON_CONFLICT = "nonConflictingStrings";
	public static final String JSON_FIELD_STRING_NON_CONFLICT_SIZE = "nonConflictingStringsSize";
	public static final String JSON_FIELD_ERROR = "error";
			
	//InputSchema validation results
	public static final String JSON_FIELD_BROKEN_CONFIGS = "brokenConfigurations";
	public static final String JSON_FIELD_BROKEN_RULES = "brokenRules";
	public static final String JSON_FIELD_DELETED_ANALYTICS_INPUT_FIELDS = "deletedAnalyticsInputFields";
	public static final String JSON_FIELD_PRON_ANALYTICS_ITEMS_QUOTA_EXCEEDED = "productionAnalyticsItemsQuotaExceeded"; 
	public static final String JSON_FIELD_BROKEN_EXPERIMENTS = "brokenExperiments";
	public static final String JSON_FIELD_BROKEN_VARIANTS = "brokenVariants";
	public static final String JSON_FIELD_BROKEN_NOTIFICATION_RULRES = "brokenNotificationRules";
	public static final String JSON_FIELD_BROKEN_NOTIFICATION_CONFIGS = "brokenNotificationConfigurations";
	
	public static final String JSON_FIELD_WARNING = "warning";
	public static final int DEFAULT_ANALYTICS_QUOTA = 100;

	
	//Analytics
	public static final String JSON_FIELD_FEATURES_CONFIGS_FOR_ANALYTICS = "featuresAndConfigurationsForAnalytics";
	public static final String JSON_FIELD_FEATURES_ATTRIBUTES_FOR_ANALYTICS = "featuresAttributesForAnalytics";

	public static final String JSON_FIELD_GLOBAL_DATA_COLLECTION = "globalDataCollection";
	public static final String JSON_FIELD_ANALYTICS_QUOTA = "analyticsQuota";

	public static final String JSON_FIELD_ANALYTICS_DATA_COLLECTION = "analyticsDataCollection";
	public static final String JSON_FIELD_SEND_TO_ANALYTICS = "sendToAnalytics";
	public static final String JSON_FIELD_ATTRIBUTES_FOR_ANALYTICS = "configAttributesForAnalytics";
	public static final String JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS = "inputFieldsForAnalytics";
	public static final String JSON_FIELD_EXPERIMENT_LIST = "experimentList";
	public static final String JSON_FIELD_EXPERIMENT = "experiment";
	public static final String JSON_FIELD_VARIANT = "variant";
	public static final String JSON_FIELD_ANALYTICS_BY_FEATURE_NAMES = "analyticsDataCollectionByFeatureNames";
	public static final String JSON_FIELD_DEVELOPMENT_ANALYTICS_COUNT = "developmentItemsReportedToAnalytics";
	public static final String JSON_FIELD_PRODUCTION_ANALYTICS_COUNT = "productionItemsReportedToAnalytics";
	public static final String JSON_FIELD_ID = "id";
	public static final String JSON_FIELD_ATTRIBUTES = "attributes";
	public static final String JSON_FIELD_ANALYTICS = "analytics";
	public static final String JSON_FIELD_REORDERED_CHILDREN = "reorderedChildren";

	//Experiments
	public static final String JSON_FIELD_EXPERIMENT_ID = "experimentId";
	public static final String JSON_FIELD_EXPERIMENT_NAME = "experimentName";
	public static final String JSON_FIELD_EXPERIMENTS = "experiments";
	public static final String JSON_FIELD_VARIANTS = "variants";
	public static final String JSON_FIELD_VARIANTS_DESCRIPTIONS = "variantsDescriptions";
	public static final String JSON_FIELD_VARIANTS_DISPLAY_NAMES = "variantsDisplayNames";
	public static final String JSON_FIELD_BRANCH_NAME = "branchName";
	public static final String JSON_FIELD_HYPOTHESIS = "hypothesis";
	public static final String JSON_FIELD_MEASUREMENTS = "measurements";
	public static final String JSON_FIELD_AVALIABLE_IN_ALL_SEASONS = "availableInAllSeasons";
	public static final String JSON_FIELD_AVALIABLE_IN_SOME_SEASONS = "availableInSomeSeasons";
	public static final String JSON_FIELD_MAX_EXPERIMENTS_ON = "maxExperimentsOn";
	public static final String JSON_FIELD_CONTROL_GROUP_VARIANTS = "controlGroupVariants";
	public static final String JSON_FIELD_INDEX_EXPERIMENT = "indexExperiment";
	public static final String JSON_FIELD_INDEXING_INFO = "indexingInfo";
	
	//Streams
	public static final String JSON_FIELD_STREAMS = "streams";
	public static final String JSON_FIELD_FILTER = "filter";
	public static final String JSON_FIELD_PROCESSOR = "processor";
	public static final String JSON_FIELD_RESULTS_SCHEMA = "resultsSchema";
	public static final String JSON_FIELD_CACHE_SIZE_KB = "cacheSizeKB";
	public static final String JSON_FIELD_QUEUE_SIZE_KB = "queueSizeKB";
	public static final String JSON_FIELD_MAX_QUEUED_EVENTS = "maxQueuedEvents";
	public static final String JSON_FIELD_EVENTS = "events";
	public static final String JSON_FIELD_EVENT_DATA = "eventData";
	public static final String JSON_FIELD_ENABLE_HISTORICAL_EVENTS = "enableHistoricalEvents";
	public static final String JSON_FIELD_MAX_HISTORY_SIZE_KB = "maxHistoryTotalSizeKB";
	public static final String JSON_FIELD_BULK_SIZE = "bulkSize";
	public static final String JSON_FIELD_FILE_SIZE_KB = "historyFileMaxSizeKB";
	public static final String JSON_FIELD_KEEP_HISTORY_OF_LAST_NUM_DAYS = "keepHistoryOfLastNumberOfDays";
	public static final String JSON_FIELD_HISTORY_BUFFER_SIZE = "historyBufferSize";
	public static final String JSON_FIELD_OPERATE_ON_HISTORICAL_EVENTS = "operateOnHistoricalEvents";
	public static final String JSON_FIELD_LIMIT_BY_START_DATE = "limitByStartDate";
	public static final String JSON_FIELD_LIMIT_BY_END_DATE = "limitByEndDate";
	public static final String JSON_FIELD_PROCESS_EVENTS_OF_LAST_NUMBER_OF_DAYS = "processEventsOfLastNumberOfDays";
	
	//Key management
	public static final String JSON_FIELD_AIRLOCK_KEY_PASSWORD = "keyPassword";
	public static final String JSON_FIELD_LAST_USED = "lastUsed";
	public static final String JSON_FIELD_AIRLOCK_API_KEYS = "airlockAPIKeys";
	
	//Notifications
	public static final String JSON_FIELD_REGISTRATION_RULE = "registrationRule";
	public static final String JSON_FIELD_CANCELLATION_RULE = "cancellationRule";
	public static final String JSON_FIELD_NOTIFICATIONS = "notifications";
	public static final String JSON_FIELD_MAX_NOTIFICATIONS = "maxNotifications";
	public static final String JSON_FIELD_MIN_INTERVAL = "minInterval";
	public static final String JSON_FIELD_NOTIFICATIONS_LIMITATIONS = "notificationsLimitations";
	
	//Webhooks
	public static final String JSON_FIELD_WEBHOOK_URL = "url";
	public static final String JSON_FIELD_WEBHOOK_PRODUCTS = "products";
	public static final String JSON_FIELD_WEBHOOK_SEND_ADMIN = "sendAdmin";
	public static final String JSON_FIELD_WEBHOOK_SEND_RUNTIME = "sendRuntime";
	public static final String JSON_FIELD_WEBHOOK_STAGE = "minStage";
	public static final String JSON_FIELD_WEBHOOK_ARRAY = "webhooks";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_PRODUCT = "product";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_SEASON = "season";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_BRANCH = "branch";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_FILES = "files";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_FILE_NAME = "fileName";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_FILE_CONTENT = "content";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_FILE_GLOBAL_USERS = "globalUsers";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_FILE_ANALYTICS = "analytics";
	public static final String JSON_FIELD_WEBHOOK_OUTPUT_TIME = "time";
	public static final String FOLDER_DELETED = "FOLDER_DELETED";
	public static final String FILE_DELETED = "FILE_DELETED";
	
	//capabilities
	public static final String JSON_FIELD_CAPABILITIES = "capabilities";
	
	//For analytics server
	public static final String JSON_FIELD_START = "start";
	public static final String JSON_FIELD_END = "end";
	public static final String JSON_FIELD_RANGES = "ranges";
		
	//Tests
	public static final String JSON_FIELD_PATH = "path";
	
	//reserved namespace
	public static final String RESERVED_NAMESPACE = "airlockExp";
	
	//public static final String CURRENT_VERSION = "V3.0";
	//public static final String CURRENT_SERVER_VERSION = "4.5";
	public static final String CURRENT_SERVER_VERSION = "5.5";
	public static final String PRE_2_1_SERVER_VERSION = "0";
	
	public static final double DEFAULT_RANDOMIZER = 0.7;
	
	
	
	//production change file name
	public static final String PRODUCTION_CHANGED_FILE_NAME = "productionChanged.txt";
    public static final String AIR_COHORTS_FIELD_USERS_NUMBER = "usersNumber";

    public static enum ServiceState {
		RUNNING,
		INITIALIZING,
		S3_IO_ERROR,
		S3_DATA_CONSISTENCY_ERROR
	}
	
	public static enum Platform {
		iOS,
		Android,
		c_sharp;
		
		public static String returnValues() {
			return iOS.toString()+", "+Android.toString() + c_sharp.toString(); 
		}
	}
	
	public enum Action {
		ADD,
		UPDATE
	}
	
	public enum Stage {
		DEVELOPMENT,
		PRODUCTION;
		
		public static String returnValues() {
			return DEVELOPMENT.toString()+", "+PRODUCTION.toString(); 
		}
	}
	
	// generate maximal tree, minimal tree, or generate the optional fields at random
	public enum InputSampleGenerationMode { 
		MINIMAL, 
		MAXIMAL, 
		PARTIAL; 
		
		public static String returnValues() {
			return MINIMAL.toString()+","+MAXIMAL.toString()+","+PARTIAL.toString(); 
		}
	}

	//in use by simulateUtility. Indicates whether the sample is rule of configuration
	public enum SimulationType {
		RULE,
		CONFIGURATION;
		
		public static String returnValues() {
			return RULE.toString()+","+CONFIGURATION.toString(); 
		}
	}
	
	//Indicates wether only validation should be performed or take the act
	public enum ActionType {
		VALIDATE,
		ACT;
		
		public static String returnValues() {
			return VALIDATE.toString()+","+ACT.toString(); 
		}
	}

	//Indicates what format the imported strings are
	public enum InputFormat {
		ANDROID,
		IOS;

		public static String returnValues() {
			return ANDROID.toString()+","+IOS.toString();
		}
	}

	//in use by copy and import. Indicates only validation should be performed or take the act
	public enum GetAnalyticsOutputMode {
		BASIC,
		VERBOSE,
		DISPLAY;
		
		public static String returnValues() {
			return BASIC.toString()+", "+VERBOSE.toString() + ", " + DISPLAY.toString(); 
		}
	}
	
	public enum APIKeyOutputMode {
		FULL,
		ONLY_PASSWORD,
		WITHOUT_PASSWORD;
	}
	
	public enum AttributeType {
		REGULAR,
		ARRAY,
		CUSTOM;
		
		public static String returnValues() {
			return REGULAR.toString()+", "+ARRAY.toString() + ", " + CUSTOM.toString(); 
		}
	}
	
	//in use by getString/getStrings. Indicates whether the output should include the string translations or not
	public enum StringsOutputMode {
		BASIC,
		INCLUDE_TRANSLATIONS;
		
		public static String returnValues() {
			return BASIC.toString()+","+INCLUDE_TRANSLATIONS.toString(); 
		}
	}
	
	//in use by getBranch/getBranches. Indicates whether the output should include the checked out/add features list
	public enum BranchesOutputMode {
		BASIC,
		INCLUDE_FEATURES;
		
		public static String returnValues() {
			return BASIC.toString()+","+INCLUDE_FEATURES.toString(); 
		}
	}
	
	// weather to cancel checkout of the given feature alone or cancel checkout of its sub features as well 
	public enum CancelCheckoutMode { 
		STAND_ALONE, 
		INCLUDE_SUB_FEATURES;
		
		public static String returnValues() {
			return STAND_ALONE.toString()+","+INCLUDE_SUB_FEATURES.toString(); 
		}
	}
		
	public enum StringStatus {
		NONE, //for old seasons that dont support statuses
		NEW_STRING,
		READY_FOR_TRANSLATION,
		REVIEWED_FOR_TRANSLATION,
		IN_TRANSLATION,
		TRANSLATION_COMPLETE; //if for all locals the string is in status OVERRIDE or TRANSLATED
		
		public static String returnValues() {
			return NONE.toString()+","+NEW_STRING.toString()+","+READY_FOR_TRANSLATION.toString()+","+REVIEWED_FOR_TRANSLATION.toString()+","+IN_TRANSLATION.toString()+","+TRANSLATION_COMPLETE.toString(); 
		}
	}
	
	public enum TranslationStatus {
		NONE, //for old seasons that dont support statuses		
		NOT_TRANSLATED,
		IN_TRANSLATION,
		TRANSLATED,
		OVERRIDE
		//PENDING_SENT_TO_RETRANSLATION,
		//SENT_TO_RETRANSLATION,		
		//FIXED_TRANSLATION,
		
	}
	
	public enum OutputJSONMode {
		ADMIN,
		RUNTIME_DEVELOPMENT,
		RUNTIME_PRODUCTION,
		DEFAULTS,
		DISPLAY
	}
	
	public enum BranchStatus {
		CHECKED_OUT,
		NEW,
		NONE, //taken from master
		TEMPORARY // mark temporary items
	}

	public enum UtilityType {
		MAIN_UTILITY,
		STREAMS_UTILITY;
		
		public static String returnValues() {
			return MAIN_UTILITY.toString()+", "+STREAMS_UTILITY.toString(); 
		}
	}
	

	public enum RoleType { 
		Viewer, 
		Editor, 
		ProductLead, 
		Administrator, 
		TranslationSpecialist, 
		AnalyticsViewer,  
		AnalyticsEditor,
		AnalyticsPowerUser;
		
		public static String returnValues() {
			return Viewer.toString()+", "+Editor.toString() +", "+
				   ProductLead.toString() +", "+Administrator.toString() +", "+
				   TranslationSpecialist.toString() +", "+AnalyticsViewer.toString() +", "+
				   AnalyticsEditor.toString() +", "+ AnalyticsPowerUser.toString(); 
		}
	}
	//capabilities
	public enum AirlockCapability { 
		STREAMS, 
		FEATURES, 
		NOTIFICATIONS, 
		TRANSLATIONS, 
		ANALYTICS, 
		EXPERIMENTS,  
		BRANCHES,
		EXPORT_IMPORT,
		API_KEY_MANAGEMENT,
		RUNTIME_ENCRYPTION,
		COHORTS,
		DATA_IMPORT,
		ENTITLEMENTS,
		ENTITIES;

		public static String returnValues() {
			return STREAMS.toString()+", "+FEATURES.toString() +", "+
					NOTIFICATIONS.toString()+", "+TRANSLATIONS.toString() +", "+
					ANALYTICS.toString()+", "+EXPERIMENTS.toString() +", "+
				   BRANCHES.toString()+", "+EXPORT_IMPORT.toString() + ", " + 
				   API_KEY_MANAGEMENT.toString() + ", " + RUNTIME_ENCRYPTION.toString() + ", " +
					COHORTS.toString() + ", " + ENTITLEMENTS.toString()+ ", " + ENTITIES.toString();
		}
	}
	
	public enum AirlockFileTypes {
		PRODUCTS,
		RUNTIME,
		ANALYTICS,
		BRANCH_FEATURES,
		BRANCH_RUNTIME_DEV,
		BRANCH_RUNTIME_PROD,
		FEATURES,
		FEATURE_FOLLOWERS,
		CONSTANTS,
		DEFAULTS,
		INPUT_SCHEMA,
		PRODUCT_FOLLOWERS,
		SEASON_BRANCHES,
		SEASON_UTILITIES,
		SERVER_INFO,
		ENCRIPTION_KEY,
		ALL_LOCALES_STRING_FILE,
		LOCALE_STRINGS_FILE_DEV,
		LOCALE_STRINGS_FILE_PROD,
		ORIGINAL_STRINGS,
		EN_STRINGS,
		EN_STRINGS_FOR_SEASON,
		BRANCH_AND_MASTER_RUNTIME,
		EXPERIMENTS,
		SEASON_STREAMS,
		SEASON_STREAMS_EVENTS,
		USER_GROUPS,
		CAPABILITIES,
		API_KEYS,
		WEBHOOKS,
		SEASON_NOTIFICATIONS,
		AIRLOCK_SERVERS,
		GLOBAL_AIRLOCK_USERS,
		PRODUCT_USERS,
		ROLES;
	}
	
	public enum EmailProviderType {
		AWS,
		SEND_GRID
	}
	
	public enum IssueStatus { NO_ISSUES, HAS_ISSUES, HAS_OPEN_ISSUES }
	

	public enum REQUEST_ITEM_TYPE {
		FEATURES,
		ENTITLEMENTS
	}


	public static final String SIMULATION_UTIL_RULE_SEPARATOR = "$#^StartOfRule^#$";
	public static final String MASTER_BRANCH_NAME = "MASTER";
	public static final String BASIC_UTILITY_NAME = "basicUtility";
	
	public static final String ROOT_FEATURE = "ROOT";
	
	//Airlytics entities
	public static final String JSON_FIELD_ENTITY_ID = "entityId";
	public static final String JSON_FIELD_ENTITIES = "entities";
	public static final String JSON_FIELD_ATTRIBUTE_TYPE_ID = "attributeTypeId";
	public static final String JSON_FIELD_DATA_TYPE = "dataType";
	public static final String JSON_FIELD_RETURNED_BY_DSR = "returnedByDSR";
	public static final String JSON_FIELD_NULLBLE = "nullable";
	public static final String JSON_FIELD_DB_COLUMN = "dbColumn";
	public static final String JSON_FIELD_ATHENA_COLUMN = "athenaColumn";
	public static final String AIRLYTICS_ENTITIES_FILE_NAME = "AirlyticsEntities.json";
	public static final String JSON_FIELD_ATTRIBUTE_TYPES = "attributeTypes";
	public static final String JSON_FIELD_DELETED_ATTRIBUTES_DATA = "deletedAttributesData";
	public static final String JSON_FIELD_PI_STATE = "piState";
	public static final String JSON_FIELD_DB_TABLE = "dbTable";
	public static final String JSON_FIELD_ATHENA_DB = "athenaDatabase";
	public static final String JSON_FIELD_ATHENA_DEV_DB = "athenaDevDatabase";
	public static final String JSON_FIELD_ATHENA_TABLE = "athenaTable";
	public static final String JSON_FIELD_DB_TABLES = "dbTables";
	public static final String JSON_FIELD_DB_SCHEMA = "dbSchema";
	public static final String JSON_FIELD_DB_DEV_SCHEMA = "dbDevSchema";
	public static final String JSON_FIELD_DB_ARCHIVE_SCHEMA = "dbArchiveSchema";
	public static final String JSON_FIELD_DB_SCHEMAS = "dbSchemas";
	public static final String JSON_FIELD_DEFAULT_VALUE = "defaultValue";
	public static final String JSON_FIELD_WITH_DEFAULT_VALUE = "withDefaultValue";
	public static final String JSON_FIELD_DEPRECATED = "deprecated";
	public static final String JSON_FIELD_IS_ARRAY = "isArray";
	public static final String JSON_FIELD_ENTITY = "entity";
	public static final String JSON_FIELD_ATTRIBUTE = "attribute";
	public static final String JSON_FIELD_ATTRIBUTES_PERMISSION = "attributesPermission";
	
	public static enum DATA_TYPE {
		STRING,
		INTEGER,
		LONG,
		BOOLEAN,
		DOUBLE,
		TIMESTAMP,
		JSON
	}
	
	
}
