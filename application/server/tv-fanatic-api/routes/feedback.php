<?php

/**
 * Rate routes
 */
$app->post('/rate/:show_id', function ($show_id) use($app) {
    $user = current_user($app);
    $score = $app->request()->post('rating');
    if ($score == null) {
        out_json($app, json_encode(array("error" => "No rating supplied.")));
        return;
    }

    // Rate show
    // show rating
    $show = R::load('show', $show_id);
    if (!$show->id) {
        out_json($app, json_encode(array("error" => "No such show.")));
        return;
    }

    // Create or update existing rating.
    $rating = R::findOne('rating', " user_id = ? AND show_id = ? ", array($user->id, $show->id));
    if ($rating == null) {
        $rating = R::dispense('rating');
        $show->ownRating[] = $rating;
        $rating->user = $user;
    }
    $rating->score = (int)$score;
    R::store($rating);
    R::store($show);

    out_json($app, json_encode($rating->export()));
});

$app->post('/rate/:show_id/:season_number/:episode_number', function ($show_id, $season_number, $episode_number) use($app) {
    // Adds a rating for a show, season, or episode from a user
    // This route accepts the following:
    // show: rate/show_id
    // episode: rate/show_id/season_number/episode_number
    // When one of these isn't set, that variable defaults to null
    $user = current_user($app);
    $score = $app->request()->post('rating');
    if ($score == null) {
        out_json($app, json_encode(array("error" => "No rating supplied.")));
        return;
    }

    // Rate episode, otherwise continue past and rate a show
    $episode = R::findOne('episode', ' show_id = ? AND season = ? AND number = ? ', array($show_id, $season_number, $episode_number));
    if ($episode == null) {
        out_json($app, json_encode(array("error" => "No such episode.")));
        return;
    }

    // Create or update existing rating.
    $rating = R::findOne('rating', " user_id = ? AND episode_id = ? ", array($user->id, $episode->id));
    if ($rating == null) {
        $rating = R::dispense('rating');
        $episode->ownRating[] = $rating;
        $rating->user = $user;
    }
    $rating->score = (int)$score;
    R::store($rating);
    R::store($episode);

    out_json($app, json_encode($rating->export()));
});

/**
 * Agree routes
 */
$app->post('/agree/:review_id', function($review_id) use($app) {
    // Marks the user as agree or disagree with review
    $user = current_user($app);
    $new_agree = $app->request()->post('agree');
    if ($new_agree == null) {
        out_json($app, json_encode(array("error" => "Invalid agree/disagree data.")));
        return;
    }

    // Create or update existing rating.
    $rr = R::load('review', $review_id);
    $agree = R::findOne('agree', " user_id = ? AND review_id = ? ", array($user->id, $review_id));
    if ($agree == null) {
        $agree = R::dispense('agree');
        $agree->user = $user;
        $rr->ownAgree[] = $agree;
    }
    $agree->value = (bool)$new_agree;
    R::store($agree);
    R::store($rr);

    out_json($app, json_encode($agree->export()));
});

/**
 * Favorite routes
 */
$app->post('/favorite/:show_id', function($show_id) use($app) {
    // Add a given show to the currently authenticated user’s favorites
    $user = current_user($app);
    $show = R::load('show', $show_id);
    if (!$show->id) {
        out_json($app, json_encode(array("error" => "No show.")));
        return;
    }

    $favorite = $user->withCondition(" show_id = $show_id ")->ownFavorite;
    if (count($favorite) > 0) {
        // remove the favorite and the favorite from the user's favorite list
        $favorite = reset($favorite);
        unset($user->ownFavorite[$favorite->id]);
        R::store($user);
        R::trash($favorite);
        out_json($app, json_encode(array("success" => "unfavorited")));
    }
    else {
        // add the show
        $favorite = R::dispense('favorite');
        $favorite->show = $show;
        R::store($favorite);

        $user->ownFavorite[] = $favorite;
        R::store($user);
        out_json($app, json_encode(array("success" => "favorited")));
    }

});

?>