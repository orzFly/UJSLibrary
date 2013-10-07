package com.orzfly.ujslibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import com.google.gson.Gson;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SearchResultActivity extends Activity {
		public static Context contextOfApplication;
		AmazingListView list;
	    ViewPager mViewPager;
	    LibraryAPI.SearchParameter sp;
	    SearchResultAdapter adapter;
	    View loading;
	    View empty;
	    
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	    	Intent intent = getIntent();
	    	
	    	contextOfApplication = getApplicationContext();
	    	
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_search_result);
	        
	        ActionBar ab = this.getActionBar();
	        ab.setDisplayHomeAsUpEnabled(true);
	        ab.setHomeButtonEnabled(true);
	        ab.setTitle(intent.getStringExtra("title"));
	        
	        sp = new LibraryAPI.SearchParameter();
	        sp.title = intent.getStringExtra("title");
	        
	        loading = this.findViewById(R.id.loading);
	        loading.setVisibility(View.VISIBLE);
	        
	        empty = this.findViewById(R.id.empty);
	        empty.setVisibility(View.GONE);
	        
	        list = (AmazingListView) this.findViewById(R.id.list);
	        list.setLoadingView(this.getLayoutInflater().inflate(R.layout.fragment_loading, null));
	        list.setAdapter(adapter = new SearchResultAdapter(sp));
	        list.setEmptyView(loading);

	        adapter.reset();
	    }

	    public static Context getContextOfApplication(){
	        return contextOfApplication;
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
	    
        class SearchResultAdapter extends AmazingAdapter {
                List<LibraryAPI.SearchBookResult> list = new ArrayList<LibraryAPI.SearchBookResult>();
        	    LibraryAPI.SearchParameter sp;
        	    Gson gson = new Gson();
        	    
                private AsyncTask<Integer, Void, Pair<Boolean, List<LibraryAPI.SearchBookResult>>> backgroundTask;

                public SearchResultAdapter(LibraryAPI.SearchParameter sp)
                {
                	this.sp = sp;
                }
                
                public void reset() {
                    if (backgroundTask != null) backgroundTask.cancel(false);
                    
                    list = new ArrayList<LibraryAPI.SearchBookResult>();
                    notifyDataSetChanged();
                    
                    onNextPageRequested(1);
                }
                
                @Override
                public int getCount() {
                    return list.size();
                }

                @Override
                public LibraryAPI.SearchBookResult getItem(int position) {
                    return list.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                protected void onNextPageRequested(int page) {
                    Log.d(TAG, "Got onNextPageRequested page=" + page);
                    
                    if (backgroundTask != null) {
                            backgroundTask.cancel(false);
                    }
                    
                    backgroundTask = new AsyncTask<Integer, Void, Pair<Boolean, List<LibraryAPI.SearchBookResult>>>() {
                    	int page;
                    	
                        @Override
                        protected Pair<Boolean, List<LibraryAPI.SearchBookResult>> doInBackground(Integer... params) {
                            page = params[0];
                            InputStream inputStream = null;
                            
                            sp.page = page;
                            try {
                            	ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
                                HttpClient httpClient = new DefaultHttpClient();

                                HttpPost httpPost = new HttpPost(LibraryAPI.buildSearchURL(sp));
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
                                LibraryAPI.SearchResult obj = gson.fromJson(bReader, LibraryAPI.SearchResult.class);
                                
                                return new Pair<Boolean, List<LibraryAPI.SearchBookResult>>(
                            		obj.pages > page, 
                            		new ArrayList<LibraryAPI.SearchBookResult>(Arrays.asList(obj.books))
                        		);
                            } catch (Exception e) {
                                Log.e("SearchResultParsing", "Error converting result " + e.toString());
                            }
                            
                            return new Pair<Boolean, List<LibraryAPI.SearchBookResult>>(
                            	false,
                            	new ArrayList<LibraryAPI.SearchBookResult>()
                            );
                        }
                        
                        @Override
                        protected void onPostExecute(Pair<Boolean, List<LibraryAPI.SearchBookResult>> result) {
                            if (isCancelled()) return; 

                            if (page == 1)
                            {
	                	        loading.setVisibility(View.GONE);
	                	        empty.setVisibility(View.VISIBLE);
	                	        SearchResultActivity.this.list.setEmptyView(empty);
                            }
                            
                            list.addAll(result.second);
                            nextPage();
                            notifyDataSetChanged();
                            
                            if (result.first) {
                                    notifyMayHaveMorePages();
                            } else {
                                    notifyNoMorePages();
                            }
                        };
                    }.execute(page);
                }

                @Override
                protected void bindSectionHeader(View view, int position, boolean displaySectionHeader) {
                }

                @Override
                public View getAmazingView(int position, View convertView, ViewGroup parent) {
                    View res = convertView;
                    if (res == null) res = getLayoutInflater().inflate(R.layout.fragment_listrow_book, null);
                    
                    TextView text = (TextView) res.findViewById(R.id.text_html);
                    LibraryAPI.SearchBookResult book = getItem(position);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("<strong><big>");
                    sb.append(StringEscapeUtils.escapeHtml4(book.title));
                    sb.append("</big></strong><br />");
                    
                    sb.append(book.count_lendable > 0 ? "<font color=\"green\">" : "<font color=\"red\">" );
                    sb.append(StringEscapeUtils.escapeHtml4(String.valueOf(book.count_lendable)));
                    sb.append("/" );
                    sb.append(StringEscapeUtils.escapeHtml4(String.valueOf(book.count)));
                    sb.append("</font>" );
                    if (book.type.length() > 0)
                    {
                    	sb.append(" ");
                    	sb.append(StringEscapeUtils.escapeHtml4(book.type));
                    }
                    sb.append(" ");
                    sb.append(StringEscapeUtils.escapeHtml4(book.call_no));
                    sb.append("<br /><small>");
                    
                    ArrayList<String> list = new ArrayList<String>();
                    
                    if (book.author.length() > 0)
                    	list.add(StringEscapeUtils.escapeHtml4(book.author));
                    
                    if (book.publisher.length() > 0)
                    	list.add(StringEscapeUtils.escapeHtml4(book.publisher));
                    
                    if (book.year > 0)
                    	list.add(StringEscapeUtils.escapeHtml4(String.valueOf(book.year)));

                    sb.append(StringUtils.join(list, "; "));
                    
                    sb.append("</small>");
                    
                    text.setText(Html.fromHtml(sb.toString()));
                    
                    return res;
                }

                @Override
                public void configurePinnedHeader(View header, int position, int alpha) {
                }

                @Override
                public int getPositionForSection(int section) {
                    return 0;
                }

                @Override
                public int getSectionForPosition(int position) {
                    return 0;
                }

                @Override
                public Object[] getSections() {
                    return null;
                }
                
        }
}
