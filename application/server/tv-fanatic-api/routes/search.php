<?php
/**
* Search route
*/
$app->get('/search/:query', function ($query) use($app) {
    // Returns a list of shows based on the query string
    $LIMIT = 10;
    $shows = R::getAll("SELECT id, title, start_year FROM show WHERE title LIKE ? LIMIT $LIMIT", array("%$query%"));
    foreach ($shows as &$show) {
        $id = $show['id'];
        global $URL;
        $show['show_url'] = "$URL/show/$id";
    }
    out_json($app, json_encode(array("data" => $shows)));
});

?>