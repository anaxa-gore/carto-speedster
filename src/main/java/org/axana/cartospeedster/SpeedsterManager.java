package org.axana.cartospeedster;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Form;
import java.util.Map;

/**
 * Created by tbonavia on 30/08/2016.
 */
public class SpeedsterManager {
    private static SpeedsterManager INSTANCE;

    private SpeedsterManager() {

    }

    /**
     * TODO TBA : More efficient solution to implement...
     *
     * @return
     */
    public synchronized static SpeedsterManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new SpeedsterManager();

        return INSTANCE;
    }

    public void submitRequest(HttpServletRequest request, AsyncResponse asyncResponse) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://demo.boundlessgeo.com");
        WebTarget path = target.path("geoserver/wms");

        Map<String, String[]> parameters = request.getParameterMap();
        for (Map.Entry<String, String[]> param : parameters.entrySet()) {
            path = path.queryParam(param.getKey(), param.getValue());
        }

        asyncResponse.resume(path.request().get());
    }
}
