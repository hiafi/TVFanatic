<?php

/**
 * Episode routes
 */
$app->get('/episode/:show_id/:season_number/:episode_number', function ($show_id, $season_number, $episode_number) use ($app) {
    // Returns the episode of given show, season and episode and ratings  info (including current user)
    $episode = R::findOne('episode', ' show_id = ? AND season = ? AND number = ? ', array($show_id, $season_number, $episode_number));
    if ($episode == null) {
        out_json($app, json_encode(array("error" => "No such episode.")));
        return;
    }

    global $URL;
    $user = current_user($app, false);
    $rating_avg = R::getCell('SELECT AVG(score) FROM rating WHERE episode_id = ? ', array($episode->id));
    if ($rating_avg == null)
        $rating_avg = 0;

    if ($user !== null)
        $me_rating = R::getCell('SELECT score FROM rating WHERE user_id = ? AND episode_id = ? ', array($user->id, $episode->id));
    else
        $me_rating = 0.0;
    if ($me_rating == null)
        $me_rating = 0.0;

    $episode = R::exportAll($episode, false, array("review"));
    $episode = $episode[0];
    $episode['show_url'] = "$URL/show/$show_id";
    $episode['season_url'] = "$URL/season/$show_id/$season_number";
    $episode['rating'] = (string) $rating_avg;
    $episode['me_rating'] = (string)$me_rating;
    $episode_ids = R::getCol("SELECT id FROM episode WHERE season = $season_number");
    $episode['season_rating'] = R::getCell("SELECT AVG(score) FROM rating WHERE episode_id IN (".R::genSlots($episode_ids).")", $episode_ids);
    if ($episode['season_rating'] == null)
        $episode['season_rating'] = "0.0";
    $episode['post_review_url'] = "$URL/review/$show_id/$season_number/" . $episode['number'];
    // TODO: Season Average
    if (isset($episode['ownReview'])) {
        foreach ($episode['ownReview'] as &$review) {
            $review['review_url'] = "$URL/review/" . $review['id'];
            $review['comment_count'] = R::getCell('SELECT COUNT(*) FROM comment WHERE review_id = ? ', array($review['id']));
            $review['agree_pct'] = R::getCell('SELECT AVG(value) FROM agree WHERE review_id = ? ', array($review['id']));
            $review['user'] = R::getCell('SELECT display_name FROM user WHERE id = ? ', array($review['user_id']));
        }
    }
    else {
        $episode['ownReview'] = array();
    }

    out_json($app, json_encode($episode));
});

?>
