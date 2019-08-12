package apis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

public class UserGroupServicesApi {
    String  url = null;

    public UserGroupServicesApi(String url) {
        this.url = url+"/admin/products/";
    }

    public JsonObject getUserGroups(String productId, String sessionToken) throws IOException {
        Utilities.RestCallResults res = Utilities.sendGet(url + productId +"/usergroups" , sessionToken);
        if (res.code!=200) {
            System.err.println("getUserGroups fail: " + res.code + ", " + res.message);
            return null;
        }
        JsonParser parser = new JsonParser();
        return parser.parse(res.message).getAsJsonObject();
    }

    public String setUserGroups(String productId, JsonObject groups, String sessionToken) throws IOException{
        Utilities.RestCallResults res = Utilities.sendPut(url + productId +"/usergroups" , groups.toString(), sessionToken);

        if (res.code!=200) {
            System.err.println("setUserGroups fail: " + res.code + ", " + res.message);
            return null;
        }

        return res.message;
    }
}
