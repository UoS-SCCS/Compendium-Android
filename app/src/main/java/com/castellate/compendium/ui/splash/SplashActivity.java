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

package com.castellate.compendium.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.castellate.compendium.MainActivity;
import com.castellate.compendium.R;

/**
 * Splash activity shown when the app is started
 *
 * TODO change how long this shows for based on when the app was last opened, i.e. don't pester the user with the splash screen on reopening
 */
public class SplashActivity extends AppCompatActivity {
    private View[][] fadeOrder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_splash);
        IntentAction loadApp = new IntentAction(this, SplashActivity.this, MainActivity.class,1000);

        FadeAction faCastellate = new FadeAction();
        faCastellate.addViewToAction(findViewById(R.id.developed_by));
        faCastellate.addViewToAction(findViewById(R.id.castellate_logo));
        faCastellate.setNextAction(loadApp,1000);
        FadeAction faApp = new FadeAction();
        faApp.addViewToAction(findViewById(R.id.compedium_logo));
        faApp.addViewToAction(findViewById(R.id.compendium_title));
        faApp.setNextAction(faCastellate,1000);
        FadeAction faSccs = new FadeAction();
        faSccs.addViewToAction(findViewById(R.id.sccs_logo));
        faSccs.setNextAction(faApp,1000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                faSccs.processAction();;

            }
        }, 150);

    }


}