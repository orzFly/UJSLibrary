package com.orzfly.ujslibrary;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public final class LibraryAPI {
	public final static String TopKeywords = "http://ujslibrary.orzfly.com/api/top_keywords.php";
	public final static String Suggestions = "http://ujslibrary.orzfly.com/api/suggestions.php";
	public final static String Search = "http://ujslibrary.orzfly.com/api/search.php";
	public final static String Book = "http://ujslibrary.orzfly.com/api/book.php";
	
	public static String buildSuggestions(String keyword)
	{
	    List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("keyword", keyword));

	    String paramString = URLEncodedUtils.format(params, "utf-8");
	    
	    return Suggestions + "?" + paramString;
	}
	
	public static String buildSearchURL(SearchParameter p)
	{
	    List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("page", String.valueOf(p.page)));
        params.add(new BasicNameValuePair("displaypg", String.valueOf(p.displaypg)));
        params.add(new BasicNameValuePair("title", p.title));

	    String paramString = URLEncodedUtils.format(params, "utf-8");
	    
	    return Search + "?" + paramString;
	}

	public static String buildBookURL(String marc_no) {
		List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("marc_no", marc_no));

	    String paramString = URLEncodedUtils.format(params, "utf-8");
	    
	    return Book + "?" + paramString;
	}
	
	public static class SearchParameter {
		public int page = 1;
		public int displaypg = 20;
		public String match_flag;
		public String title;
	}
	
	public static class SearchResult {
		public int pages;
		public int count;
		public int displaypg;
		public SearchBookResult[] books;
		public String[] suggestions;
		public String[] translations;
		public SearchCategoryResult[] categories;
		public SearchDocTypeResult[] doctypes;
		public SearchLocationResult[] locations;
		public SearchSubjectResult[] subjects;
		
		public static class SearchBookResult {
			public int id;
			public String title;
			public String marc_no;
			public String type;
			public String call_no;
			public String author;
			public String publisher;
			public int year;
			public int count;
			public int count_lendable;
		}
		
		public static class SearchCategoryResult {
			public String callno;
			public String description;
			public int count;
		}
		
		public static class SearchDocTypeResult {
			public String doctype;
			public String description;
			public int count;
		}
		
		public static class SearchLocationResult {
			public String location;
			public String description;
			public int count;
		}
		
		public static class SearchSubjectResult {
			public String subject;
			public int count;
		}
	}
	
	public static class BookResult {
		public BookStatusResult[] status;
		public BookDoubanResult douban;
		public BookMARCResult marc;
		public BookTrendResult trend;
		
		public static class BookStatusResult {
			public String callno;
			public String barcode;
			public String year;
			public String library;
			public String location;
			public String status;
			public Boolean available;
		}
		
		public static class BookTrendResult {
			public String[] dates;
			public int[] values;
		}
		
		public static class BookDoubanResult {
			public String id;
			public String isbn10;
			public String isbn13;
			public String title;
			public String origin_title;
			public String alt_title;
			public String subtitle;
			public String url;
			public String alt;
			public String image;
			public BookDoubanImagesResult images;
			public String[] author;
			public String[] translator;
			public String publisher;
			public String pubdate;
			public BookDoubanRatingResult rating;
			public BookDoubanTagResult[] tags;
			public String binding;
			public String price;
			public String pages;
			public String author_intro;
			public String summary;
			public String catelog;
			
			public static class BookDoubanImagesResult {
				public String small;
				public String large;
				public String medium;
			}
			
			public static class BookDoubanRatingResult {
				public int max;
				public int numRaters;
				public String average;
				public int min;
			}
			
			public static class BookDoubanTagResult {
				public int count;
				public String name;
			}
		}
		
		public static class BookMARCResult {
			
		}
	}

}
