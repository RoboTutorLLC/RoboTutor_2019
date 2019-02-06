package cmu.xprize.comp_inventedspelling;

import java.util.HashMap;

/**
 * Generated automatically w/ code written by Kevin DeLand
 */

public class CClassMap {

    static public HashMap<String, Class> classMap = new HashMap<String, Class>();

    //
    // This is used to map "type" (class names) used in json HashMap specs to real classes

    static {
        classMap.put("CSpellingSW_Data", CSpellingSW_Data.class);
        classMap.put("CSpellingEN_Data", CSpellingEN_Data.class);
        classMap.put("CPhoneme", CPhoneme.class);

        classMap.put("string", String.class);
        classMap.put("bool", Boolean.class);
        classMap.put("int", Integer.class);
        classMap.put("float", Float.class);
        classMap.put("byte", Byte.class);
        classMap.put("long", Long.class);
        classMap.put("short", Short.class);
        classMap.put("object", Object.class);
    }
}