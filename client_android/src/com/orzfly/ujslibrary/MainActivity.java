package com.orzfly.ujslibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

	public static Context contextOfApplication;

    SectionsPagerAdapter mSectionsPagerAdapter;
    
    public static MainActivity instance;

    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	contextOfApplication = getApplicationContext();
    	instance = this;
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    public static Context getContextOfApplication(){
        return contextOfApplication;
    }
    
    public static MainActivity getInstance()
    {
    	return instance;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu); 	
        return true;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }
    
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            
            if (position == 0)
            {
            	fragment = new RecordSearchSectionFragment();
            }
            else
            {
	            fragment = new DummySectionFragment();
	            Bundle args = new Bundle();
	            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
	            fragment.setArguments(args);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_record_search).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    public static class DummySectionFragment extends Fragment {
        public static final String ARG_SECTION_NUMBER = "section_number";

        public DummySectionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
            TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    public static class RecordSearchSectionFragment extends Fragment {
    	public static List<HotKeyword> kwlist = null;
    	
    	public static final String PREF_HISTORY_KEYWORDS = "history_keywords";
    	public static final String PREF_TOP_KEYWORDS = "top_keywords";
    	public static final String PREF_TOP_KEYWORDS_CACHED = "top_keywords_cached";
    	public HotKeywordsAdapter adapter;
    	public AutoCompleteTextView keyword;
    	public HotKeywordsSuggestionsAsyncTask lastsuggesttask;
    	
        public RecordSearchSectionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	kwlist = new ArrayList<HotKeyword>();
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContextOfApplication());
        	Set<String> historys = prefs.getStringSet(PREF_HISTORY_KEYWORDS, new HashSet<String>());
        	for(String item : historys)
        	{
        		kwlist.add(new HotKeyword(item, 0, HotKeyword.TYPE_HISTORY));
        	}
        	
            final View rootView = inflater.inflate(R.layout.fragment_main_record_search, container, false);
            
            final ListView keywords = (ListView) rootView.findViewById(R.id.list_hot_keywords);
            keywords.setAdapter(adapter = new HotKeywordsAdapter(getContextOfApplication(), 0, kwlist));
            HotKeywordsAsyncTask task = new HotKeywordsAsyncTask();
            task.execute("", "");
            
            final Button submit = (Button) rootView.findViewById(R.id.button_submit);
            keyword = (AutoCompleteTextView) rootView.findViewById(R.id.edit_keywords);
            keyword.addTextChangedListener(new TextWatcher(){  
            	  
                public void afterTextChanged(Editable editable) {  
      
                }  
      
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {  
      
                }  
      
                public void onTextChanged(CharSequence s, int start, int before, int count) {  
                    String newText = s.toString();  
                    if (lastsuggesttask != null)
                    {
                    	lastsuggesttask.cancel(false);
                    }
                    (lastsuggesttask = new HotKeywordsSuggestionsAsyncTask()).execute(newText);  
                }  
            });
            
            submit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					String kw = keyword.getText().toString().trim();
					
					if (kw.length() > 0)
					{
						int i = 0;
						List<HotKeyword> tmplist = new ArrayList<HotKeyword>(kwlist);
						for(HotKeyword k : tmplist)
			        	{
			        		if (k.keyword.equals(kw))
			        		{
			        			adapter.remove(k);
			        		}
			        		else
			        		{
				        		if (k.type == HotKeyword.TYPE_HISTORY)
				        		{
				        			if (i <= 10)
				        				i += 1;
				        			else
				        				adapter.remove(k);
				        		}
			        		}
			        	}
			        	adapter.insert(new HotKeyword(kw, 0, HotKeyword.TYPE_HISTORY), 0);
			        	
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContextOfApplication());
						Set<String> historys = new HashSet<String>();
						for(HotKeyword k : kwlist)
			        	{
			        		if (k.type == HotKeyword.TYPE_HISTORY)
			        			historys.add(k.keyword);
			        	}
						SharedPreferences.Editor editor = prefs.edit();
						editor.putStringSet(PREF_HISTORY_KEYWORDS, historys);
						editor.commit();
					
						Intent myIntent = new Intent(MainActivity.getInstance(), SearchResultActivity.class);
						myIntent.putExtra("title", kw);
						MainActivity.getInstance().startActivity(myIntent);
					}
					else
					{
						Toast.makeText(getContextOfApplication(), R.string.toast_keyword_required, Toast.LENGTH_LONG).show();
					}
				}
            });
            
            keywords.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					HotKeyword key = (HotKeyword)parent.getAdapter().getItem(position);
					keyword.setText(key.keyword);
					keyword.requestFocus();
					keyword.selectAll();
				}
            	
            });
            
            return rootView;
        }

        void addKw(List<HotKeyword> list, HotKeyword nw)
        {
        	if (nw.keyword.trim().isEmpty())
        		return;
        	
        	for(HotKeyword kw : list)
        	{
        		if (kw.keyword.equals(nw.keyword))
        			return;
        	}
        	list.add(nw);
        }
        
        class HotKeyword {
        	private final String keyword;
        	private final int count;
        	private final int type;
        	
        	public static final int TYPE_HISTORY = 0;
        	public static final int TYPE_HOT = 1;
        	public static final int TYPE_SUGGESTION = 2;
        	public static final int TYPE_TRANSLATION = 3;
        	
        	public HotKeyword(String keyword, int count, int type)
        	{
        		this.keyword = keyword;
        		this.count = count;
        		this.type = type;
        	}
        	
        	public String getKeyword() { return this.keyword; }
        	public int getCount() { return this.count; }
        	public int getType() { return this.type; }

			@Override
			public String toString() {
				return this.keyword;
			}
        }
        
        class HotKeywordsAdapter extends ArrayAdapter<HotKeyword> {
        	private Context context;
            public HotKeywordsAdapter(android.content.Context context, int textViewResourceId, List<HotKeyword> objects) {
            	super(context, textViewResourceId, objects);
            	this.context = context;
            }
            
        	@Override public View getView(int position, View convertView, ViewGroup parent) {
        	    if (convertView == null)
        	    {
        	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	        convertView = inflater.inflate(R.layout.fragment_listrow_hot_keyword, null);
        	    }

        	    HotKeyword data = getItem(position);  
                if(null != data )
                {
            	    TextView text = (TextView)convertView.findViewById(R.id.text_keyword);
	                text.setText(data.getKeyword()); 
	                
	                ImageView icon = (ImageView) convertView.findViewById(R.id.image_icon);
	                switch(data.getType())
	                {
	                case HotKeyword.TYPE_HISTORY:
	                	icon.setImageResource(R.drawable.ic_action_time);
	                	break;
	                case HotKeyword.TYPE_HOT:
	                	icon.setImageResource(R.drawable.ic_action_cloud);
	                	break;
	                case HotKeyword.TYPE_SUGGESTION:
	                	icon.setImageResource(R.drawable.ic_action_about);
	                	break;
	                case HotKeyword.TYPE_TRANSLATION:
	                	icon.setImageResource(R.drawable.ic_action_web_site);
	                	break;
	                }
                }

        	    return convertView;
        	}
        }
        
        class HotKeywordsSuggestionsAsyncTask extends AsyncTask<String, String, Void> {
        	InputStream inputStream = null;
            String result = ""; 
            String kww = "";
            
            @Override
            protected Void doInBackground(String... params) {
            	kww = params[0];
            	
                ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();

                try {
                    HttpClient httpClient = new DefaultHttpClient();

                    HttpPost httpPost = new HttpPost(LibraryAPI.buildSuggestions(kww));
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

                    String line = null;
                    while ((line = bReader.readLine()) != null) {
                        sBuilder.append(line + "\n");
                    }

                    inputStream.close();
                    result = sBuilder.toString();
                    
                } catch (Exception e) {
                    Log.e("StringBuilding & BufferedReader", "Error converting result " + e.toString());
                }
				return null;
            }

            
            protected void onPostExecute(Void v) {
            	if (isCancelled()) return;
            	
            	List<HotKeyword> list = new ArrayList<HotKeyword>();
            	for(HotKeyword kw : kwlist)
            	{
            		if (kw.type == HotKeyword.TYPE_HISTORY)
            		{
            			if (kw.keyword.contains(kww) && !kw.equals(kww))
            				addKw(list, kw);
            		}
            	}
            	
				try {
					JSONObject obj = new JSONObject(result);
					JSONArray arr;
					
					arr = obj.optJSONArray("suggestions");
					if (arr != null) {
						for(int i = 0; i < arr.length(); i++){
							String keyword = arr.getString(i);
							if (!keyword.equals(kww))
								addKw(list, new HotKeyword(keyword, 0, HotKeyword.TYPE_SUGGESTION));
				        }
					}
					
					arr = obj.optJSONArray("translations");
					if (arr != null) {
						for(int i = 0; i < arr.length(); i++){
				            String keyword = arr.getString(i);
				            if (!keyword.equals(kww))
				            	addKw(list, new HotKeyword(keyword, 0, HotKeyword.TYPE_TRANSLATION));
				        }
					}
					
				} catch (JSONException e) {
				}

				for(HotKeyword kw : kwlist)
            	{
            		if (kw.type == HotKeyword.TYPE_HOT)
            		{
            			if (kw.keyword.contains(kww))
            				addKw(list, kw);
            		}
            	}
				
				HotKeywordsAdapter ada = new HotKeywordsAdapter(getContextOfApplication(), 0, list);
				keyword.setAdapter(ada);
				ada.notifyDataSetChanged();
            }

        } 
        
        class HotKeywordsAsyncTask extends AsyncTask<String, String, Void> {
        	InputStream inputStream = null;
            String result = ""; 
            
            @Override
            protected Void doInBackground(String... params) {
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContextOfApplication());
            	String cached = prefs.getString(PREF_TOP_KEYWORDS, "[]");
            	Time cachedAt = new Time();
            	cachedAt.set(prefs.getLong(PREF_TOP_KEYWORDS_CACHED, 0));
            	Time now = new Time();
            	now.setToNow();
            	if (!cached.equals("[]") && (now.toMillis(true) - cachedAt.toMillis(true)) < 3600000)
            	{
            		result = cached;
            	}
            	else
            	{
                    ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();

                    try {
                        HttpClient httpClient = new DefaultHttpClient();

                        HttpPost httpPost = new HttpPost(LibraryAPI.TopKeywords);
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

                        String line = null;
                        while ((line = bReader.readLine()) != null) {
                            sBuilder.append(line + "\n");
                        }

                        inputStream.close();
                        result = sBuilder.toString();
                        
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(PREF_TOP_KEYWORDS_CACHED, now.toMillis(true));
                        editor.putString(PREF_TOP_KEYWORDS, result);
                        editor.commit();

                    } catch (Exception e) {
                        Log.e("StringBuilding & BufferedReader", "Error converting result " + e.toString());
                    }
            	}
				return null;
            }

            protected void onPostExecute(Void v) {
                JSONArray jArray;
				try {
					jArray = new JSONArray(result);
					for(int i = 0; i < jArray.length(); i++){
			            String keyword = jArray.getJSONObject(i).getString("keyword");
			            int count = jArray.getJSONObject(i).getInt("count");
			            addKw(kwlist, new HotKeyword(keyword, count, HotKeyword.TYPE_HOT));
			        }
					adapter.notifyDataSetChanged();
				} catch (JSONException e) {
				}
                
            }

        } 
    }
}
