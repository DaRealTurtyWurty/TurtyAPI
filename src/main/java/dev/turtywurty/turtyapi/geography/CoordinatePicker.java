package dev.turtywurty.turtyapi.geography;

import dev.turtywurty.turtyapi.TurtyAPI;
import kotlin.Pair;
import lombok.Getter;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class CoordinatePicker {
    public static final CoordinatePicker INSTANCE = new CoordinatePicker();

    private final URL shapeFileURL;
    private final ShapefileDataStore dataStore;
    private final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
    private final String geometryAttributeName;
    private final ResourceInfo resourceInfo;
    private final CoordinateReferenceSystem crs;
    private final Hints hints;
    private final FilterFactory filterFactory;
    private final GeometryFactory geometryFactory;

    private CoordinatePicker() {
        this.shapeFileURL = TurtyAPI.getResourceAsURL("geography/world/world.shp");
        this.dataStore = new ShapefileDataStore(this.shapeFileURL);

        try {
            this.featureSource = this.dataStore.getFeatureSource();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to get feature source from shape file!", exception);
        }

        this.geometryAttributeName = this.featureSource.getSchema().getGeometryDescriptor().getLocalName();

        GeoTools.init();

        this.resourceInfo = this.featureSource.getInfo();
        this.crs = this.resourceInfo.getCRS();
        this.hints = GeoTools.getDefaultHints();
        this.hints.put(Hints.JTS_SRID, 4326);

        this.filterFactory = CommonFactoryFinder.getFilterFactory(this.hints);
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory(this.hints);
    }

    public Coordinate findRandomLandCoordinate(int maxAttempts) {
        Random random = ThreadLocalRandom.current();
        try {
            double latitude = -90 + (180 * random.nextDouble());
            latitude = Math.round(latitude * 1e6) / 1e6; // Round to 6 decimal places

            // Generate random longitude between -180 and 180 with 6 decimal places
            double longitude = -180 + (360 * random.nextDouble());
            longitude = Math.round(longitude * 1e6) / 1e6; // Round to 6 decimal places

            var coordinate = new Coordinate(latitude, longitude);
            Point point = this.geometryFactory.createPoint(coordinate);

            Intersects filter = this.filterFactory.intersects(
                    this.filterFactory.property(this.geometryAttributeName),
                    this.filterFactory.literal(point));

            FeatureCollection<SimpleFeatureType, SimpleFeature> features = this.featureSource.getFeatures(filter);
            int attempts = 0;
            while (features.isEmpty() && attempts++ < maxAttempts) {
                latitude = -90 + (180 * random.nextDouble());
                latitude = Math.round(latitude * 1e6) / 1e6; // Round to 6 decimal places

                // Generate random longitude between -180 and 180 with 6 decimal places
                longitude = -180 + (360 * random.nextDouble());
                longitude = Math.round(longitude * 1e6) / 1e6; // Round to 6 decimal places

                coordinate = new Coordinate(latitude, longitude);
                point = this.geometryFactory.createPoint(coordinate);

                filter = this.filterFactory.intersects(
                        this.filterFactory.property(this.geometryAttributeName),
                        this.filterFactory.literal(point));

                features = this.featureSource.getFeatures(filter);
            }

            return coordinate;
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public Coordinate findRandomLandCoordinate() {
        return this.findRandomLandCoordinate(25_000);
    }
}
