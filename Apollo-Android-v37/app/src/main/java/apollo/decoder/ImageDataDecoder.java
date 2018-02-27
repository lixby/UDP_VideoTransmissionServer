package apollo.decoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import java.io.File;

/**
 * Load an image into a surface
 * Created by dongfeng on 2016/9/21.
 */
public class ImageDataDecoder extends DataDecoder {
    private final static String TAG = "ImageDataDecoder";
    private String mImagePath;

    public ImageDataDecoder(Context context, String imagePath){
        mImagePath = imagePath;
    }

    @Override
    public void startThreadDecoding(){
    }

    @Override
    public void stopDecoding(){
    }

    @Override
    public void setSurface(Surface surface){
        super.setSurface(surface);

        if(surface == null){
            Log.e(TAG, "Surface is null");
            return;
        }

        Bitmap bitmap = loadImage(mImagePath);
        if(bitmap == null){
            Log.e(TAG, "Image cannot be loaded");
            return;
        }

        // draw the bitmap in the surface canvas
        Rect sourceRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Canvas canvas = surface.lockCanvas(null);
        Rect targetRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, sourceRect, targetRect, null);
        surface.unlockCanvasAndPost(canvas);

        Log.d(TAG, "Image loaded " + bitmap.getWidth() + ", " + bitmap.getHeight());
    }

    /**
     * Load a local image into a bitmap object
     * @param path
     * @return
     */
    public static Bitmap loadImage(String path){
        try {
            File f = new File(path);
            if (!f.exists()) { return null; }
            Bitmap tmp = BitmapFactory.decodeFile(path);
            return tmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}