package com.orzfly.ujslibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import cn.trinea.android.common.service.impl.ImageCache;
import cn.trinea.android.common.service.impl.ImageCache.OnImageCallbackListener;

import com.spaceprogram.kittycache.KittyCache;

public final class LibraryAPI {
	public final static KittyCache<String, String> SuggestionsCache;
	public final static KittyCache<String, BookResult> BookResultCache;
	private static ImageCache IconCache;
	static {
		SuggestionsCache = new KittyCache<String, String>(50);
		BookResultCache = new KittyCache<String, BookResult>(50);
	}
	
	public final static String TopKeywords = "http://ujslibrary.orzfly.com/api/top_keywords.php";
	public final static String Suggestions = "http://ujslibrary.orzfly.com/api/suggestions.php";
	public final static String Search = "http://ujslibrary.orzfly.com/api/search.php";
	public final static String Book = "http://ujslibrary.orzfly.com/api/book.php";

	public static ImageCache getIconCache()
	{
		if (IconCache == null)
		{
			IconCache = new ImageCache();
		    IconCache.setOnImageCallbackListener(new OnImageCallbackListener() {
		        private static final long serialVersionUID = 1L;
		 
		        @Override
		        public void onImageLoaded(String imageUrl, Drawable imageDrawable, View view, boolean isInCache) {
		            if (view != null && imageDrawable != null) {
		                ImageView imageView = (ImageView)view;
		                imageView.setImageDrawable(imageDrawable);
		            }
		        }
		    });	
		}
		return IconCache;
	}
	
    public static class HotKeyword {
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
		public String raw;
		public String marc_no;
		public BookStatusResult[] status;
		public BookDoubanResult douban;
		public BookMARCResult marc;
		public BookTrendResult trend;
		
		public static class BookStatusGroup  implements Comparable<BookStatusGroup>{
        	public List<LibraryAPI.BookResult.BookStatusResult> Children = new ArrayList<LibraryAPI.BookResult.BookStatusResult>();
        	public String callno;
			public String year;
			public String library;
			public String location;
			public int count;
			public int count_lendable;
			
			public String getHTML() {
				StringBuilder sb = new StringBuilder();
				sb.append(this.count_lendable > 0 ? "<font color=\"green\">" : "<font color=\"red\">" );
                sb.append(StringEscapeUtils.escapeHtml4(String.valueOf(this.count_lendable)));
                sb.append("/" );
                sb.append(StringEscapeUtils.escapeHtml4(String.valueOf(this.count)));
                sb.append("</font> " );
                sb.append("<strong>" + StringEscapeUtils.escapeHtml4(this.callno) + "</strong>");
                sb.append("<br />" );
                sb.append(StringEscapeUtils.escapeHtml4(this.library));
                sb.append(", ");
                sb.append(StringEscapeUtils.escapeHtml4(this.location));
                sb.append(", ");
                sb.append(StringEscapeUtils.escapeHtml4(this.year));
				return sb.toString();
			}

			@Override
			public int compareTo(BookStatusGroup that) {
				return this.getCompareString().compareToIgnoreCase(that.getCompareString());
			}
			
			public String getCompareString()
			{
				return (this.library != null ? this.library : "") +
						(this.location != null ? this.location : "") +
						(this.year != null ? this.year : "") + 
						(this.callno != null ? this.callno : ""); 
			}
        }
		
		public static class BookStatusResult implements Comparable<BookStatusResult> {
			public String barcode;
			public String status;
			
			public String callno;
			public String year;
			public String library;
			public String location;
			public Boolean available;
			
			public String getHTML() {
				StringBuilder sb = new StringBuilder();
				sb.append(StringEscapeUtils.escapeHtml4(this.barcode));
				sb.append(": " );
				sb.append(this.available ? "<font color=\"green\">" : "<font color=\"red\">" );
                sb.append(StringEscapeUtils.escapeHtml4(String.valueOf(this.status)));
                sb.append("</font>" );
                return sb.toString();
			}

			@Override
			public int compareTo(BookStatusResult that) {
				return (this.status != null ? this.status : "").compareToIgnoreCase(that.status != null ? that.status : "");
			}
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
			public String[] title;
			public String[] publisher_location;
			public String[] publisher;
			public String[] publisher_date;
			public String[] author;
			public String[] unknown;
			public BookMARCTitleAlternativesResult[] title_alternatives;
			public BookMARCISBNResult[] isbn;
			
			public static class BookMARCTitleAlternativesResult {
				public Boolean meaningless;
				public String name;
				public String[] name_other;
				public String[] album_id;
				public String[] album_name;
				public String number;
				public String other;
				public String language;
			}
			public static class BookMARCISBNResult {
				public String id;
				public String limited;
				public String price;
				public String[] wrong;
			}
		}

		public String getTitle() {
			return this.marc.title != null ? StringUtils.join(this.marc.title) : (
						this.douban != null && this.douban.title != null ? this.douban.title : this.marc_no 
					);
		}
		
		private String cleanAndJoinString(String det, String... T)
		{
		    List<String> list = new ArrayList<String>(Arrays.asList(T));
		    list.removeAll(Collections.singleton(null));
			return StringUtils.join(list, det);
		}
		
		public String getSummaryHTML() {
			StringBuilder sb = new StringBuilder();
			appendHTMLField(sb, "正题名", this.marc.title);
			if (this.marc.title_alternatives != null)
			{
				for(BookMARCResult.BookMARCTitleAlternativesResult i : this.marc.title_alternatives)
				{
					appendHTMLField(sb, "并列名", cleanAndJoinString(", ", i.name, StringUtils.join(i.name_other, ", ")));
				}
			}
			appendHTMLField(sb, "责任者", this.marc.author);
			appendHTMLField(sb, "出版者", 
					(this.marc.publisher_location != null ? StringUtils.join(this.marc.publisher_location) + ":" : "") +
					(this.marc.publisher != null ? StringUtils.join(this.marc.publisher) : "") + 
					(this.marc.publisher_date != null ? ", " + StringUtils.join(this.marc.publisher_date) : "")
				);
			if (this.marc.isbn != null)
			{
				for(BookMARCResult.BookMARCISBNResult i : this.marc.isbn)
				{
					appendHTMLField(sb, "ISBN", cleanAndJoinString(", ", i.id, i.limited, i.price, StringUtils.join(i.wrong)));
				}
			}
			return sb.toString();
		}
	
		public String getDetailsHTML() {
			StringBuilder sb = new StringBuilder();
			if (this.marc.unknown != null)
			{
				sb.append("<strong>Unknown</strong>: <br/>");
				for(String item: this.marc.unknown)
				{
					appendHTMLText(sb, item);
					appendHTMLBR(sb);
				}
			}
			if (this.douban != null)
			{
				sb.append("<br/><br/><small>以下数据来自于");
				if (this.douban.alt != null)
				{
					sb.append("<a href=\"");
					appendHTMLText(sb, this.douban.alt);
					sb.append("\">豆瓣图书</a>");
				}
				else
				{
					sb.append("豆瓣图书");
				}
				sb.append("，信息可能和馆藏书籍存在差异。</small><br/>");
				appendHTMLField(sb, "书名", this.douban.title);
				appendHTMLField(sb, "子名", this.douban.subtitle);
				appendHTMLField(sb, "原名", this.douban.origin_title);
				appendHTMLField(sb, "又名", this.douban.alt_title);
				appendHTMLField(sb, "作者", StringUtils.join(this.douban.author, ", "));
				appendHTMLField(sb, "译者", StringUtils.join(this.douban.translator, ", "));
				appendHTMLField(sb, "出版", this.douban.publisher);
				appendHTMLField(sb, "日期", this.douban.pubdate);
				appendHTMLField(sb, "装订", this.douban.binding);
				appendHTMLField(sb, "页数", this.douban.pages);
				appendHTMLField(sb, "价格", this.douban.price);
				appendHTMLField(sb, "ISBN10", this.douban.isbn10);
				appendHTMLField(sb, "ISBN13", this.douban.isbn13);
				if (this.douban.tags != null)
				{
					List<String> list = new ArrayList<String>();
					for(BookResult.BookDoubanResult.BookDoubanTagResult tag : this.douban.tags)
						list.add(StringEscapeUtils.escapeHtml4(tag.name) + "<small>" + String.valueOf(tag.count) + "</small>");
					appendHTMLFieldRaw(sb, "标签", StringUtils.join(list, ", "));
				}
				if (this.douban.rating != null)
					appendHTMLField(sb, "评分", 
							String.valueOf(this.douban.rating.average) + 
							" (" + 
							String.valueOf(this.douban.rating.numRaters) + 
							" 人, " + 
							String.valueOf(this.douban.rating.min) + 
							"~" + 
							String.valueOf(this.douban.rating.max) + 
							")"
							);
				appendHTMLFieldLong(sb, "内容介绍", this.douban.summary);
				appendHTMLFieldLong(sb, "作者介绍", this.douban.author_intro);
				appendHTMLFieldLong(sb, "目录", this.douban.catelog);
			}
			if (this.raw != null)
			{
				appendHTMLBR(sb);
				sb.append("<strong>Raw</strong>: <br/>");
				StringBuffer resultString = new StringBuffer();
            	Pattern regex = Pattern.compile("\\\\u([0-9a-f]{4})");
            	Matcher regexMatcher = regex.matcher(this.raw);
            	while (regexMatcher.find()) {
            		regexMatcher.appendReplacement(resultString, String.valueOf((char)Integer.parseInt(regexMatcher.group(1), 16)));
            	}
            	regexMatcher.appendTail(resultString);
            	appendHTMLText(sb, resultString.toString());
			}
			return sb.toString();
		}
		
		private void appendHTMLText(StringBuilder sb, String text)
		{
			sb.append(StringEscapeUtils.escapeHtml4(text));
		}
		
		private void appendHTMLBR(StringBuilder sb)
		{
			sb.append("<br />");
		}
		
		private void appendHTMLFieldRaw(StringBuilder sb, String key, String value)
		{
			if (value == null) return;
			if (value.length() == 0) return;
			sb.append("<strong>" + StringEscapeUtils.escapeHtml4(key) + "</strong>: ");
			sb.append(value + "<br />");
		}
		
		private void appendHTMLField(StringBuilder sb, String key, String value)
		{
			if (value == null) return;
			if (value.length() == 0) return;
			sb.append("<strong>" + StringEscapeUtils.escapeHtml4(key) + "</strong>: ");
			sb.append(StringEscapeUtils.escapeHtml4(value) + "<br />");
		}
		
		private void appendHTMLFieldLong(StringBuilder sb, String key, String value)
		{
			if (value == null) return;
			if (value.length() == 0) return;
			sb.append("<strong>" + StringEscapeUtils.escapeHtml4(key) + "</strong>: <br/>");
			sb.append(StringEscapeUtils.escapeHtml4(value) + "<br />");
		}
		
		private void appendHTMLField(StringBuilder sb, String key, String[] value)
		{
			if (value == null) return;
			if (value.length == 0) return;
			sb.append("<strong>" + StringEscapeUtils.escapeHtml4(key) + "</strong>: ");
			sb.append(StringEscapeUtils.escapeHtml4(StringUtils.join(value)) + "<br />");
		}
	}

}
