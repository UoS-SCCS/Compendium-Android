package com.castellate.compendium;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.data.StorageException;
import com.castellate.compendium.databinding.ActivityCompendiumRequestBinding;
import com.castellate.compendium.ws.CompanionDevice;

import org.java_websocket.client.WebSocketClient;
import org.json.JSONException;
import org.json.JSONObject;

public class CompendiumRequestActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityCompendiumRequestBinding binding;

    private CompanionDevice companionDevice;
    private WebSocketClient mWebSocketClient;

    private static final String TAG = "CompendiumRequestActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompendiumRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        IdentityStore identityStore = IdentityStore.getInstance();
        try {
            identityStore.init(getFilesDir());
        } catch (StorageException e) {
            e.printStackTrace();
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_compendium_request);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        JSONObject incomingMsg = new JSONObject();
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                try {
                    incomingMsg.put(key,value);
                } catch (JSONException e) {
                    Log.d(TAG,"Exception preparing incoming message",e);
                }
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
            PushRequestSharedViewModel psvm = new ViewModelProvider(this).get(PushRequestSharedViewModel.class);

            psvm.setMessage(incomingMsg);

        }

    }





    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_compendium_request);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}