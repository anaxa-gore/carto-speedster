package org.axana.cartospeedster;

import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tbonavia on 02/09/2016.
 */
public class WMSInfosLoader {

    static Map<String, ReferencedEnvelope> getWMSInfos(URL url) {
        final Map<String, ReferencedEnvelope> result = new HashMap<>();
        WebMapServer wms = null;
        try {
            wms = new WebMapServer(url);
            final WMSCapabilities caps = wms.getCapabilities();

            final List<Layer> layers = caps.getLayerList();
            for (Layer layer : layers) {
                final CRSEnvelope bb = layer.getLatLonBoundingBox();
                final ReferencedEnvelope envelope = new ReferencedEnvelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY(), CRS.decode("EPSG:4326"));
                result.put(layer.getName(), envelope);
            }

            return result;
        } catch (SAXException | IOException | FactoryException e) {
            //Unable to parse the response from the server
            //For example, the capabilities it returned was not valid

            //There was an error communicating with the server
            //For example, the server is down

            e.printStackTrace();
            return null;
        }
    }
}
