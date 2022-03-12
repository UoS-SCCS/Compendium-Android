package com.castellate.compendium;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.transition.TransitionInflater;

import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.data.StorageException;
import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.enrol.EnrolProtocol;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.ws.CompanionDevice;
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
    private CompanionDevice companionDevice;
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
        companionDevice = new CompanionDevice(deviceId);

    }
    //
    private void fadeIn(View view){
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate()
                .alpha(1f)
                .setDuration(100)
                .setListener(null);

    }
    private void crossFade(View fadeMeOut, View fadeMeIn){
        fadeIn(fadeMeIn);
        fadeMeOut.animate()
                .alpha(0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fadeMeOut.setVisibility(View.GONE);

                    }
                });


    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_complete_enrolment, container, false);
        view.findViewById(R.id.confirm_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewButton) {
                fadeIn(view.findViewById(R.id.progress_spinner));
                fadeIn(view.findViewById(R.id.progress_status));
                fadeIn(view.findViewById(R.id.network_progress));

                //view.findViewById(R.id.progress_spinner).setVisibility(View.VISIBLE);
                //view.findViewById(R.id.progress_status).setVisibility(View.VISIBLE);
                //view.findViewById(R.id.network_progress).setVisibility(View.VISIBLE);
                view.findViewById(R.id.confirm_button).setEnabled(false);
                companionDevice.updateFromUI(null);
                //((ProgressBar)view.findViewById(R.id.progress_horizontal)).setProgress(20,true);
            }
        });

        CompleteEnrolmentViewModel completeEnrolModel = new ViewModelProvider(requireActivity()).get(CompleteEnrolmentViewModel.class);
        companionDevice.setProtocolViewModel(completeEnrolModel);
        completeEnrolModel.getProtocolStatus().observe(getViewLifecycleOwner(), status -> {
            Protocol.STATUS protStatus = (Protocol.STATUS) status;
            String protState = companionDevice.getCurrentStateOfProtocol();
            Log.d("TEST","AWAITUI:"+ protState);
            if(protStatus == Protocol.STATUS.AWAITING_UI){
                if(protState=="INIT_WSS"){
                    ((TextView)view.findViewById(R.id.device_name)).setText(companionDevice.getProtocolData(Constants.ID_PC));
                }
            }
            if(protStatus != Protocol.STATUS.IDLE) {
                ((CircularProgressIndicator) view.findViewById(R.id.progress_spinner)).setProgress(companionDevice.getProgress(), true);
            }
            Log.d(TAG,"ProtocolStatus:" + protStatus);
            if(protStatus == Protocol.STATUS.FINISHED){
                Log.d(TAG,"Protocol finished will write out data");
                IdentityStore identityStore = IdentityStore.getInstance();
                try {
                    identityStore.storePublicIdentity(companionDevice.getProtocolData(Constants.ID_PC),companionDevice.getProtocolData(Constants.PC_PUBLIC_KEY));
                } catch (StorageException e) {
                    e.printStackTrace();
                }
                companionDevice.reset();
                crossFade(view.findViewById(R.id.network_progress),view.findViewById(R.id.complete));

                GifDrawable drawable = (GifDrawable)((GifImageView)view.findViewById(R.id.complete)).getDrawable();
                drawable.addAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationCompleted(int loopNumber) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                NavHostFragment.findNavController(CompleteEnrolment.this)
                                        .navigate(R.id.action_completeEnrolment_to_HomeFragment);
                            }
                        }, 1500);


                    }
                });
                //This must be called last as on completion of the animation the page will
                //navigate away.


            }
        });
        completeEnrolModel.getProtocolState().observe(getViewLifecycleOwner(), state -> {
            String stateStr = (String) state;
            ((TextView)view.findViewById(R.id.progress_status)).setText(stateStr);
        });
        SharedViewModel model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        model.getMessage().observe(getViewLifecycleOwner(), item -> {
            Log.d("TAG","View Model Observed");
            EnrolProtocol enrolProtocol = new EnrolProtocol();
            enrolProtocol.setProtocolViewModel(completeEnrolModel);
            try {
                companionDevice.runProtocol(enrolProtocol);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            companionDevice.processMessage((String)item);
        });
        return view;
    }
}