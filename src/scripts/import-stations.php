<?php
$mongo = new Mongo("localhost");
$coll = $mongo->selectCollection("av-wx", "stations");
$coll->drop();
$coll->ensureIndex(array("location" => '2dsphere'));
$coll->ensureIndex(array("icao" => '1'));


$fh = fopen("php://stdin", "r");
while (($line = fgets($fh, 1024)) !== false) {
  $icao = trim(substr($line, 20, 4));
  $iata = trim(substr($line, 26, 3));
  $lat = substr($line, 39, 6); 
  $lon = substr($line, 47, 7);
  $lat_dec = DMStoDEC(substr($lat, 0, 2), substr($lat, 3, 2), 0, substr($lat, -1, 1) );
  $lon_dec = DMStoDEC(substr($lon, 0, 3), substr($lon, 4, 2), 0, substr($lon, -1, 1) );
  $metar = trim(substr($line, 62, 1));
  $taf = trim(substr($line, 68, 1));
  if ($metar === "X" && (intval($lat_dec) !== 0 || intval($lon_dec) !== 0)) {
    echo "$icao $iata $lat ($lat_dec) $lon ($lon_dec)\n";
    $insert = array('location' => array(floatval($lon_dec), floatval($lat_dec)), 'metar' => false, 'taf' => false);
    if (isset($icao) && strlen($icao) > 1) $insert['icao'] = $icao;
    if (isset($iata) && strlen($iata) > 1) $insert['iata'] = $iata;
    if (isset($metar) && $metar === "X") $insert['metar'] = true;
    if (isset($taf) && ($taf === "U" || $taf === "T")) $insert['taf'] = true;
    if (isset($insert['icao']) || isset($insert['iata'])) {
      echo json_encode($insert) . "\n";
      $coll->save($insert);
    }
  }
}
fclose($fh);

function DMStoDEC($degrees, $minutes, $seconds, $hemisphere) {
  $decimal = $degrees + ($minutes / 60) + ($seconds / 3600);
  $decimal = number_format($decimal, 4);
  return ($hemisphere == 'S' || $hemisphere == 'W') ? ($decimal *= -1) : $decimal;
}
