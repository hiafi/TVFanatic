package edu.wwu.cs412.tvfanatic;

import android.R;
import android.R.anim;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class Search extends ListActivity { 
   public void onCreate(Bundle savedInstanceState) { 
      super.onCreate(savedInstanceState);
      handleIntent(getIntent()); 
   } 

   public void onNewIntent(Intent intent) { 
      setIntent(intent); 
      handleIntent(intent); 
   } 

   public void onListItemClick(ListView l, 
      View v, int position, long id) { 
	    // call detail activity for clicked entry 
		Intent intent = new Intent(this, ViewShow.class);
		intent.putExtra(Constants.TVF_PACKAGE + ".show_id", String.valueOf(id));
		startActivity(intent);
   } 

   private void handleIntent(Intent intent) {
      if (Intent.ACTION_SEARCH.equals(intent.getAction())) { 
         String query = intent.getStringExtra(SearchManager.QUERY); 
         doSearch(query); 
      } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
    	    // Handle a suggestions click (because the suggestions all use ACTION_VIEW)
    	    Uri data = intent.getData();
			intent = new Intent(this, ViewShow.class);
			intent.putExtra(Constants.TVF_PACKAGE + ".show_id", data.toString());
			startActivity(intent);
			finish();
    	}
   }    

   private void doSearch(String queryStr) { 
	   // get a Cursor, prepare the ListAdapter
	   // and set it
	   ContentResolver cr = getContentResolver();
	   Cursor cursor = cr.query(
			   Uri.parse("content://edu.wwu.cs412.tvfanatic.search_content_provider/search_suggest_query/" + queryStr),
			   null, null, null, null);
	   this.setListAdapter(new SimpleCursorAdapter(this, R.layout.simple_list_item_1, cursor, new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1}, new int[] {R.id.text1}, 0));
	} 
}