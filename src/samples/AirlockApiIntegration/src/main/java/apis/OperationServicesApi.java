package apis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

public class OperationServicesApi {
    String url = null;

    public OperationServicesApi(String url) {
        this.url = url + "/ops";
    }

    public JsonArray getUserRoleSets(String identifier, String sessionToken) throws IOException {
        JsonObject content = new JsonObject();
        content.addProperty("identifier", identifier);
        Utilities.RestCallResults res = Utilities.sendPost(url +"/userrolesets/user", content.toString(), sessionToken);

        if (res.code != 200) {
            System.err.println("getUserRoleSets fail: " + res.code + ", " + res.message);
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(res.message).getAsJsonObject();

        return jsonObject.getAsJsonArray("userRoleSets");
    }

    public String createGlobalUserRoleSet(String identifier, String creator, boolean isGroupRepresentation,
                                       JsonArray roles, String sessionToken) throws IOException {

        JsonObject content = new JsonObject();
        content.addProperty("creator", creator);
        content.addProperty("identifier", identifier);
        content.addProperty("isGroupRepresentation", isGroupRepresentation);
        content.add("roles", roles);


        Utilities.RestCallResults res = Utilities.sendPost(url+"/userrolesets", content.toString(), sessionToken);

        if (res.code != 200) {
            System.err.println("addGlobalUserRoleSet fail: " + res.code + ", " + res.message);
            return null;
        }

        String userId = Utilities.getUniquId(res.message);
        return userId;
    }


    public String createProductUserRoleSet(String productId, String identifier, String creator,
                                        boolean isGroupRepresentation, JsonArray roles, String sessionToken) throws IOException{

        JsonObject content = new JsonObject();
        content.addProperty("creator", creator);
        content.addProperty("identifier", identifier);
        content.addProperty("isGroupRepresentation", isGroupRepresentation);
        content.add("roles", roles);


        Utilities.RestCallResults res = Utilities.sendPost(url+ "/products/" + productId +  "/userrolesets", content.toString(), sessionToken);

        if (res.code != 200) {
            System.err.println("addProductUserRoleSet fail: " + res.code + ", " + res.message);
            return null;
        }

        String userId = Utilities.getUniquId(res.message);
        return userId;
    }

    public String updateUserRoleSet(String userId, String body, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendPut(url+ "/userrolesets/" + userId, body, sessionToken);

        if (res.code != 200) {
            System.err.println("updateUserRoleSet fail: " + res.code + ", " + res.message);
            return null;
        }

        return res.message;
    }

    public int deleteUserRoleSet(String roleSetId, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendDelete(url+ "/userrolesets/" + roleSetId, sessionToken);

        if (res.code != 200) {
            System.err.println("deleteUserRoleSet fail: " + res.code + ", " + res.message);
        }

        return res.code;
    }

    public JsonObject getUserRoleSet(String roleSetId, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendGet(url+ "/userrolesets/" + roleSetId, sessionToken);
        if (res.code != 200) {
            System.err.println("getUserRoleSet fail: " + res.code + ", " + res.message);
            return null;
        }

        JsonParser parser = new JsonParser();
        return parser.parse(res.message).getAsJsonObject();
    }

}
