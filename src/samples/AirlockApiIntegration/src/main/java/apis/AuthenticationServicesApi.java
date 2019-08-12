package apis;

import com.google.gson.JsonObject;

import java.io.IOException;


public class AuthenticationServicesApi {

    String  url = null;

    public AuthenticationServicesApi(String url) {
        this.url = url+"/admin/authentication/";
    }

    public String startSessionFromKey(String apiKey, String keyPassword) throws IOException {
        JsonObject content = new JsonObject();
        content.addProperty("key", apiKey);
        content.addProperty("keyPassword", keyPassword);
        Utilities.RestCallResults res = Utilities.sendPost(url+"startSessionFromKey/", content.toString(), null);
        if (res.code!=200) {
            System.err.println("startSessionFromKey fail: " + res.code + ", " + res.message);
            return null;
        }
        return res.message;
    }

    public String extendSession(String token) throws IOException {
        Utilities.RestCallResults res = Utilities.sendGet(url+"extend/", token);
        if (res.code!=200) {
            System.err.println("extendSession fail: " + res.code + ", " + res.message);
            return null;
        }
        return res.message;
    }
}
