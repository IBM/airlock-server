import apis.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.Charset;

public class AirlockIntegrationSample {
    static public void main (String[] args) {
        String serverUrl = "https://airlockbackend.azurewebsites.net/airlock/api";
        String key = "sampleIntegrationKey";
        String keyPassword = "GtjZhG60spI=";

        AuthenticationServicesApi authenticationApi = new AuthenticationServicesApi(serverUrl);
        ProductServicesApi productApi = new ProductServicesApi(serverUrl);
        UserGroupServicesApi userGroupApi = new UserGroupServicesApi(serverUrl);
        OperationServicesApi operationApi = new OperationServicesApi(serverUrl);


        String productId = null;
        String token = null;
        try {

            //get token from api key
            token = authenticationApi.startSessionFromKey(key, keyPassword);
            if (token == null) {
                return;
            }
            System.out.println("token = " + token);

            //create product
            productId = productApi.createProduct("testProd", token);
            if (productId == null) {
                return;
            }
            System.out.println("productId = " + productId);

            //create season
            String seasonId = productApi.createSeason("1.0", productId, token);
            if (seasonId == null) {
                return;
            }
            System.out.println("seasonId = " + seasonId);

            //get encryption key
            String encryptionKey = productApi.getEncryptionKey(seasonId, token);
            if (encryptionKey == null) {
                return;
            }
            System.out.println("encryptionKey = " + encryptionKey);

            //add user group
            addUserGroup(userGroupApi, productId, "QA", token);

            //create feature
            JsonArray userGroups = new JsonArray();
            userGroups.add("QA");

            JsonObject configurationSchema = new JsonObject();
            JsonObject attributeType = new JsonObject();
            attributeType.addProperty("type", "string");
            configurationSchema.add("color", attributeType);


            JsonObject defaultConfiguration = new JsonObject();
            defaultConfiguration.addProperty("color", "green");

           String featureId = productApi.createFeature(seasonId, "ROOT", token,
                    "testFeature", "test","DEVELOPMENT",
                    "Jon", userGroups, false, "test description" , "true", "1.0",
                    "Jon", 10, false, configurationSchema, defaultConfiguration);
            if (featureId == null) {
                return;
            }
            System.out.println("featureId = " + featureId);


            //add user group
            addUserGroup(userGroupApi, productId, "DEV", token);

            //create configuration rule
            userGroups = new JsonArray();
            userGroups.add("DEV");

            String configuration = "{\"color\":\"green\"}";

            String configRuleId = productApi.createConfigurationRule(seasonId, featureId, token,
                    "testConfigRule", "test","DEVELOPMENT",
                    "Jon", userGroups, false, "test description" , "true", "1.0",
                    "Jon", 5, false, configuration);
            if (configRuleId == null) {
                return;
            }
            System.out.println("configRuleId = " + configRuleId);


            //retrieve and write defaults file
            String defaultsFilePath = productApi.writeDefaultsFile(seasonId, token);
            if (defaultsFilePath == null) {
                return;
            }
            System.out.println("defaultsFilePath = " + defaultsFilePath);


            //retrieve and write constants files
            String constantsFilesPaths = productApi.writeConstantFiles(seasonId, token);
            if (constantsFilesPaths == null) {
                return;
            }
            System.out.println("constantsFilesPaths = " + constantsFilesPaths);

            //extend session
            token = authenticationApi.extendSession(token);
            if (token == null) {
                return;
            }
            System.out.println("new token = " + token);


            //update input schema
            JsonObject inputSchemaJson = productApi.getInputSchema(seasonId, token);
            String newInputSchemaStr = Utilities.readFile("/Users/iritma/Documents/develop/AirlockApiIntegration/src/main/resources/sampleInputSchema.json", Charset.forName("UTF-8"));
            JsonParser parser = new JsonParser();
            JsonObject newSchemaJson = parser.parse(newInputSchemaStr).getAsJsonObject();
            inputSchemaJson.add("inputSchema", newSchemaJson);
            String updateRes = productApi.updateInputSchema(seasonId, inputSchemaJson, token);

            if (updateRes!=null) {
                System.out.println("Input schema was updated");
            }


            //Authorization
            //---------------

            //add general user role set
            JsonArray roles = new JsonArray();
            roles.add("Viewer");

            String globalUserRoleSetId = operationApi.createGlobalUserRoleSet("xxx@gmail.com", "Jon", false, roles, token);
            if (globalUserRoleSetId == null) {
                return;
            }
            System.out.println("globalUserRoleSetId = " + globalUserRoleSetId);


            //add general user role set
            roles = new JsonArray();
            roles.add("Viewer");
            roles.add("Editor");

            String productUserRoleSetId = operationApi.createProductUserRoleSet(productId, "xxx@gmail.com", "Jon", false, roles, token);
            if (productUserRoleSetId == null) {
                return;
            }
            System.out.println("productUserRoleSetId = " + productUserRoleSetId);

            //update user role set in product
            JsonObject usrRoleSetJson = operationApi.getUserRoleSet(productUserRoleSetId, token);
            JsonArray userRoles = usrRoleSetJson.getAsJsonArray("roles");
            System.out.println("original userRoles = " + userRoles.toString());
            userRoles.add("ProductLead");
            usrRoleSetJson.add("roles", userRoles);
            operationApi.updateUserRoleSet(productUserRoleSetId, usrRoleSetJson.toString(), token);

            usrRoleSetJson = operationApi.getUserRoleSet(productUserRoleSetId, token);
            userRoles = usrRoleSetJson.getAsJsonArray("roles");
            System.out.println("updated userRoles = " + userRoles.toString());

            //get all user's role sets
            JsonArray userRoleSets = operationApi.getUserRoleSets( "xxx@gmail.com", token);
            if (productUserRoleSetId == null) {
                return;
            }

            //delete user by deleting all of its role sets
            for (JsonElement userRolSet:userRoleSets) {
                JsonObject roleSetJson = userRolSet.getAsJsonObject();
                String roleSetId = roleSetJson.get("uniqueId").getAsString();
                int code = operationApi.deleteUserRoleSet(roleSetId, token );
                if (code != 200) {
                    System.out.println("fail deleteing userRoleSet " + roleSetId);
                }
                System.out.println("deleteRoleSet = " + roleSetId);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (token!=null && productId!=null) {
                try {
                    //delete product
                    int code = productApi.deleteProduct(productId, token);
                    if (code == 200) {
                        System.out.println("product " + productId + " was deleted.");

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void addUserGroup (UserGroupServicesApi userGroupApi, String productId, String newUserGroup, String token) throws IOException {
        JsonObject allUserGroups = userGroupApi.getUserGroups(productId, token);

        if (!isUserGroupExist(allUserGroups, newUserGroup)) {
            JsonArray userGroupsArray = allUserGroups.getAsJsonArray("internalUserGroups");
            userGroupsArray.add(newUserGroup);
            allUserGroups.add("internalUserGroups", userGroupsArray);
            userGroupApi.setUserGroups(productId, allUserGroups, token);
            System.out.println("UserGroup " + newUserGroup + " added");
        }
        else {
            System.out.println("UserGroup " + newUserGroup + " already exist");
        }

    }

    private static boolean isUserGroupExist(JsonObject allUserGroups, String newUserGroup) {
        JsonArray userGroupsArray = allUserGroups.getAsJsonArray("internalUserGroups");
        for (JsonElement userGroup:userGroupsArray) {
            if (userGroup.getAsString().equals(newUserGroup)) {
                return true;
            }
        }
        return false;
    }
}
