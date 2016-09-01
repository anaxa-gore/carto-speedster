package org.axana.cartospeedster;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.AsyncResponse;
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
}