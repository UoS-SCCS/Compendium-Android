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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.castellate.compendium.R;

/**
 * Concrete AnimAction that instead of animation starts a new activity
 */
public class IntentAction extends AnimAction {
    private Context context;
    private Class<?> target;
    private long delay;
    private Activity activity;

    /**
     * Create a new Intent Action, specifying the activity and context to use and the target
     * activity class to start
     * @param activity current activity
     * @param context current content
     * @param target target class to start
     * @param delay delay before starting it
     */
    public IntentAction(Activity activity, Context context, Class<?> target, long delay){
        this.activity=activity;
        this.delay=delay;
        this.context=context;
        this.target=target;
    }
    @Override
    public void processAction() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                context.startActivity(new Intent(context, target));
                activity.overridePendingTransition(R.anim.fade_in_act, R.anim.fade_out_act);

            }
        }, delay);
    }
}
