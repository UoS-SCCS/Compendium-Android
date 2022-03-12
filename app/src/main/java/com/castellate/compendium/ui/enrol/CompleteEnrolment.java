package com.castellate.compendium.ui.enrol;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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

import com.castellate.compendium.R;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.enrol.EnrolProtocol;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.CompanionDevice;
import com.castellate.compendium.protocol.ProtocolViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import pl.droidsonroids.gif.AnimationListener;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CompleteEnrolment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CompleteEnrolment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = "CompleteEnrolment";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int newDeviceIdx = 1;
    private CompanionDevice companionDevice;
    private boolean inError =false;
    public CompleteEnrolment() {
        // Required empty public constructor

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CompleteEnrolment.
     */
    // TODO: Rename and change types and number of parameters
    public static CompleteEnrolment newInstance(String param1, String param2) {
        CompleteEnrolment fragment = new CompleteEnrolment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));
        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);
        String deviceId = prefs.getString("id", android.os.Build.MODEL);
        newDeviceIdx = prefs.getInt("deviceCounter", 1);

        companionDevice = new CompanionDevice(deviceId);

    }

    //
    private void fadeIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate().alpha(1f).setDuration(100).setListener(null);

    }
    private void fadeOut(View view) {
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
        fadeMeOut.animate().alpha(0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fadeMeOut.setVisibility(View.GONE);

            }
        });


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
        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);
        EditText deviceName = (EditText) view.findViewById(R.id.device_name);
        if (deviceName.getText().toString().equals("Device_" + String.valueOf(newDeviceIdx))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("deviceCounter", ++newDeviceIdx);
            editor.commit();
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
        view.findViewById(R.id.confirm_button).setEnabled(false);
        viewButton.setEnabled(false);
        companionDevice.reset();
        GifDrawable drawable = (GifDrawable) ((GifImageView) view.findViewById(R.id.failed)).getDrawable();
        drawable.addAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationCompleted(int loopNumber) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment);
                    }
                }, 1500);


            }
        });
        crossFade(view.findViewById(R.id.network_progress), view.findViewById(R.id.failed));

    }

    private void processAwaitingUI(String protocolState) throws StorageException {
        View view= getView();
        if (protocolState == "INIT_WSS") {
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
            ((CircularProgressIndicator) view.findViewById(R.id.progress_spinner)).setProgress(companionDevice.getProgress(), true);
        }
    }

    private void protocolFinished() throws StorageException {
        View view= getView();
        Log.d(TAG, "Protocol finished will write out data");
        IdentityStore identityStore = IdentityStore.getInstance();
        String deviceNameStr = ((EditText) view.findViewById(R.id.device_name)).getText().toString();
        identityStore.storePublicIdentity(deviceNameStr, companionDevice.getProtocolData(Constants.PC_PUBLIC_KEY));
        companionDevice.reset();

        GifDrawable drawable = (GifDrawable) ((GifImageView) view.findViewById(R.id.complete)).getDrawable();
        drawable.addAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationCompleted(int loopNumber) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment);
                    }
                }, 1500);


            }
        });
        //This must be called last as on completion of the animation the page will
        //navigate away.
        crossFade(view.findViewById(R.id.network_progress), view.findViewById(R.id.complete));


    }

    private void protocolStatusUpdate(Protocol.STATUS status) throws StorageException {
        View view= getView();
        Protocol.STATUS protocolStatus = status;
        String protocolState = companionDevice.getCurrentStateOfProtocol();
        Log.d(TAG, "ProtocolStatus:" + protocolStatus);
        Log.d(TAG, "ProtocolState:" + protocolState);
        updateProgress(protocolStatus);
        if (protocolStatus == Protocol.STATUS.AWAITING_UI) {
            processAwaitingUI(protocolState);
        }
        if (protocolStatus == Protocol.STATUS.FINISHED) {
            protocolFinished();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_complete_enrolment, container, false);
        EditText deviceName = (EditText) view.findViewById(R.id.device_name);
        deviceName.setText("Device_" + String.valueOf(this.newDeviceIdx));
        view.findViewById(R.id.confirm_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewButton) {
                confirmClick(viewButton);
            }
        });
        view.findViewById(R.id.ok_error_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewButton) {
                NavHostFragment.findNavController(CompleteEnrolment.this).navigate(R.id.action_completeEnrolment_to_HomeFragment);

            }
        });
        view.findViewById(R.id.cancel_enrolment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewButton) {
                cancelClicked(viewButton);
            }
        });
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
            String stateStr = (String) state;
            ((TextView) view.findViewById(R.id.progress_status)).setText(stateStr);
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