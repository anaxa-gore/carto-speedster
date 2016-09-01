package org.axana.cartospeedster;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tbonavia on 30/08/2016.
 */
public class SpeedsterManager {
    private static SpeedsterManager INSTANCE;
    private final ExecutorService ex = Executors.newCachedThreadPool();

    private SpeedsterManager() {

    }

    /**
     * TODO TBA : More efficient solution to implement...
     *
     * @return
     */
    synchronized static SpeedsterManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new SpeedsterManager();

        return INSTANCE;
    }

    void submitRequest(HttpServletRequest request, AsyncResponse asyncResponse) {
        this.ex.submit(new MapBuilder(request, asyncResponse));
    }

    private String formatValues(String[] values) {
        StringBuilder sb = new StringBuilder();

        for (String value : values) {
            sb.append(value).append(',');
        }

        return sb.toString();
    }
}

class MapBuilder implements Runnable {
    private final WMSParams params;
    private final HttpServletRequest originalRequest;
    private final AsyncResponse response;

    MapBuilder(HttpServletRequest originalRequest, AsyncResponse response) {
        this.params = WMSParams.decodeParameters(originalRequest);
        this.originalRequest = originalRequest;
        this.response = response;
    }

    public void run() {
        // Build n requests, 1 per layer
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://demo.boundlessgeo.com");
        WebTarget path = target.path("geoserver/wms");

        Map<String, Invocation.Builder> requests = new HashMap<>();
        Map<String, String[]> parameters = this.originalRequest.getParameterMap();
        for (String layer : this.params.layers) {
            WebTarget newRequest = path.path("");
            for (Map.Entry<String, String[]> param : parameters.entrySet()) {
                if ("LAYERS".equals(param.getKey())) {
                    newRequest = newRequest.queryParam("LAYERS", layer);
                } else {
                    newRequest = newRequest.queryParam(param.getKey(), (Object[]) param.getValue());
                }
            }
            requests.put(layer, newRequest.request());
        }

        // Send the individual requests to the WMS Server
        Map<String, Response> res = new HashMap<>();
        requests.forEach((layer, builder) -> {
            builder.header("Accept", "image/png");
            res.put(layer, builder.get());
        });

        // Build the resulting image
        BufferedImage resultingImage = new BufferedImage(this.params.width, this.params.height, BufferedImage.TYPE_INT_ARGB);
        Map<String, BufferedImage> images = new HashMap<>();
        res.forEach((layer, result) -> {
            try (final InputStream is = result.readEntity(InputStream.class)) {
                final BufferedImage layerImage = ImageIO.read(is);
                images.put(layer, layerImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        this.params.layers.forEach((layer) -> {
            BufferedImage image = images.get(layer);
            resultingImage.getGraphics().drawImage(image, 0, 0, null);
        });

        // Resume the async response with a custom response
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(resultingImage, "png", baos);
            byte[] imageData = baos.toByteArray();

            this.response.resume(Response.ok(imageData).build());
        } catch (IOException e) {
            e.printStackTrace();
            this.response.cancel();
        }
    }
}

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