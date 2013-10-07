<?php
define("ENDPOINT", "http://huiwen.ujs.edu.cn:8080");
//define("ENDPOINT", "http://opac.zjlib.net");
//define("ENDPOINT", "http://58.194.172.34");
//define("ENDPOINT", "http://lib.mad9.tv");
define("ENDPOINT_TOP_KEYWORDS", ENDPOINT . "/opac/top100.php");
define("ENDPOINT_SEARCH", ENDPOINT . "/opac/openlink.php");
define("ENDPOINT_DICT", ENDPOINT . "/opac/ajax_dict.php");

define("MEMCACHE_PREFIX", "[orzfly.com@ujslibrary][" . sha1(ENDPOINT) . "]");
$_memcache = memcache_pconnect('localhost', 11211) or NULL;

function CacheGet($key)
{
	global $_memcache;
	if (!$_memcache) return NULL;
	$v = memcache_get($_memcache,MEMCACHE_PREFIX.$key);
	if ($v)
	{
		$v = unserialize($v);
		if (time() < $v[0])
			return $v[1];
		else
			CacheDelete($key);
	}
	return NULL;
}

function CacheSet($key, $value, $expired)
{
	global $_memcache;
	if (!$_memcache) return NULL;
	$v = array(
		strtotime($expired),
		$value,
	);
    return memcache_set($_memcache,MEMCACHE_PREFIX.$key,serialize($v));
}

function CacheDelete($key)
{
	global $_memcache;
	if (!$_memcache) return NULL;
	return memcache_delete($_memcache, MEMCACHE_PREFIX.$key);
}

function nocached($key, $func, $expired = "+1 hour")
{
	$v = $func();
	return $v;
}function cached($key, $func, $expired = "+1 hour")
{
	$v = $func();
	return $v;
}
/*
function cached($key, $func, $expired = "+1 hour")
{
	if ($result = CacheGet($key))
		return $result;
	
	$v = $func();
	CacheSet($key, $v, $expired);
	return $v;
}*/

function result($obj)
{
	Header("Content-Type: application/json");
	echo json_encode($obj);
	die();
}

function getSuggestions($param)
{
	if (!$param) return array();
	
	return cached(
		"suggestions:" . sha1($param), 
		function() use ($param) {
			try
			{
				require_once('httpful.phar');
				$url = "http://www.yidu.edu.cn/easyservice/com.cnebula.educhina.suggest.service.ISuggest?request=%7B_i_%3A0%2Cid%3A%221380708796051_3_id%22%2Cmethod%3A%22getSuggestWord_1%22%2Cparams%3A%5B%22" . urlencode($param) . "%22%5D%7D";
				$response = \Httpful\Request::get($url)
					->addHeader("Content-Type", "x-application/es:ws-json-http")
					->send();
				return(
					explode(
						"</name><name>",
						stripslashes(
							str_replace(
								array(
									'{_t_: "com.cnebula.common.remote.Response",_i_:0,result:"',
									'",id:"1380708796051_3_id"}',
									'<?xml version=\"1.0\" encoding=\"utf-8\"?><response><name>',
									'<\/name><\/response>',
								),
								'',
								gzdecode($response->body)
							)
						)
					)
				);
			}catch(Exception $e){
			}
			return array();
		},
		"+10 minutes"
	);
}


function getTranslations($param)
{
	if (!$param) return array();
	
	return cached(
		"translations:" . sha1($param), 
		function() use ($param) {
			try
			{
				$url = ENDPOINT_DICT . "?type=orzfly&q=" . urlencode($param);
				$body = file_get_contents($url);
				if (preg_match_all("/<a[^>]*? href=\"openlink\\.php\\?orzfly=[^\"]*?\">([^<]*?)<\\/a>/", $body, $matches, PREG_SET_ORDER))
				{
					return array_map(function($in) {
						return str_replace(array(" ","\t","\r","\n"),"",html_entity_decode($in[1]));
					}, $matches);
				}
			}catch(Exception $e){
			}
			return array();
		},
		"+10 minutes"
	);
}
