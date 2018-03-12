package com.ss.qa.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.Manifest.permission.READ_PHONE_STATE;


public class MainActivity extends AppCompatActivity {

    private TextView system;
    private TextView imei;
    private TextView brand;
    private TextView model;
    private TextView version;
    private EditText user;
    private TextView button;
    private String dialog;
    public static String mRegId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        context = this.getApplication().getApplicationContext();

        system = (TextView) findViewById(R.id.SYSTEM);
        imei = (TextView) findViewById(R.id.IMEI);
        brand = (TextView) findViewById(R.id.BRAND);
        model = (TextView) findViewById(R.id.MODEL);
        version = (TextView) findViewById(R.id.VERSION);
        user = (EditText) findViewById(R.id.USER);
        button = (TextView) findViewById(R.id.BUTTON);

        bindData();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!user.getText().toString().equals("")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            dialog = executePost();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    showSuccessDialog(dialog);
                                }

                            });
                        }
                    }).start();
                } else {
                    showSuccessDialog("请填写使用者姓名");  //please fill in user name
                }
            }
        });

    }

    private void bindData() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(MainActivity.this, new String[]{READ_PHONE_STATE},
                            0);
        } else {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
            imei.setText(tm.getDeviceId());
        }
        system.setText("Android");

        brand.setText(android.os.Build.MANUFACTURER);
        model.setText(android.os.Build.MODEL);
        version.setText(android.os.Build.VERSION.RELEASE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
//          判断获取单条权限
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 如果请求被取消， grantResults 列表总是为空
            if (PermissionUtils.verifyPermissions(grantResults)) {
                //请求权限通过
                Toast.makeText(MainActivity.this, "获取权限成功", Toast.LENGTH_SHORT).show();   //popup toast "get success"
                bindData();

            } else {
                //请求权限被拒
                Toast.makeText(MainActivity.this, "请去设置页允许电话权限", Toast.LENGTH_SHORT).show();  //popup toast "Please go to the Settings page to allow phone privileges"
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void hideSoftKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
        );
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                hideSoftKeyboard(user);
                break;
        }

        return true;
    }

    /**
     * {
     * "imei": "0123456789abcdef",
     * "brand": "apple",
     * "type": "iPhone 6",
     * "sys_ver": "10.1.1",
     * "current_user": "xxx"
     * }
     */
    private String executePost() {

        URL url = null;
        HttpURLConnection conn = null;
        JSONObject jsonParam = new JSONObject();
        ByteArrayOutputStream baos = null;
        OutputStreamWriter out = null;
        InputStream is = null;
        int responseCode;

        try {
            url = new URL("https://ies-qa.byted.org/small_tools/data/submit_phone_data"); //server's url
            conn = (HttpURLConnection) url.openConnection();
            //convert to string
            baos = new ByteArrayOutputStream();

            //ignore https certificate validation
            if (url.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url
                        .openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setConnectTimeout(10000);        //connection time
            conn.setDoInput(true);                  // open input stream for get data from server
            conn.setDoOutput(true);                 //open output stream for post data from server
            conn.setRequestMethod("POST");    //post data
            conn.setUseCaches(false);                //do not use the cache
            conn.setRequestProperty("Content-Type", "application/json");

            //Create JSONObject here
            jsonParam.put("imei", imei.getText().toString());
            jsonParam.put("brand", brand.getText().toString());
            jsonParam.put("type", model.getText().toString());
            jsonParam.put("sys_ver", version.getText().toString());
            jsonParam.put("current_user", user.getText().toString());
            jsonParam.put("system", system.getText().toString());
            jsonParam.put("push_token", mRegId);

            conn.connect();
            out = new OutputStreamWriter(conn.getOutputStream());
            out.write(jsonParam.toString());
            out.flush();
            out.close();

            responseCode = conn.getResponseCode();            //get response code
            if (responseCode == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();

                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String json = baos.toString();
                is.close();
                baos.close();
                //json处理
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject.getString("data");
            }
        } catch (UnknownHostException e) {
            return "和服务器不在同一网段哦～";  //cannot find server
        } catch (IOException e) {
            System.out.print(e.toString());
            return "请求失败，重新尝试";   //request fails, try again
        } catch (JSONException e) {
            return "json解析错误";   //Json parse error

        }
        return "请求失败，ResponseCode: " + responseCode;  //request fails, try again
    }

    public static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        // Android use X509 cert
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private void showSuccessDialog(String content) {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(content);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog = builder.create();
        dialog.show();

    }

}
