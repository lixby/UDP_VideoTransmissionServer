package apollo.decoder.util;

import android.util.Pair;

/**
 * Created by dongfeng on 2016/9/7.
 */
public class InfoExtractor {

    public static boolean isIFrame(byte[] data){
        return data[4] == 39;
    }

    public static Pair<byte[], byte[]> extractSpsAndPps(byte[] data){
        byte[] sps = extractSection(data, 0);
        byte[] pps = extractSection(data, sps.length);
        return new Pair<>(sps, pps);
    }

    private static byte[] extractSection(byte[] data, int offset){
        // end by 0x00000001
        int limit = data.length - 3;
        for(int i = offset + 1; i < limit; i ++){
            if(data[i] == 0 && data[i+1] == 0 && data[i+2] == 0 && data[i+3] == 1){
                int length = i - offset;
                byte[] found = new byte[length];
                System.arraycopy(data, offset, found, 0, length);
                return found;
            }
        }
        return data;
    }
}
