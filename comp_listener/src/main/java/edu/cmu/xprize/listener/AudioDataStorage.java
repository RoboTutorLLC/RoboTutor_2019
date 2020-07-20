package edu.cmu.xprize.listener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class AudioDataStorage {

    /**
     * audioData stores the data
     * finIndex is used to add to audioData without overwriting existing data
     * Volatile because it is accessed by the RecognizerThread but also by the _______ thread
     */
    public static final int MAX_LEN = 160* 60 * 100; // can store 60 seconds worth of audio
    public static volatile short[] audioData = new short[MAX_LEN];
    public static volatile int finIndex = 0;
    public static final int HEADER_LENGTH = 44;

    // I initialize the array and add to it

    public static synchronized void addAudioData(short[] buffer) {
        try {
            for (short s : buffer) {
                audioData[finIndex] = s;
                finIndex++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // ??
            // Should I save the audio file that we have and start writing to a new file?
        }
    }

    public static synchronized void clearAudioData() {
        Arrays.fill(audioData, (short) 0);
        finIndex = 0;
    }

    public static void saveAudioData(String filepath) {
        try {
            // Remove trailing silence
            short[] trimmedAudio = null;
            for(int x = audioData.length - 1; x > 0; x++) {
                if (audioData[x] != 0) {
                    trimmedAudio = new short[x + 1];
                    for(int y = 0; y<x; y++) {
                        trimmedAudio[y] = audioData[y];
                    }
                    break;
                }
            }

            FileOutputStream os = new FileOutputStream(filepath + ".wav");
            int dataLen = trimmedAudio.length;
            ByteBuffer dataBuffer = ByteBuffer.allocate(dataLen * 2 /* the data */ + 44 /* The header */);

            dataBuffer.order(ByteOrder.LITTLE_ENDIAN); // I *think* little endian is what .wav uses, not sure about .mp3

            /* byte[] header = new byte[]{'R','I','F','F',
                    (byte) (dataLen & 0xff), (byte) (byte) ((dataLen >> 8) & 0xff), (byte) ((dataLen >> 16) & 0xff),(byte) ((dataLen >> 24) & 0xff),
                    'W','A','V','E','f','m','t',' ', (byte) 16, 0, 0, 1, 0, 1, 0,  };
            dataBuffer.put(header); */

            ShortBuffer dataBufferShort = dataBuffer.asShortBuffer();
            dataBufferShort.put(trimmedAudio);

            FileChannel out = os.getChannel();
            out.write(dataBuffer);
            out.close();
            os.close();

        } catch (IOException e) {
            e.printStackTrace(); // ?
        } catch(NullPointerException e) {
            e.printStackTrace();
            // Log empty audio
        }
    }

}
