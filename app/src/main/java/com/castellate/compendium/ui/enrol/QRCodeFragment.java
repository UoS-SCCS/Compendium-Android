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

package com.castellate.compendium.ui.enrol;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.transition.Slide;

import com.castellate.compendium.R;
import com.castellate.compendium.protocol.enrol.InitKeyReqProtocolMessage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Fragment to show a camera to use for detecting the QRCode
 */
public class QRCodeFragment extends Fragment {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    private static final String TAG = "QRCodeFragment";
    private SharedViewModel sharedModel;

    /**
     * Create a new QRCodeFragment
     */
    public QRCodeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new Slide(Gravity.START));



    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_q_r_code, container, false);
        previewView = view.findViewById(R.id.preview_view);
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                //TODO Better exception handling
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
        return view;
    }

    /**
     * Called when a QRCode is detected, this will check if it is of the correct structure and
     * if so stop the camera and move to enrolment, otherwise it ignores it.
     * @param imageProxy image proxy used to process the captured frames
     * @param barcodes the list of barcodes that have been found
     */
    private void processBarcode(ImageProxy imageProxy,List<Barcode> barcodes){

        for (Barcode barcode : barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();

            if((new InitKeyReqProtocolMessage()).parse(rawValue)) {
                sharedModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

                sharedModel.setMessage(rawValue);
                Log.d(TAG, barcode.getRawValue());
                try {
                    cameraProviderFuture.get().unbindAll();
                } catch (ExecutionException | InterruptedException e) {
                    //TODO Better exception handling
                    e.printStackTrace();
                }
                vibrate();
                NavHostFragment.findNavController(QRCodeFragment.this)
                        .navigate(R.id.action_QRCodeFragment_to_completeEnrolment);
            }


        }
        imageProxy.close();
    }

    /**
     * Vibrate the device to indicate that a QRCode has been found to provide haptic feedback
     */
    private void vibrate(){
        // this type of vibration requires API 29
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            final Vibrator vibrator = (Vibrator)requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
            // create vibrator effect with the constant EFFECT_TICK
            VibrationEffect vEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel();
            vibrator.vibrate(vEffect);
        }
    }

    /**
     * Bind the image analysis to the camera provider
     * @param cameraProvider Camera provider to bind to
     */
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new ImageAnalysis.Analyzer() {
            @ExperimentalGetImage
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {
                    InputImage image =
                            InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    BarcodeScannerOptions options =
                            new BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(
                                            Barcode.FORMAT_QR_CODE)
                                    .build();
                    BarcodeScanner scanner = BarcodeScanning.getClient(options);
                    scanner.process(image)
                            .addOnSuccessListener(barcodes -> processBarcode(imageProxy, barcodes))
                            .addOnFailureListener(e -> {
                                // Task failed with an exception
                                // ...
                                Log.d(TAG, "QRCode scanner failed", e);
                            });

                }

            }

        });

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }
}