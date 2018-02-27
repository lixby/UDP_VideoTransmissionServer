package net.ossrs.yasea.flv;

import android.media.MediaCodec;
import android.util.Log;

import com.github.faucamp.simplertmp.RtmpHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by dongfeng on 2016/11/29.
 */

public class SrsFlvAvc extends SrsFlv {

    private final static String TAG = "SrsFlvAvc";

    private SrsRawH264Stream avc;
    private ByteBuffer h264_sps;
    private boolean h264_sps_changed;
    private ByteBuffer h264_pps;
    private boolean h264_pps_changed;
    private boolean h264_sps_pps_sent;

    public SrsFlvAvc(FrameListener frameListener, RtmpHandler handler) {
        super(frameListener, handler);
        avc = new SrsRawH264Stream();
    }

    @Override
    public void reset() {
        super.reset();
        h264_sps_changed = false;
        h264_pps_changed = false;
        h264_sps_pps_sent = false;
    }

    @Override
    public void writeVideoSample(final ByteBuffer bb, MediaCodec.BufferInfo bi) {
        int pts = (int)(bi.presentationTimeUs / 1000);
        int dts = (int)pts;

        ArrayList<SrsFlvFrameBytes> ibps = new ArrayList<SrsFlvFrameBytes>();
        int frame_type = SrsCodecVideoAVCFrame.InterFrame;

        // send each frame.
        while (bb.position() < bi.size) {
            SrsFlvFrameBytes frame = avc.annexb_demux(bb, bi);

            // 5bits, 7.3.1 NAL unit syntax,
            // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
            // 7: SPS, 8: PPS, 5: I Frame, 1: P Frame
            int nal_unit_type = (int)(frame.data.get(0) & 0x1f);
            if (nal_unit_type == SrsAvcNaluType.SPS || nal_unit_type == SrsAvcNaluType.PPS) {
                Log.i(TAG, String.format("annexb demux %dB, pts=%d, frame=%dB, nalu=%d", bi.size, pts, frame.size, nal_unit_type));
            }

            // for IDR frame, the frame is keyframe.
            if (nal_unit_type == SrsAvcNaluType.IDR) {
                frame_type = SrsCodecVideoAVCFrame.KeyFrame;
            }

            // ignore the nalu type aud(9)
            if (nal_unit_type == SrsAvcNaluType.AccessUnitDelimiter) {
                continue;
            }

            // for sps
            if (avc.is_sps(frame)) {
                if (!frame.data.equals(h264_sps)) {
                    byte[] sps = new byte[frame.size];
                    frame.data.get(sps);
                    h264_sps_changed = true;
                    h264_sps = ByteBuffer.wrap(sps);
                }
                continue;
            }

            // for pps
            if (avc.is_pps(frame)) {
                if (!frame.data.equals(h264_pps)) {
                    byte[] pps = new byte[frame.size];
                    frame.data.get(pps);
                    h264_pps_changed = true;
                    h264_pps = ByteBuffer.wrap(pps);
                }
                continue;
            }

            // ibp frame.
            SrsFlvFrameBytes nalu_header = avc.mux_ibp_frame(frame);
            ibps.add(nalu_header);
            ibps.add(frame);
        }

        write_h264_sps_pps(dts, pts);

        write_h264_ipb_frame(ibps, frame_type, dts, pts);
    }

    private void write_h264_sps_pps(int dts, int pts) {
        // when sps or pps changed, update the sequence header,
        // for the pps maybe not changed while sps changed.
        // so, we must check when each video ts message frame parsed.
        if (h264_sps_pps_sent && !h264_sps_changed && !h264_pps_changed) {
            return;
        }

        // when not got sps/pps, wait.
        if (h264_pps == null || h264_sps == null) {
            return;
        }

        // h264 raw to h264 packet.
        ArrayList<SrsFlvFrameBytes> frames = new ArrayList<SrsFlvFrameBytes>();
        avc.mux_sequence_header(h264_sps, h264_pps, dts, pts, frames);

        // h264 packet to flv packet.
        int frame_type = SrsCodecVideoAVCFrame.KeyFrame;
        int avc_packet_type = SrsCodecVideoAVCType.SequenceHeader;
        SrsFlvFrameBytes flv_tag = avc.mux_avc2flv(frames, frame_type, avc_packet_type, dts, pts);

        // the timestamp in rtmp message header is dts.
        rtmp_write_packet(SrsCodecFlvTag.Video, dts, frame_type, avc_packet_type, flv_tag);

        // reset sps and pps.
        h264_sps_changed = false;
        h264_pps_changed = false;
        h264_sps_pps_sent = true;
        Log.i(TAG, String.format("flv: h264 sps/pps sent, sps=%dB, pps=%dB", h264_sps.array().length, h264_pps.array().length));
    }

    private void write_h264_ipb_frame(ArrayList<SrsFlvFrameBytes> ibps, int frame_type, int dts, int pts) {
        // when sps or pps not sent, ignore the packet.
        // @see https://github.com/simple-rtmp-server/srs/issues/203
        if (!h264_sps_pps_sent) {
            return;
        }

        int avc_packet_type = SrsCodecVideoAVCType.NALU;
        SrsFlvFrameBytes flv_tag = avc.mux_avc2flv(ibps, frame_type, avc_packet_type, dts, pts);

        // the timestamp in rtmp message header is dts.
        rtmp_write_packet(SrsCodecFlvTag.Video, dts, frame_type, avc_packet_type, flv_tag);
    }


    /**
     * Table 7-1 – NAL unit type codes, syntax element categories, and NAL unit type classes
     * H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 83.
     */
    private class SrsAvcNaluType
    {
        // Unspecified
        public final static int Reserved = 0;

        // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int NonIDR = 1;
        // Coded slice data partition A slice_data_partition_a_layer_rbsp( )
        public final static int DataPartitionA = 2;
        // Coded slice data partition B slice_data_partition_b_layer_rbsp( )
        public final static int DataPartitionB = 3;
        // Coded slice data partition C slice_data_partition_c_layer_rbsp( )
        public final static int DataPartitionC = 4;
        // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int IDR = 5;
        // Supplemental enhancement information (SEI) sei_rbsp( )
        public final static int SEI = 6;
        // Sequence parameter set seq_parameter_set_rbsp( )
        public final static int SPS = 7;
        // Picture parameter set pic_parameter_set_rbsp( )
        public final static int PPS = 8;
        // Access unit delimiter access_unit_delimiter_rbsp( )
        public final static int AccessUnitDelimiter = 9;
        // End of sequence end_of_seq_rbsp( )
        public final static int EOSequence = 10;
        // End of stream end_of_stream_rbsp( )
        public final static int EOStream = 11;
        // Filler data filler_data_rbsp( )
        public final static int FilterData = 12;
        // Sequence parameter set extension seq_parameter_set_extension_rbsp( )
        public final static int SPSExt = 13;
        // Prefix NAL unit prefix_nal_unit_rbsp( )
        public final static int PrefixNALU = 14;
        // Subset sequence parameter set subset_seq_parameter_set_rbsp( )
        public final static int SubsetSPS = 15;
        // Coded slice of an auxiliary coded picture without partitioning slice_layer_without_partitioning_rbsp( )
        public final static int LayerWithoutPartition = 19;
        // Coded slice extension slice_layer_extension_rbsp( )
        public final static int CodedSliceExt = 20;
    }

    /**
     * the raw h.264 stream, in annexb.
     */
    private class SrsRawH264Stream {
        private SrsUtils utils;
        private final static String TAG = "SrsFlvMuxer";

        public SrsRawH264Stream() {
            utils = new SrsUtils();
        }

        public boolean is_sps(SrsFlvFrameBytes frame) {
            if (frame.size < 1) {
                return false;
            }
            return (frame.data.get(0) & 0x1f) == SrsAvcNaluType.SPS;
        }

        public boolean is_pps(SrsFlvFrameBytes frame) {
            if (frame.size < 1) {
                return false;
            }
            return (frame.data.get(0) & 0x1f) == SrsAvcNaluType.PPS;
        }

        public SrsFlvFrameBytes mux_ibp_frame(SrsFlvFrameBytes frame) {
            SrsFlvFrameBytes nalu_header = new SrsFlvFrameBytes();
            nalu_header.size = 4;
            nalu_header.data = ByteBuffer.allocate(nalu_header.size);

            // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
            // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size
            int NAL_unit_length = frame.size;

            // mux the avc NALU in "ISO Base Media File Format"
            // from H.264-AVC-ISO_IEC_14496-15.pdf, page 20
            // NALUnitLength
            nalu_header.data.putInt(NAL_unit_length);

            // reset the buffer.
            nalu_header.data.rewind();
            return nalu_header;
        }

        public void mux_sequence_header(ByteBuffer sps, ByteBuffer pps, int dts, int pts, ArrayList<SrsFlvFrameBytes> frames) {
            // 5bytes sps/pps header:
            //      configurationVersion, AVCProfileIndication, profile_compatibility,
            //      AVCLevelIndication, lengthSizeMinusOne
            // 3bytes size of sps:
            //      numOfSequenceParameterSets, sequenceParameterSetLength(2B)
            // Nbytes of sps.
            //      sequenceParameterSetNALUnit
            // 3bytes size of pps:
            //      numOfPictureParameterSets, pictureParameterSetLength
            // Nbytes of pps:
            //      pictureParameterSetNALUnit

            // decode the SPS:
            // @see: 7.3.2.1.1, H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 62
            if (true) {
                SrsFlvFrameBytes hdr = new SrsFlvFrameBytes();
                hdr.size = 5;
                hdr.data = ByteBuffer.allocate(hdr.size);

                // @see: Annex A Profiles and levels, H.264-AVC-ISO_IEC_14496-10.pdf, page 205
                //      Baseline profile profile_idc is 66(0x42).
                //      Main profile profile_idc is 77(0x4d).
                //      Extended profile profile_idc is 88(0x58).
                byte profile_idc = sps.get(1);
                //u_int8_t constraint_set = frame[2];
                byte level_idc = sps.get(3);

                // generate the sps/pps header
                // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
                // configurationVersion
                hdr.data.put((byte)0x01);
                // AVCProfileIndication
                hdr.data.put(profile_idc);
                // profile_compatibility
                hdr.data.put((byte)0x00);
                // AVCLevelIndication
                hdr.data.put(level_idc);
                // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size,
                // so we always set it to 0x03.
                hdr.data.put((byte)0x03);

                // reset the buffer.
                hdr.data.rewind();
                frames.add(hdr);
            }

            // sps
            if (true) {
                SrsFlvFrameBytes sps_hdr = new SrsFlvFrameBytes();
                sps_hdr.size = 3;
                sps_hdr.data = ByteBuffer.allocate(sps_hdr.size);

                // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
                // numOfSequenceParameterSets, always 1
                sps_hdr.data.put((byte) 0x01);
                // sequenceParameterSetLength
                sps_hdr.data.putShort((short) sps.array().length);

                sps_hdr.data.rewind();
                frames.add(sps_hdr);

                // sequenceParameterSetNALUnit
                SrsFlvFrameBytes sps_bb = new SrsFlvFrameBytes();
                sps_bb.size = sps.array().length;
                sps_bb.data = sps.duplicate();
                frames.add(sps_bb);
            }

            // pps
            if (true) {
                SrsFlvFrameBytes pps_hdr = new SrsFlvFrameBytes();
                pps_hdr.size = 3;
                pps_hdr.data = ByteBuffer.allocate(pps_hdr.size);

                // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
                // numOfPictureParameterSets, always 1
                pps_hdr.data.put((byte) 0x01);
                // pictureParameterSetLength
                pps_hdr.data.putShort((short) pps.array().length);

                pps_hdr.data.rewind();
                frames.add(pps_hdr);

                // pictureParameterSetNALUnit
                SrsFlvFrameBytes pps_bb = new SrsFlvFrameBytes();
                pps_bb.size = pps.array().length;
                pps_bb.data = pps.duplicate();
                frames.add(pps_bb);
            }
        }

        public SrsFlvFrameBytes mux_avc2flv(ArrayList<SrsFlvFrameBytes> frames, int frame_type, int avc_packet_type, int dts, int pts) {
            SrsFlvFrameBytes flv_tag = new SrsFlvFrameBytes();

            // for h264 in RTMP video payload, there is 5bytes header:
            //      1bytes, FrameType | CodecID
            //      1bytes, AVCPacketType
            //      3bytes, CompositionTime, the cts.
            // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
            flv_tag.size = 5;
            for (int i = 0; i < frames.size(); i++) {
                SrsFlvFrameBytes frame = frames.get(i);
                flv_tag.size += frame.size;
            }

            flv_tag.data = ByteBuffer.allocate(flv_tag.size);

            // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
            // Frame Type, Type of video frame.
            // CodecID, Codec Identifier.
            // set the rtmp header
            flv_tag.data.put((byte)((frame_type << 4) | SrsCodecVideo.AVC));

            // AVCPacketType
            flv_tag.data.put((byte)avc_packet_type);

            // CompositionTime
            // pts = dts + cts, or
            // cts = pts - dts.
            // where cts is the header in rtmp video packet payload header.
            int cts = pts - dts;
            flv_tag.data.put((byte)(cts >> 16));
            flv_tag.data.put((byte)(cts >> 8));
            flv_tag.data.put((byte)cts);

            // h.264 raw data.
            for (int i = 0; i < frames.size(); i++) {
                SrsFlvFrameBytes frame = frames.get(i);
                byte[] frame_bytes = new byte[frame.size];
                frame.data.get(frame_bytes);
                flv_tag.data.put(frame_bytes);
            }

            // reset the buffer.
            flv_tag.data.rewind();
            return flv_tag;
        }

        public SrsFlvFrameBytes annexb_demux(ByteBuffer bb, MediaCodec.BufferInfo bi) {
            SrsFlvFrameBytes tbb = new SrsFlvFrameBytes();

            while (bb.position() < bi.size) {
                // each frame must prefixed by annexb format.
                // about annexb, @see H.264-AVC-ISO_IEC_14496-10.pdf, page 211.
                SrsAnnexbSearch tbbsc = utils.srs_avc_startswith_annexb(bb, bi);
                if (!tbbsc.match || tbbsc.nb_start_code < 3) {
                    Log.e(TAG, "annexb not match.");
                    SrsFlv.srs_print_bytes(TAG, bb, 16);
                    mHandler.notifyRtmpIllegalArgumentException(new IllegalArgumentException(
                            String.format("annexb not match for %dB, pos=%d", bi.size, bb.position())));
                }

                // the start codes.
                ByteBuffer tbbs = bb.slice();
                for (int i = 0; i < tbbsc.nb_start_code; i++) {
                    bb.get();
                }

                // find out the frame size.
                tbb.data = bb.slice();
                int pos = bb.position();
                while (bb.position() < bi.size) {
                    SrsAnnexbSearch bsc = utils.srs_avc_startswith_annexb(bb, bi);
                    if (bsc.match) {
                        break;
                    }
                    bb.get();
                }

                tbb.size = bb.position() - pos;
                break;
            }

            return tbb;
        }
    }

}
