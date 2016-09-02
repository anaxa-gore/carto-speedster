package org.axana.cartospeedster;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by tbonavia on 01/09/2016.
 */
public class MapBuilder implements Runnable {
    private final WMSParams params;
    private final HttpServletRequest originalRequest;
    private final AsyncResponse response;
    private SpeedsterManager speedsterManager;

    private final ExecutorService s = Executors.newCachedThreadPool(); // TODO Find the best...

    MapBuilder(HttpServletRequest originalRequest, AsyncResponse response, SpeedsterManager speedsterManager) {
        this.params = WMSParams.decodeParameters(originalRequest);
        this.originalRequest = originalRequest;
        this.response = response;
        this.speedsterManager = speedsterManager;
    }

    public void run() {
        final long start = System.currentTimeMillis();

        // Build n requests, 1 per layer
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SpeedsterManager.WMS_URL);
//        WebTarget target = client.target("http://demo.boundlessgeo.com");
//        WebTarget path = target.path("geoserver/wms");

        List<IndividualRequestor> requests = new ArrayList<>();
        Map<String, String[]> parameters = this.originalRequest.getParameterMap();
        int nbQueries = 0;
        for (String layer : this.params.layers) {
            // Queries are done only on layers that intersect the query envelope
            if(!this.speedsterManager.isLayerInBounds(layer, this.params.queryEnvelope))
                continue;

            nbQueries++;

            WebTarget newRequest = target.path("");
            for (Map.Entry<String, String[]> param : parameters.entrySet()) {
                if ("LAYERS".equals(param.getKey())) {
                    newRequest = newRequest.queryParam("LAYERS", layer);
                } else {
                    newRequest = newRequest.queryParam(param.getKey(), (Object[]) param.getValue());
                }
            }

            final Invocation.Builder req = newRequest.request().header("Accept", "image/png");
            requests.add(new IndividualRequestor(req));
        }

        // Send the individual requests to the WMS Server and Build the resulting image
        BufferedImage resultingImage = new BufferedImage(this.params.width, this.params.height, BufferedImage.TYPE_INT_ARGB);
        long afterRequests = 0;
        try {
            final List<Future<Response>> futures = this.s.invokeAll(requests);

            afterRequests = System.currentTimeMillis();
            System.out.println("Durée des " + nbQueries + " requêtes : " + (afterRequests - start) + " ms");
            for (Future<Response> f : futures) {
                Response result = f.get();
                try (final InputStream is = result.readEntity(InputStream.class)) {
                    final BufferedImage layerImage = ImageIO.read(is);
                    resultingImage.getGraphics().drawImage(layerImage, 0, 0, null);
                }
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }

        final long afterBuildingImage = System.currentTimeMillis();

//        System.out.println("Durée de construction des image : " + (afterBuildingImage - afterRequests) + " ms");

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
