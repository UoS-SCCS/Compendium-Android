package com.castellate.compendium.ui.request;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.castellate.compendium.R;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.databinding.ActivityCompendiumRequestBinding;
import com.castellate.compendium.exceptions.StorageException;

import org.json.JSONException;
import org.json.JSONObject;

public class CompendiumRequestActivity extends AppCompatActivity {

    private static final String TAG = "CompendiumRequestActivity";
    private AppBarConfiguration appBarConfiguration;
    private ActivityCompendiumRequestBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean errorOccurred = false;
        binding = ActivityCompendiumRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        IdentityStore identityStore = IdentityStore.getInstance();
        try {
            identityStore.init(getFilesDir());
        } catch (StorageException e) {
            Log.d(TAG, "Unable to access Identity Store, will stop", e);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error Loading");
            builder.setMessage("The app is unable to access the identity store and will close.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finishAffinity();
                }
            });
            builder.show();
            errorOccurred = true;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_compendium_request);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if (errorOccurred) {
            return;
        }
        JSONObject incomingMsg = new JSONObject();
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                try {
                    incomingMsg.put(key, value);
                } catch (JSONException e) {
                    Log.d(TAG, "Exception preparing incoming message", e);
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
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}