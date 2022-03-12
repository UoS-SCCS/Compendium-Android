package com.castellate.compendium;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.databinding.ConnectFragmentBinding;
import com.castellate.compendium.protocol.Protocol;
import com.castellate.compendium.protocol.ProtocolException;
import com.castellate.compendium.protocol.core.CoreProtocol;
import com.castellate.compendium.protocol.core.req.CoreGetReqMessage;
import com.castellate.compendium.protocol.core.req.CorePutReqMessage;
import com.castellate.compendium.protocol.core.req.CoreRegReqMessage;
import com.castellate.compendium.protocol.core.req.CoreVerifyReqMessage;
import com.castellate.compendium.protocol.core.res.CoreGetResMessage;
import com.castellate.compendium.protocol.core.res.CorePutResMessage;
import com.castellate.compendium.protocol.core.res.CoreRegResMessage;
import com.castellate.compendium.protocol.core.res.CoreVerifyResMessage;
import com.castellate.compendium.protocol.messages.Constants;
import com.castellate.compendium.ws.CompanionDevice;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class ConnectFragment extends Fragment {
    private static final String TAG = "ConnectFragment";
    private ConnectFragmentBinding binding;
    private CompanionDevice companionDevice;
    private CompanionKeyManager ckm = new CompanionKeyManager();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        binding = ConnectFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();


        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);

        String deviceId = prefs.getString("id", android.os.Build.MODEL);
        companionDevice = new CompanionDevice(deviceId);

        RequestViewModel requestViewModel = new ViewModelProvider(requireActivity()).get(RequestViewModel.class);

        requestViewModel.getProtocolStatus().observe(getViewLifecycleOwner(), status -> {
            Protocol.STATUS protStatus = (Protocol.STATUS) status;
            String protState = companionDevice.getCurrentStateOfProtocol();
            Log.d(TAG, "State:" + protState);
            Log.d(TAG, "Status:" + protStatus);
            if (protStatus == Protocol.STATUS.AWAITING_UI) {
                if (protState == "CORE_RESP") {
                    Map<String, String> map = preparePromptText();
                    if (!canAuthenticateWithStrongBiometrics()) {
                        Log.d(TAG, "Cannot use biometrics");
                    } else {
                        BiometricPrompt.PromptInfo prompt = this.buildBiometricPrompt(map.get("title"), map.get("subtitle"), map.get("code"));
                        String type = companionDevice.getProtocolData("type");
                        switch (type) {
                            case "Get":
                                try {
                                    JSONObject obj = new JSONObject(companionDevice.getProtocolData("encdata"));
                                    requestBiometric(prompt, ckm.getDecryptionCipher(getKeyId(), B64.decode(obj.getString("iv"))));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "Put":
                                requestBiometric(prompt, ckm.getEncryptionCipher(getKeyId()));

                                break;
                            case "Reg":
                                //Generate or get the public key
                                companionDevice.putInProtocolData(CoreRegResMessage.Fields.APP_PK, CryptoUtils.encodePublicKey(ckm.getPublicSigningKey(getKeyId())));
                                requestBiometric(prompt, ckm.getSignatureObject(getKeyId()));
                                break;
                            case "Verify":
                                //requestBiometric(prompt, ckm.getEncryptionCipher(getKeyId()));
                                requestBiometric(prompt, ckm.getSignatureObject(getKeyId()));
                                break;
                        }


                    }
                    /**if(map.containsKey("device")) {
                     ((TextView) view.findViewById(R.id.device_details)).setText(map.get("device"));
                     }
                     if(map.containsKey("reason")) {
                     ((TextView) view.findViewById(R.id.reason_details)).setText(map.get("reason"));
                     }
                     if(map.containsKey("code")) {
                     ((TextView) view.findViewById(R.id.code_details)).setText(map.get("code"));
                     }
                     ((TextView) view.findViewById(R.id.instructions)).setText("Place your finger on the fingerprint reader to approve the request");
                     ((Button) view.findViewById(R.id.cancel_button)).setVisibility(View.VISIBLE);
                     */
                    //
                }

            }
            ((CircularProgressIndicator) view.findViewById(R.id.progress_spinner_req)).setProgress(companionDevice.getProgress(), true);
            Log.d(TAG, "ProtocolStatus:" + protStatus);
            if (protStatus == Protocol.STATUS.FINISHED) {
                Log.d(TAG, "Protocol finished will write out data");
            }
        });
        requestViewModel.getProtocolState().observe(getViewLifecycleOwner(), state -> {
            String stateStr = (String) state;
            Log.d(TAG, "State:" + state);
            ((TextView) view.findViewById(R.id.progress_status_req)).setText(stateStr);
        });

        PushRequestSharedViewModel model = new ViewModelProvider(requireActivity()).get(PushRequestSharedViewModel.class);
        model.getMessage().observe(getViewLifecycleOwner(), item -> {
            Log.d(TAG, "View Model Observed");
            CoreProtocol coreProtocol = new CoreProtocol();
            //enrolProtocol.setProtocolViewModel(completeEnrolModel);
            try {
                companionDevice.runProtocol(coreProtocol);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            companionDevice.setProtocolViewModel(requestViewModel);
            companionDevice.processMessage((JSONObject) item);
        });

        return binding.getRoot();

    }

    private boolean canAuthenticateWithStrongBiometrics() {
        return BiometricManager.from(getContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private String getKeyId() {
        return companionDevice.getProtocolData(Constants.HASH_PC_PUBLIC_KEY) + ":" + companionDevice.getProtocolData("app_id");
    }

    private Map<String, String> preparePromptText() {
        Map<String, String> map = new HashMap<>();
        String type = companionDevice.getProtocolData("type");
        String deviceName = "SOMEDEVICE"; //TODO extract friendly name given by user

        switch (type) {
            case "Get": {
                String appId = companionDevice.getProtocolData(CoreGetReqMessage.Fields.APP_ID);
                String code = companionDevice.getProtocolData(CoreGetReqMessage.Fields.CODE);
                String desc = companionDevice.getProtocolData(CoreGetReqMessage.Fields.DESC);
                map.put("title", createTitleString(deviceName, appId, " requests access to its data."));
                map.put("subtitle", createSubtitleString(desc));
                map.put("code", createDesc(code));
            }
            break;
            case "Put": {
                String appId = companionDevice.getProtocolData(CorePutReqMessage.Fields.APP_ID);
                String code = companionDevice.getProtocolData(CorePutReqMessage.Fields.CODE);
                String desc = companionDevice.getProtocolData(CorePutReqMessage.Fields.DESC);
                map.put("title", createTitleString(deviceName, appId, " requests permission to store data."));
                map.put("subtitle", createSubtitleString(desc));
                map.put("code", createDesc(code));
            }
            break;
            case "Reg": {
                String appId = companionDevice.getProtocolData(CoreRegReqMessage.Fields.APP_ID);
                String desc = companionDevice.getProtocolData(CoreRegReqMessage.Fields.DESC);
                map.put("title", createTitleString(deviceName, appId, " requests permission to create a user verification key."));
                map.put("subtitle", createSubtitleString(desc));
            }
            break;
            case "Verify": {
                String appId = companionDevice.getProtocolData(CoreVerifyReqMessage.Fields.APP_ID);
                String code = companionDevice.getProtocolData(CoreVerifyReqMessage.Fields.CODE);
                String desc = companionDevice.getProtocolData(CoreRegReqMessage.Fields.DESC);
                map.put("title", createTitleString(deviceName, appId, " requests a user verification."));
                map.put("subtitle", createSubtitleString(desc));
                map.put("code", createDesc(code));
            }
            break;

        }
        return map;
    }


    private String createTitleString(String deviceName, String appId, String request) {
        StringBuffer sb = new StringBuffer();
        sb.append(appId).append(" on ").append(deviceName).append(request);
        return sb.toString();
    }

    private String createSubtitleString(String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append("Reason: ").append(desc);
        return sb.toString();
    }

    private String createDesc(String code) {
        StringBuffer sb = new StringBuffer();
        sb.append("Security Code: ").append(code);
        return sb.toString();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**binding.buttonEnrol1.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
        NavHostFragment.findNavController(ConnectFragment.this).navigate(R.id.action_First2Fragment_to_Second2Fragment);
        }
        });*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void requestBiometric(BiometricPrompt.PromptInfo prompt, Cipher cipher) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = getAuthenticationCallback();
        ContextCompat.getMainExecutor(this.getActivity());
        BiometricPrompt mBiometricPrompt = new BiometricPrompt(this, authenticationCallback);
        mBiometricPrompt.authenticate(prompt, new BiometricPrompt.CryptoObject(cipher));
    }

    private void requestBiometric(BiometricPrompt.PromptInfo prompt, Signature signature) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = getAuthenticationCallback();
        ContextCompat.getMainExecutor(this.getActivity());
        BiometricPrompt mBiometricPrompt = new BiometricPrompt(this, authenticationCallback);
        mBiometricPrompt.authenticate(prompt, new BiometricPrompt.CryptoObject(signature));
    }

    private BiometricPrompt.PromptInfo buildBiometricPrompt(String title, String subtitle, String description) {

        // Set prompt info
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle).setDescription(description).setNegativeButtonText("Cancel").build();
        return promptInfo;

    }

    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        // Callback for biometric authentication result
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Log.e(TAG, "Error code: " + errorCode + "error String: " + errString);
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                Log.i(TAG, "onAuthenticationSucceeded");
                super.onAuthenticationSucceeded(result);
                processCrypto(result.getCryptoObject());
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        };
    }

    private void processCrypto(BiometricPrompt.CryptoObject cryptoObject) {
        String type = companionDevice.getProtocolData("type");
        switch (type) {
            case "Put":
                doEncryption(cryptoObject.getCipher(), companionDevice.getProtocolData(CorePutReqMessage.Fields.DATA));
                break;
            case "Get":
                doDecryption(cryptoObject.getCipher(), companionDevice.getProtocolData(CoreGetReqMessage.Fields.ENC_DATA));
                break;
            case "Reg":
                //We don't want to create a signature during reg, so we just return
                //immediately, but we want to check that the key is bound to the biometric
                //hence why we make this call
                companionDevice.updateFromUI();
                break;
            case "Verify":
                doSignature(cryptoObject.getSignature(),companionDevice.getProtocolData(CoreVerifyReqMessage.Fields.NONCE));
                break;

        }

    }
    private String doSignature(Signature signature, String data){
        try {
            signature.update(B64.decode(data));
            String sig = B64.encode(signature.sign());
            Map<String, String> updateData = new HashMap<String, String>();
            updateData.put(CoreVerifyResMessage.Fields.APP_SIG, sig);
            companionDevice.updateFromUI(updateData);
        }catch (SignatureException e){
            e.printStackTrace();
        }
        return null;
    }
    private JSONObject doEncryption(Cipher cipher, String data) {
        try {
            byte[] cipherBytes = cipher.doFinal(B64.decode(data));
            byte[] iv = cipher.getIV();
            JSONObject cipherText = new JSONObject();
            cipherText.put("cipher_text", B64.encode(cipherBytes));
            cipherText.put("iv", B64.encode(iv));
            Map<String, String> updateData = new HashMap<String, String>();

            updateData.put(CorePutResMessage.Fields.ENC_DATA, cipherText.toString());
            companionDevice.updateFromUI(updateData);
        } catch (JSONException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject doDecryption(Cipher cipher, String encdata) {
        try {
            JSONObject cipherText = new JSONObject(encdata);

            byte[] cipherBytes = B64.decode(cipherText.getString("cipher_text"));
            byte[] iv = B64.decode(cipherText.getString("iv"));
            String plaintext = B64.encode(cipher.doFinal(cipherBytes));
            Map<String, String> data = new HashMap<String, String>();
            data.put(CoreGetResMessage.Fields.DATA, plaintext);
            companionDevice.updateFromUI(data);
        } catch (JSONException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }
}