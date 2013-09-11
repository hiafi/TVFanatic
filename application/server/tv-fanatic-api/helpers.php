<?php

// Sets headers and outputs a json string
function out_json($app, $json_string) {
    $app->response()->header('Content-Type', 'application/json;charset=utf-8');
    // hacky way of transforming relative image urls into base64
    global $URL;

    // Base64 now used primarily, but old media tunnel remains for
    // backwards compatibility.
    function base64_img_encode($matches) {
        global $URL;
        if (count($matches[1]) > 0) {
            global $MEDIA_DIR;
            global $TVDB;
            $path = stripslashes($matches[1]);
            $type = pathinfo($path);
            $type = $type['extension'];
            $name = (string)md5($path) . ".$type";
            $file = "$MEDIA_DIR/$name";

//            return '"image":"' . $file;
            
            if (!file_exists($file)) {
                // download/cache the image
                try {
                    file_put_contents("$MEDIA_DIR/$name", file_get_contents("$TVDB/$path"));
                    $image = new SimpleImage();
                    $image->load("$MEDIA_DIR/$name");
                    $image->resizeToHeight(300);
                    $image->save("$MEDIA_DIR/$name");
                }
                catch(Exception $e){} // this could 404 frequently
            }

            if (file_exists($file)) {
                return '"image":"' . base64_encode(file_get_contents($file));
            }
        }
        return '"image":"';
    }
    $json_string = preg_replace_callback('/"image":"([^"]*)/', "base64_img_encode", $json_string);
    echo $json_string;
}

// gets the current user based on the secret_key supplied
// if $authenticate=true the application will halt if the user
// is not logged in.
function current_user($app, $authenticate=true) {
    $request = $app->request();
    $user_secret = $request->params('user_secret');

    if ($user_secret == null && $authenticate) {
        out_json($app, json_encode(array("error" => "No currently authenticated user.")));
        $app->halt(403);
        return;
    }

    $user = R::findOne('user', ' user_secret = ? ', array($user_secret));
    if ($authenticate && !$user->id) {
        out_json($app, json_encode(array("error" => "No currently authenticated user.")));
        $app->halt(403);
        return;
    }
    return $user;
}

// Update a user's recently viewed shows (up to 20)
function update_recently_viewed($user, $show) {
    // If we're over $MAX_HISTORY, then we delete the user's oldest
    // recent post
    $MAX_HISTORY = 20;

    $user_id = $user->id;
    $recent_count = R::getCell("SELECT COUNT(*) FROM recent WHERE user_id = $user_id");
    if ($recent_count > $MAX_HISTORY) {
        $recently_viewed = $user->with(' ORDER BY viewed ASC ')->ownRecent;
        $oldest = reset($recently_viewed);
        unset($user->ownRecent[$oldest->id]);
        R::store($user);
        R::trash($oldest);
    }

    // Update the visited time if it already exists
    $recent = R::findOne('recent', ' show_id = ? AND user_id = ? ', array($show->id, $user->id));
    if ($recent == null) {
        $recent = R::dispense('recent');
        $recent->show = $show;
    }
    $recent->viewed = R::isoDateTime();
    R::store($recent);

    $user->ownRecent[] = $recent;
    R::store($user);
}

function on_demand_show_update($show, $prune=true) {
    // Returns true on successful run
    // Returns null when given an invalid show according to tvdb
    // Returns false if the http request failed
    if ($show->last_updated != 0) {
        return $show;
    }

    // $DATA_DIR gets cleared, so make sure you point to the right directory!
    $DATA_DIR = "tvdb-data";
    $API_KEY = "39A28663B1A09BDD";

    if (!is_dir($DATA_DIR)) {
        mkdir($DATA_DIR);
    }

    function loadXml($url) {
        $xml = new SimpleXMLElement($url, null, true);
        return $xml;
    }

    function downloadFile($url, $path) {
        $newfname = "$path";
        return false !== file_put_contents($newfname, file_get_contents($url));
    }

    // API Docs
    // http://thetvdb.com/wiki/index.php/Programmers_API
    $MIRROR_PATH = "http://thetvdb.com";
    $ERROR_FILE = "db_errors.txt";

    /**
     * Step 2: Get the current server time
     */
    $time = loadXml("http://www.thetvdb.com/api/Updates.php?type=none");
    $time = $time->Time;

    // TODO: When diff updating is implemented, check for the timestamp.
    if (true) {
        // Then populate our show
        try {
            $title = $show->title;

            // Series with years don't return results from the TVDB
            // So we remove the year from the search and try and find a show
            // with a matching year
            preg_match('/^(.*?) ?(\d{4})?$/i', trim($title), $matches);
            $search_title = trim($matches[1]);
            $search_year = null;
            if (isset($matches[2]))
                $search_year = (int) $matches[2]; // null if no year
            $series = loadXml("http://www.thetvdb.com/api/GetSeries.php?seriesname=$search_title");

            if (count($series->Series) > 0) {
                if ($search_year !== null) {
                    // If this show has a year associated with it, find
                    // the matching show in the result set.
                    for ($i = 0; $i < count($series->Series); $i += 1) {
                        $s = $series->Series[$i];
                        $start_year = (int) substr((string)$s->FirstAired, 0, 4); // get the start year

                        // +/- 1 to handle wikipedia inconsistency with TVDB
                        if ($start_year == $search_year || $start_year + 1 == $search_year || $start_year - 1 == $search_year) {
                            // Updates the title's year in case of off-by-one issue.
                            $show->title = "$search_title $start_year";
                            $series = $s;
                            break;
                        }
                    }
                }
                if (isset($series->Series)) {
                    $series = $series->Series[0];   // assume the first search result if no series has been set yet
                }


                $show->description = (string) $series->Overview;
                $show->tvdb = (int) $series->seriesid;
                $show->image = (string) $series->banner; //switched to poster image below
                $show->start_year = (int) substr((string)$series->FirstAired, 0, 4); // get the start year
                $show->end_year = 0; // Initially, assume show to be continuing (status) - gets set below if not
                $show->last_updated = (int) $time;

                /**
                 * Step 4: Get base information for each series
                 */
                $id = $show->tvdb;
                downloadFile("$MIRROR_PATH/api/$API_KEY/series/$id/all/en.zip", "$DATA_DIR/en.zip");
                $zip = new ZipArchive;
                if ($zip->open("$DATA_DIR/en.zip") === TRUE) {
                    $zip->extractTo("$DATA_DIR/", array('en.xml', 'banners.xml'));
                    $zip->close();
                } elseif ($prune) {
                    R::trash($show);
                    return null;
                }

                $show_data = simplexml_load_file("$DATA_DIR/en.xml");
                $series_data = $show_data->Series[0]; // Data on the series as a whole (there's always just one element)
                $actors = array_slice(explode("|", $series_data->Actors, 6), 1, -1);
                $show->actors = implode(", ", $actors);
                if ((string) $series_data->poster != "")
                    $show->image = (string) $series_data->poster;
                
                // Status = 'Continuing', 'Ended', 'On Hiatus', 'Other'
                $ended = ((string) $series_data->Status == 'Ended');
                $show->latest_season = 1;
                
                foreach ($show_data->Episode as $ep) {
                    if ($ep->SeasonNumber <= 0)
                        continue;
                    
                    $episode = R::dispense('episode');
                    $episode->number = (int) $ep->EpisodeNumber;
                    $episode->season = (int) $ep->SeasonNumber;
                    $episode->title = (string) $ep->EpisodeName;
                    $episode->summary = (string) $ep->Overview;
                    $episode->date_aired = (string) $ep->FirstAired;
                    $episode->image = (string) $ep->filename;
                    $episode->ownReview = array();
                    $episode->ownRating = array();
                    R::store($episode);
                    
                    // Find show's latest season
                    if ($episode->season > $show->latest_season)
                        $show->latest_season = (int) $episode->season;
                    
                    // If the show has ended, keep track of the last year in which an
                    // episode aired - this is the show's end_year.
                    if ($ended) {
                        $episode_year = (int) substr($episode->date_aired, 0, 4);
                        if ($episode_year > $show->end_year) {
                            $show->end_year = $episode_year;
                        }
                    }
                    
                    // Add episode to show
                    $show->ownEpisode[] = $episode;
                }

                // Clean up dir
                foreach (new DirectoryIterator("$DATA_DIR/") as $fileInfo)
                    if(!$fileInfo->isDot())
                        unlink("$DATA_DIR/" . $fileInfo->getFilename());

                R::store($show);
            }
            elseif ($prune) {
                R::trash($show);
                return null;
            }
        }
        catch (Exception $e) {
            $error = "Caught exception for $title : " .  $e->getMessage() . "  Line " . $e->getLine() . "\n";
            file_put_contents($ERROR_FILE, $error, FILE_APPEND);
            return false;
        }
    }
    /*
    else {
        // TODO Diff update
    }*/
    return true;
}

function tv_log($message) {
    $file = fopen("log.txt", "a");
    $datetime = date("m-d-y H:i:s");
    fwrite($file, $datetime . "   " . $message . PHP_EOL);
    fclose($file);
}

function hashImagePath($imagePath) {
    global $MEDIA_DIR;
    
    $path = stripslashes($imagePath);
    $type = pathinfo($path);
    $type = $type['extension'];
    return "$MEDIA_DIR/" . md5($path) . ".$type";
}
?>
