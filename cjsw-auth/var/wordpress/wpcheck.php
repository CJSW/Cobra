<?php

require_once( 'class.passwordhash.php');

if (sizeof($_SERVER['argv'])<3) {
	print("Missing arguments!\nUsage: php5 wpcheck.php [password] [wphash]\n");
	exit(2);
}

$pass=$_SERVER['argv'][1];
$hash=$_SERVER['argv'][2];
//print("pass: ".$pass." hash: ".$hash."\n");
$wp_hasher = new PasswordHash(8, TRUE);
$check=$wp_hasher->CheckPassword($pass, $hash);
if (!$check) {
  print("incorrect password\n");
  exit(1);
}
print("correct password!\n");

?>