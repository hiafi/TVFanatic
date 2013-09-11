<?php

/**
 * This file dynamically generates our database schema by
 * inserting stub data for test purposes.
 * Using Redbean PHP
 *
 * NOTE: No dependencies (aka cascade deletes) for maintaing key integrity
 * are in place yet.
 */

require_once('db_config.php');
R::nuke();

$file = fopen("shows.txt", "r");
while (!feof($file)) {
    $name = fgets($file);
    $show = R::dispense('show');
    $show->title = "$name";
    $show->creator = "";
    $show->actors = "";
    $show->description = "";
    $show->image = "";
    $show->ownEpisode = array();
    $show->ownRating = array();
    $show->start_year = 0;
    $show->end_year = 0;
    $show->latest_season = 0;
    $show->last_updated = 0;
    $show->tvdb = 0;
    R::store($show);
}
fclose($file);

// now create the rest of the schema
/**
 * Predeclare user and show as it's referenced in other tables.
 */
$show = R::load('show', 1);

$user = R::dispense('user');
$user->display_name = 'Philip Bjorge';
$user->email = 'unknown@example.com';
$user->allow_comments_on_reviews = true;
R::store($user);

$user2 = R::dispense('user');
$user2->display_name = 'John Bjorge';
$user2->allow_comments_on_reviews = true;
R::store($user2);

/**
 * Comment Table
 * - belongs to user
 */
$comment2 = R::dispense('comment');
$comment2->user = $user2; // creates a many-to-one relationship
$comment2->posted_at = R::isoDateTime();
$comment2->content = "Hello world reply!";
$comment2->ownComment = array(); // parent
$comment2->viewed_by_reviewer = false;
$comment2->viewed_by_parent_commenter = false;
R::store($comment2);

$comment = R::dispense('comment');
$comment->user = $user; // creates a many-to-one relationship
$comment->posted_at = R::isoDateTime();
$comment->content = "Hello world comment!";
$comment->ownComment = array($comment2);
$comment->viewed_by_reviewer = false;
$comment->viewed_by_parent_commenter = false;
R::store($comment);

/**
 * Ratings Table
 * Handles ratings at all levels (episodes, reviews, seasons, etc)
 * - many to one with user
 */
$ratingA = R::dispense('rating');
$ratingA->score = 2.5;
$ratingA->user = $user;
R::store($ratingA);

$ratingB = R::dispense('rating');
$ratingB->score = 4;
$ratingB->user = $user;
R::store($ratingB);

/**
 * Agrees/Disagrees Table
 */
$disagree = R::dispense('agree');
$disagree->value = false;
$disagree->user = $user;
R::store($disagree);

/**
 * Review Table
 * - many to one with user
 * - has agree/disagree
 */
$review = R::dispense('review');
$review->user = $user;
$review->title = "Review Title";
$review->content = "Sample review...";
$review->ownAgree = array($disagree);
$review->ownComment = array($comment, $comment2);
$review->posted_at = R::isoDateTime();
R::store($review);

/**
 * Episodes Table
 * - has reviews
 * - has ratings
 */
$episode = R::dispense('episode');
$episode->number = 7;
$episode->season = 5;
$episode->title = "S05E07 - The Chemist";
$episode->summary = "Summary here...";
$episode->date_aired = '1995-12-05';
$episode->image = "/test/image.jpg";
$episode->ownReview = array($review); // ownReview creates a one-to-many relationship
$episode->ownRating = array($ratingA);
R::store($episode);

/**
 * Favorite + Recent
 */
$favoriteA = R::dispense('favorite');
$favoriteA->show = $show;
R::store($favoriteA);

$favoriteB = R::dispense('favorite');
$favoriteB->show = $show;
R::store($favoriteA);

$recentA = R::dispense('recent');
$recentA->show = $show;
$recentA->viewed = R::isoDateTime();
R::store($recentA);

/**
 * User Table
 * - has favorite episodes
 * - has recently viewed episodes
 *
 * Note: Because a user has many favorites and recently_viewed (which are both shows)
 * Favorites Query: R::related( $album, 'track', ' length > ? ', array($seconds) );
 * Recent Query: R::related( $album, 'track', ' length > ? ', array($seconds) );
 */
$user->user_secret = "abc";
$user->ownFavorite = array($favoriteA);
$user->ownRecent = array($recentA);
R::store($user);

$user2->user_secret = "def";
$user2->ownFavorite = array($favoriteB);
$user2->ownRecent = array();
R::store($user2);

$show->ownEpisode[] = $episode;
$show->ownRating[] = $ratingB;
R::store($show);

// Clear sample data
R::freeze(true);
$listOfTables = R::$writer->getTables();
foreach($listOfTables as $table) {
    if ($table != "show") { R::exec("DELETE FROM $table"); }
}
$show->ownEpisode = array();
$show->ownRating = array();
R::store($show);

?>