package com.signomix.core.adapter.in;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.proprietary.AccountTypesIface;
import com.signomix.common.proprietary.ExtensionConfig;
import com.signomix.proprietary.AccountTypes;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@InboundAdapter
@Path("/api/core/info")
public class SystemInfoRestAdapter {
    private static final Logger LOG = Logger.getLogger(DeviceRestAdapter.class);

    /*
     * @ConfigProperty(name = "signomix.release")
     * String webappReleaseNumber;
     */

    @ConfigProperty(name = "signomix.release.version")
    String releaseNumber;

    @ConfigProperty(name = "signomix.webapp.version")
    String webappVersion;

    Long defaultOrganizationId = null;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    private long getDefaultOrganizationId() {
        if (defaultOrganizationId == null) {
            if ("postgresql".equals(databaseType)) {
                defaultOrganizationId = 1L;
            } else if ("h2".equals(databaseType)) {
                defaultOrganizationId = -1L;
            } else {
                LOG.error("Unknown database type: " + databaseType);
            }
        }
        return defaultOrganizationId;
    }

    @GET
    public Response getPlatformInfo() {
        AccountTypes accountTypes = new AccountTypes();
        HashMap<String, Object> info = new HashMap<>();
        // info.put("release", releaseNumber);
        info.put("defaultOrganizationId", getDefaultOrganizationId());
        info.put("webappRelease", getWebappVersion());
        info.put("platformRelease", getPlatformVersion());
        info.put("paidVersionAvailable", getPaidVersionAvailable());
        // info.put("platformRelease", getPlatformReleaseNumber());
        HashMap<String, Double> pricesMonthlyPLN = new HashMap<>();
        pricesMonthlyPLN.put("t4", accountTypes.getMonthlyPrice(4, "pln"));
        pricesMonthlyPLN.put("t0", accountTypes.getMonthlyPrice(0, "pln"));
        pricesMonthlyPLN.put("t5", accountTypes.getMonthlyPrice(5, "pln"));
        HashMap<String, Double> pricesYearlyPLN = new HashMap<>();
        pricesYearlyPLN.put("t4", accountTypes.getYearlyPrice(4, "pln"));
        pricesYearlyPLN.put("t0", accountTypes.getYearlyPrice(0, "pln"));
        pricesYearlyPLN.put("t5", accountTypes.getYearlyPrice(5, "pln"));
        info.put("pricesMonthlyPLN", pricesMonthlyPLN);
        info.put("pricesYearlyPLN", pricesYearlyPLN);
        info.put("memoryUnit", "MB");
        info.put("diskUnit", "GB");

        // get memory info
        // convert to MB
        long mb = 1024 * 1024;
        long gb = 1024 * mb;
        Runtime runtime = Runtime.getRuntime();

        // maximal memory JVM can use
        long maxMemory = runtime.maxMemory();
        info.put("maxMemory", maxMemory / mb);
        // total memory currently reserved by JVM
        long totalMemory = runtime.totalMemory();
        info.put("totalMemory", totalMemory / mb);
        // free memory within already reserved pool
        long freeMemory = runtime.freeMemory();
        info.put("freeMemory", freeMemory / mb);
        // memory currently used by JVM
        long usedMemory = totalMemory - freeMemory;
        info.put("usedMemory", usedMemory / mb);
        // memory potentially allocatable
        long availableMemory = maxMemory - usedMemory;
        info.put("availableMemory", availableMemory / mb);
        // get disk space info
        try {
            // Get the FileStore for the current path ("."),
            // which in a container refers to its file system.
            FileStore store = Files.getFileStore(Paths.get("."));
            // Available free space for a regular user
            long usableSpace = store.getUsableSpace();
            info.put("diskUsableSpace", usableSpace / gb);
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return Response.ok().entity(info).build();
    }

    /**
     * Get release number of webapp
     * 
     * @return
     */
    private String getWebappVersion() {
        return webappVersion;
    }

    private String getPlatformVersion() {
        return releaseNumber;
    }

    private boolean getPaidVersionAvailable() {
        try {
            AccountTypesIface at = (AccountTypesIface) ExtensionConfig
                    .getExtension("com.signomix.proprietary.AccountTypes");
            return at.paidVersionAvailable();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error getting paid version availability", e);
            return false;
        }
    }

    /**
     * Bulid hash number composed of release numbers of all platform services.
     * 
     * @return compressed release number
     */
    private String getPlatformReleaseNumber() {
        byte[] bytes1 = new byte[3];
        String[] parts = "1.0.0".split("\\.");
        for (int i = 0; i < 3; i++) {
            bytes1[i] = Byte.parseByte(parts[i]);
        }
        return Base64.getEncoder().encodeToString(bytes1);
    }

}
