package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import cz.msebera.android.httpclient.Header;

import org.json.JSONException;
import org.json.JSONObject;

import static android.util.Log.*;

public class RegActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        setTitle("Sign in");
        Button btn_reg = (Button) findViewById(R.id.btn_reg);
        final EditText et_username = (EditText) findViewById(R.id.et_username);
        final EditText et_password = (EditText) findViewById(R.id.et_password);

        btn_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String user_name = et_username.getText().toString();
                String pass_word = et_password.getText().toString();

                if(user_name.equals("") || pass_word.equals("")){
                    Toast.makeText(RegActivity.this, "username or password should not be blank", Toast.LENGTH_LONG ).show();
                }
                else{
                    AsyncHttpClient client = new AsyncHttpClient();
                    RequestParams params = new RequestParams();
                    params.add("username", user_name);
                    params.add("password", pass_word);
                    client.post("http://192.168.11.109/chat/API/reg.php", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int i, Header[] headers, byte[] bytes) {
                            String response = new String(bytes);
                            Log.e("debug", response);
                            JSONObject object = null;
                            try {
                                object = new JSONObject(response);
                                String status = object.getString("status");
                                if (status.equals("exists")) {
                                    Toast.makeText(RegActivity.this, "用户名已存在，请更换", Toast.LENGTH_LONG).show();
                                } else if(status.equals("error")) {
                                    Toast.makeText(RegActivity.this, "出现错误，请稍后重试", Toast.LENGTH_LONG).show();

                                }
                                else if(status.equals("success")){
                                    String token = object.getString("token");
                                    App.token = token;
                                    App.username = user_name;
                                    App.isLogin = true;
                                    Intent intent = new Intent(RegActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    RegActivity.this.finish();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                            Toast.makeText(RegActivity.this, "网络错误，请稍后重试", Toast.LENGTH_LONG).show();
                        }

                    });
                }
            }
        });
    }
}
