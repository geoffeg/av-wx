<?php
$mongo = new Mongo("localhost");
$coll = $mongo->selectCollection("av-wx", "airports");
$coll->drop();

$fh = fopen("php://stdin", "r");
while (($data = fgetcsv($fh)) !== false) {
	$insert['_id'] = $data[1];
	$insert['name'] = ucwords(strtolower($data[2]));
  $coll->save($insert);
}
fclose($fh);
