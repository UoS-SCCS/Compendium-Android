package com.castellate.compendium.ui.request;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.castellate.compendium.R;
import com.castellate.compendium.crypto.B64;
import com.castellate.compendium.crypto.CompanionKeyManager;
import com.castellate.compendium.crypto.CryptoException;
import com.castellate.compendium.crypto.CryptoUtils;
import com.castellate.compendium.data.IdentityStore;
import com.castellate.compendium.exceptions.StorageException;
import com.castellate.compendium.databinding.ConnectFragmentBinding;
import com.castellate.compendium.exceptions.CompendiumException;
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
import com.castellate.compendium.protocol.messages.EncryptedMessage;
import com.castellate.compendium.CompanionDevice;
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
    private CompanionKeyManager ckm;
    private boolean inError = false;
    private boolean delayedError =false;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ckm = new CompanionKeyManager();
        } catch (CryptoException e) {
            Log.d(TAG,"Exception loading CompanionKeyManager, will close",e);
            delayedError=true;
        }
    }

    private void processAwaitingUI(String protocolState) throws CompendiumException {

        try {
            if (protocolState.equals(CoreProtocol.STATE.CORE_RESP.name())) {
                Map<String, String> map = preparePromptText();
                if (!canAuthenticateWithStrongBiometrics()) {
                    showGenericError("Your device is not configured to use biometrics");
                    Log.d(TAG, "Cannot use biometrics");
                } else {
                    BiometricPrompt.PromptInfo prompt = this.buildBiometricPrompt(map.get("title"), map.get("subtitle"), map.get("code"));
                    String type = companionDevice.getProtocolData("type");
                    switch (type) {
                        case "Get":
                            JSONObject obj = new JSONObject(companionDevice.getProtocolData(CoreGetReqMessage.Fields.ENC_DATA));
                            requestBiometric(prompt, ckm.getDecryptionCipher(getKeyId(), B64.decode(obj.getString(EncryptedMessage.IV))));
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
                            requestBiometric(prompt, ckm.getSignatureObject(getKeyId()));
                            break;
                    }


                }
            }
        } catch (JSONException e) {
            showGenericError("Error decrypting message from PC");
            throw new CompendiumException("Exception processing request", e);
        } catch (CryptoException e) {
            showGenericError("Cryptography error - cannot process request");
            throw new CompendiumException("Exception processing request", e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        binding = ConnectFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        if(delayedError){
            showGenericError("Error during initialisation, will close",view);
            return binding.getRoot();
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        if(prefs == null){
            showGenericError("Invalid application settings - cannot get device ID",view);
            return binding.getRoot();
        }
        view.findViewById(R.id.connect_ok_error_button).setOnClickListener(viewButton -> requireActivity().finishAffinity());
        String deviceId = prefs.getString("id", android.os.Build.MODEL);
        companionDevice = new CompanionDevice(deviceId);

        RequestViewModel requestViewModel = new ViewModelProvider(requireActivity()).get(RequestViewModel.class);

        requestViewModel.getProtocolStatus().observe(getViewLifecycleOwner(), status -> {
            try {
                String protocolState = companionDevice.getCurrentStateOfProtocol();
                Log.d(TAG, "State:" + protocolState);
                Log.d(TAG, "Status:" + status);
                if (status == Protocol.STATUS.AWAITING_UI) {
                    processAwaitingUI(protocolState);
                }
                ((CircularProgressIndicator) view.findViewById(R.id.progress_spinner_req)).setProgress(companionDevice.getProgress(), true);
                if (status == Protocol.STATUS.FINISHED) {
                    Log.d(TAG, "Protocol finished will write out data");
                }
            } catch (CompendiumException e) {
                Log.d(TAG, "Compendium exception", e);
                showGenericError();
            }
        });
        requestViewModel.getProtocolState().observe(getViewLifecycleOwner(), state -> {
            Log.d(TAG, "State:" + state);
            ((TextView) view.findViewById(R.id.progress_status_req)).setText(state);
        });

        PushRequestSharedViewModel model = new ViewModelProvider(requireActivity()).get(PushRequestSharedViewModel.class);
        model.getMessage().observe(getViewLifecycleOwner(), item -> {
            Log.d(TAG, "Push message received");
            try {
                CoreProtocol coreProtocol = new CoreProtocol();
                companionDevice.runProtocol(coreProtocol);
                companionDevice.setProtocolViewModel(requestViewModel);
                companionDevice.processMessage(item);
            } catch (ProtocolException e) {
                showGenericError("Error processing request");
            }
        });

        return binding.getRoot();

    }

    private boolean canAuthenticateWithStrongBiometrics() {
        return BiometricManager.from(requireContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private String getKeyId() {
        return companionDevice.getProtocolData(Constants.HASH_PC_PUBLIC_KEY) + ":" + companionDevice.getProtocolData("app_id");
    }

    private Map<String, String> preparePromptText() throws StorageException {
        Map<String, String> map = new HashMap<>();
        String type = companionDevice.getProtocolData("type");
        IdentityStore identityStore = IdentityStore.getInstance();
        String deviceName = identityStore.getNameByKeyID(companionDevice.getProtocolData(Constants.PC_PUBLIC_KEY));

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
        return appId + " on " + deviceName + request;
    }

    private String createSubtitleString(String desc) {
        return "Reason: " + desc;
    }

    private String createDesc(String code) {
        return "Security Code: " + code;
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void requestBiometric(BiometricPrompt.PromptInfo prompt, Cipher cipher) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = getAuthenticationCallback();
        ContextCompat.getMainExecutor(requireActivity());
        BiometricPrompt mBiometricPrompt = new BiometricPrompt(this, authenticationCallback);
        mBiometricPrompt.authenticate(prompt, new BiometricPrompt.CryptoObject(cipher));
    }

    private void requestBiometric(BiometricPrompt.PromptInfo prompt, Signature signature) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = getAuthenticationCallback();
        ContextCompat.getMainExecutor(requireActivity());
        BiometricPrompt mBiometricPrompt = new BiometricPrompt(this, authenticationCallback);
        mBiometricPrompt.authenticate(prompt, new BiometricPrompt.CryptoObject(signature));
    }

    private BiometricPrompt.PromptInfo buildBiometricPrompt(String title, String subtitle, String description) {
        // Set prompt info
        return  new BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle).setDescription(description).setNegativeButtonText("Cancel").build();
    }

    private void fadeIn(View view) {
        if(view == null){
            return;
        }
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate().alpha(1f).setDuration(100).setListener(null);

    }

    private void fadeOut(View view) {
        if(view == null){
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

    private void showGenericError() {
        showGenericError(null);
        companionDevice.reset();
    }

    private void showGenericError(String customText) {
        showGenericError(customText,null);
    }
    private void showGenericError(String customText, View passedView) {
        if (inError) {
            return;
        }
        if(companionDevice!=null) {
            companionDevice.reset();
        }
        View view;
        if(passedView!=null){
            view = passedView;
        }else {
            view = requireView();
        }
        this.inError = true;
        crossFade(view.findViewById(R.id.connect_preloader), view.findViewById(R.id.failed_connect));
        //Fade out other content
        fadeOut(view.findViewById(R.id.cancel_button));
        fadeOut(view.findViewById(R.id.instructions));
        fadeOut(view.findViewById(R.id.code_details));
        fadeOut(view.findViewById(R.id.reason_details));
        fadeOut(view.findViewById(R.id.device_details));


        //Fade in error contents
        fadeIn(view.findViewById(R.id.error_title));
        fadeIn(view.findViewById(R.id.error_explain));
        fadeIn(view.findViewById(R.id.connect_ok_error_button));

        ((TextView) view.findViewById(R.id.error_title)).setText(R.string.error_title);
        if (customText != null) {
            ((TextView) view.findViewById(R.id.error_explain)).setText(customText);
        } else {
            ((TextView) view.findViewById(R.id.error_explain)).setText(R.string.generic_enrol_error);
        }

    }

    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        // Callback for biometric authentication result
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Log.e(TAG, "Error code: " + errorCode + "error String: " + errString);
                super.onAuthenticationError(errorCode, errString);
                showGenericError("Biometric authentication failed");
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
                showGenericError("Biometric authentication failed");
            }
        };
    }

    private void processCrypto(BiometricPrompt.CryptoObject cryptoObject) {
        try {
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
                    doSignature(cryptoObject.getSignature(), companionDevice.getProtocolData(CoreVerifyReqMessage.Fields.NONCE));
                    break;

            }
        } catch (CryptoException e) {
            Log.d(TAG, "Exception processing biometric crypto", e);
            showGenericError();
        }

    }

    private void doSignature(Signature signature, String data) throws CryptoException {
        if(signature==null){
            throw new CryptoException("Null signature object");
        }
        try {
            signature.update(B64.decode(data));
            String sig = B64.encode(signature.sign());
            Map<String, String> updateData = new HashMap<>();
            updateData.put(CoreVerifyResMessage.Fields.APP_SIG, sig);
            companionDevice.updateFromUI(updateData);
        } catch (SignatureException e) {
            throw new CryptoException("Exception creating signature", e);
        }
    }

    private void doEncryption(Cipher cipher, String data) throws CryptoException {
        if(cipher==null){
            throw new CryptoException("Null cipher object");
        }
        try {
            byte[] cipherBytes = cipher.doFinal(B64.decode(data));
            byte[] iv = cipher.getIV();
            JSONObject cipherText = new JSONObject();
            cipherText.put("cipher_text", B64.encode(cipherBytes));
            cipherText.put("iv", B64.encode(iv));
            Map<String, String> updateData = new HashMap<>();

            updateData.put(CorePutResMessage.Fields.ENC_DATA, cipherText.toString());
            companionDevice.updateFromUI(updateData);
        } catch (JSONException | BadPaddingException | IllegalBlockSizeException e) {
            throw new CryptoException("Exception doing encryption", e);
        }
    }

    private void doDecryption(Cipher cipher, String encryptedData) throws CryptoException {
        if(cipher==null){
            throw new CryptoException("Null cipher object");
        }
        try {
            JSONObject cipherText = new JSONObject(encryptedData);

            byte[] cipherBytes = B64.decode(cipherText.getString(EncryptedMessage.CIPHER_TEXT));

            String plaintext = B64.encode(cipher.doFinal(cipherBytes));
            Map<String, String> data = new HashMap<>();
            data.put(CoreGetResMessage.Fields.DATA, plaintext);
            companionDevice.updateFromUI(data);
        } catch (JSONException | BadPaddingException | IllegalBlockSizeException e) {
            throw new CryptoException("Exception doing decryption", e);
        }

    }
}