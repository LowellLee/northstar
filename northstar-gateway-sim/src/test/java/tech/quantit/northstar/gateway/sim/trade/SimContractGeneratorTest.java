package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimContractGeneratorTest {

	@Test
	void test() {
		SimContractGenerator gen = new SimContractGenerator();
		assertThat(gen.getContract("test")).isNotNull();
	}

}
