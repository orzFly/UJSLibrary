<?php
require_once('functions.php');

result(
	array(
		"suggestions" => getSuggestions($_REQUEST['keyword']),
		"translations" => getTranslations($_REQUEST['keyword'])
	)
);
