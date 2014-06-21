<?php
//echo shell_exec("/usr/local/bin/tesseract /var/www/html/uploads/3.tiff output -l eng -psm 10");

//error_reporting(E_ALL);
$resp = 'error';
$out = array();

if(!empty($_POST)) {
  if ($_FILES["file"]["error"] > 0) {
    $resp = $_FILES["file"]["error"];
  } else {
    $resp = 'file uploaded';
    //echo $_FILES["file"]["tmp_name"] . '<br />';
    $temp_file = "/var/www/html/results/" . str_replace("/tmp/","",$_FILES["file"]["tmp_name"]);

    move_uploaded_file($_FILES["file"]["tmp_name"], "uploads/" . $_FILES["file"]["name"]);

    $input_file = "/var/www/html/uploads/" . $_FILES["file"]["name"];
    //echo "$input_file uploaded <br />";

    $shell_response = shell_exec("tesseract $input_file $temp_file -l eng -psm 7 -c tessedit_char_whitelist=\"0123456789+=\"") . "<br />";
    $string = trim(file_get_contents("$temp_file".".txt"));

    //echo "tesseract $input_file $temp_file -l eng -psm 7 <br />";

    //echo "'$string' detected<br />";
    if($_POST['answer'] == 1) $out['string'] = $string;

    //evaluate if the statement is correct
    if(strstr($string, '=')) {
      $split = explode("=", $string); 
      if($_POST['answer'] == 1) $out['left'] = eval("return ($split[0]);");
      if($_POST['answer'] == 1) $out['right'] = eval("return ($split[1]);");
      if(eval("return ($split[0]);") == eval("return ($split[1]);")) {
        $resp = 1;
      } else {
        $resp = 0;
      }
    } else {
      //compute the statement
      $out['result'] = eval("return ($string);");
      $resp = "Computed";
    }
    
    //no character recognition
    if(strlen($string) < 1) $resp = "Could not read text.";
  }
} else {
  $resp = "no file";

  echo '<html>
  <body>

  <form action="" method="post"
  enctype="multipart/form-data">
  <label for="file">Filename:</label>
  <input type="file" name="file" id="file"><br>
  <input type="hidden" value="1" name="answer">
  <input type="submit" name="submit" value="Submit">
  </form>

  </body>
  </html>';

}
//echo $resp;
$out['resp'] = $resp;
if(empty($_POST['answer'])) {
  echo $resp;
} else {
  echo json_encode($out);
}
?>