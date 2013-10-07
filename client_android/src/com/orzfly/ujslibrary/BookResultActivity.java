package com.orzfly.ujslibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class BookResultActivity extends Activity {
	public static Context contextOfApplication;
	
	public View empty;
	public View loading;
	public TextView text;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Intent intent = getIntent();

    	contextOfApplication = getApplicationContext();
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_result);
        
        ActionBar ab = this.getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        ab.setTitle(intent.getStringExtra("title"));
        
        loading = this.findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE);
        
        empty = this.findViewById(R.id.empty);
        empty.setVisibility(View.GONE);
        
        text = (TextView) this.findViewById(R.id.text_book);
        text.setVisibility(View.GONE);
        
        new AsyncTask<String, Void, LibraryAPI.BookResult>() {
        	String line = null;
        	
            @Override
            protected LibraryAPI.BookResult doInBackground(String... params) {
            	String marc_no = params[0];
            	Gson gson = new Gson();
            	
                InputStream inputStream = null;
                
                try {
                	ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
                    HttpClient httpClient = new DefaultHttpClient();

                    HttpPost httpPost = new HttpPost(LibraryAPI.buildBookURL(marc_no));
                    httpPost.setEntity(new UrlEncodedFormEntity(param));
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();

                    inputStream = httpEntity.getContent();
                } catch (UnsupportedEncodingException e1) {
                } catch (ClientProtocolException e2) {
                } catch (IllegalStateException e3) {
                } catch (IOException e4) {
                }
                try {
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"), 8);
                    StringBuilder sBuilder = new StringBuilder();

                    while ((line = bReader.readLine()) != null) {
                        sBuilder.append(line + "\n");
                    }
                    line = sBuilder.toString();
                    
                    LibraryAPI.BookResult obj = gson.fromJson(line, LibraryAPI.BookResult.class);
                    BuildText(obj);
                    return obj;
                } catch (Exception e) {
                    Log.e("BookResultParsing", "Error converting result " + e.toString());
                    e.printStackTrace();
                }
                return null;
            }
            
            public void BuildText(LibraryAPI.BookResult result) {
            	text.setText(line);
            }
            
            @Override
            protected void onPostExecute(LibraryAPI.BookResult result) {
                if (isCancelled()) return; 

                if (result != null)
                {
        	        loading.setVisibility(View.GONE);
        	        empty.setVisibility(View.GONE);
        	        text.setVisibility(View.VISIBLE);
                }
                else
                {
        	        loading.setVisibility(View.GONE);
        	        empty.setVisibility(View.VISIBLE);
        	        text.setVisibility(View.GONE);
                }
                
            };
        }.execute(intent.getStringExtra("marc_no"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
    	if (menuItem.getItemId() == android.R.id.home)
    	{
    		onBackPressed();
        	return true;
    	}
    	else
    	{
    		return super.onOptionsItemSelected(menuItem);
    	}
    }
    
	public static Context getContextOfApplication(){
        return contextOfApplication;
    }
}
