<?php
$m = new Mongo("127.0.0.1");
$coll = $m->selectCollection("av-wx", "zipcodes");
$coll->remove();
$coll->ensureIndex(array("loc" => '2dsphere'));

$stdin = fopen("php://stdin", 'r');

while (($line = fgets($stdin, 8192)) !== false) {
  $chunks = explode("\t", $line);
  $record = array(
    '_id' => trim($chunks[1]),
    'loc' => array(floatval($chunks[10]), floatval($chunks[9]))
  );
  $coll->save($record);
}

