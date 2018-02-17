package com.mindorks.cameralibrary;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;

import com.google.api.services.vision.v1.model.Image;
import com.mindorks.paracamera.Camera;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by janisharali on 15/11/16.
 */

public class CameraFragment extends Fragment {

    private ImageView picFrame;
    private Camera camera;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        picFrame = (ImageView) rootView.findViewById(R.id.picFrame);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        camera = new Camera.Builder()
                .resetToCorrectOrientation(true)
                .setTakePhotoRequestCode(1)
                .setDirectory("pics")
                .setName("ali_" + System.currentTimeMillis())
                .setImageFormat(Camera.IMAGE_JPEG)
                .setCompression(75)
                .setImageHeight(1000)
                .build(this);
        try {
            camera.takePicture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
            final Bitmap bitmap = camera.getCameraBitmap();
            if (bitmap != null) {
                picFrame.setImageBitmap(bitmap);
                Vision.Builder visionBuilder = new Vision.Builder(
                        new NetHttpTransport(),
                        new AndroidJsonFactory(),
                        null);

                visionBuilder.setVisionRequestInitializer(
                        new VisionRequestInitializer("AIzaSyB_wbusucGiv0ZAzH73_w_rm2tM7G88SR8"));

                final Vision vision = visionBuilder.build();
                Toast.makeText(getActivity().getApplicationContext(), "Google Vision API Is Great!", Toast.LENGTH_SHORT).show();


                // Create new thread
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {

                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "we made it here!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Convert photo to byte array
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "changed to byte array", Toast.LENGTH_SHORT).show();
                            }
                        });

                        Image inputImage = new Image();
                        inputImage.encodeContent(byteArray);
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "content encoded", Toast.LENGTH_SHORT).show();
                            }

                        });

                        Feature desiredFeature = new Feature();
                        desiredFeature.setType("LABEL_DETECTION");

                        AnnotateImageRequest request = new AnnotateImageRequest();
                        request.setImage(inputImage);
                        request.setFeatures(Arrays.asList(desiredFeature));

                        BatchAnnotateImagesRequest batchRequest =
                                new BatchAnnotateImagesRequest();

                        batchRequest.setRequests(Arrays.asList(request));
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "before the try", Toast.LENGTH_SHORT).show();
                            }

                        });

                        try {
                            BatchAnnotateImagesResponse batchResponse =
                                    vision.images().annotate(batchRequest).execute();
                            List<AnnotateImageResponse> responses = batchResponse.getResponses();




                            //for (int i = 0; i < responses.size(); i++) {
                                final String result = responses.get(0).getLabelAnnotations().get(0).toString();
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getActivity().getApplicationContext(), result, Toast.LENGTH_LONG).show();
                                    }

                                });
                            //}

                            /*for (AnnotateImageResponse res : responses) {
                                /*if (res.hasError()) {
                                    out.printf("Error: %s\n", res.getError().getMessage());
                                    return;
                                }

                                // For full list of available annotations, see http://g.co/cloud/vision/docs
                                for (EntityAnnotation annotation : res.getLabelAnnotations()) {
                                    annotation.getDescription().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
                                }
                            }*/


                        } catch (IOException EX) {
                            Toast.makeText(getActivity().getApplicationContext(), "OH NOOOOOOO!", Toast.LENGTH_SHORT).show();
                        }


                    }
                });

            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Picture not taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.deleteImage();
    }
}
