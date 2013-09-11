<?php

/**
 * Season routes
 */
function outputSeasonEpisodes($app, $show_id, $season_number, $latest) {
    // Returns the season information for the given show's season number.
    $show = R::load('show', $show_id);
    if (!$show->id) {
        out_json($app, json_encode(array("error" => "No such show.")));
        return;
    }

    global $URL;
    $episodes = $show->withCondition(" season = $season_number ")->ownEpisode;
    $episodes = R::exportAll($episodes);
    foreach ($episodes as &$episode) {
        unset($episode['summary']);
        unset($episode['season']);
        unset($episode['image']);
        $episode['episode_url'] = "$URL/episode/$show_id/$season_number/" . $episode['number'];
        $episode['rating'] = R::getCell('SELECT AVG(score) FROM rating WHERE episode_id = ? ', array($episode['id']));
        $episode['review_count'] = 0;
        if (isset($episode['ownReview'])) {
            $episode['review_count'] = count($episode['ownReview']);
            unset($episode['ownReview']);
        }
    }
    
    $season = array("season" => $season_number);
    $season["episodes"] = $episodes;
    
    // TODO: Previous and next not presently used in client due to the way seasons
    // are loaded into the ViewPager. We might be able to remove these from the JSON.
    if ($season_number > 1)
        $season["previous"] = "$URL/season/$show_id/" . ($season_number-1);
    if (!$latest)
        $season["next"] = "$URL/season/$show_id/" . ($season_number+1);
    out_json($app, json_encode($season));
}

$app->get('/season/:show_id/latest/', function ($show_id) use($app) {
    // Returns the /season/<show_id>/<season_number> for the latest season of the given show.
    $season_number = R::getCell("SELECT MAX(season) FROM episode WHERE show_id = $show_id");
    if ($season_number == false) {
        out_json($app, json_encode(array("error" => "No such season.")));
        return;
    }

    outputSeasonEpisodes($app, $show_id, $season_number, true);
});

$app->get('/season/:show_id/:season_number', function ($show_id, $season_number) use($app) {
    $latest = R::getCell("SELECT MAX(season) FROM episode WHERE show_id = $show_id");
    outputSeasonEpisodes($app, $show_id, $season_number, $season_number == $latest);
});

?>