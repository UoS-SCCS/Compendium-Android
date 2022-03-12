package com.castellate.compendium.ui.enrol;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.transition.TransitionInflater;

import com.castellate.compendium.CompanionDevice;
import com.castellate.compendium.R;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.ProtocolViewModel;
import com.castellate.compendium.protocol.enrol.EnrolProtocol;
import com.castellate.compendium.protocol.messages.Constants;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Objects;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * A simple {@link Fragment} subclass.

 */
public class CompleteEnrolment extends Fragment {


    private static final String TAG = "CompleteEnrolment";

    private int newDeviceIdx = 1;
    private CompanionDevice companionDevice;
    private boolean inError =false;
    public CompleteEnrolment() {
        // Required empty public constructor

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String deviceId = prefs.getString("id", android.os.Build.MODEL);
        newDeviceIdx = prefs.getInt("deviceCounter", 1);

        companionDevice = new CompanionDevice(deviceId);

    }

    //
    private void fadeIn(View view) {
        if(view==null){
            return;
        }
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate().alpha(1f).setDuration(100).setListener(null);

    }
    private void fadeOut(View view) {
        if(view==null){
            return;
        }
        //view.setAlpha(1f);
        //view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate().alpha(0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);

            }
        });

    }

    private void crossFade(View fadeMeOut, View fadeMeIn) {
        fadeIn(fadeMeIn);
        if(fadeMeOut!=null) {
            fadeMeOut.animate().alpha(0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    fadeMeOut.setVisibility(View.GONE);

                }
            });
        }

    }

    private void showDuplicateError() {
        showGenericError("This device has already enrolled with the PC");
    }

    private void showGenericError() {
        showGenericError(null);

    }
    private void showGenericError(String customText) {
        if(inError){
            return;
        }
        companionDevice.reset();
        View view = getView();
        if(view==null){
            Log.d(TAG,"Cannot display error because there is no view");
            return;
        }
        this.inError=true;
        crossFade(view.findViewById(R.id.network_progress), view.findViewById(R.id.failed));
        fadeOut(view.findViewById(R.id.cancel_enrolment));
        fadeOut(view.findViewById(R.id.confirm_button));
        fadeIn(view.findViewById(R.id.ok_error_button));
        fadeOut(view.findViewById(R.id.device_name));
        fadeOut(view.findViewById(R.id.device_label));
        ((TextView)view.findViewById(R.id.title)).setText(R.string.error_title);
        if(customText!=null) {
            ((TextView) view.findViewById(R.id.explain)).setText(customText);
        }else{
            ((TextView)view.findViewById(R.id.explain)).setText(R.string.generic_enrol_error);
        }

    }

    private void confirmClick(View viewButton) {
        View view= getView();
        SharedPreferences prefs =requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        EditText deviceName = (EditText) Objects.requireNonNull(view).findViewById(R.id.device_name);
        if (deviceName!=null && deviceName.getText().toString().equals("Device_" + newDeviceIdx)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("deviceCounter", ++newDeviceIdx);
            editor.apply();
        }

        fadeIn(view.findViewById(R.id.progress_spinner));
        fadeIn(view.findViewById(R.id.progress_status));
        fadeIn(view.findViewById(R.id.network_progress));

        view.findViewById(R.id.confirm_button).setEnabled(false);
        view.findViewById(R.id.cancel_enrolment).setEnabled(false);
        companionDevice.updateFromUI(null);

    }

    private void cancelClicked(View viewButton) {
        View view= getView();

        View confirmButton = Objects.requireNonNull(view).findViewById(R.id.confirm_button);
        if(confirmButton !=null){
            confirmButton.setEnabled(false);
        }
        viewButton.setEnabled(false);
        companionDevice.reset();
        GifDrawable drawable = (GifDrawable) ((GifImageView) view.findViewById(R.id.failed)).getDrawable();
        drawable.addAnimationListener(loopNumber -> {
            final Handler handler = new Handler();
            handler.postDelayed(() -> NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment), 1500);


        });
        crossFade(view.findViewById(R.id.network_progress), view.findViewById(R.id.failed));

    }

    private void processAwaitingUI(String protocolState) throws StorageException {

        if (protocolState.equals("INIT_WSS")) {
            IdentityStore identityStore = IdentityStore.getInstance();
            if (identityStore.hasPublicIdentity(companionDevice.getProtocolData(Constants.HASH_PC_PUBLIC_KEY))) {
                showDuplicateError();
                companionDevice.setProtocolInError();

            }
        }
    }

    private void updateProgress(Protocol.STATUS protocolStatus) {
        View view= getView();
        if (protocolStatus != Protocol.STATUS.IDLE) {
            ((CircularProgressIndicator) Objects.requireNonNull(view).findViewById(R.id.progress_spinner)).setProgress(companionDevice.getProgress(), true);
        }
    }

    private void protocolFinished() throws StorageException {
        View view= getView();
        Log.d(TAG, "Protocol finished will write out data");
        IdentityStore identityStore = IdentityStore.getInstance();
        String deviceNameStr = ((EditText) Objects.requireNonNull(view).findViewById(R.id.device_name)).getText().toString();
        identityStore.storePublicIdentity(deviceNameStr, companionDevice.getProtocolData(Constants.PC_PUBLIC_KEY));
        companionDevice.reset();

        GifDrawable drawable = (GifDrawable) ((GifImageView) view.findViewById(R.id.complete)).getDrawable();
        drawable.addAnimationListener(loopNumber -> {
            final Handler handler = new Handler();
            handler.postDelayed(() -> NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment), 1500);


        });
        //This must be called last as on completion of the animation the page will
        //navigate away.
        crossFade(view.findViewById(R.id.network_progress), view.findViewById(R.id.complete));


    }

    private void protocolStatusUpdate(Protocol.STATUS status) throws StorageException {


        String protocolState = companionDevice.getCurrentStateOfProtocol();
        Log.d(TAG, "ProtocolStatus:" + status);
        Log.d(TAG, "ProtocolState:" + protocolState);
        updateProgress(status);
        if (status == Protocol.STATUS.AWAITING_UI) {
            processAwaitingUI(protocolState);
        }
        if (status == Protocol.STATUS.FINISHED) {
            protocolFinished();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_complete_enrolment, container, false);
        EditText deviceName = (EditText) view.findViewById(R.id.device_name);
        deviceName.setText(getString(R.string.device_name_placeholder, this.newDeviceIdx));
        view.findViewById(R.id.confirm_button).setOnClickListener(this::confirmClick);
        view.findViewById(R.id.ok_error_button).setOnClickListener(viewButton -> NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment));
        view.findViewById(R.id.cancel_enrolment).setOnClickListener(this::cancelClicked);
        CompleteEnrolmentViewModel completeEnrolModel = new ViewModelProvider(requireActivity()).get(CompleteEnrolmentViewModel.class);
        companionDevice.setProtocolViewModel(completeEnrolModel);
        completeEnrolModel.getProtocolStatus().observe(getViewLifecycleOwner(), status -> {
            try {
                protocolStatusUpdate(status);
            } catch (StorageException e) {
                Log.d(TAG,"StorageException",e);
                showGenericError();
            }
        });
        completeEnrolModel.getProtocolState().observe(getViewLifecycleOwner(), state -> {

            ((TextView) view.findViewById(R.id.progress_status)).setText(state);
        });
        SharedViewModel model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        model.getMessage().observe(getViewLifecycleOwner(), item -> {
            handleQRCode(item, completeEnrolModel);
        });
        return view;
    }

    private void handleQRCode(String item, ProtocolViewModel completeEnrolModel) {
        Log.d(TAG, "QRCode received, starting Enrol Protocol");
        EnrolProtocol enrolProtocol = new EnrolProtocol();
        enrolProtocol.setProtocolViewModel(completeEnrolModel);
        try {
            companionDevice.runProtocol(enrolProtocol);
        } catch (ProtocolException e) {
            Log.d(TAG, "Exception starting protocol", e);
            showGenericError();
        }
        companionDevice.processMessage((String) item);
    }
}