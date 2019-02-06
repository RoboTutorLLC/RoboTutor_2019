package cmu.xprize.comp_inventedspelling;

import org.json.JSONObject;

import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;

/**
 * RoboTutor
 * <p>
 * Created by kevindeland on 11/8/18.
 */

public class CPhoneme implements ILoadableObject{

    // json loadable
    public String letters;
    public String phoneme;

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {
        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }
}
