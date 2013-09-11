<?php

/**
* Show routes
*/
$app->get('/show/:show_id', function ($show_id) use ($app) {
    // Returns a show with show_id
    $show = R::load('show', $show_id);
    if (!$show->id) {
        out_json($app, json_encode(array("error" => "No such show.")));
        return;
    }

    // Update the recently viewed show list
    $user = current_user($app, $authenticate=false);
    if ($user != null) {
        update_recently_viewed($user, $show);
    }

    global $URL;
    $rating_avg = R::getCell('SELECT AVG(score) FROM rating WHERE show_id = ? ', array($show_id));
    $show = $show->export();
    $show['season_url'] = "$URL/season/$show_id/latest/";
    $show['rating'] = (string)$rating_avg;
    $show['is_favorited'] = false;

    // If authenticated clients supply a 'user_secret' parameter to this GET request,
    // the server will check if the show is in the user's Favorites list, and set
    // is_favorited=true in the response if it is.
    $user_secret = $app->request()->get('user_secret');
    if ($user_secret != null) {
        $user = R::findOne('user', ' user_secret = ? ', array($user_secret));
        if ($user != null) {
            $favorite = $user->withCondition(" show_id = $show_id ")->ownFavorite;
            $show['is_favorited'] = ($favorite != null);
        }
    }
    
    out_json($app, json_encode($show));
});

?>