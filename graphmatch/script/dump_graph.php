<?php

if ($argc != 3) {
	echo "usage: <dbname> <prefix of data files>\n";
	exit;
}

//$minactmov = $argv[1];
//$minmovact = $argv[2];
//$minmovdir = $argv[3];

$dbname = $argv[1];
$prefix = $argv[2];

$db = new mysqli("localhost", "root", "", $dbname);

$res = $db->query ("select * from movies");
if (!$res) {
    echo "error: " . $db->error;
    exit;
}


$movielist = array ();

$fp = fopen("$prefix.movie", "w");

$idx = 0;
while ( ($row = $res->fetch_assoc()) != null ) {
	$movielist[$row['id']] = $idx++;
	fwrite($fp, $row['id'] . "\t" . $movielist[$row['id']] . "\n");
}
$res->close();
fclose($fp);


$res = $db->query ("select * from directors");
if (!$res) {
    echo "error: " . $db->error;
    exit;
}


$directorlist = array ();

$fp = fopen("$prefix.director", "w");

$idx = 0;
while ( ($row = $res->fetch_assoc()) != null ) {
	$directorlist[$row['id']] = $idx++;
	fwrite($fp, $row['id'] . "\t" . $directorlist[$row['id']] . "\n");
}
$res->close();
fclose($fp);

$res = $db->query ("select * from actors");
if (!$res) {
    echo "error: " . $db->error;
    exit;
}


$actorlist = array ();

$fp = fopen("$prefix.actor", "w");

$idx = 0;
while ( ($row = $res->fetch_assoc()) != null ) {
	$actorlist[$row['id']] = $idx++;
	fwrite($fp, $row['id'] . "\t" . $actorlist[$row['id']] . "\n");
}
$res->close();
fclose($fp);


$res = $db->query ("select actor_id, movie_id from roles");
if (!$res) {
    echo "error: " . $db->error;
    exit;
}


$fp = fopen("$prefix.movie_actor", "w");

$idx = 0;
while ( ($row = $res->fetch_array()) != null ) {
	if (!isset($movielist[$row[1]])) {
		echo "cannot find movie [1]: $row[1]\n";
		exit;
	}
	if (!isset($actorlist[$row[0]])) {
		print_r($row);
		echo "cannot find actor: $row[0]\n";
		exit;
	}
	fwrite($fp, $movielist[$row[1]] . "\t" . $actorlist[$row[0]] . "\n");
}
$res->close();

fclose($fp);

$res = $db->query ("select * from movies_directors");
if (!$res) {
    echo "error: " . $db->error;
    exit;
}


$fp = fopen("$prefix.movie_director", "w");

$idx = 0;
while ( ($row = $res->fetch_assoc()) != null ) {
	if (!isset($movielist[$row['movie_id']])) {
		echo "cannot find movie [2]: $row[movie_id]\n";
		exit;
	}
	if (!isset($directorlist[$row['director_id']])) {
		echo "cannot find director: $row[director_id]\n";
		exit;
	}
	fwrite($fp, $movielist[$row['movie_id']] . "\t" . $directorlist[$row['director_id']] . "\n");
}
$res->close();

fclose($fp);

$fp = fopen("$prefix.README", "w");
fwrite($fp, "nactor: " . count($actorlist) . "\n");
fwrite($fp, "nmovie: " . count($movielist) . "\n");
fwrite($fp, "ndirec: " . count($directorlist) . "\n");
fwrite($fp, "format of movie_actor: movieid <tab> actorid\n");
fwrite($fp, "format of movie_director: movieid <tab> directorid\n");
fclose($fp);
?>
