package com.castellate.compendium;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.enrol.EnrolProtocol;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.ws.CompanionDevice;
import com.google.android.material.progressindicator.CircularProgressIndicator;

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
        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);
        String deviceId = prefs.getString("id", android.os.Build.MODEL);
        companionDevice = new CompanionDevice(deviceId);

    }
    //

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_complete_enrolment, container, false);
        view.findViewById(R.id.confirm_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewButton) {
                view.findViewById(R.id.progress_spinner).setVisibility(View.VISIBLE);
                view.findViewById(R.id.progress_status).setVisibility(View.VISIBLE);
                view.findViewById(R.id.network_progress).setVisibility(View.VISIBLE);
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
            ((CircularProgressIndicator)view.findViewById(R.id.progress_spinner)).setProgress(companionDevice.getProgress(),true);

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