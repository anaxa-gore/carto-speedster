package org.axana.cartospeedster;

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

    static WMSParams decodeParameters(HttpServletRequest req) {
        final WMSParams result = new WMSParams();

        result.layers = Arrays.asList(req.getParameter("LAYERS").split(","));
        result.width = Integer.valueOf(req.getParameter("WIDTH"));
        result.height = Integer.valueOf(req.getParameter("HEIGHT"));

        return result;
    }
}
