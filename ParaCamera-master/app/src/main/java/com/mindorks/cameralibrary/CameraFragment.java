package com.mindorks.cameralibrary;

import android.app.Fragment;
import android.content.Entity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
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

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

/**
 * Created by janisharali on 15/11/16.
 */

public class CameraFragment extends Fragment {

    private ImageView picFrame;
    private Camera camera;
    //private TextView tv;

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

    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
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

                Feature feature = new Feature();
                feature.setType("LABEL_DETECTION");
                feature.setMaxResults(10);
                final AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
                annotateImageReq.setFeatures(Arrays.asList(feature));

                annotateImageReq.setImage(getImageEncodeImage(bitmap));

                // Create new thread
                new AsyncTask<Object, Void, String>() {
                    @Override
                    protected String doInBackground(Object... params) {
                        try {

                            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                            VisionRequestInitializer requestInitializer = new VisionRequestInitializer("AIzaSyB_wbusucGiv0ZAzH73_w_rm2tM7G88SR8");

                            Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                            builder.setVisionRequestInitializer(requestInitializer);

                            BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                            batchAnnotateImagesRequest.setRequests(Arrays.asList(annotateImageReq));

                            Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                            annotateRequest.setDisableGZipContent(true);

                            BatchAnnotateImagesResponse response = annotateRequest.execute();

                            String result = "";
                            for (AnnotateImageResponse imgResponse : response.getResponses()) {
                                for (EntityAnnotation entity : imgResponse.getLabelAnnotations()) {
                                    Log.d(TAG, entity.getDescription());
                                    result = classify(entity.getDescription());
                                    if (!result.equals("")) break;
                                }
                                if (result.equals("")) result = "landfill";
                                Log.d(TAG, result);
                            }

                            final String finalMessage = "This item is " + result.toLowerCase() + "!";
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    //Toast.makeText(getActivity().getApplicationContext(), finalMessage, Toast.LENGTH_LONG).show();
                                    ((CameraActivity) getActivity()).setTV(finalMessage);
                                }
                            });

                            Log.d(TAG, result);
                        } catch (GoogleJsonResponseException e) {
                            Log.d(TAG, "failed to make API request because " + e.getContent());
                        } catch (IOException e) {
                            Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
                        }
                        return "Cloud Vision API request failed. Check logs for details.";
                    }

                    protected void onPostExecute(String result) {
                        //visionAPIData.setText(result);
                        //imageUploadProgress.setVisibility(View.INVISIBLE);
                    }
                }.execute();

            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Picture not taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String classify(String description) {
        InputStream plasticStream = getActivity().getResources().openRawResource(R.raw.plastic);
        InputStream paperStream = getActivity().getResources().openRawResource(R.raw.paper);
        InputStream compostStream = getActivity().getResources().openRawResource(R.raw.compost);
        InputStream metalsStream = getActivity().getResources().openRawResource(R.raw.metals);

        Scanner scanner = new Scanner(plasticStream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals(description)) {
                return "plastic";
            }
        }
        scanner = new Scanner(paperStream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals(description)) {
                return "Paper";
            }
        }
        scanner = new Scanner(compostStream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals(description)) {
                return "Compost";
            }
        }
        scanner = new Scanner(metalsStream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals(description)) {
                return "Metal";
            }
        }
        scanner.close();
        return "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.deleteImage();
    }
}
