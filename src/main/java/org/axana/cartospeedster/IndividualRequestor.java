package org.axana.cartospeedster;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;

/**
 * Created by tbonavia on 01/09/2016.
 */
public class IndividualRequestor implements Callable<Response> {
    private Invocation.Builder toExec;

    public IndividualRequestor(Invocation.Builder toExec) {
        this.toExec = toExec;
    }

    @Override
    public Response call() throws Exception {
        return this.toExec.get();
    }
}
