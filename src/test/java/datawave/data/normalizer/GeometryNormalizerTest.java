package datawave.data.normalizer;

import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.geowave.core.geotime.util.GeometryUtils;
import org.locationtech.geowave.core.index.ByteArrayRange;
import org.locationtech.geowave.core.index.numeric.MultiDimensionalNumericData;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeometryNormalizerTest {
    
    private GeometryNormalizer normalizer = null;
    
    @BeforeEach
    public void setup() {
        normalizer = new GeometryNormalizer();
    }
    
    @Test
    public void testPoint() {
        Geometry point = new GeometryFactory().createPoint(new Coordinate(10, 10));
        List<String> insertionIds = new ArrayList<>(normalizer.expand(new WKTWriter().write(point)));
        assertEquals(1, insertionIds.size());
        assertEquals("1f200a80a80a80a80a", insertionIds.get(0));
    }
    
    @Test
    public void testLine() {
        Geometry line = new GeometryFactory().createLineString(new Coordinate[] {new Coordinate(-10, -10), new Coordinate(0, 0), new Coordinate(10, 20)});
        List<String> insertionIds = new ArrayList<>(normalizer.expand(new WKTWriter().write(line)));
        Collections.sort(insertionIds);
        assertEquals(4, insertionIds.size());
        assertEquals("042a", insertionIds.get(0));
        assertEquals("047f", insertionIds.get(1));
        assertEquals("0480", insertionIds.get(2));
        assertEquals("04d5", insertionIds.get(3));
    }
    
    @Test
    public void testPolygon() {
        Geometry polygon = new GeometryFactory().createPolygon(new Coordinate[] {new Coordinate(-10, -10), new Coordinate(10, -10), new Coordinate(10, 10),
                new Coordinate(-10, 10), new Coordinate(-10, -10)});
        List<String> insertionIds = new ArrayList<>(normalizer.expand(new WKTWriter().write(polygon)));
        assertEquals(4, insertionIds.size());
        assertEquals("0500aa", insertionIds.get(0));
        assertEquals("0501ff", insertionIds.get(1));
        assertEquals("050200", insertionIds.get(2));
        assertEquals("050355", insertionIds.get(3));
    }
    
    @Test
    public void testWKTPoint() {
        Geometry geom = AbstractGeometryNormalizer.parseGeometry("POINT(10 20)");
        assertEquals(10.0, geom.getGeometryN(0).getCoordinate().x, 0.0);
        assertEquals(20.0, geom.getGeometryN(0).getCoordinate().y, 0.0);
        
        List<String> insertionIds = new ArrayList<>(normalizer.expand(new WKTWriter().write(geom)));
        assertEquals(1, insertionIds.size());
        assertEquals("1f20306ba4306ba430", insertionIds.get(0));
    }
    
    @Test
    public void testWKTPointz() {
        Geometry geom = AbstractGeometryNormalizer.parseGeometry("POINT Z(10 20 30)");
        assertEquals(10.0, geom.getGeometryN(0).getCoordinate().x, 0.0);
        assertEquals(20.0, geom.getGeometryN(0).getCoordinate().y, 0.0);
        assertEquals(30.0, geom.getGeometryN(0).getCoordinate().z, 0.0);
        
        List<String> insertionIds = new ArrayList<>(normalizer.expand(new WKTWriter().write(geom)));
        assertEquals(1, insertionIds.size());
        assertEquals("1f20306ba4306ba430", insertionIds.get(0));
    }
    
    @Test
    public void testQueryRanges() throws Exception {
        Geometry polygon = new GeometryFactory().createPolygon(new Coordinate[] {new Coordinate(-10, -10), new Coordinate(10, -10), new Coordinate(10, 10),
                new Coordinate(-10, 10), new Coordinate(-10, -10)});
        
        List<ByteArrayRange> allRanges = new ArrayList<>();
        for (MultiDimensionalNumericData range : GeometryUtils.basicConstraintsFromEnvelope(polygon.getEnvelopeInternal())
                        .getIndexConstraints(GeometryNormalizer.index)) {
            allRanges.addAll(Lists.reverse(GeometryNormalizer.indexStrategy.getQueryRanges(range).getCompositeQueryRanges()));
        }
        
        assertEquals(3746, allRanges.size());
        
        StringBuffer result = new StringBuffer();
        for (ByteArrayRange range : allRanges) {
            result.append(Hex.encodeHexString(range.getStart()));
            result.append(Hex.encodeHexString(range.getEnd()));
        }
        
        String expected = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("datawave/data/normalizer/geoRanges.txt"), "UTF8");
        
        assertEquals(expected, result.toString());
    }
}
