package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

import static com.jikexueyuan.cicada_chat.App.token;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Login");
        Button bt_login = (Button) findViewById(R.id.bt_login);
        Button bt_reg = (Button) findViewById(R.id.bt_reg);
        final EditText et_username = (EditText) findViewById(R.id.et_username);
        final EditText et_password = (EditText) findViewById(R.id.et_password);

        bt_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();
            }
        });

        bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = et_username.getText().toString();
                String password = et_password.getText().toString();
                if(username.equals("") || password.equals("")){
                    Toast.makeText(LoginActivity.this,"用户名或密码不能为空",Toast.LENGTH_LONG).show();
                }
                else {
                    AsyncHttpClient client = new AsyncHttpClient();
                    RequestParams params = new RequestParams();
                    params.add("username",username);
                    params.add("password",password);

                    client.post("http://192.168.11.109/chat/API/login.php", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int i, Header[] headers, byte[] bytes) {
                            String response = new String(bytes);
                            Log.e("debugu",response);
                            JSONObject object = null;
                            try {
                                object = new JSONObject(response);
                                String status = object.getString("status");
                                if(status.equals("success")){
                                    String token = object.getString("token");
                                    App.isLogin = true;
                                    App.username = username;
                                    App.token = token;
                                    Toast.makeText(LoginActivity.this,"token"+App.token,Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    LoginActivity.this.finish();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(LoginActivity.this, "用户名或密码错误，请一会儿重试",Toast.LENGTH_LONG).show();
                            }

                        }

                        @Override
                        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                            Toast.makeText(LoginActivity.this,"网络错误，请待会儿重试",Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        });
    }

}