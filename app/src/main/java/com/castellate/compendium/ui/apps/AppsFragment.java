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

package com.castellate.compendium.ui.apps;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;

import com.castellate.compendium.R;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that lists registered Apps
 */
public class AppsFragment extends Fragment implements AppItemClickedListener {
    private static final String TAG = "AppsFragment";
    private String keyId;
    private List<AppItem> data;
    private AppsListRecyclerViewAdapter adapter;
    /**
     * Create a new Apps Fragment
     */
    public AppsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static AppsFragment newInstance(int columnCount) {
        AppsFragment fragment = new AppsFragment();
        return fragment;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        keyId = AppsFragmentArgs.fromBundle(getArguments()).getKeyId();
// Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                    ((LinearLayoutManager)recyclerView.getLayoutManager()).getOrientation());
            recyclerView.addItemDecoration(dividerItemDecoration);


            data=new ArrayList<>();
            try {
                data = IdentityStore.getInstance().getKeyNameAppEntries(this.keyId);
            }catch(StorageException e){
                Log.d(TAG,"Exception getting data list");
            }

            int selectColor = getResources().getColor(R.color.sccsColour, null);
            adapter=new AppsListRecyclerViewAdapter(data,this,selectColor);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new Slide(Gravity.START));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps_item_list, container, false);


        return view;
    }

    @Override
    public void itemClicked(AppItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete App")
                .setMessage("Do you really want delete the keys associated with " + item.getName()+". This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        IdentityStore identityStore = IdentityStore.getInstance();
                        try {
                            identityStore.cleanUpUnusedApp(keyId,item.getName());
                        } catch (StorageException e) {
                            e.printStackTrace();
                        }
                        try {
                            CompanionKeyManager ckm = new CompanionKeyManager();
                            ckm.deleteKey(keyId + ":" + item.getName());
                        } catch (CryptoException e) {
                            e.printStackTrace();
                        }
                        List<AppItem> updatedList=new ArrayList<>();
                        try {
                            updatedList = IdentityStore.getInstance().getKeyNameAppEntries(keyId);
                        }catch(StorageException e){
                            Log.d(TAG,"Exception getting data list");
                        }
                        data.clear();
                        data.addAll(updatedList);
                        Log.d(TAG,data.toString());

                        adapter.notifyDataSetChanged();
                    }})
                .setNegativeButton(android.R.string.no, null).show();



    }
}