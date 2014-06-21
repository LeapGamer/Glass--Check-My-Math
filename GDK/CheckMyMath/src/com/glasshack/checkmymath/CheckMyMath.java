/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glasshack.checkmymath;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.CameraManager;

/**
 * Activity showing the stopwatch options menu.
 */
public class CheckMyMath extends Activity {

    private final Handler mHandler = new Handler();
    private Card mCard;
    private View mCardView;
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showBlankCard();
        
        // Added this to connect to web service 
    	if (android.os.Build.VERSION.SDK_INT > 9) {
	      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	      StrictMode.setThreadPolicy(policy);
	    }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.checkmymath, menu);
        return true;
    }
    
    @Override
    protected void onResume() {
        // To receive touch events from the touchpad, the view should have focus.
        mCardView.requestFocus();
        super.onResume();
    }
    
    
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // user tapped touchpad, open menu
        	openOptionsMenu();
            return true;
        }
        super.onKeyDown(keycode, event);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        // Handle item selection.
        switch (item.getItemId()) {
        	case R.id.stop:
        		// Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting a new Activity or stopping a Service
                // that published a LiveCard.
                /*post(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(CheckMyMath.this, CheckMyMathService.class));
                    }
                });*/
        		return true;
            case R.id.takepicture:
            	Log.e("WHOA", "about to take a pic");
            	takePicture();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the Activity.
        //finish();
    }

    /**
     * Posts a {@link Runnable} at the end of the message loop, overridable for testing.
     */
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }
    
    /** Cards **/
    private void showBlankCard(){
    	mCard = new Card(this);
    	mCard.setText("");
    	mCardView = mCard.getView();
    	//mCardView.setOnClickListener();
    	setContentView(mCardView);
    }
    
    private void showCorrectCard(){
    	mCard.setImageLayout(Card.ImageLayout.LEFT);
    	mCard.addImage(R.drawable.ic_correct);
    	mCard.setText("Correct Correct");
    	mCardView = mCard.getView();
    	setContentView(mCardView);
    }
    
    private void showWrongCard(){
    	mCard.setImageLayout(Card.ImageLayout.LEFT);
    	mCard.addImage(R.drawable.ic_incorrect);
    	mCard.setText("Wrong Wrong Wrong");
    	mCardView = mCard.getView();
    	setContentView(mCardView);
    }
    /** Cards End **/
    
    // Taking picture stuff
    private static final int TAKE_PICTURE_REQUEST = 1;
	
    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }
    
    private void evaluateResponse(String resp){
    	if(resp.contains("1")){
    		showCorrectCard();
    		Log.e("Evaluation", "Correct!");
    	}else{
    		showWrongCard();
    		Log.e("Evaluation", "Incorrect!");
    	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String picturePath = data.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
            processPictureWhenReady(picturePath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            // The picture is ready; process it.
        	Log.e("Picture Path", picturePath);
        	List<NameValuePair> postData = new ArrayList<NameValuePair>();
        	postData.add(new BasicNameValuePair("file", picturePath));
        	postData.add(new BasicNameValuePair("answer", "1"));
        	postData.add(new BasicNameValuePair("submit", "Submit"));
        	String postResp = post("http://54.187.58.53/glassmath.php", postData);
        	evaluateResponse(postResp);
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }
    
    // End taking picture stuff
    
    // Posting image to server stuff
    
    public String post(String url, List<NameValuePair> nameValuePairs) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(url);
        String readableResponse = null;
        try {
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            for(int index=0; index < nameValuePairs.size(); index++) {
                if(nameValuePairs.get(index).getName().equalsIgnoreCase("file")) {
                    // If the key equals to "file", we use FileBody to transfer the data
                    entity.addPart("file", new FileBody(new File (nameValuePairs.get(index).getValue()), "image/jpeg"));
                } else {
                    // Normal string data
                    entity.addPart(nameValuePairs.get(index).getName(), new StringBody(nameValuePairs.get(index).getValue()));
                }
            }

            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost, localContext);
            readableResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            
            Log.e("response", readableResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return readableResponse;
    }
    
}
