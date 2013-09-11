<?php
/**
 * Constants
 */
// NO trailing slash. e.g. http://google.com
$URL = "http://127.0.0.1/~philipbjorge/tv-fanatic-api";
$MEDIA_DIR = "media";
$TVDB = "http://thetvdb.com/banners";

/**
 * Routing Framework
 * Slim PHP
 */
require 'Slim/Slim.php';

\Slim\Slim::registerAutoloader();
$app = new \Slim\Slim();

/**
 * Database framework
 * Redbean
 */
require('db_config.php');
// Freeze the database to keep schema from being modified
R::freeze( true );

require('helpers.php');

/**
 * On demand show loading hook
 */
$app->hook('slim.before.dispatch', function () use ($app) {
    $route = $app->router()->getCurrentRoute();
    $params = $route->getParams();
    if (array_key_exists('show_id', $params)) {
        $show = R::load('show', $route->getParam('show_id'));
        on_demand_show_update($show); // helper.php
    }
});

/**
 * Routes
 */
require('routes/search.php');
require('routes/show.php');
require('routes/season.php');
require('routes/episode.php');
require('routes/review.php');
require('routes/comment.php');
require('routes/user.php');
require('routes/feedback.php'); // include rate, agree, and favorite
require('routes/media.php');

$app->run();

?>