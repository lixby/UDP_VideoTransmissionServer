package apollo.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/11/11.
 */

public interface IMediaMuxer {
    boolean isStarted();
    boolean start();
    void stop();
    int addTrack(final MediaFormat format);
    void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo);
}
