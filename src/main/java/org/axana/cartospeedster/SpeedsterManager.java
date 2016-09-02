package org.axana.cartospeedster;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.BoundingBox;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.AsyncResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tbonavia on 30/08/2016.
 */
public class SpeedsterManager {
    private static SpeedsterManager INSTANCE;
    private final ExecutorService ex = Executors.newCachedThreadPool();

    static final String WMS_URL = "http://demo.boundlessgeo.com/geoserver/wms";

    final Map<String, ReferencedEnvelope> LAYERS_BOUNDS = Collections.synchronizedMap(new HashMap<>());

    private SpeedsterManager() throws MalformedURLException {
        synchronized (LAYERS_BOUNDS){
            LAYERS_BOUNDS.putAll(WMSInfosLoader.getWMSInfos(new URL(WMS_URL)));
        }
    }

    /**
     * TODO TBA : More efficient solution to implement...
     *
     * @return
     */
    synchronized static SpeedsterManager getInstance() throws MalformedURLException {
        if (INSTANCE == null)
            INSTANCE = new SpeedsterManager();

        return INSTANCE;
    }

    void submitRequest(HttpServletRequest request, AsyncResponse asyncResponse) {
        this.ex.submit(new MapBuilder(request, asyncResponse, this));
    }

    boolean isLayerInBounds(String layer, ReferencedEnvelope currentBounds) {
        ReferencedEnvelope layerBounds;

        synchronized (LAYERS_BOUNDS) {
            layerBounds = LAYERS_BOUNDS.get(layer);
        }

        return layerBounds == null || layerBounds.intersects((BoundingBox) currentBounds);

    }
}