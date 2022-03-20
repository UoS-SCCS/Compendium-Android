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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.castellate.compendium.databinding.FragmentKeysItemBinding;

import java.util.List;

/**
 * Keys List Recycler adapter for showing and managing KeyItems list
 */
public class KeysListRecyclerViewAdapter extends RecyclerView.Adapter<KeysListRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "KeysListRecyclerViewAdapter";
    private final List<KeyItem> mValues;
    private KeyItemClickedListener listener;
    private int selectColor;

    /**
     * Create a new KeysListRecyclerViewAdapter
     * @param items list of KeyItmes to be shown
     * @param listener listener for click events on key items
     * @param selectColor colour to change background to when item is selected
     */
    public KeysListRecyclerViewAdapter(List<KeyItem> items, KeyItemClickedListener listener, int selectColor) {
        mValues = items;
        this.listener = listener;
        this.selectColor = selectColor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentKeysItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(mValues.get(position).getName());

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    ((LinearLayout) holder.mContentView.getParent()).setBackgroundColor(selectColor);
                    listener.itemClicked(holder.mItem);
                }
                // Here You Do Your Click Magic
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * Inner class to represent the ViewHolder to correctly format the key item
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mContentView;
        private final LinearLayout mLayout;
        public KeyItem mItem;

        /**
         * Create a new KeyItem ViewHolder
         * @param binding FragmentKeysItemBinding
         */
        public ViewHolder(FragmentKeysItemBinding binding) {
            super(binding.getRoot());
            mContentView = binding.keysContent;
            mLayout = binding.keyLayout;
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}