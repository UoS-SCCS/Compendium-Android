/*
 *  © Copyright 2022. University of Surrey
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.castellate.compendium.ui.request;

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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity for processing incoming push message requests
 */
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
        if(!identityStore.isInitialised()){
            Log.d(TAG, "Unable to access Identity Store, will stop");
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Error Loading");
            builder.setMessage("The app is unable to access the identity store and will close.");
            builder.setPositiveButton("OK", (dialogInterface, i) -> finishAffinity());
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