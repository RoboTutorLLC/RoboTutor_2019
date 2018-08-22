package cmu.xprize.robotutor.tutorengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cmu.xprize.robotutor.tutorengine.graph.defdata_tutor;
import cmu.xprize.robotutor.tutorengine.graph.defvar_tutor;
import cmu.xprize.robotutor.tutorengine.graph.vars.IScope2;
import cmu.xprize.robotutor.tutorengine.graph.vars.TScope;
import cmu.xprize.robotutor.tutorengine.util.CClassMap2;
import cmu.xprize.util.JSON_Helper;

/**
 * RoboTutor
 * UNIT_TEST this should eventually go into CTutorEngine and replace the vars there.
 * <p>
 * Created by kevindeland on 8/22/18.
 */

public class CTutorEngineDescriptor {

    private static CTutorEngineDescriptor singletonDescriptor;

    // json loadable
    static public String                         descr_version;                 //
    static public String                         defTutor; // defined in engine_descriptor.json
    static public HashMap<String, defvar_tutor>  tutorVariants;
    static public HashMap<String, defdata_tutor> bindingPatterns;
    static public String                         language;                       // Accessed from a static context

    private CTutorEngineDescriptor(String filepath, TScope rootScope) {


        // UNIT_TEST the vars should be loaded here
        /*try {
            loadJSON(new JSONObject(JSON_Helper.cacheData(filepath)), rootScope);

        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    static public CTutorEngineDescriptor getEngineDescriptor(String filepath, TScope rootScope) {

        if (singletonDescriptor == null) {
            singletonDescriptor = new CTutorEngineDescriptor(filepath, rootScope);
        }

        return singletonDescriptor;
    }

    /**
     * Load the Tutor specification from JSON file data
     *
     * @param jsonData
     */
    public void loadJSON(JSONObject jsonData, IScope2 scope) {

        JSON_Helper.parseSelf(jsonData, this, CClassMap2.classMap, scope);
    }
}
