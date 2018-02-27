package net.ossrs.yasea.flv;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.github.faucamp.simplertmp.RtmpHandler;

import java.nio.ByteBuffer;

/**
 * Created by dongfeng on 2016/11/29.
 */


/**
 * remux the annexb to flv tags.
 */
public class SrsFlv {
    private final static String TAG = "SrsFlv";
    private MediaFormat videoTrack;
    private MediaFormat audioTrack;
    private int achannel;
    private int asample_rate;


    private byte[] aac_specific_config;
    private FrameListener mFrameListener;
    protected RtmpHandler mHandler;

    public SrsFlv(FrameListener frameListener, RtmpHandler handler) {
        mFrameListener = frameListener;
        mHandler = handler;
        reset();
    }

    public void reset() {
        aac_specific_config = null;
    }

    public void setVideoTrack(MediaFormat format) {
        videoTrack = format;
    }

    public void setAudioTrack(MediaFormat format) {
        audioTrack = format;
        achannel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        asample_rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }

    public void writeAudioSample(final ByteBuffer bb, MediaCodec.BufferInfo bi) {
        int pts = (int)(bi.presentationTimeUs / 1000);
        int dts = pts;

        byte[] frame = new byte[bi.size + 2];
        byte aac_packet_type = 1; // 1 = AAC raw
        if (aac_specific_config == null) {
            frame = new byte[4 + 7];

            // @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf
            // AudioSpecificConfig (), page 33
            // 1.6.2.1 AudioSpecificConfig
            // audioObjectType; 5 bslbf
            byte ch = (byte)(bb.get(0) & 0xf8);
            // 3bits left.

            // samplingFrequencyIndex; 4 bslbf
            byte samplingFrequencyIndex = 0x04;
            if (asample_rate == SrsCodecAudioSampleRate.R22050) {
                samplingFrequencyIndex = 0x07;
            } else if (asample_rate == SrsCodecAudioSampleRate.R11025) {
                samplingFrequencyIndex = 0x0a;
            }
            ch |= (samplingFrequencyIndex >> 1) & 0x07;
            frame[2] = ch;

            ch = (byte)((samplingFrequencyIndex << 7) & 0x80);
            // 7bits left.

            // channelConfiguration; 4 bslbf
            byte channelConfiguration = 1;
            if (achannel == 2) {
                channelConfiguration = 2;
            }
            ch |= (channelConfiguration << 3) & 0x78;
            // 3bits left.

            // GASpecificConfig(), page 451
            // 4.4.1 Decoder configuration (GASpecificConfig)
            // frameLengthFlag; 1 bslbf
            // dependsOnCoreCoder; 1 bslbf
            // extensionFlag; 1 bslbf
            frame[3] = ch;

            aac_specific_config = frame;
            aac_packet_type = 0; // 0 = AAC sequence header

            write_adts_header(frame, 4);
        } else {
            bb.get(frame, 2, frame.length - 2);
        }

        byte sound_format = 10; // AAC
        byte sound_type = 0; // 0 = Mono sound
        if (achannel == 2) {
            sound_type = 1; // 1 = Stereo sound
        }
        byte sound_size = 1; // 1 = 16-bit samples
        byte sound_rate = 3; // 44100, 22050, 11025
        if (asample_rate == 22050) {
            sound_rate = 2;
        } else if (asample_rate == 11025) {
            sound_rate = 1;
        }

        // for audio frame, there is 1 or 2 bytes header:
        //      1bytes, SoundFormat|SoundRate|SoundSize|SoundType
        //      1bytes, AACPacketType for SoundFormat == 10, 0 is sequence header.
        byte audio_header = (byte)(sound_type & 0x01);
        audio_header |= (sound_size << 1) & 0x02;
        audio_header |= (sound_rate << 2) & 0x0c;
        audio_header |= (sound_format << 4) & 0xf0;

        frame[0] = audio_header;
        frame[1] = aac_packet_type;

        SrsFlvFrameBytes tag = new SrsFlvFrameBytes();
        tag.data = ByteBuffer.wrap(frame);
        tag.size = frame.length;

        rtmp_write_packet(SrsCodecFlvTag.Audio, dts, 0, aac_packet_type, tag);
    }

    private void write_adts_header(byte[] frame, int offset) {
        // adts sync word 0xfff (12-bit)
        frame[offset] = (byte) 0xff;
        frame[offset + 1] = (byte) 0xf0;
        // versioin 0 for MPEG-4, 1 for MPEG-2 (1-bit)
        frame[offset + 1] |= 0 << 3;
        // layer 0 (2-bit)
        frame[offset + 1] |= 0 << 1;
        // protection absent: 1 (1-bit)
        frame[offset + 1] |= 1;
        // profile: audio_object_type - 1 (2-bit)
        frame[offset + 2] = (SrsAacObjectType.AacLC - 1) << 6;
        // sampling frequency index: 4 (4-bit)
        frame[offset + 2] |= (4 & 0xf) << 2;
        // channel configuration (3-bit)
        frame[offset + 2] |= (2 & (byte) 0x4) >> 2;
        frame[offset + 3] = (byte) ((2 & (byte) 0x03) << 6);
        // original: 0 (1-bit)
        frame[offset + 3] |= 0 << 5;
        // home: 0 (1-bit)
        frame[offset + 3] |= 0 << 4;
        // copyright id bit: 0 (1-bit)
        frame[offset + 3] |= 0 << 3;
        // copyright id start: 0 (1-bit)
        frame[offset + 3] |= 0 << 2;
        // frame size (13-bit)
        frame[offset + 3] |= ((frame.length - 2) & 0x1800) >> 11;
        frame[offset + 4] = (byte) (((frame.length - 2) & 0x7f8) >> 3);
        frame[offset + 5] = (byte) (((frame.length - 2) & 0x7) << 5);
        // buffer fullness (0x7ff for variable bitrate)
        frame[offset + 5] |= (byte) 0x1f;
        frame[offset + 6] = (byte) 0xfc;
        // number of data block (nb - 1)
        frame[offset + 6] |= 0x0;
    }

    public void writeVideoSample(final ByteBuffer bb, MediaCodec.BufferInfo bi) {
        // TODO
    }

    protected void rtmp_write_packet(int type, int dts, int frame_type, int avc_aac_type, SrsFlvFrameBytes tag) {
        SrsFlvFrame frame = new SrsFlvFrame();
        frame.flvTag = ByteBuffer.allocate(tag.size);
        frame.flvTag.put(tag.data.array());
        frame.type = type;
        frame.dts = dts;
        frame.frame_type = frame_type;
        frame.avc_aac_type = avc_aac_type;

        mFrameListener.frameAvailable(frame);
    }

    /**
     * print the size of bytes in bb
     * @param bb the bytes to print.
     * @param size the total size of bytes to print.
     */
    public static void srs_print_bytes(String tag, ByteBuffer bb, int size) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int bytes_in_line = 16;
        int max = bb.remaining();
        for (i = 0; i < size && i < max; i++) {
            sb.append(String.format("0x%s ", Integer.toHexString(bb.get(i) & 0xFF)));
            if (((i + 1) % bytes_in_line) == 0) {
                Log.i(tag, String.format("%03d-%03d: %s", i / bytes_in_line * bytes_in_line, i, sb.toString()));
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            Log.i(tag, String.format("%03d-%03d: %s", size / bytes_in_line * bytes_in_line, i - 1, sb.toString()));
        }
    }


    public interface FrameListener{
        void frameAvailable(SrsFlvFrame frame);
    }


    // E.4.3.1 VIDEODATA
// Frame Type UB [4]
// Type of video frame. The following values are defined:
//     1 = key frame (for AVC, a seekable frame)
//     2 = inter frame (for AVC, a non-seekable frame)
//     3 = disposable inter frame (H.263 only)
//     4 = generated key frame (reserved for server use only)
//     5 = video info/command frame
    public class SrsCodecVideoAVCFrame
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;
        public final static int Reserved1 = 6;

        public final static int KeyFrame                     = 1;
        public final static int InterFrame                 = 2;
        public final static int DisposableInterFrame         = 3;
        public final static int GeneratedKeyFrame            = 4;
        public final static int VideoInfoFrame                = 5;
    }

    // AVCPacketType IF CodecID == 7 UI8
// The following values are defined:
//     0 = AVC sequence header
//     1 = AVC NALU
//     2 = AVC end of sequence (lower level NALU sequence ender is
//         not required or supported)
    public class SrsCodecVideoAVCType
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                    = 3;

        public final static int SequenceHeader                 = 0;
        public final static int NALU                         = 1;
        public final static int SequenceHeaderEOF             = 2;
    }

    /**
     * E.4.1 FLV Tag, page 75
     */
    public class SrsCodecFlvTag
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;

        // 8 = audio
        public final static int Audio = 8;
        // 9 = video
        public final static int Video = 9;
        // 18 = script data
        public final static int Script = 18;
    };

    // E.4.3.1 VIDEODATA
// CodecID UB [4]
// Codec Identifier. The following values are defined:
//     2 = Sorenson H.263
//     3 = Screen video
//     4 = On2 VP6
//     5 = On2 VP6 with alpha channel
//     6 = Screen video version 2
//     7 = AVC
    public class SrsCodecVideo
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved                = 0;
        public final static int Reserved1                = 1;
        public final static int Reserved2                = 9;

        // for user to disable video, for example, use pure audio hls.
        public final static int Disabled                = 8;

        public final static int SorensonH263             = 2;
        public final static int ScreenVideo             = 3;
        public final static int On2VP6                 = 4;
        public final static int On2VP6WithAlphaChannel = 5;
        public final static int ScreenVideoVersion2     = 6;
        public final static int AVC                     = 7;
        public final static int HEVC                     = 12;
    }

    /**
     * the aac object type, for RTMP sequence header
     * for AudioSpecificConfig, @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 33
     * for audioObjectType, @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 23
     */
    private class SrsAacObjectType
    {
        public final static int Reserved = 0;

        // Table 1.1 – Audio Object Type definition
        // @see @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 23
        public final static int AacMain = 1;
        public final static int AacLC = 2;
        public final static int AacSSR = 3;

        // AAC HE = LC+SBR
        public final static int AacHE = 5;
        // AAC HEv2 = LC+SBR+PS
        public final static int AacHEV2 = 29;
    }

    /**
     * the aac profile, for ADTS(HLS/TS)
     * @see //https://github.com/simple-rtmp-server/srs/issues/310
     */
    private class SrsAacProfile
    {
        public final static int Reserved = 3;

        // @see 7.1 Profiles, aac-iso-13818-7.pdf, page 40
        public final static int Main = 0;
        public final static int LC = 1;
        public final static int SSR = 2;
    }

    /**
     * the FLV/RTMP supported audio sample rate.
     * Sampling rate. The following values are defined:
     * 0 = 5.5 kHz = 5512 Hz
     * 1 = 11 kHz = 11025 Hz
     * 2 = 22 kHz = 22050 Hz
     * 3 = 44 kHz = 44100 Hz
     */
    private class SrsCodecAudioSampleRate
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                 = 4;

        public final static int R5512                     = 0;
        public final static int R11025                    = 1;
        public final static int R22050                    = 2;
        public final static int R44100                    = 3;
    }


    /**
     * utils functions from srs.
     */
    public class SrsUtils {

        public SrsAnnexbSearch srs_avc_startswith_annexb(ByteBuffer bb, MediaCodec.BufferInfo bi) {
            SrsAnnexbSearch as = new SrsAnnexbSearch();
            as.match = false;

            int pos = bb.position();
            while (pos < bi.size - 3) {
                // not match.
                if (bb.get(pos) != 0x00 || bb.get(pos + 1) != 0x00) {
                    break;
                }

                // match N[00] 00 00 01, where N>=0
                if (bb.get(pos + 2) == 0x01) {
                    as.match = true;
                    as.nb_start_code = pos + 3 - bb.position();
                    break;
                }

                pos++;
            }

            return as;
        }

        public boolean srs_aac_startswith_adts(ByteBuffer bb, MediaCodec.BufferInfo bi)
        {
            int pos = bb.position();
            if (bi.size - pos < 2) {
                return false;
            }

            // matched 12bits 0xFFF,
            // @remark, we must cast the 0xff to char to compare.
            if (bb.get(pos) != (byte)0xff || (byte)(bb.get(pos + 1) & 0xf0) != (byte)0xf0) {
                return false;
            }

            return true;
        }
    }

    /**
     * the search result for annexb.
     */
    public class SrsAnnexbSearch {
        public int nb_start_code = 0;
        public boolean match = false;
    }

    /**
     * the demuxed tag frame.
     */
    public class SrsFlvFrameBytes {
        public ByteBuffer data;
        public int size;
    }

    /**
     * the muxed flv frame.
     */
    public class SrsFlvFrame {
        // the tag bytes.
        public ByteBuffer flvTag;
        // the codec type for audio/aac and video/avc for instance.
        public int avc_aac_type;
        // the frame type, keyframe or not.
        public int frame_type;
        // the tag type, audio, video or data.
        public int type;
        // the dts in ms, tbn is 1000.
        public int dts;

        public boolean is_keyframe() {
            return is_video() && frame_type == SrsCodecVideoAVCFrame.KeyFrame;
        }

        public boolean is_sequenceHeader() {
            return avc_aac_type == 0;
        }

        public boolean is_video() {
            return type == SrsCodecFlvTag.Video;
        }

        public boolean is_audio() {
            return type == SrsCodecFlvTag.Audio;
        }
    }


    private class SrsRawAacStreamCodec {
        public byte protection_absent;
        // SrsAacObjectType
        public int aac_object;
        public byte sampling_frequency_index;
        public byte channel_configuration;
        public short frame_length;

        public byte sound_format;
        public byte sound_rate;
        public byte sound_size;
        public byte sound_type;
        // 0 for sh; 1 for raw data.
        public byte aac_packet_type;

        public byte[] frame;
    }
}
