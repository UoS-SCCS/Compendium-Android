package com.castellate.compendium;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.castellate.compendium.databinding.ActivityCompendiumRequestBinding;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class CompendiumRequestActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityCompendiumRequestBinding binding;


    private WebSocketClient mWebSocketClient;

    private static final String TAG = "CompendiumRequestActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompendiumRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_compendium_request);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }

        }
        connectToSocket();
    }



    private void connectToSocket() {
        URI uri;
        try {
            uri = new URI("ws://10.0.2.2:8001");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri, new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.connect_progress_text);
                        textView.setText("WebSocket Connected");
                        ProgressBar progress = (ProgressBar)findViewById(R.id.progressBar);
                        progress.setProgress(25);
                    }
                });
                Log.d(TAG,"Connected");
                JSONObject msg = new JSONObject();
                try {
                    msg.put("type","INIT");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mWebSocketClient.send(msg.toString());
            }
            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.d(TAG,s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.connect_progress_text);
                        textView.setText("WebSocket Initialised");
                        ProgressBar progress = (ProgressBar)findViewById(R.id.progressBar);
                        progress.setProgress(45);
                    }
                });
            }
            @Override
            public void onClose(int i, String s, boolean b) {
                Log.d(TAG,"WebSocket Closed");
                //Logger.LogInfo("Websocket", "Closed " + s);
            }
            @Override
            public void onError(Exception e) {
                Log.d(TAG,"WebSocket Error",e);
                //Logger.LogInfo("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_compendium_request);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}