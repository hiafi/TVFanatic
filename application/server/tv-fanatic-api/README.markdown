1. Modify the htaccess file's RewriteBase
2. Modify the $URL constant at the top of index.php
3. Modify db_config.php to point to your permanent database file.

To populate data:
    From the command line execute (Note: the db will be cleared!):
        php prepopulate_shows.php

    This will populate your database with stub data and shows.

    Content is loaded on demand from the tv-db when any route with a <show_id> variable is called.
    No update mechanism is in place yet.