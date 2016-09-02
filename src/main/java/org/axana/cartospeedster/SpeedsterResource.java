package org.axana.cartospeedster;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;

/**
 * Created by tbonavia on 30/08/2016.
 */
@Path("/speedster")
public class SpeedsterResource {
    private HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getInfo(@Suspended final AsyncResponse asyncResponse){
        try {
            // The request is submitted for further resolution
            SpeedsterManager.getInstance().submitRequest(this.request, asyncResponse);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Context
    public void setQuery(HttpServletRequest request){
        this.request = request;
    }
}