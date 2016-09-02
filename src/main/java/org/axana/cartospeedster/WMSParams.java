package org.axana.cartospeedster;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tbonavia on 01/09/2016.
 */
class WMSParams {
    // An ordered list of layers
    List<String> layers;
    // The resulting image width
    int width;
    // The resulting image height
    int height;
    // The request bounds
    ReferencedEnvelope queryEnvelope;

    static WMSParams decodeParameters(HttpServletRequest req) {
        final WMSParams result = new WMSParams();

        final String layers = req.getParameter("LAYERS") != null ? req.getParameter("LAYERS") : req.getParameter("layers");
        result.layers = Arrays.asList(layers.split(","));
        final String width = req.getParameter("WIDTH") != null ? req.getParameter("WIDTH") : req.getParameter("width");
        result.width = Integer.valueOf(width);
        final String height = req.getParameter("HEIGHT") != null ? req.getParameter("HEIGHT") : req.getParameter("height");
        result.height = Integer.valueOf(height);
        final String envelope = req.getParameter("BBOX") != null ? req.getParameter("BBOX") : req.getParameter("bbox");
        final String crs = req.getParameter("CRS") != null ? req.getParameter("CRS") : req.getParameter("crs");
        result.queryEnvelope = decodeBBOX(envelope, crs);

        return result;
    }

    private static ReferencedEnvelope decodeBBOX(String bbox, String crs) {
        final String[] corners = bbox.split(",");
        if(corners.length != 4)
            throw new IllegalArgumentException("A BBOX must has 4 corners");

        final double minX = Double.parseDouble(corners[0]);
        final double minY = Double.parseDouble(corners[1]);
        final double maxX = Double.parseDouble(corners[2]);
        final double maxY = Double.parseDouble(corners[3]);

        try {
            final CoordinateReferenceSystem queryCRS = CRS.decode(crs);
            final CoordinateReferenceSystem standardCRS = CRS.decode("EPSG:4326");
            final DirectPosition2D lowerCorner = new DirectPosition2D(queryCRS, minX, minY);
            final DirectPosition2D upperCorner = new DirectPosition2D(queryCRS, maxX, maxY);

            MathTransform mathTransform = CRS.findMathTransform(queryCRS, standardCRS, true);
            mathTransform.transform(lowerCorner, lowerCorner);
            mathTransform.transform(upperCorner, upperCorner);

            // Invert X and Y coordinates because EPSG:4326 is Lat/Lon not X/Y !
            return new ReferencedEnvelope(lowerCorner.getY(), upperCorner.getY(), lowerCorner.getX(), upperCorner.getX(), standardCRS);
        } catch (FactoryException | TransformException e) {
            throw new IllegalArgumentException("Unable get envelope for request.", e);
        }
    }
}
