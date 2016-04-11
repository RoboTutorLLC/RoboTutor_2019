//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.ltk;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A StrokeSet represents a single glyph where each elemental stroke is a part
 * of the final character.  The glyph is created in absolute time and then normalized when
 * the glyph is complete.  It is serialized in a normalized form
 *
 */
public class StrokeSet {

    // volatile data
    private StrokeInfo        _strokeInfo;
    private long              _startTime;
    private long              _lastTime;
    private Stroke            _currentStroke;

    // persistent glyph data
    private ArrayList<StrokeInfo> _strokeInfoList;
    private RectF                 _glyphBoundingBox = null;
    private RectF                 _fontBoundingBox  = null;
    private Float                 _glyphBaseLine;
    private Float                 _fontBaseLine;

    private static String      TAG  = "StrokeSet";


    public StrokeSet(float baseline) {

        _strokeInfoList = new ArrayList<>();
        _glyphBaseLine  = baseline;
    }


    public void newStroke() {

        _currentStroke = new Stroke();
        _startTime     = System.currentTimeMillis();
        _strokeInfo    = new StrokeInfo(_currentStroke, _startTime);

        _strokeInfoList.add(_strokeInfo);
    }


    public void endStroke() {

        _strokeInfo.setDuration(_lastTime - _startTime);
    }


    public void terminateGlyph() {

        normalizeGlyph();
        serializeGlyph();
    }


    /**
     * Add the next point to the current stroke - record when the last segment was added to
     * the stroke.  This permits calculating inter-stroke times (to animate hand motions).
     *
     * @param touchPt
     */
    public void addPoint(PointF touchPt) {

        _lastTime  =  System.currentTimeMillis();

        _currentStroke.addPoint(touchPt, _lastTime);

        addPointToBoundingBox(touchPt);
    }


    private void addPointToBoundingBox(PointF point) {

        if (_glyphBoundingBox == null) {
            _glyphBoundingBox = new RectF(point.x, point.y, point.x, point.y);
            _fontBoundingBox = new RectF(point.x, point.y, point.x, point.y);
            return;
        }

        // Expand the bounding box to include it, if necessary
        _glyphBoundingBox.union(point.x, point.y);
    }

    public void setFontBoundingBox(RectF bounds) {
        _fontBoundingBox = bounds;
    }

    public RectF getGlyphBoundingBox() {
        return _glyphBoundingBox;
    }
    public RectF getFontBoundingBox() {
        return _fontBoundingBox;
    }
    public float getGlyphBaselineRatio() { return (_glyphBaseLine - _glyphBoundingBox.top) / _glyphBoundingBox.height(); }
    public float getFontBaselineRatio() { return (_fontBaseLine - _fontBoundingBox.top) / _fontBoundingBox.height(); }

    public int size() {return _strokeInfoList.size(); }
    public Stroke getStroke(int index) {
        return  _strokeInfoList.get(index).stroke();
    }

    /**
     * Each stroke in the glyph is normalized relative to the glyph bounding box upper left
     * corner. The stroke itself is also time normalized so that each point in a stroke segment
     * is relative to the previous point (delta) - simplifies animation code
     */
    public void normalizeGlyph() {

        float normalX = _glyphBoundingBox.left;
        float normalY = _glyphBoundingBox.top;

        // First normalize each stroke relative to the glyph bound box origin.  This allows
        // us to replay relative to the upper left corner of a bounding region.
        //
        for (int i1 = 0 ; i1 < _strokeInfoList.size() ; i1++) {

            long  cTime = _strokeInfoList.get(i1).time();

            _strokeInfoList.get(i1).stroke().normalizeStroke(normalX, normalY, cTime);
        }

        // Normalize the timeline of the StrokeSet itself.  The start of each stroke is
        // made relative to the start of the entire glyph itself.
        //
        long baseTime = _strokeInfoList.get(0).time();

        for (int i1 = 1 ; i1 < _strokeInfoList.size() ; i1++) {

            _strokeInfoList.get(i1).normalizeTime(baseTime);
        }

        // This is Important: record the ratio of the bounding box above the baseline to the height of the
        // bounding box itself. This allows us to replay a glyph drawn relative to a given baseline
        // by re-positioning any bounding region vertically relative to a new baseline by the same
        // ratio.
        //
        _fontBaseLine   = _glyphBaseLine - _fontBoundingBox.top;
        _glyphBaseLine -= _glyphBoundingBox.top;

        // Normalize the bounding region itself.
        //
        _glyphBoundingBox.right  -= _glyphBoundingBox.left;
        _glyphBoundingBox.bottom -= _glyphBoundingBox.top;
        _glyphBoundingBox.left = 0;
        _glyphBoundingBox.top  = 0;

    }



    public class StrokeInfo {

        private Stroke _stroke;
        private Long   _time;
        private Long   _duration;

        StrokeInfo(Stroke newStroke, long time) {
            _stroke = newStroke;
            _time   = time;
        }

        public void setDuration(long duration) {
            _duration = duration;
        }

        public Stroke stroke() {
            return _stroke;
        }
        public long time() {
            return _time;
        }
        public void normalizeTime(long baseTime) {
            _time = _time - baseTime;
        }
        public long duration() {
            return _duration;
        }
    }

    public Iterator<StrokeInfo> iterator() {
        return _strokeInfoList.iterator();
    }




    public boolean serializeGlyph() {

        JsonWriter writer = new JsonWriter(new StringWriter());

        try {
            writer.setIndent("  ");

            writer.beginObject();
            writer.name("glyphstrokes");
            writeStrokeInfo(writer, _strokeInfoList);

            writeNamedRectF(writer, "glyphBounds", _glyphBoundingBox);
            writeNamedRectF(writer, "fontBounds", _fontBoundingBox);
            writer.name("glyphBaseLine").value(_glyphBaseLine);
            writer.name("fontBaseLine").value(_fontBaseLine);
            writer.endObject();

            writer.close();
        }
        catch(Exception e) {

        }

        Log.i(TAG, "GlyphData" + writer.toString());

        return true;
    }

    public void writeStrokeInfo(JsonWriter writer, ArrayList<StrokeInfo> strokes) throws IOException {

        writer.beginArray();

        for (StrokeInfo stroke : strokes) {
            writer.beginObject();
            writer.name("stroke");
            writeStroke(writer, stroke._stroke);
            writer.name("time").value(stroke._time);
            writer.name("duration").value(stroke._duration);
            writer.endObject();
        }
        writer.endArray();
    }

    public void writeStroke(JsonWriter writer, Stroke stroke) throws IOException {

        writer.beginArray();

        for (Stroke.StrokePoint point : stroke.getPoints()) {
            writer.beginObject();
            writer.name("point");
            writePoint(writer, point.getPoint());
            writer.name("time").value(point.getTime());
            writer.endObject();
        }
        writer.endArray();
    }

    public void writePoint(JsonWriter writer, PointF point) throws IOException {

        writer.beginArray();

        writer.value(point.x);
        writer.value(point.y);

        writer.endArray();
    }

    public void writeNamedRectF(JsonWriter writer, String name, RectF rect) throws IOException {

        writer.name(name);

        writer.beginArray();

        writer.value(rect.left);
        writer.value(rect.top);
        writer.value(rect.right);
        writer.value(rect.bottom);

        writer.endArray();
    }

}