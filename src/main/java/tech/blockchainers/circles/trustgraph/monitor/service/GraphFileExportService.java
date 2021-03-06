package tech.blockchainers.circles.trustgraph.monitor.service;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tech.blockchainers.circles.trustgraph.user.api.Data;
import tech.blockchainers.circles.trustgraph.user.service.EnrichmentService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@Profile("import")
public class GraphFileExportService implements GraphService {

    private final File output = new File("trustgraph_test.csv");
    private FileOutputStream outputStream;
    private Map<String, Data> userDataCache = Maps.newHashMap();

    private final EnrichmentService enrichmentService;

    public GraphFileExportService(EnrichmentService enrichmentService) throws FileNotFoundException {
        this.enrichmentService = enrichmentService;
        outputStream = new FileOutputStream(output);
        try {
            outputStream.write(("blockNumber,truster,truster_name,truster_image_url,trustee,trustee_name,trustee_image_url,amount\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // bla
        }
    }

    @Override
    public void addTrustGraph(String truster, String trustee, BigInteger amount, BigInteger blockNumber) {
        Data trusterDto = userDataCache.containsKey(truster) ? userDataCache.get(truster) : (
                !enrichmentService.enrichUserAddress(truster).getData().isEmpty() ?
                enrichmentService.enrichUserAddress(truster).getData().get(0) : Data.builder().safeAddress(truster).build());
        Data trusteeDto = userDataCache.containsKey(trustee) ? userDataCache.get(trustee) : (
                !enrichmentService.enrichUserAddress(trustee).getData().isEmpty() ?
                enrichmentService.enrichUserAddress(trustee).getData().get(0) : Data.builder().safeAddress(trustee).build());
        userDataCache.put(truster, trusterDto);
        userDataCache.put(trustee, trusteeDto);
        try {
            outputStream.write(String.join(",",
                    new String[]{
                            String.valueOf(blockNumber.intValueExact()),
                            trusterDto.getSafeAddress(), trusterDto.getUsername(), trusterDto.getAvatarUrl(),
                            trusteeDto.getSafeAddress(), trusteeDto.getUsername(), trusteeDto.getAvatarUrl(),
                            String.valueOf(amount.intValueExact()) + "\n"}).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Cannot write to file.", ex);
        }
        log.debug("Created {}, {}, {}", blockNumber, truster, trustee);
    }

    public void close() throws IOException {
        outputStream.close();
    }
}
