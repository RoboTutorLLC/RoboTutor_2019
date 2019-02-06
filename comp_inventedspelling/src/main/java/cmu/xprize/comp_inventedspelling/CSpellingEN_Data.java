package cmu.xprize.comp_inventedspelling;

import org.json.JSONObject;

import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;

/**
 * Differs from SpellingSW_Data because each words contains an array of phonemes
 */

public class CSpellingEN_Data extends CSpelling_Data implements ILoadableObject{

    // json loadable
    public String level;
    public String task;
    public String layout;
    public String image;
    public CPhoneme[] word;
    public String sound;

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {
        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }
}
