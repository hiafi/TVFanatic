<?php

/**
 * Comment routes
 */
$app->get('/comment/:review_id(/:page)', function ($review_id, $page=1) use ($app) {
    // returns all comments under the review review_id
    $limit = 10000;
    $offset = ($page - 1) * $limit;
    $comments = R::findAll('comment', " WHERE review_id = $review_id ORDER BY posted_at LIMIT $limit OFFSET $offset ");
    $comments = R::exportAll($comments, true, array("user"));

    global $URL;
    foreach ($comments as &$comment) {
        $id = $comment['id'];
        $comment['review_url'] = "$URL/review/$review_id";
        $comment['post_reply'] = "$URL/comment/$review_id/$id";
        $comment['parent_id'] = $comment['comment_id'];
        $comment['parent_name'] = null;
        if ($comment['parent_id'] !== null) {
            $c = R::load('comment', $comment['comment_id']);
            $comment['parent_name'] = $c->user->display_name;
        }
        unset($comment['comment_id']);
    }

    if (($page * $limit) < R::getCell("SELECT COUNT(*) FROM comment WHERE review_id = $review_id AND comment_id != ''"))
        $reviews['previous'] = "$URL/comment/$review_id/" . ($page + 1);
    if ($page > 1)
        $reviews['next'] = "$URL/comment/$review_id/" . ($page - 1);

    out_json($app, json_encode($comments));
});

$app->post('/comment/:review_id(/:parent)', function($review_id, $parent=null) use($app) {
    //Adds a review under review with review_id
    $user = current_user($app);
    $comment_content = $app->request()->post('comment');
    $review = R::load('review', $review_id);

    if ($comment_content == null) {
        out_json($app, json_encode(array("error" => "Incomplete comment.")));
        return;
    }

    if (!$review->id) {
        out_json($app, json_encode(array("error" => "No review.")));
        return;
    }

    // Save the new comment
    $comment = R::dispense('comment');
    $comment->user = $user;
    $comment->posted_at = R::isoDateTime();
    $comment->content = $comment_content;
    $comment->viewed_by_reviewer = false;
    $comment->viewed_by_parent_commenter = false;
    R::store($comment);

    if ($parent !== null) {
        $parent = R::load('comment', $parent);
        if (!$parent->id) {
            out_json($app, json_encode(array("error" => "Invalid comment parent.")));
            return;
        }
        $parent->ownComment[] = $comment;
        R::store($parent);
    }

    // Attribute the comment to the review
    $review->ownComment[] = $comment;
    R::store($review);

    out_json($app, json_encode($comment->export()));
});

?>