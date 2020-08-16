package edu.cmu.xprize.listener;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioDataStorage {

    /**
     * audioData stores the data
     * finIndex is used to add to audioData without overwriting existing data
     * Volatile because it is a ccessed by the RecognizerThread but also by the _______ thread
     */
    public static final int MAX_LEN = 160* 60 * 100; // can store 60 seconds worth of audio
    public static volatile short[] audioData = new short[MAX_LEN];
    public static volatile int finIndex = 0;
    public static final int HEADER_LENGTH = 44;
    public static List<ListenerBase.HeardWord> segmentation = new ArrayList<ListenerBase.HeardWord>();
    static JSONObject storyData;
    static int currentSentence = 0;
    static int sampleRate;

    static FileOutputStream outputStream;
    static FileChannel outChannel;

    public static void initStoryData(JSONObject JSONObj) {
        storyData = JSONObj;
    }

    // I initialize the array and add to it
    static synchronized void addAudioData(int index, short[] buffer) {
        try {
            for(int i = 0; i < index; i++) {
                audioData[finIndex + i] = buffer[i];
            }
            finIndex += index;
            Log.d("AudioDataStorage", "Successfully stored Audio Data");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ??
            // Should I save the audio file that we have and start writing to a new file?
            Log.d("AudioDataStorage", "out of bounds. finIndex is " + finIndex);
        } catch (Exception e) {
            Log.d("AudioDataStorage", "Failed to capture audio");
        }
    }

    public static synchronized void clearAudioData() {
        Log.d("AudioDataStorage", "AudioData Cleared");
        Arrays.fill(audioData, (short) 0);
        finIndex = 0;
    }

    public static void saveAudioData(String fileName, String assetLocation) {
        Log.d("ADSSave", "attempting to save audiodata.");

        sampleRate = 16000;

        String completeFilePath = assetLocation + fileName + "fail.wav";

        Log.d("ADSSave", completeFilePath);
        // Write the audio to file
        try {
            // Remove trailing silence
            short[] trimmedAudio = null;
            for(int x = MAX_LEN - 1; x > 0; x--) {
                if (audioData[x] != 0) {
                    trimmedAudio = new short[x + 1];
                    System.arraycopy(audioData, 0, trimmedAudio, 0, x);
                    break;
                }
            }


            FileOutputStream os = new FileOutputStream(completeFilePath);
            int trimmedLen = trimmedAudio.length;
            int dataLen = (trimmedLen * 2) + 36;
            ByteBuffer dataBuffer = ByteBuffer.allocate(trimmedLen * 2 /* the data */ + 36 /* The header */);

            dataBuffer.order(ByteOrder.BIG_ENDIAN); // Audio is recorded in Big Endian

            // Sample rate is determined by the recognizer (through some external file but I don't know what)
            // CHANNEL_IN_MONO (1 channel input)

            int channelNumber = 1;
            long bitRate = sampleRate * channelNumber * 16; // sampleRate times number of Channels times the number of bits per sample (16)
            /*byte[] header = new byte[]{'R','I','F','F',
                    (byte) (dataLen & 0xff), (byte) (byte) ((dataLen >> 8) & 0xff), (byte) ((dataLen >> 16) & 0xff),(byte) ((dataLen >> 24) & 0xff),
                    'W','A','V','E','f','m','t',' ',
                    (byte) 16, 0, 0, 1, 0, (byte) channelNumber, 0,(byte) (sampleRate & 0xff), (byte) ((sampleRate >> 8) & 0xff), (byte) ((sampleRate >> 16) & 0xff), (byte) ((sampleRate >> 24) & 0xff),
                    (byte) ((bitRate / 8) & 0xff), (byte) (((bitRate / 8) >> 8) & 0xff), (byte) (((bitRate / 8) >> 16) & 0xff), (byte) (((bitRate / 8) >> 24) & 0xff), (byte) ((channelNumber * 16) / 8),
                    0, 16, 0, 'd', 'a', 't', 'a', (byte) (dataLen * 2 & 0xff), (byte) (((dataLen * 2) >> 8) &  0xff), (byte) (((dataLen * 2) >> 16) &  0xff), (byte) (((dataLen * 2) >> 24) &  0xff)
             };

             */
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
            dataBuffer.put(header, 0, 44);

            Log.d("AudiDataStorageLog", "Just wrote header to file!");

            for(short s : trimmedAudio) {
                dataBuffer.putShort(s);
            }
            Log.d("AudioDataStorageLog", "Put " + trimmedAudio.length + " shorts into file!");

            //ShortBuffer dataBufferShort = dataBuffer.asShortBuffer();
            //dataBufferShort.put(trimmedAudio); // dataBuffer changes alongside dataBufferShort

            dataBuffer.flip();
            FileChannel out = os.getChannel();
            out.write(dataBuffer);
            os.flush();
            os.close();

            RandomAccessFile rafOut = new RandomAccessFile(completeFilePath, "rws");
            FileChannel rafChannel = rafOut.getChannel();
            int total = rafChannel.write(dataBuffer);
            Log.d("AudioDataStorageLog", "Wrote out " + total + "bytes!");

            rafChannel.close();
            rafOut.close();
        } catch (IOException | NullPointerException e) {
            Log.wtf("AudioDataStorage", "Failed to save recording to file!");
            Log.d("AudioRecordingFail", Log.getStackTraceString(e));
        }

        // write segmentation to file
        try {
            FileOutputStream os = new FileOutputStream(assetLocation + "/" + fileName + ".seg");
            StringBuilder segData = new StringBuilder("");

            Log.d("Segprogress", "FileOutputStream Created at " + assetLocation + "/" + fileName + ".seg");
            for(ListenerBase.HeardWord word: segmentation) {
                segData.append(word.hypWord + "\t" + word.startFrame + "\t" + word.endFrame);
                segData.append("\n");
            }
            segData.deleteCharAt(segData.lastIndexOf("\n"));

            byte[] segBytes = segData.toString().getBytes();
            os.write(segBytes);
            os.close();

        } catch(IOException | NullPointerException e) {
            Log.wtf("AudioDataStorage", "Failed to write segmentation!");
            Log.d("SegmentationFail", Log.getStackTraceString(e));
        }

        // Update Storydata.json
        try {
            boolean isSentence = true;
            JSONObject sentenceData = storyData
                    .getJSONArray("data")
                    .getJSONObject(currentSentence)
                    .getJSONArray("text")
                    .getJSONArray(0)
                    .getJSONObject(0)
                    .getJSONArray("narration")
                    .getJSONObject(0);

            JSONArray segm = sentenceData.getJSONArray("segmentation");
            long finalEndTime = 0;
            for(ListenerBase.HeardWord heardWord : segmentation) {
                JSONObject segObj = new JSONObject();
                segObj.put("end", heardWord.endTime);
                segObj.put("start", heardWord.startTime);
                segObj.put("word", heardWord.hypWord);
                segm.put(segObj);
                finalEndTime = heardWord.endTime;
            }
            sentenceData.put("from", ""); // TODO: figure out what 'from' is supposed to represent
            sentenceData.put("audio", completeFilePath);
            sentenceData.put("until", finalEndTime);
            sentenceData.put("utterance", fileName);

        } catch(JSONException e) {
            Log.wtf("ADSStoryData", "Failed to update storyData!");
            Log.d("StoryDataFail", Log.getStackTraceString(e));
        }

    }

    static void updateHypothesis(ListenerBase.HeardWord[] heardWords) {
        Log.d("AudioDataStorageHyp", "Hypothesis Updated");
        segmentation.clear();
        segmentation.addAll(Arrays.asList(heardWords));
    }

    static void setSampleRate(int samplerate) {
        // samplerate is always 16000 from my experience
    }
}
