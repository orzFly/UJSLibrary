<?php
require_once('functions.php');
result(
	cached(
		"top_keywords", 
		function() {
			$result = array();
			$body = file_get_contents(ENDPOINT_TOP_KEYWORDS);
			if (preg_match_all("/<a class=\"blue\" href=\"openlink\\.php\\?title=[^\"]*\" >(.*?) \\((\\d+)\\)<\\/a>/", $body, $matches, PREG_SET_ORDER))
			{
				$result = array_map(function($in) {
					return array(
						"keyword" => html_entity_decode($in[1]),
						"count" => intval($in[2]),
					);
				}, $matches);
			}
			return $result;
		}
	)
);
