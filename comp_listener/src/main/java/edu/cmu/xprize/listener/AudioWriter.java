package edu.cmu.xprize.listener;

import android.media.AudioRecord;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

public class AudioWriter {

    public static volatile short[] audioData = new short[160 * 60 * 100];
    public static volatile OutputStream outputStream;
    public static volatile FileChannel fileChannel;
    public static volatile BufferedOutputStream bufferedOutputStream;
    public static volatile DataOutputStream dataOutputStream;

    public static String audioFileName;
    public static String audioAssetLocation;
    public static String completeFilePath;

    public static int dataLen;

    static byte[] savedFileData;

    public static void closeStreams() {
        try {
            outputStream.close();
        } catch (Exception ignored) {}

        try {
            bufferedOutputStream.close();
        } catch (Exception ignored) {}

        try {
            dataOutputStream.close();
        } catch (Exception ignored) {}
    }

    // AudioWriter is updated with a new filepath
    public static void initializePath(String fileName, String assetLocation) {
        dataLen = 0;
        // Close up  old file
        closeStreams();

        //Reopen to add header to the beginning of the file
        try {
            addHeader(new RandomAccessFile(completeFilePath, "rws"));
        } catch (Exception e) {
            Log.d("AudioWriterHeaderFail", Log.getStackTraceString(e));
        }

        // Prepare new file
        audioFileName = fileName.toLowerCase().replace(" ", "_");
        audioAssetLocation = assetLocation;
        completeFilePath = assetLocation + audioFileName + ".wav";

        // Just in case the new file is already existing, save its contents
        try {
            File inFile = new File(completeFilePath);
            FileInputStream input = new FileInputStream(inFile);
            savedFileData = new byte[(int) inFile.length()];
            BufferedInputStream bis =new BufferedInputStream(input);
            bis.read(savedFileData,0,savedFileData.length);
            bis.close();
            input.close();
        } catch (IOException ignored) {}

        try {
            outputStream = new FileOutputStream(completeFilePath);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            dataOutputStream = new DataOutputStream(bufferedOutputStream);

            for(int i = 0; i < 44; i++) {
                dataOutputStream.writeByte(0);
            }
        } catch (IOException e) {
            Log.wtf("AudioWriterFail", Log.getStackTraceString(e));
        }
    }

    public static void addAudio(int noOfShorts, short[] dataBuffer) {
        try {
            ByteBuffer bytes = ByteBuffer.allocate(noOfShorts * 2);
            bytes.order(ByteOrder.LITTLE_ENDIAN);

            for(int i = 0; i < noOfShorts; i++) {
                bytes.putShort(dataBuffer[i]);
            }

            dataOutputStream.write(bytes.array());
            dataOutputStream.flush();
            dataLen += noOfShorts;
            Log.d("AudioWriter", "Just wrote " + noOfShorts + " shorts to the audio file!");
        } catch (IOException e) {
            Log.wtf("AudioWriterFail", Log.getStackTraceString(e));
        }
    }

    public static void destroyContent() {
        initializePath(audioFileName, audioAssetLocation);
    }

    public static void addHeader(RandomAccessFile raf) throws IOException {
        long dataLen = raf.length() - 8;
        int sampleRate = 16000;
        int channelNumber = 1;
        int bitRate = sampleRate * channelNumber * 16;
        long trimmedLen = dataLen - 36;

        byte[] header = new byte[44];

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (dataLen & 0xff);
        header[5] = (byte) ((dataLen >> 8) & 0xff);
        header[6] = (byte) ((dataLen >> 16) & 0xff);
        header[7] = (byte) ((dataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) 1;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) ((bitRate / 8) & 0xff);
        header[29] = (byte) (((bitRate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitRate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitRate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channelNumber * 16) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) ((trimmedLen * 2)  & 0xff);
        header[41] = (byte) (((trimmedLen * 2)  >> 8) & 0xff);
        header[42] = (byte) (((trimmedLen * 2)  >> 16) & 0xff);
        header[43] = (byte) (((trimmedLen * 2)  >> 24) & 0xff);

        raf.seek(0);

        raf.write(header, 0, 44);

        raf.seek(raf.length());
        raf.close();
    }

    // When the sentence is not being recorded, put the original file contents back
    public static void abortOperation() {
        try {
            closeStreams();

            // rewrite old contents of the file
            FileOutputStream output = new FileOutputStream(completeFilePath);
            output.write(savedFileData);
        } catch (IOException ignored) {

        } catch (NullPointerException ignored) {// this is thrown if file doesn't exist
        }

    }
}
