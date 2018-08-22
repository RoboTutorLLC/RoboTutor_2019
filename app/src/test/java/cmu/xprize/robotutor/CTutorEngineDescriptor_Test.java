package cmu.xprize.robotutor;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import cmu.xprize.robotutor.tutorengine.CTutorEngineDescriptor;
import cmu.xprize.robotutor.tutorengine.graph.vars.TScope;

import static org.junit.Assert.assertEquals;

/**
 * RoboTutor
 * <p>
 * Created by kevindeland on 8/22/18.
 */

public class CTutorEngineDescriptor_Test {

    CTutorEngineDescriptor engineDescriptor;


    @Before
    public void setup() {

        // UNIT_TEST initiliaze here with the Test helper like this
        // UNIT_TEST create new folder src/test/resources
        // UNIT_TEST do this https://stackoverflow.com/questions/29341744/android-studio-unit-testing-read-data-input-file/

        // UNIT_TEST this should be done by JSON_Helper.cacheDataByName...
        URL url = getClass().getClassLoader().getResource("engine_descriptor.json");
        File f = new File(url.getPath());

        String json;// = JSON_Helper.cacheDataByName(url.getPath());

        StringBuilder buffer = new StringBuilder();
        try {

            InputStream in = new FileInputStream(url.getPath());

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = null;

            // Filter Comments out of the json source
            //
            while ((line = br.readLine()) != null) {

                line = line.replaceFirst("//.*$","");
                buffer.append(line);
            }
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        json = buffer.toString();

        System.out.println(url.getPath());
        System.out.println(json.length());

        TScope rootScope = new TScope(null, "root", null);

        engineDescriptor = CTutorEngineDescriptor.getEngineDescriptor(null, null);

        try {
            engineDescriptor.loadJSON(new JSONObject(json), rootScope);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testThings() {

        assertEquals(CTutorEngineDescriptor.defTutor, "activity_selector");

        // UNIT_TEST this just prints stuff out so you can see it
        System.out.println("------ tutorVariants ------");
        for (String tutorVariant :CTutorEngineDescriptor.tutorVariants.keySet()) {
            System.out.println(tutorVariant + ": " + CTutorEngineDescriptor.tutorVariants.get(tutorVariant));
        }

        // UNIT_TEST this just prints stuff out so you can see it
        System.out.println("------ bindingPatterns ------");
        for (String bindingPattern : CTutorEngineDescriptor.bindingPatterns.keySet()) {
            System.out.println(bindingPattern + ": " + CTutorEngineDescriptor.bindingPatterns.get(bindingPattern));
        }

        assertEquals(CTutorEngineDescriptor.language, "LANG_SW");
    }

}
