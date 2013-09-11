package edu.wwu.cs412.tvfanatic.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageUtil {
	private static final String TAG = "ImageUtil";
	private static float density = 0.0f;
	
	/**
	 * Decodes Base64-encoded image data.
	 * 
	 * @param base64 The Base64-encoded image data.
	 * @return The image as a {@link android.graphics.Bitmap Bitmap}.
	 */
	public static Bitmap fromBase64(String base64) {
		byte[] encodeByte = Base64.decode(base64, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
	}

	/**
	 * Scales an image to fit within the height of its ImageView. Do not use with ImageViews
	 * with layout_height equal to <tt>wrap_content</tt>.
	 * 
	 * @param view The ImageView that houses the image to scale.
	 */
	public static void scaleImage(ImageView view)
	{
		// Get the density during first call of this method, then cache the value
		if (density == 0.0f)
			density = view.getContext().getApplicationContext().getResources().getDisplayMetrics().density;
		
	    Drawable drawing = view.getDrawable();
	    if (drawing == null) {
	        return; // Checking for null & return, as suggested in comments
	    }
	    Bitmap bitmap = ((BitmapDrawable)drawing).getBitmap();

	    // Get current dimensions AND the desired bounding box
	    int width = bitmap.getWidth();
	    int height = bitmap.getHeight();
	    int ivHeight = view.getHeight();
	    int bounding = dpToPx(ivHeight);

	    // Determine how much to scale: the dimension requiring less scaling is
	    // closer to the its side. This way the image always stays inside your
	    // bounding box AND either x/y axis touches it.  
	    float xScale = ((float) bounding) / width;
	    float yScale = ((float) bounding) / height;
	    float scale = (xScale <= yScale) ? xScale : yScale;

	    // Create a matrix for the scaling and add the scaling data
	    Matrix matrix = new Matrix();
	    matrix.postScale(scale, scale);

	    // Create a new bitmap and convert it to a format understood by the ImageView 
	    Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	    width = scaledBitmap.getWidth(); // re-use
	    height = scaledBitmap.getHeight(); // re-use
	    BitmapDrawable result = new BitmapDrawable(scaledBitmap);

	    // Apply the scaled bitmap
	    view.setImageDrawable(result);

	    // Now change ImageView's dimensions to match the scaled image
	    ViewGroup.LayoutParams params = view.getLayoutParams(); 
	    params.width = width;
	    params.height = height;
	    view.setLayoutParams(params);
	}

	private static int dpToPx(int dp)
	{
	    return Math.round((float)dp * density);
	}
}
