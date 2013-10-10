package com.orzfly.ujslibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import cn.trinea.android.common.service.impl.ImageSDCardCache;

import com.google.gson.Gson;
import com.orzfly.ujslibrary.LibraryAPI.BookResult.BookStatusGroup;
import com.orzfly.ujslibrary.LibraryAPI.HotKeyword;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class BookResultActivity extends Activity {
	public static Context contextOfApplication;
	
	public View empty;
	public View loading;
	public String marcno;
	public ExpandableListView list;
	
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
        
        list = (ExpandableListView) this.findViewById(R.id.list_main);
        list.setVisibility(View.GONE);
        
        new AsyncTask<String, Void, LibraryAPI.BookResult>() {
        	String line = null;
        	
            @Override
            protected LibraryAPI.BookResult doInBackground(String... params) {
            	String marc_no = params[0];
            	Gson gson = new Gson();
            	
                InputStream inputStream = null;
                LibraryAPI.BookResult res = LibraryAPI.BookResultCache.get(marc_no);
                if (res != null)
                {
                	Log.e("BookResultLoader", "HIT " + marc_no);
                	return res;
                }
                
                Log.e("BookResultLoader", "MISS " + marc_no);
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
                    obj.marc_no = marcno;
                    obj.raw = line;
                    LibraryAPI.BookResultCache.put(marc_no, obj, -1);
                    return obj;
                } catch (Exception e) {
                    Log.e("BookResultParsing", "Error converting result " + e.toString());
                    e.printStackTrace();
                }
                return null;
            }
            
            public List<BookStatusGroup> groups;
            
            public void BuildText(final LibraryAPI.BookResult result) {
            	BookResultActivity.this.getActionBar().setTitle(result.getTitle());
            	
            	View header = BookResultActivity.this.getLayoutInflater().inflate(R.layout.fragment_book_result_1_header, null);
                TextView summary = (TextView) header.findViewById(R.id.text_summary);
                summary.setText(Html.fromHtml(result.getSummaryHTML()));
                
                View footer = BookResultActivity.this.getLayoutInflater().inflate(R.layout.fragment_book_result_1_footer, null);
                TextView details = (TextView) footer.findViewById(R.id.text_details);
                details.setText(Html.fromHtml(result.getDetailsHTML()));
                details.setMovementMethod(LinkMovementMethod.getInstance());
                /* if (result.raw != null)
                {
                	StringBuffer resultString = new StringBuffer();
                	Pattern regex = Pattern.compile("\\\\u([0-9a-f]{4})");
                	Matcher regexMatcher = regex.matcher(result.raw);
                	while (regexMatcher.find()) {
                		regexMatcher.appendReplacement(resultString, String.valueOf((char)Integer.parseInt(regexMatcher.group(1), 16)));
                	}
                	regexMatcher.appendTail(resultString);
                	details.setText(details.getText() + resultString.toString());
                }*/
                list.setHeaderDividersEnabled(true);
                list.setFooterDividersEnabled(true);
            	list.addHeaderView(header, null, false);
            	list.addFooterView(footer, null, false);
            	
            	if (result.douban != null && result.douban.images != null && result.douban.images.medium != null)
            	{
            		ImageView cover = (ImageView) header.findViewById(R.id.image_cover);
            		LibraryAPI.getIconCache().get(result.douban.images.medium, cover);
            		cover.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View view) {
							String url = result.douban.images.large != null ? result.douban.images.large : result.douban.images.medium;
							Intent myIntent = new Intent(BookResultActivity.this, TouchImageViewActivity.class);
							myIntent.putExtra("url", url);
							BookResultActivity.this.startActivity(myIntent);
						}
            		});
            	}            	
            	
            	if (result.status != null)
            	{
            		groups = new ArrayList<BookStatusGroup>();
            		for(LibraryAPI.BookResult.BookStatusResult book : result.status)
            		{
            			Boolean found = false;
            			for(BookStatusGroup group : groups)
            			{
            				if (
            						group.callno.equalsIgnoreCase(book.callno) &&
            						group.year.equalsIgnoreCase(book.year) &&
            						group.library.equalsIgnoreCase(book.library) &&
            						group.location.equalsIgnoreCase(book.location)
            					)
            				{
            					group.Children.add(book);
            					found = true;
            				}
            			}
            			
            			if (found) continue;
            			
            			BookStatusGroup group = new BookStatusGroup();
            			group.callno = book.callno;
            			group.year = book.year;
            			group.library = book.library;
            			group.location = book.location;
            			group.Children.add(book);
            			
            			groups.add(group);
            		}
            		
            		List<BookStatusGroup> groups2 = new ArrayList<BookStatusGroup>();
            		for(BookStatusGroup group : groups)
            		{
            			List<LibraryAPI.BookResult.BookStatusResult> books = new ArrayList<LibraryAPI.BookResult.BookStatusResult>();
            			for(LibraryAPI.BookResult.BookStatusResult book : group.Children)
            			{
            				if (book.available)
            					group.count_lendable += 1;
            				else
            					books.add(book);
            				
            				group.count += 1;
            			}
            			group.Children.removeAll(books);
            			Collections.sort(books);
            			Collections.sort(group.Children);
            			group.Children.addAll(books);
            			
            			if (group.count_lendable == 0)
            				groups2.add(group);
            		}
            		groups.removeAll(groups2);
            		Collections.sort(groups);
            		Collections.sort(groups2);
            		groups.addAll(groups2);
            		
		            list.setAdapter(new BaseExpandableListAdapter() {
						@Override
						public boolean areAllItemsEnabled() {
							return true;
						}

						@Override
						public LibraryAPI.BookResult.BookStatusResult getChild(int groupPosition,
								int childPosition) {
							return groups.get(groupPosition).Children.get(childPosition);
						}

						@Override
						public long getChildId(int groupPosition,
								int childPosition) {
							return childPosition;
						}

						@Override
						public View getChildView(int groupPosition,
								int childPosition, boolean isLastChild,
								View convertView, ViewGroup parent) {
							if (convertView == null)
	                	    {
	                	    	LayoutInflater inflater = BookResultActivity.this.getLayoutInflater();
	                	        convertView = inflater.inflate(R.layout.fragment_listrow_book_status, null);
	                	    }
	
	                	    LibraryAPI.BookResult.BookStatusResult data = getChild(groupPosition, childPosition);  
	                        if(null != data )
	                        {
	                    	    TextView text = (TextView)convertView.findViewById(R.id.text_html);
	        	                text.setText(Html.fromHtml(data.getHTML()));
	                        }
	
	                	    return convertView;
						}

						@Override
						public int getChildrenCount(int groupPosition) {
							return groups.get(groupPosition).Children.size();
						}
						
						@Override
						public BookStatusGroup getGroup(int groupPosition) {
							return groups.get(groupPosition);
						}

						@Override
						public int getGroupCount() {
							return groups.size();
						}

						@Override
						public long getGroupId(int groupPosition) {
							return groupPosition;
						}

						@Override
						public View getGroupView(int groupPosition,
								boolean isExpanded, View convertView,
								ViewGroup parent) {
							if (convertView == null)
	                	    {
	                	    	LayoutInflater inflater = BookResultActivity.this.getLayoutInflater();
	                	        convertView = inflater.inflate(R.layout.fragment_listrow_book_status_group, null);
	                	    }
	
							BookStatusGroup data = getGroup(groupPosition);  
	                        if(null != data )
	                        {
	                    	    TextView text = (TextView)convertView.findViewById(R.id.text_html);
	        	                text.setText(Html.fromHtml(data.getHTML()));
	                        }
	
	                	    return convertView;
						}

						@Override
						public boolean hasStableIds() {
							return true;
						}

						@Override
						public boolean isChildSelectable(int groupPosition,
								int childPosition) {
							return true;
						}

						@Override
						public boolean isEmpty() {
							return false;
						}

						@Override
						public void onGroupCollapsed(int groupPosition) {
							
						}

						@Override
						public void onGroupExpanded(int groupPosition) {
							
						}

						@Override
						public void registerDataSetObserver(
								DataSetObserver observer) {
							
						}

						@Override
						public void unregisterDataSetObserver(
								DataSetObserver observer) {
							
						}
	            	});
            	}
            }
            
            @Override
            protected void onPostExecute(LibraryAPI.BookResult result) {
                if (isCancelled()) return; 

                if (result != null)
                {
                	BuildText(result);
        	        loading.setVisibility(View.GONE);
        	        empty.setVisibility(View.GONE);
        	        list.setVisibility(View.VISIBLE);
                }
                else
                {
        	        loading.setVisibility(View.GONE);
        	        empty.setVisibility(View.VISIBLE);
        	        list.setVisibility(View.GONE);
                }
                
            };
        }.execute(marcno = intent.getStringExtra("marc_no"));
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
