package com.jikexueyuan.cicada_chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;



public class MainActivity extends Activity implements View.OnClickListener {

    ArrayAdapter adapter = null;
    List<String> list = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("好友列表");


        if(App.isLogin){
            RongIM.connect(App.token, new RongIMClient.ConnectCallback() {
            @Override
            public void onTokenIncorrect() {
                Toast.makeText(MainActivity.this, "Token Incorrect", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(String s) {
                Toast.makeText(MainActivity.this, "Login Success " + s, Toast.LENGTH_LONG).show();

            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                Toast.makeText(MainActivity.this, "Error, Please try again later" + errorCode , Toast.LENGTH_LONG).show();
            }
        });
        }
        else {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            this.finish();
        }

        final ListView listView = (ListView)findViewById(R.id.lv_friendlist);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String target = (String) listView.getAdapter().getItem(position);
                RongIM.getInstance().startPrivateChat(MainActivity.this, target, null);
            }
        });

        findViewById(R.id.btn_Iat).setOnClickListener(MainActivity.this);

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btn_Iat:
                Intent intent = new Intent(MainActivity.this, IatDemo.class);
                startActivity(intent);
            default:
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshFriendList();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        // 退出时释放连接
//        mIat.cancel();
//        mIat.destroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflate = getMenuInflater();
        menuInflate.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.addFriend){
            Intent intent = new Intent(MainActivity.this, AddFriendActivity.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.refresh){
            refreshFriendList();
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshFriendList() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("username", App.username);
        client.post("http://192.168.11.109/chat/API/getFriendList.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String response = new String(bytes);
                Log.e("debuge", response);

                try {
                   JSONArray array = new JSONArray(response);
                    list.clear();
                    for (int j = 0; j < array.length(); j++) {
                        list.add((String) array.get(j));
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                String re = new String(bytes);
                Log.e("debugi",re);
                Toast.makeText(MainActivity.this, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });

    }

}
