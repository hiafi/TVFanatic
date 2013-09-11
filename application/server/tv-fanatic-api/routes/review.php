<?php

/**
 * Review routes
 */

$app->get('/review/:show_id/:season_number/:episode_number(/:page)', function ($show_id, $season_number, $episode_number, $page=1) use ($app) {
    // Returns all reviews of an episode of a show
    $limit = 10000;
    $offset = ($page - 1) * $limit;
    $episode = R::findOne('episode', " show_id = ? AND season = ? AND number = ? ", array($show_id, $season_number, $episode_number));
    if ($episode == null) {
        out_json($app, json_encode(array("error" => "No such episode.")));
        return;
    }

    global $URL;
    $episode_id = $episode->id;
    $reviews = R::exportAll($episode->with(" LIMIT $limit OFFSET $offset ")->ownReview, true);

    foreach ($reviews as &$review) {
        $id = $review['id'];
        $review['review_url'] = "$URL/review/$id";
        $review['comment_url'] = "$URL/comment/$id";
        $agree = R::getCell("SELECT AVG(value) FROM agree WHERE review_id = ? ", array($id));
        $review['agree'] = $agree;
    }

    $reviews['show_url'] = "$URL/show/$show_id";
    $reviews['season_url'] = "$URL/season/$show_id/$season_number";
    $reviews['episode_url'] = "$URL/episode/$show_id/$season_number/$episode_number";
    if (($page * $limit) < R::getCell("SELECT COUNT(*) FROM review WHERE episode_id = $episode_id"))
        $reviews['previous'] = "$URL/review/$show_id/$season_number/$episode_number/" . ($page + 1);
    if ($page > 1)
        $reviews['next'] = "$URL/review/$show_id/$season_number/$episode_number/" . ($page - 1);

    out_json($app, json_encode($reviews));
});


$app->get('/review/:review_id', function ($review_id) use($app) {
    // Returns the review with id review_id
    $review = R::load('review', $review_id);
    if (!$review->id) {
        out_json($app, json_encode(array("error" => "No such review.")));
        return;
    }
    $agree = R::getCell("SELECT AVG(value) FROM agree WHERE review_id = ? ", array($review_id));
    $review = $review->export(false, true, true);
    global $URL;
    $review['agree'] = $agree;
    $review['comment_url'] = "$URL/comment/$review_id";
    $review['user'] = $review['user']['display_name'];
    out_json($app, json_encode($review));
});

$app->post('/review/:show_id/:season_number/:episode_number', function($show_id, $season_number, $episode_number) use($app) {
    // Adds a review under show_id / season_number / episode_number
    $user = current_user($app);
    $review_title = $app->request()->post('title');
    $review_content = $app->request()->post('content');
    $episode = R::findOne('episode', ' show_id = ? AND season = ? AND number = ? ', array($show_id, $season_number, $episode_number));

    if ($episode == null) {
        out_json($app, json_encode(array("error" => "No such episode.")));
        return;
    }
    if ($review_content == null || $review_title == null) {
        out_json($app, json_encode(array("error" => "Incomplete review.")));
        return;
    }

    $review = R::dispense('review');
    $review->user = $user;
    $review->title = $review_title;
    $review->content = $review_content;
    $review->ownAgree = array();
    $review->ownComment = array();
    $review->posted_at = R::isoDateTime();
    R::store($review);

    $episode->ownReview[] = $review;
    R::store($episode);

    out_json($app, json_encode($review->export()));
});

?>
