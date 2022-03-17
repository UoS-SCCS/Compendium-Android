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

package com.castellate.compendium;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.databinding.ActivityMainBinding;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.push.PushServerManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            URL url = new URL("https://compendium.dev.castellate.com:4500/register");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        boolean errorOccurred = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH));
        }


    try {
        CompanionKeyManager ckm = new CompanionKeyManager();
        Log.d(TAG, "Keys:" + ckm.getKeyList());

    }catch(Exception e){
        e.printStackTrace();
    }
        IdentityStore identityStore = IdentityStore.getInstance();
        if(!identityStore.isInitialised()){
            Log.d(TAG,"Unable to access Identity Store, will stop");
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Error Loading");
            builder.setMessage("The app is unable to access the identity store and will close.");
            builder.setPositiveButton("OK", (dialogInterface, i) -> finish());
            builder.show();
            errorOccurred=true;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if(errorOccurred){
            return;
        }
        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        PushServerManager.checkRegistered(getApplicationContext());
        if (!hasCameraPermission()) {
            this.requestPermission();
        }

        /**
         * This shouldn't be needed because the notification service will receive the token
         * refresh call and we can update from there.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                // Log and toast
                String msg = getString(R.string.msg_token_fmt, token);
                Log.d(TAG, msg);
                //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if(id == R.id.action_reset) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Reset App");
            builder.setMessage("Are you sure you want to reset the app? This will delete all keys and identity information?");
            builder.setPositiveButton("Yes", (dialogInterface, i) -> resetApp());
            builder.setNegativeButton("No",null);
            builder.show();

        }

        return super.onOptionsItemSelected(item);
    }
    public void resetApp(){
        try {
            CompanionKeyManager ckm = new CompanionKeyManager();
            ckm.reset();
            IdentityStore.getInstance().reset();
        } catch (CryptoException | StorageException e) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Reset Failed");
            builder.setMessage("An error occurred whilst resetting the device.");
            builder.setPositiveButton("Ok", null);
            builder.show();
        }
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Completed");
        builder.setMessage("Your Companion App has been reset. You should remove any enrolments from your PC as well.");
        builder.setPositiveButton("Ok", null);
        builder.show();
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, CAMERA_REQUEST_CODE

        );
    }
}