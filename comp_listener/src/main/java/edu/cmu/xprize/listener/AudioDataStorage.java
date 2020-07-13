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
    public static volatile short[] audioData = new short[160 * 60 * 100]; // creates a buffer that can store 60 seconds worth of audio
    public static volatile int finIndex = 0;


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
            FileOutputStream os = new FileOutputStream(filepath + ".pcm");
            ByteBuffer dataBuffer = ByteBuffer.allocate(audioData.length * 2);
            ShortBuffer dataBufferShort = dataBuffer.asShortBuffer();
            dataBuffer.order(ByteOrder.LITTLE_ENDIAN); // I *think* little endian is what .wav uses, not sure about .mp3
            dataBufferShort.put(audioData);
            FileChannel out = os.getChannel();
            out.close();
            os.close();

            // TODO: now it needs to be converted to either .wav or .mp3. I think the header can take care of that
        } catch (IOException e) {
            e.printStackTrace(); // ?
        }
    }

}
