package tech.blockchainers.circles.trustgraph.user.api;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Keys;
import tech.blockchainers.circles.trustgraph.monitor.service.ContractEventListenerService;
import tech.blockchainers.circles.trustgraph.user.model.User;
import tech.blockchainers.circles.trustgraph.user.service.EnrichmentService;
import tech.blockchainers.circles.trustgraph.user.service.UserService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
class TrustGraphController {

	private final UserService userService;
	private final EnrichmentService enrichmentService;
	@Value("${tcb.id}")
	private String tcbId;
	private final ContractEventListenerService contractEventListenerService;

	TrustGraphController(UserService userService, EnrichmentService enrichmentService, ContractEventListenerService contractEventListenerService) {
		this.userService = userService;
		this.enrichmentService = enrichmentService;
		this.contractEventListenerService = contractEventListenerService;
	}

	@GetMapping("/trust/{truster}/{trustee}/{amount}")
	@ApiOperation(value = "Get transtive trust from trustee to truster.",
			notes = "To get a result, a transitive trust graph must exist from truster to trustee with at least the submitted amount.")
	List<User> search(@PathVariable("truster") String truster, @PathVariable("trustee")  String trustee, @PathVariable("amount") BigInteger amount) {
		return userService.findTrustGraph(Keys.toChecksumAddress(truster), Keys.toChecksumAddress(trustee), amount);
	}

	@PostMapping(path = "/trust/{truster}/{trustee}/{amount}/{blockNumber}")
	@ApiOperation(value = "Create a trust connection between truster and trustee with amount at blocknumber. Should be used by the Monitor.",
			notes = "Can only be used with a valid TCB-ID.")
	public ResponseEntity<String> addTrustLine(@RequestHeader("TCB-ID") String tcbId, @PathVariable("truster") String truster, @PathVariable("trustee") String trustee, @PathVariable(value = "amount") Integer amount, @PathVariable(value = "blockNumber") Integer blockNumber) {
		if (!this.tcbId.equals(tcbId)) {
			throw new IllegalArgumentException("Cannot retrieve registrations.");
		}
		String trusterAddress = Keys.toChecksumAddress(truster);
		String trusteeAddress = Keys.toChecksumAddress(trustee);
		Data trusterDto = !enrichmentService.enrichUserAddress(trusterAddress).getData().isEmpty() ?
				enrichmentService.enrichUserAddress(trusterAddress).getData().get(0) : Data.builder().safeAddress(trusterAddress).build();
		Data trusteeDto = !enrichmentService.enrichUserAddress(trusteeAddress).getData().isEmpty() ?
				enrichmentService.enrichUserAddress(trusteeAddress).getData().get(0) : Data.builder().safeAddress(trusteeAddress).build();;
		userService.addTrustLine(trusterDto.getSafeAddress(), trusterDto.getUsername(), trusterDto.getAvatarUrl(),
				trusteeDto.getSafeAddress(), trusteeDto.getUsername(), trusteeDto.getAvatarUrl(), blockNumber, amount);
		log.info("Created at {} trustline {},{}", blockNumber, trusterAddress, trusteeAddress, amount);
		return new ResponseEntity<>("{\"message\": \"" + trusteeAddress + "|" + trusteeAddress + "|" + amount + " created.\"}", HttpStatus.CREATED);
	}

	@GetMapping("/stats")
	@ApiOperation(value = "Return latestBlock (which is currently processed) and currentBlock (of the xDai Mainnet).")
	public Map<String, String> getStats() throws IOException {
		Map<String, String> stats = Maps.newHashMap();
		stats.put("latestBlock", contractEventListenerService.getLatestBlock());
		stats.put("currentBlock", contractEventListenerService.getCurrentBlock());
		return stats;
	}

}
