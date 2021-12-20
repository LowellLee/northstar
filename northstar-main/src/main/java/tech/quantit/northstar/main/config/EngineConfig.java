package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.event.StrategyEventBus;
import tech.quantit.northstar.main.engine.event.DisruptorFastEventEngine;
import tech.quantit.northstar.main.engine.event.DisruptorFastEventEngine.WaitStrategyEnum;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;

/**
 * 引擎配置
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class EngineConfig {

	@Bean
	public SocketIOMessageEngine createMessageEngine(SocketIOServer server) {
		log.debug("创建SocketIOMessageEngine");
		return new SocketIOMessageEngine(server);
	}
	
	@Bean
	public FastEventEngine createEventEngine() {
		log.debug("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	public InternalEventBus createInternalEventBus() {
		log.debug("创建InternalEventBus");
		return new InternalEventBus();
	}
	
	@Bean
	public StrategyEventBus createStrategyEventBus() {
		log.debug("创建StrategyEventBus");
		return new StrategyEventBus();
	}

}
