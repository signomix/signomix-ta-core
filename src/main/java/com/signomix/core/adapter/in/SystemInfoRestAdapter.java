package com.signomix.core.adapter.in;

import java.util.Base64;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.annotation.InboundAdapter;

@InboundAdapter
@Path("/api/core/info")
public class SystemInfoRestAdapter {
    private static final Logger LOG = Logger.getLogger(DeviceRestAdapter.class);

    @ConfigProperty(name = "signomix.release")
    String webappReleaseNumber;
    @ConfigProperty(name = "signomix.organization.default")
    String defaultOrganizationId;

    @GET
    public Response getPlatformInfo() {
        HashMap<String,Object> info=new HashMap<>();
        //info.put("release", releaseNumber);
        info.put("defaultOrganizationId", defaultOrganizationId);
        info.put("webappRelease", getWebappReleaseNumber());
        //info.put("platformRelease", getPlatformReleaseNumber());
        return Response.ok().entity(info).build();
    }

    /**
     * Get release number of webapp
     * @return
     */
    private String getWebappReleaseNumber(){
        return "";
    }

    /**
     * Bulid hash number composed of release numbers of all platform services.
     * @return compressed release number
     */
    private String getPlatformReleaseNumber(){
        byte[] bytes1 = new byte[3];
        String[] parts = "1.0.0".split("\\.");
        for (int i = 0; i < 3; i++) {
            bytes1[i] = Byte.parseByte(parts[i]);
        }
        return Base64.getEncoder().encodeToString(bytes1);
    }



}
