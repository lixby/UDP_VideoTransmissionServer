package apollo.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.File;

/**
 * Created by dongfeng on 2016/7/15.
 */
public class Util {
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    public final static String ION360U = "ION360U";
    public final static String ION360U_DIR = Environment.getExternalStorageDirectory().getPath() + File.separator + ION360U;
    public final static String FIRMWARE_DIR = ION360U_DIR + "/Firmware";
    public final static String VIDEO_DIR = ION360U_DIR + "/Videos";
    public final static String TEMP_VIDEO_DIR = ION360U_DIR + "/TempVideos";
    public final static String PICTURE_DIR = ION360U_DIR + "/Pictures";
    public final static String SCREENSHOT_DIR = ION360U_DIR + "/Screenshots";

    public static void createFolders() {
        createFolder(VIDEO_DIR);
        createFolder(TEMP_VIDEO_DIR);
        createFolder(PICTURE_DIR);
        createFolder(SCREENSHOT_DIR);
    }

    public static void createFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public static boolean isLenParamValid(String params) {
        if (params != null && params.length() == 288 && params.substring(0, 4).equals("0001") && params.substring(params.length() - 4).equals("FFFF")) {
            return true;
        }
        return false;
    }

}
