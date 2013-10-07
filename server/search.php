<?php
require_once('functions.php');
require_once('nokogiri.php');
function mfield($k)
{
	$r = null;
	if (isset($_REQUEST[$k]))
	{
		if (is_string($_REQUEST[$k]))
			$r = array($_REQUEST[$k]);
		else
			$r = $_REQUEST[$k];
	}
	else
	{
		$r = array();
	}
	return implode($r, "HUIWEN_SEARCH_TWO");
}
function sfield($k, $def)
{
	if (isset($_REQUEST[$k]))
	{
		$r = $_REQUEST[$k];
	}
	else
	{
		$r = $def;
	}
	return $r;
}
$result = array();
$param = http_build_query(array(
	"title" => mfield("title"),
	"author" => mfield("author"),
	"keyword" => mfield("keyword"),
	"isbn" => mfield("isbn"),
	"asordno" => mfield("asordno"),
	"coden" => mfield("coden"),
	"callno" => mfield("callno"),
	"publisher" => mfield("publisher"),
	"series" => mfield("series"),
	"tpinyin" => mfield("tpinyin"),
	"apinyin" => mfield("apinyin"),
	
	"location" => sfield("location",null),
	"subject" => sfield("subject",null),
	"doctype" => sfield("doctype", "ALL"),
	"showloca" => sfield("page", 1) > 1 ? 0 : 1,
	//"showsubject" => 1,
	"page" => sfield("page", 1),
	"onlylendable" => sfield("onlylendable", "no"),
	"showmode" => "list",
	"displaypg" => sfield("displaypg", 20),
	"match_flag" => sfield("matchflag", "forward"), //前方一致forward/完全匹配full/任意匹配any
	"lang_code" => sfield("langcode", "ALL"),
));
result(
	cached(
		"search:" . sha1($param), 
		function() use ($param) {
			$body = file_get_contents(ENDPOINT_SEARCH . "?" . $param);
			$noko = new nokogiri($body);
			$result = $noko->get("div.list_books")->toArray();
			$result = @array_map(function($in) {
				$res = array(
					"id" => 0,
					"title" => $in['h3'][0]['a'][0]['#text'],
					"marc_no" => str_replace("item.php?marc_no=", "", $in['h3'][0]['a'][0]['href']),
					"type" => $in['h3'][0]['span'][0]['#text'],
					"call_no" => trim($in['h3'][0]['#text'][0]),
					"author" => str_replace("\r", "", str_replace("\n", "", trim($in['p'][0]['#text'][1]))),
					"publisher" => str_replace("\r", "", str_replace("\n", "", trim($in['p'][0]['#text'][2]))),
					"year" => 0,
					"count" => intval(trim($in['p'][0]['span'][0]['#text'][0])),
					"count_lendable" => intval(trim($in['p'][0]['span'][0]['#text'][1])),
				);
				if (preg_match("/^(\\d+)\\.(.*?)$/", $res['title'], $match))
				{
					$res['title'] = $match[2];
					$res['id'] = intval($match[1]);
				}
				if (preg_match("/^(.*?)(\\xc2\\xa0|\\s|\\r|\\n)*(\\d\\d\\d\\d)$/", $res['publisher'], $match))
				{
					$res['publisher'] = trim($match[1]);
					$res['year'] = intval($match[3]);
				}
				if (preg_match("/^(.*?), (\\d\\d\\d\\d)\.$/", $res['publisher'], $match))
				{
					$res['publisher'] = trim($match[1]);
					$res['year'] = intval($match[2]);
				}
				return $res;
			}, $result);
			$all = array(
				"pages" => 1,
				"displaypg" => sfield("displaypg", 20),
				"count" => count($result),
				"books" => $result,
				"suggestions" => array(),
				"translations" => array(),
				"categories" => array(), //done
				"doctypes" => array(), //done
				"locations" => array(), //done
				"subjects" => array(), //done
			);
			if (preg_match("{<option value='(\\d*?)'>\\1</option>(\n|\r|\s)*</select>}", $body, $match))
			{
				$all['pages'] = intval($match[1]);
			}
			if (preg_match("{<strong class=\"red\">(\\d*?)</strong>}", $body, $match))
			{
				$all['count'] = intval($match[1]);
			}
			if (sfield("page", 1) <= 1)
			{
				$all['suggestions'] = getSuggestions(trim(str_replace("HUIWEN_SEARCH_TWO","",mfield("title")." ".mfield("author"))));
				$all['translations'] = getTranslations(trim(str_replace("HUIWEN_SEARCH_TWO","",mfield("title")." ".mfield("author"))));
				if (preg_match_all("/<dd>&middot;<a href=\"\\?[^\"]*?&callno=([^\"]*?)\">([^<]*?)<\\/a>\\((\d*?)\\)<\\/dd>/", $body, $matches, PREG_SET_ORDER))
				{
					$all['categories'] = array_map(function($in) {
						return array(
							"callno" => $in[1],
							"description" => html_entity_decode($in[2]),
							"count" => intval($in[3]),
						);
					}, $matches);
				}
				if (preg_match_all("/<dd>&middot;<a href=\"\\?[^\"]*?&doctype=(\\d*?)\">([^<]*?)<\\/a>\\((\d*?)\\)<\\/dd>/", $body, $matches, PREG_SET_ORDER))
				{
					$all['doctypes'] = array_map(function($in) {
						return array(
							"doctype" => $in[1],
							"description" => html_entity_decode($in[2]),
							"count" => intval($in[3]),
						);
					}, $matches);
				}
				if (preg_match_all("/<dd>&middot;<a href=\"\\?[^\"]*?&location=(\\d*?)\">([^<]*?)<\\/a>\\((\d*?)\\)<\\/dd>/", $body, $matches, PREG_SET_ORDER))
				{
					$all['locations'] = array_map(function($in) {
						return array(
							"location" => $in[1],
							"description" => html_entity_decode($in[2]),
							"count" => intval($in[3]),
						);
					}, $matches);
				}
				if (preg_match_all("/<dd>&middot;<a href=\"\\?[^\"]*?&subject=([^\"]*?)\">([^<]*?)<\\/a>\\((\d*?)\\)<\\/dd>/", $body, $matches, PREG_SET_ORDER))
				{
					$all['subjects'] = array_map(function($in) {
						return array(
							"subject" => html_entity_decode($in[2]),
							"count" => intval($in[3]),
						);
					}, $matches);
				}
			}
			return $all;
		},
		"+10 minutes"
	)
);
