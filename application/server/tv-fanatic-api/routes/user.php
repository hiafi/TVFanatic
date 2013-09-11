<?php

/**
* User routes
 * user info passed in as ?user_secret= parameter
*/
$app->get('/user/me/reviews', function () use($app) {
    // returns the currently authenticated user’s most recent reviews
    $user = current_user($app);
    
    // Our desired data is scattered across 4 tables
    $reviews = R::getAll(
'SELECT r.id as review_id, r.title as review_title, r.posted_at, c.new_comments, 
    e.id as episode_id, e.number as episode_number, e.season, e.title as episode_title, 
    s.id as show_id, s.title as show_title, s.image as show_image_path
FROM review as r, episode as e, show as s 
    LEFT JOIN 
        (SELECT review_id, COUNT(1) as new_comments
        FROM comment
        WHERE viewed_by_reviewer=0
        GROUP BY review_id) as c 
    ON c.review_id=r.id
WHERE r.episode_id=e.id AND e.show_id=s.id AND r.user_id = :user_id
ORDER BY c.new_comments DESC, r.posted_at DESC
LIMIT 10 OFFSET 0;', array("user_id"=>$user->id));
    
    foreach ($reviews as &$review) {
        if ($review['new_comments'] === null)
            $review['new_comments'] = "0";
        
        // Currently, show titles end with a newline (/n) in database -- we trim it here
        $review['show_title'] = trim($review['show_title']);
        
        $review['agree'] = R::getCell("SELECT AVG(value) FROM agree WHERE review_id = ? ", 
                array((int) $review['review_id']));
        if ($review['agree'] !== null)
            $review['agree'] = (int) round($review['agree'] * 100.0);
        
        $review['show_image_path'] = hashImagePath($review['show_image_path']);
    }

    out_json($app, json_encode(array("data"=>$reviews)));
});

$app->get('/user/me/recently_viewed', function () use($app) {
// returns the currently authenticated user’s most recently viewed shows
    $RV_LIMIT = 6;
    $user = current_user($app);
    $recents = $user->with(" ORDER BY viewed DESC  LIMIT " . $RV_LIMIT)->ownRecent;
    $favorites = R::getCol('SELECT show_id FROM favorite WHERE user_id=?', array($user->id));
    
    if (count($recents) >= $RV_LIMIT) {
        // Clear older recently-viewed shows for this user
        $oldest_viewed = end($recents)->viewed;
        R::exec('DELETE FROM recent WHERE user_id = :user_id AND viewed < :oldest_viewed', 
                array('user_id'=>$user->id, 'oldest_viewed'=>$oldest_viewed));
    }
    
    R::preload($recents, array("show"));
    global $URL;
    $recent_shows = array();
    foreach ($recents as &$recent) {
        $last_viewed = $recent->viewed;
        $show = $recent->show;
        
        // Excludes shows that are alreday in the user's favorites
        if (!in_array($show->id, $favorites)) {
            $recent_shows[] = $show;
            $show->viewed = $last_viewed;
            $show['show_url'] = "$URL/show/" . $show->id;
            $show['image_path'] = hashImagePath($show['image']);

            // Remove unneeded data to reduce JSON response size
            unset($show['image']);
            unset($show['creator']);
            unset($show['actors']);
            unset($show['description']);
            unset($show['last_updated']);
            unset($show['tvdb']);
        }
    }
    
    $recent_shows = R::exportAll($recent_shows, false, array("ownEpisode"));
    out_json($app, json_encode(array("data"=>$recent_shows)));
});

$app->get('/user/me/favorites', function () use($app) {
// returns the currently authenticated user’s favorites
    $user = current_user($app);
    $favorites = $user->ownFavorite;
    $favorites = R::exportAll($favorites, true, array("show"));
    global $URL;
    foreach ($favorites as &$favorite) {
        $favorite['show_url'] = "$URL/show/" . $favorite['show_id'];
        $show = &$favorite['show'];
        
        // Send the image path (relative to server base) - rename field to 'image_path' to prevent base64 
        // embedding so that client can make full use of local caching.
        $show['image_path'] = hashImagePath($show['image']);
        
        // Remove unneeded data to reduce JSON response size
        unset($show['image']);
        unset($show['creator']);
        unset($show['actors']);
        unset($show['description']);
        unset($show['last_updated']);
        unset($show['tvdb']);
    }
    out_json($app, json_encode(array("data"=>$favorites)));
});

$app->get('/user/me/replies', function () use($app) {
// returns most recent replies to the currently authenticated user’s comments
    $user = current_user($app);
    
    // Desired data is scattered across several tables
    $replies = R::getAll(
'SELECT
    c1.id as comment_id, c1.content as comment_content, c1.posted_at as comment_posted_at, 
        c1.user_id as commenter_user_id, u_c1.display_name as commenter_display_name,
    c2.id as reply_comment_id, c2.content as reply_content, c2.posted_at as reply_posted_at, 
        c2.user_id as replier_user_id, u_c2.display_name as replier_display_name,
    r.id as review_id, r.user_id as reviewer_user_id, u_r.display_name as reviewer_display_name, 
    r.title as review_title, r.posted_at as review_posted_at,
    e.id as episode_id, e.number as episode_number, e.season, e.title as episode_title, 
    s.id as show_id, s.title as show_title, s.image as show_image_path
FROM
    review as r, episode as e, show as s, user as u_r, user as u_c1, user as u_c2, comment as c1, comment as c2
WHERE
    c2.viewed_by_parent_commenter=0 AND c2.comment_id=c1.id AND c1.review_id=r.id AND c1.user_id=:user_id AND 
    r.episode_id=e.id AND e.show_id=s.id AND r.user_id=u_r.id AND c1.user_id=u_c1.id AND 
    c2.user_id=u_c2.id
ORDER BY reply_posted_at DESC LIMIT 10 OFFSET 0;', 
    array("user_id"=>$user->id));
    
    foreach ($replies as &$reply) {
        // Currently, show titles end with a newline (/n) in database -- we trim it here
        $reply['show_title'] = trim($reply['show_title']);
        $reply['show_image_path'] = hashImagePath($reply['show_image_path']);
    }

    out_json($app, json_encode(array("data"=>$replies)));
});

$app->post('/user/assert', function () use($app) {
    $request = $app->request();
    $email = $request->post('email');
    
    $user = R::findOne('user', ' email = ? ', array($email));
    if ($user == null) {
        // Create the user
        $user = R::dispense('user');
        $user->email = $request->post('email');
        $user->user_secret = null;
        $user->display_name = null;
        $user->allow_comments_on_reviews = null;
    }
    
    if ($user->display_name === null) {
        $user->display_name = $request->post('display_name');
    }
    if ($user->allow_comments_on_reviews === null) {
        $user->allow_comments_on_reviews = (boolean) $request->post('allow_comments');
    }
    if ($user->user_secret === null) {
        $user->user_secret = sha1(microtime(true) . mt_rand(10000, 90000));
    }
    R::store($user);
    out_json($app, json_encode($user->export()));
});

// Update an existing user account (only display_name and allow_comments_on_reviews may be changed)
$app->post('/user/update', function () use($app) {
    $request = $app->request();
    $user_secret = $request->post("user_secret");
    $user = R::findOne('user', ' user_secret = ? ', array($user_secret));
    
    if ($user == null) {
        out_json($app, "");
    }
    
    if ($request->post('display_name') && $request->post('display_name') != "") { 
        $user->display_name = $request->post('display_name');
    }
    $user->allow_comments_on_reviews = (boolean) $request->post('allow_comments');
    R::store($user);
    
    out_json($app, json_encode($user->export()));
});

?>
