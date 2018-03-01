package com.xlm.h264senddemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

public class AvcEncoder
{
	private final static String TAG = AvcEncoder.class.getSimpleName();
	private final static String MIME_TYPE = "video/avc";
	private final static int I_FRAME_INTERVAL = 1;
	
    MediaCodec mediaCodec;  
    int width;  
    int height;
    long frameIndex = 0;
    byte[] spsPpsInfo = null;
    private final static int TIME_UNIT = 50 * 1000;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public AvcEncoder() 
    {         
    }  
    
    public boolean init(int width, int height, int framerate, int bitrate)
    {
        try {
            this.width  = width;
            this.height = height;

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1024*256);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        }
        catch (IOException e) {
			return false;
		}

        mediaCodec.start();

        return true;
    }


    public void close() 
    {  
        try {
            mediaCodec.stop();  
            mediaCodec.release();  
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage(),e);
        }  
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public byte[] offerEncoder(byte[] input)
    {
        try {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIME_UNIT);//等缓冲区
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(input);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
                frameIndex++;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIME_UNIT);
                        
            while (outputBufferIndex >= 0)   
            {  
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[bufferInfo.size];  
                outputBuffer.get(outData);  
                  
                if (spsPpsInfo == null)  
                {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);    
                    if (spsPpsBuffer.getInt() == 0x00000001)   
                    {
                        spsPpsInfo = new byte[outData.length];  
                        System.arraycopy(outData, 0, spsPpsInfo, 0, outData.length); 
                    }   
                    else   
                    {    
                    	return null;  
                    }  
                }
                else
                { 
                	outputStream.write(outData);
                }  
                  
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);  
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIME_UNIT);
            }  
            byte[] ret = outputStream.toByteArray();
            if (ret.length > 5 && ret[4] == 0x65) { //key frame need to add sps pps
                outputStream.reset();
                outputStream.write(spsPpsInfo);
                outputStream.write(ret);
            }
        }
        catch (Throwable t) {
            Log.e(TAG,t.getMessage(),t);
        }  
        byte[] ret = outputStream.toByteArray();
        outputStream.reset();
        return ret;  
    }  

}
