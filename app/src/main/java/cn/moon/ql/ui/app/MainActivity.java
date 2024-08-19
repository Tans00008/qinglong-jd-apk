package cn.moon.ql.ui.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import cn.moon.ql.R;
import cn.moon.ql.SettingsActivity;
import cn.moon.ql.api.QlApi;
import cn.moon.ql.data.model.JDCookie;
import cn.moon.ql.data.model.QLEnvData;
import cn.moon.ql.util.PreferenceUtil;

import static cn.moon.ql.Config.JD_URL;
import static cn.moon.ql.data.model.JDCookie.JD_COOKIE;

public class MainActivity extends AppCompatActivity {


    private WebView webView;
    private Button uploadCookieButton;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            String str = (String) msg.obj;
            if (str != null) {
                Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private QlApi qlApi = new QlApi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        webView = findViewById(R.id.webView);
        uploadCookieButton = findViewById(R.id.uploadCookieButton);
        Button clearWebviewBtn = findViewById(R.id.clear_webview);

        // 设置 WebView 的基本属性
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(JD_URL);



        uploadCookieButton.setOnClickListener(v -> uploadCookie());
        clearWebviewBtn.setOnClickListener(v->clearWebview());

        findViewById(R.id.btn_setting).setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });


        String cid = PreferenceUtil.getInstance().get("cid");
        Toast.makeText(getBaseContext(),"cid="+cid, Toast.LENGTH_SHORT).show();;
    }


    private void info(String msg) {
        handler.sendMessage(handler.obtainMessage(1, msg));
    }

    private void err(String msg) {
        handler.sendMessage(handler.obtainMessage(-1, msg));
    }

    private void uploadCookie() {
        JDCookie jdCookie = getJDCookie();
        if (jdCookie == null) {
            err("☹️请先登录JD账号");
            return;
        }

        new Thread() {
            @Override
            public void run() {
                doUploadJDCookie(jdCookie);
            }
        }.start();

    }




    private JDCookie getJDCookie() {
        String cookies = CookieManager.getInstance().getCookie(webView.getUrl());
        String[] cookiesArr = cookies.split(";");

        String ptPin = null;
        String ptKey = null;
        for (String ck : cookiesArr) {
            if (ck.contains("pt_pin")) {
                ptPin = ck.replace("pt_pin=", "").trim();
            }

            if (ck.contains("pt_key")) {
                ptKey = ck.replace("pt_key=", "").trim();
            }
        }

        if (ptPin != null && ptKey != null) {
            return new JDCookie(ptPin, ptKey);
        }

        return null;
    }

    private void doUploadJDCookie(JDCookie jdCookie) {
        try {
            List<QLEnvData> envDataList = qlApi.listEnv();
            Integer id = null;
            String remarks = null;
            for (QLEnvData envData : envDataList) {
                String name = envData.getName();
                String value = envData.getValue();
                if (JD_COOKIE.equals(name) && value.contains(jdCookie.getPtPin())) {
                    id = envData.getId();
                    remarks = envData.getRemarks();
                }
            }

            String finalremarks = Build.BRAND + " " + jdCookie.getPtPin();
            if (remarks != null) {
                finalremarks = remarks;
            }


            QLEnvData updateEnv = new QLEnvData(JD_COOKIE, jdCookie.joinPinAndKey(), finalremarks);
            if (id == null) {
                qlApi.addEnv(updateEnv);
                info(String.format("🎉添加JDCookie【%s】成功", jdCookie.getPtPin()));
            } else {
                updateEnv.setId(id);
                qlApi.updateEnv(updateEnv);
                info(String.format("🎉更新JDCookie【%s】成功", jdCookie.getPtPin()));
            }
        } catch (Exception e) {
            MainActivity.this.err(String.format("☹️更新JDCookie【%s】失败", jdCookie.getPtPin()));
        }
    }

    private void clearWebview(){
        CookieManager.getInstance().removeAllCookies(aBoolean -> {

        });
        CookieManager.getInstance().flush();

        webView.loadUrl(JD_URL);
    }

}
