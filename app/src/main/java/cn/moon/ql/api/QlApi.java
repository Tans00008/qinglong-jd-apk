package cn.moon.ql.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.moon.ql.data.model.QLEnvData;
import cn.moon.ql.util.PreferenceUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class QlApi {
    private OkHttpClient client = new OkHttpClient();

    private String auth;


    private void login() throws Exception {
        String cid = PreferenceUtil.getInstance().get("cid");
        String csk = PreferenceUtil.getInstance().get("csk");

        JSONObject data = (JSONObject) this.doRequest("/open/auth/token?client_id=" + cid + "&client_secret=" + csk, "GET", null);

        Log.d(QlApi.class.getName(), "login: " + data.toString());
        String tokenType = data.getString("token_type");
        String tokenValue = data.getString("token");

        auth = tokenType + " " + tokenValue;
    }

    public List<QLEnvData> listEnv() throws Exception {
        Object response = this.doRequest("/open/envs", "GET", null);
        if (response == null) {
            return Collections.emptyList();
        }

        JSONArray jsonArray = (JSONArray) response;
        List<QLEnvData> envList = new ArrayList<>();
        if (jsonArray.length() < 1) {
            return envList;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jo = jsonArray.getJSONObject(i);
            QLEnvData envData = new QLEnvData(jo.getInt("id"),
                    jo.getString("name"),
                    jo.getString("value"),
                    jo.getString("remarks"));
            envList.add(envData);
        }
        return envList;
    }

    public void addEnv(QLEnvData envData) throws Exception {
        JSONArray arr = new JSONArray();
        arr.put(envData.toJson());
        this.doRequest("/open/envs", "POST", arr.toString());
    }

    public void updateEnv(QLEnvData envData) throws Exception {
        this.doRequest("/open/envs", "PUT", envData.toJsonString());
    }

    private Object doRequest(String uri, String method, String content) throws Exception {
        this.login();
        return this.send(uri, method, content);
    }

    private Object send(String uri, String method, String content) {
        String url = PreferenceUtil.getInstance().get("url");

        if (url == null) {
            throw new IllegalStateException("请先设置青龙相关配置");
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }


        url = url + uri;

        RequestBody body = null;
        if (content != null) {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            body = RequestBody.create(mediaType, content);
        }

        Request.Builder builder = new Request.Builder();
        if (auth != null) {
            builder.addHeader("Authorization", auth);
        }
        Request request = builder
                .url(url + uri)
                .method(method, body)
                .build();

        try {
            Response response = client.newCall(request).execute();

            String rsBody = response.body().string();
            JSONObject rs = new JSONObject(rsBody);

            if (!response.isSuccessful()) {

                throw new IllegalStateException(uri + " " + rs.getString("message"));
            }


            System.out.println(rs);
            if (rs.getInt("code") != 200) {
                String message = rs.getString("message");
                throw new IllegalStateException(message);
            }
            if (rs.has("data")) {
                return rs.get("data");
            }
            return null;
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage());
            return null;
        }

    }
}
