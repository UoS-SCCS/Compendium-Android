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

import android.animation.AnimatorListenerAdapter;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that defines an animation action. This will perform an animation on a view and
 * then call the next action if one has been configured. Actions can also be IntentAction to
 * start a new activity
 */
public abstract class AnimAction {
    protected List<View> items = new ArrayList<>();
    protected AnimAction nextAction;
    protected long runtime;
    public abstract void processAction();

    /**
     * Fade the view in and don't wait for the result
     * @param view view to fade
     */
    protected void fadeIn(View view) {
        fadeIn(view,null);
    }

    /**
     * Fade the view in and receive notification that it has finished
     * @param view view to fade
     * @param listener listener to receive complete notification
     */
    protected void fadeIn(View view, AnimatorListenerAdapter listener) {
        if (view == null) {
            return;
        }
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate().alpha(1f).setDuration(runtime).setListener(listener);
    }

    /**
     * Add a view to this action, i.e. another view to be faded at the same time
     * @param view View to add
     */
    public void addViewToAction(View view){
        items.add(view);
    }

    /**
     * Sets the next action to call once this one is complete
     * @param next the next AnimAction to call
     * @param runtime how long an animation should take before calling the next action
     */
    public void setNextAction(AnimAction next, long runtime){
        this.runtime=runtime;
        this.nextAction=next;
    }
}
