<?php

$app->get('/media/:path+', function ($path) use($app) {
    // returns tvdb graphics (with caching)
    global $MEDIA_DIR;
    global $TVDB;
    $path = implode("/", $path);
    $type = pathinfo($path);
    $type = $type['extension'];
    $name = (string)md5($path) . ".$type";
    $file = "$MEDIA_DIR/$name";

    if (!file_exists($file)) {
        // download/cache the image
        try {
            file_put_contents("$MEDIA_DIR/$name", file_get_contents("$TVDB/$path"));
        }
        catch(Exception $e){} // this could 404 frequently
    }

    if (file_exists($file)) {
        $app->response()->header('Content-Type', mime_content_type($file));
        echo file_get_contents($file);
        return;
    }

    $app->halt(404);
});

?>