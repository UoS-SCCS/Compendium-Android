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

package com.castellate.compendium.ui.keys;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;

import com.castellate.compendium.R;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of keys, specifically KeyItems
 */
public class KeysFragment extends Fragment implements KeyItemClickedListener{
    private static final String TAG = "KeysFragment";


    /**
     * Construct new KeysFragment
     */
    public KeysFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static KeysFragment newInstance(int columnCount) {
        KeysFragment fragment = new KeysFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new Slide(Gravity.START));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_keys_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                    ((LinearLayoutManager)recyclerView.getLayoutManager()).getOrientation());
            recyclerView.addItemDecoration(dividerItemDecoration);

            List<KeyItem> data;
            data=new ArrayList<>();
            try {
                data = IdentityStore.getInstance().getKeyNameEntries();
            }catch(StorageException e){
                Log.d(TAG,"Exception getting data list");
            }
            int selectColor = getResources().getColor(R.color.sccsColour, null);
            recyclerView.setAdapter(new KeysListRecyclerViewAdapter(data,this,selectColor));
        }
        return view;
    }

    @Override
    public void itemClicked(KeyItem item) {
        Log.d(TAG,item.getName());
        KeysFragmentDirections.ActionKeysFragmentToAppsFragment action = KeysFragmentDirections.actionKeysFragmentToAppsFragment();
        action.setKeyId(item.getKeyId());

        NavController nav = NavHostFragment.findNavController(KeysFragment.this);
        nav.navigate(action);
        //navController.navigate(action);


    }
}