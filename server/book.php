<?php
require_once('functions.php');
require_once('nokogiri.php');

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
mb_internal_encoding("UTF-8");
function parsecnmarc_sub($rest)
{
	$r = array();
	
	$r["ctl1"] = mb_substr($rest, 0, 1);
	if ($r["ctl1"] == "_") $r["ctl1"] = "";
	
	$r["ctl2"] = mb_substr($rest, 1, 1);
	if ($r["ctl2"] == "_") $r["ctl2"] = "";
	
	$sub = explode(" |", $rest);
	array_shift($sub);
	$lastkey = "orig";
	while(count($sub) > 0)
	{
		$s = array_shift($sub);
		if (mb_strlen($s) > 0 && mb_substr($s, 1, 1) == " " && preg_match('/^[a-zA-Z0-9]+$/', mb_substr($s, 0, 1)))
		{
			$key = mb_substr($s, 0, 1);
			$new = mb_substr($s, 2);
			if (isset($r[$key]))
			{
				if (is_array($r[$key]))
				{
					$r[$key][] = $new;
				}
				else
				{
					$r[$key] = array($r[$key], $new);
				}
			}
			else
			{
				$r[$key] = $new;
			}
			$lastkey = $key;
		}
		else
		{
			if (isset($r[$lastkey]))
			{
				if (is_array($r[$lastkey]))
				{
					$r[$lastkey][count($r[$lastkey]) - 1] .= ($lastkey == "orig" ? "|" : "") . $s;
				}
				else
				{
					$r[$lastkey] .= ($lastkey == "orig" ? "|" : "") . $s;
				}
			}
			else
			{
				$r[$lastkey] = ($lastkey == "orig" ? "|" : "") . $s;
			}
		}
	}
	
	return $r;
}

function parsecnmarc_array($obj)
{
	if (is_null($obj)) return NULL;
	if (is_array($obj)) return $obj;
	return array($obj);
}

function parsecnmarc($body)
{
	$err = error_reporting();
	error_reporting($err & ~E_WARNING & ~E_NOTICE);
	$r = array();
	$lines = explode("\n", $body);
	foreach($lines as $line)
	{
		$id = mb_substr($line, 0, 3);
		$rest = mb_substr($line, 4);
		switch($id)
		{
			case "000":
				// mb_substr($rest, 0, 5); // 记录长度，5个十进制数。右边对齐，不足5个数字时用零补齐。
				$r["record_status"] = mb_substr($rest, 5, 1);
				$r["record_type"] = mb_substr($rest, 6, 1);
				$r["record_level"] = mb_substr($rest, 7, 1);
				$r["record_level"] = mb_substr($rest, 8, 1);
				break;
			case "001":
				$r["marc_no"] = $rest;
				break;
			case "005":
				$r["record_updated"] = mktime(
					mb_substr($rest, 8, 2), //H
					mb_substr($rest, 10, 2), //M
					mb_substr($rest, 12, 2), //S
					mb_substr($rest, 4, 2), //M
					mb_substr($rest, 6, 2), //D
					mb_substr($rest, 0, 4)  //Y
				);
				break;
			default:
				$sub = parsecnmarc_sub($rest);
				switch($id)
				{
					case "100": // 什么鸡吧反正老子看不懂……
					case "105":
					case "920":
						break;
					case "010":
						$r['isbn'][] = array(
							'id' => $sub['a'],
							'limited' => $sub['b'],
							'price' => $sub['d'],
							'wrong' => parsecnmarc_array($sub['z']),
						);
						break;
					case "011":
						$r['issn'][] = array(
							'id' => $sub['a'],
							'limited' => $sub['b'],
							'price' => $sub['d'],
							'wrong' => parsecnmarc_array($sub['z']),
						);
						break;
					case "016":
						$r['isrc'][] = array(
							'id' => $sub['a'],
							'type' => $sub['b'],
						);
						break;
					case "092":
						$r['orderno'][] = array(
							'country' => $sub['a'],
							'inner' => $sub['b'],
							'outer' => $sub['c'],
							'wrong' => parsecnmarc_array($sub['z']),
						);
						break;
					case "099":
						$r['othercode'][] = $sub['a'];
						break;
					case "101":
						$r['is_translated'] = $sub['ctl1'];
						$r['language_body'] = parsecnmarc_array($sub['a']);
						$r['language_media'] = parsecnmarc_array($sub['b']);
						$r['language_orig'] = parsecnmarc_array($sub['c']);
						$r['language_excerpt'] = parsecnmarc_array($sub['d']);
						$r['language_index'] = parsecnmarc_array($sub['e']);
						$r['language_front'] = parsecnmarc_array($sub['f']);
						$r['language_title'] = $sub['g'];
						$r['language_lyric'] = parsecnmarc_array($sub['h']);
						$r['language_attachment'] = parsecnmarc_array($sub['i']);
						$r['language_subtitle'] = parsecnmarc_array($sub['j']);
						break;
					case "102":
						$r['publisher_country'] = $sub['a'];
						$r['publisher_areacode'] = $sub['b'];
						break;
					case "106":
						$r['physical_type'] = $sub['a'];
						break;
					case "200":
						$r['title_meaningless'] = !(bool)($sub['ctl1'] ?: 1);
						$r['title'] = parsecnmarc_array($sub['a']);
						$r['general_material_designation'] = parsecnmarc_array($sub['b']);
						$r['title_alternative_author'] = parsecnmarc_array($sub['c']);
						$r['title_alternative'] = parsecnmarc_array($sub['d']);
						$r['subtitle'] = parsecnmarc_array($sub['e']);
						$r['author'] = parsecnmarc_array($sub['f']);
						$r['author_other'] = parsecnmarc_array($sub['g']);
						$r['album_id'] = parsecnmarc_array($sub['h']);
						$r['album_name'] = parsecnmarc_array($sub['i']);
						$r['language_title_alternative'] = $sub['z'];
						$r['volume'] = parsecnmarc_array($sub['v']);
						$r['title_pinyin'] = parsecnmarc_array($sub['A']);
						break;
					case "205":
						$r['editon'][] = array(
							"title" => $sub['a'],
							"subtitle" => parsecnmarc_array($sub['b']),
							"title_alternative" => parsecnmarc_array($sub['d']),
							"author" => parsecnmarc_array($sub['f']),
							"author_other" => parsecnmarc_array($sub['g']),
						);
						break;
					case "210":
						$r['publisher_location'] = parsecnmarc_array($sub['a']);
						$r['publisher_address'] = parsecnmarc_array($sub['b']);
						$r['publisher'] = parsecnmarc_array($sub['c']);
						$r['publisher_date'] = parsecnmarc_array($sub['d']);
						$r['creator_location'] = parsecnmarc_array($sub['e']);
						$r['creator_address'] = parsecnmarc_array($sub['f']);
						$r['creator'] = parsecnmarc_array($sub['g']);
						$r['creator_date'] = parsecnmarc_array($sub['h']);
						break;
					case "215":
						$r['physical_description'][] = array(
							"page" => parsecnmarc_array($sub['a']),
							"detail" => $sub['b'],
							"size" => parsecnmarc_array($sub['c']),
							"attachment" => parsecnmarc_array($sub['d'])
						);
						break;
					case "225":
						$r['series'][] = array(
							"type" => $sub['ctl1'],
							"title" => $sub['a'],
							"title_alternative" => parsecnmarc_array($sub['d']),
							"subtitle" => parsecnmarc_array($sub['e']),
							"author" => parsecnmarc_array($sub['f']),
							"album_id" => parsecnmarc_array($sub['h']),
							"album_name" => parsecnmarc_array($sub['i']),
							"language" => parsecnmarc_array($sub['z']),
							"volume" => parsecnmarc_array($sub['v']),
							'issn' => parsecnmarc_array($sub['x']),
						);
						break;
					case "300": //一般性附注
						$r['memo'][] = $sub['a'];
						break;
					case "301": //标识号附注
						$r['memo_id'][] = $sub['a'];
						break;
					case "302": //编码信息附注
						$r['memo_code'][] = $sub['a'];
						break;
					case "303": //标识号附注
						$r['memo_general'][] = $sub['a'];
						break;
					case "304": //题名与责任说明附注
						$r['memo_title'][] = $sub['a'];
						break;
					case "305": //版本与书目史附注
						$r['memo_version'][] = $sub['a'];
						break;
					case "306": //出版发行等附注
						$r['memo_publisher'][] = $sub['a'];
						break;
					case "307": //载体形态附注
						$r['memo_physical'][] = $sub['a'];
						break;
					case "308": //丛编附注
						$r['memo_series'][] = $sub['a'];
						break;
					case "310": //装订及获得方式附注
						$r['memo_price'][] = $sub['a'];
						break;  
					case "311": //连接字段附注
						$r['memo_linking'][] = $sub['a'];
						break;  
					case "312": //相关题名附注
						$r['memo_related_title'][] = $sub['a'];
						break;  
					case "313": //主题附注
						$r['memo_subject'][] = $sub['a'];
						break;  
					case "314": //知识责任附注
						$r['memo_copyright'][] = $sub['a'];
						break;  
					case "315": //资料(或出版物类型)特殊细节附注
						$r['memo_special'][] = $sub['a'];
						break;  
					case "320": //书目、索引附注
						$r['memo_index'][] = $sub['a'];
						break;  
					case "327": //内容附注
						$r['memo_content'][] = $sub['a'];
						break;
					case "330": //摘要
						$r['memo_excerpt'][] = $sub['a'];
						break;
					case "333": //使用对象附注
						$r['memo_user'][] = $sub['a'];
						break;
					case "510":
						$r['title_alternatives'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"name_other" => parsecnmarc_array($sub['e']),
							"album_id" => parsecnmarc_array($sub['h']),
							"album_name" => parsecnmarc_array($sub['i']),
							"number" => $sub['j'],
							"other" => $sub['n'],
							"language" => $sub['z'],
						);
						break;
					case "512":
						$r['title_front'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"name_other" => parsecnmarc_array($sub['e']),
							"pinyin" => $sub['A'],
						);
						break;
					case "513":
						$r['title_front_additional'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"name_other" => parsecnmarc_array($sub['e']),
							"album_id" => $sub['h'],
							"album_name" => $sub['i'],
							"pinyin" => $sub['A'],
						);
						break;
					case "517":
						$r['title_other'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"name_other" => parsecnmarc_array($sub['e']),
							"pinyin" => $sub['A'],
						);
						break;
					case "540": //编目员补充的附加题名
						$r['title_cataloguer'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"pinyin" => $sub['A'],
						);
						break;
					case "541": //编目员补充的翻译题名
						$r['title_cataloguer_translated'][] = array(
							"meaningless" => !(bool)($sub['ctl1'] ?: 1),
							"name" => $sub['a'],
							"name_other" => $sub['e'],
							"album_id" => $sub['h'],
							"album_name" => $sub['i'],
							"pinyin" => $sub['A'],
							"language" => $sub['z'],
						);
						break;
					case "600":
						$r['category_person'][] = array(
							"type" => $sub['ctl1'],
							"name" => $sub['a'],
							"name_other" => $sub['b'],
							"name_suffix" => parsecnmarc_array($sub['c']),
							"gen" => $sub['d'],
							"year" => $sub['f'],
							"title" => $sub['title'],
							"subtheme" => parsecnmarc_array($sub['x']),
							"area" => parsecnmarc_array($sub['y']),
							"year" => parsecnmarc_array($sub['z']),
							"systemcode" => $sub['2'],
							"regularno" => $sub['3'],
							"name_pinyin" => $sub['A'],
						);
						break;
					case "606":
						$r['category_theme'][] = array(
							"type" => $sub['ctl1'],
							"name" => $sub['a'],
							"subtheme" => parsecnmarc_array($sub['x']),
							"area" => parsecnmarc_array($sub['y']),
							"year" => parsecnmarc_array($sub['z']),
							"systemcode" => $sub['2'],
							"regularno" => $sub['3'],
							"name_pinyin" => $sub['A'],
						);
						break;
					case "660":
						$r['category_area'][] = array(
							"code" => $sub['a'],
							"inner_code" => parsecnmarc_array($sub['b']),
						);
						break;
					case "690":
						$r['category_prc'][] = array(
							"id" => $sub['a'],
							"version" => $sub['v'],
						);
						break;
					case "692":
						$r['category_prcsci'][] = array(
							"id" => $sub['a'],
							"version" => $sub['v'],
						);
						break;
					case "700":
						$r['author_details'][] = array(
							"type" => $sub['ctl2'],
							"name" => $sub['a'],
							"name_other" => $sub['b'],
							"name_suffix" => parsecnmarc_array($sub['c']),
							"gen" => $sub['d'],
							"year" => $sub['f'],
							"initial_expand" => $sub['g'],
							"organization" => $sub['p'],
							"regularno" => $sub['3'],
							"relation_code" => parsecnmarc_array($sub['4']),
							"pinyin" => $sub['A']
						);
						break;
					case "701":
						$r['author_first_details'][] = array(
							"type" => $sub['ctl2'],
							"name" => $sub['a'],
							"name_other" => $sub['b'],
							"name_suffix" => parsecnmarc_array($sub['c']),
							"gen" => $sub['d'],
							"year" => $sub['f'],
							"initial_expand" => $sub['g'],
							"organization" => $sub['p'],
							"regularno" => $sub['3'],
							"relation_code" => parsecnmarc_array($sub['4']),
							"pinyin" => $sub['A']
						);
						break;
					case "702":
						$r['author_second_details'][] = array(
							"type" => $sub['ctl2'],
							"name" => $sub['a'],
							"name_other" => $sub['b'],
							"name_suffix" => parsecnmarc_array($sub['c']),
							"gen" => $sub['d'],
							"year" => $sub['f'],
							"initial_expand" => $sub['g'],
							"organization" => $sub['p'],
							"regularno" => $sub['3'],
							"relation_code" => parsecnmarc_array($sub['4']),
							"pinyin" => $sub['A']
						);
						break;
					case "801":
						$r['record_source'][] = array(
							"type" => $sub['ctl2'],
							"country" => $sub['a'],
							"name" => $sub['b'],
							"date" => $sub['c'] ? mktime(
								0, //H
								0, //M
								0, //S
								mb_substr($sub['c'], 4, 2), //M
								mb_substr($sub['c'], 6, 2), //D
								mb_substr($sub['c'], 0, 4)  //Y
							) : NULL,
							"rule" => parsecnmarc_array($sub['g'])
						);
						break;
					case "856":
						$r['url'][] = $sub['u'];
						break;
					case "905":
						$r['callno'][] = array(
							"type" => $sub['a'],
							"code" => $sub['d']
						);
						break;
					default:
						$r['unknown'] = $line;
						break;
				}
				break;
		}
	}
	foreach($r as $k => $v)
	{
		if (is_null($v))
			unset($r[$k]);
			
		if (is_array($v) && count($v) == 0)
			unset($r[$k]);
	}
	error_reporting($err);
	return $r;
}

function getmarc($marc_no)
{
	$opts = array(
	  'http'=>array(
		'method'=>"GET",
		'header'=>"User-Agent: Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B334b Safari/531.21.102011-10-16 20:23:10\r\n"
	  )
	);
	$context = stream_context_create($opts);
	$book = @file_get_contents("http://huiwen.ujs.edu.cn:8080/opac/item.php?marc_no=" . $marc_no, false, $context);
	preg_match("/show_format_marc\\.php\\?marc_no=([0-9a-fA-F]*)/", $book, $match);
	$en_marc_no = $match[1];
	$body = @file_get_contents("http://huiwen.ujs.edu.cn:8080/opac/show_format_marc.php?marc_no=" . $en_marc_no, false, $context);
	preg_match_all("|<li>(<b>\\d\\d\\d</b>.*?)</li>|", $body, $match);
	$result = $match[1];
	return array($book, html_entity_decode(implode("\n", array_map(function($i){
		return str_replace(array("<b>", "</b>", "<STRONG>", "</STRONG>"), "", $i);
	}, $result))));
}

result(
	cached(
		"book:" . sha1(sfield("marc_no")), 
		function() {
			$content = getmarc(sfield("marc_no"));
			$book = $content[0];
			$noko = new nokogiri($book);
			$result = $noko->get("div#s_c_left div table")->toArray();
			$books = array();
			if (isset($result[0]["tr"][1]))
			{
				array_shift($result[0]["tr"]);
				$books = array_map(
					function($in) {
						return array(
							"callno" => $in["td"][0]["#text"],
							"barcode" => $in["td"][1]["#text"],
							"year" => $in["td"][2]["#text"],
							"library" => $in["td"][3]["#text"],
							"location" => $in["td"][4]["#text"],
							"status" => isset($in["td"][5]["font"]) ? $in["td"][5]['font']["#text"] : $in["td"][5]["#text"],
							"available" => isset($in["td"][5]["font"]) ? true : false
						);
					},
					$result[0]["tr"]
				);
			}
			$marc = parsecnmarc($content[1]);
			try{
				$douban = cached(
					"douban:" . sha1($marc['isbn'][0]['id']),
					function() use ($marc) {
						return json_decode(file_get_contents("https://api.douban.com/v2/book/isbn/" . $marc['isbn'][0]['id']), TRUE);
					},
					"+10 days"
				);
				if (isset($douban['code']))
					$douban = NULL;
			}
			catch(Exception $e)
			{
				$douban = NULL;
			}
			try{
				$trendjson = file_get_contents("http://huiwen.ujs.edu.cn:8080/opac/ajax_lend_trend.php?id=" . sfield("marc_no"));
				$trendjson = preg_replace('|^.*?\\{|', '{', $trendjson);
				$trend = json_decode($trendjson, TRUE);
				$trend = array(
					"dates" => $trend["x_axis"]["labels"]["labels"],
					"values" => $trend["elements"][0]["values"],
				);
			}
			catch(Exception $e)
			{
				$trend = NULL;
			}
			return array(
				"status" => $books,
				"marc" => $marc,
				"douban" => $douban,
				"trend" => $trend
			);
		},
		"+10 minutes"
	)
);