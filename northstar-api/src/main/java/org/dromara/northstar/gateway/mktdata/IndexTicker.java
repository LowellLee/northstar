package org.dromara.northstar.gateway.mktdata;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.contract.IndexContract;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexTicker {

	private IndexContract idxContract;
	
	private Consumer<Tick> onTickCallback;
	
	private static final long PARA_THRESHOLD = 2;
	
	private final Set<Contract> memberContracts;
	
	private ConcurrentHashMap<Contract, Tick> tickMap = new ConcurrentHashMap<>(20);
	private ConcurrentHashMap<Contract, Double> weightedMap = new ConcurrentHashMap<>(20);
	
	private long lastTickTimestamp = -1;
	
	private double lastPrice;
	private double highPrice;
	private double lowPrice;
	private double openPrice;
	private long totalVolume;
	private long totalVolumeDelta;
	private double totalOpenInterest;
	private double totalOpenInterestDelta;
	private double totalTurnover;
	private double totalTurnoverDelta;
	private double preClose;
	private double preOpenInterest;
	private double preSettlePrice;
	private double settlePrice;
	
	public IndexTicker(IndexContract idxContract, Consumer<Tick> onTickCallback) {
		this.idxContract = idxContract;
		this.memberContracts = idxContract.memberContracts().stream().map(c -> c.contract()).collect(Collectors.toSet());
		this.onTickCallback = onTickCallback;
	}
	
	private boolean isReady() {
		return (double) tickMap.size() / memberContracts.size() > 0.9;
	}

	public synchronized void update(Tick tick) {
		if(!memberContracts.contains(tick.contract())) {
			log.warn("[{}]指数TICK生成器，无法处理 [{}] 的行情数据", idxContract.contract().unifiedSymbol(), tick.contract().unifiedSymbol());
			return;
		}
		// 如果有过期的TICK数据(例如不活跃的合约),则并入下个K线
		if (0 < lastTickTimestamp && lastTickTimestamp < tick.actionTimestamp()) {
				if(isReady()) {					
					final Double zeroD = Constants.ZERO_D;
					final Integer zero = Constants.ZERO;
					//进行运算
					calculate();
					onTickCallback.accept(Tick.builder()
							.gatewayId(tick.gatewayId())
							.contract(idxContract.contract())
							.actionDay(tick.actionDay())
							.actionTime(tick.actionTime())
							.tradingDay(tick.tradingDay())
							.actionTimestamp(lastTickTimestamp)
							.openPrice(openPrice)
							.highPrice(highPrice)
							.lowPrice(lowPrice)
							.lastPrice(lastPrice)
							.openInterest(totalOpenInterest)
							.openInterestDelta(totalOpenInterestDelta)
							.volume(totalVolume)
							.volumeDelta(totalVolumeDelta)
							.turnover(totalTurnover)
							.turnoverDelta(totalTurnoverDelta)
							.preClosePrice(preClose)
							.preOpenInterest(preOpenInterest)
							.preSettlePrice(preSettlePrice)
							.settlePrice(settlePrice)
							.askPrice(List.of(zeroD,zeroD,zeroD,zeroD,zeroD))
							.bidPrice(List.of(zeroD,zeroD,zeroD,zeroD,zeroD))
							.askVolume(List.of(zero,zero,zero,zero,zero))
							.bidVolume(List.of(zero,zero,zero,zero,zero))
							.type(tick.type())
							.channelType(tick.channelType())
							.build());
				} else {
					log.debug("{} 因月份数据不足，未达到指数合成条件，忽略指数TICK合成计算", idxContract.name());
				}
		}
		if(tick.type() == TickType.MARKET_TICK) {			
			lastTickTimestamp = tick.actionTimestamp();
		}
		// 同一个指数Tick
		tickMap.compute(tick.contract(), (k, v) -> tick);
	}
	
	private void calculate() {
		// 合计持仓量
		totalOpenInterest = tickMap.reduceValuesToDouble(PARA_THRESHOLD, Tick::openInterest, 0, (a, b) -> a + b);
		totalOpenInterestDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, Tick::openInterestDelta, 0, (a, b) -> a + b);
		// 合约权值计算
		tickMap.forEachEntry(PARA_THRESHOLD, e -> weightedMap.compute(e.getKey(), (k,v) -> e.getValue().openInterest() * 1.0 / totalOpenInterest));
		
		// 合计成交量
		totalVolume = tickMap.reduceValuesToLong(PARA_THRESHOLD, Tick::volume, 0, (a, b) -> a + b);
		totalVolumeDelta = tickMap.reduceValuesToLong(PARA_THRESHOLD, Tick::volumeDelta, 0, (a, b) -> a + b);
		// 合计成交额
		totalTurnover = tickMap.reduceValuesToDouble(PARA_THRESHOLD, Tick::turnover, 0, (a, b) -> a + b);
		totalTurnoverDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, Tick::turnoverDelta, 0, (a, b) -> a + b);
		
		//加权均价
		double rawWeightedLastPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).lastPrice() * e.getValue());
		double weightedLastPrice = roundWithPriceTick(rawWeightedLastPrice);	//通过最小变动价位校准的加权均价
		lastPrice = weightedLastPrice;
		
		//加权最高价
		double rawWeightedHighPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).highPrice() * e.getValue());
		double weightedHighPrice = roundWithPriceTick(rawWeightedHighPrice);	//通过最小变动价位校准的加权均价
		highPrice = weightedHighPrice;
		
		//加权最低价
		double rawWeightedLowPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).lowPrice() * e.getValue());
		double weightedLowPrice = roundWithPriceTick(rawWeightedLowPrice);		//通过最小变动价位校准的加权均价
		lowPrice = weightedLowPrice;
		
		//加权开盘价
		double rawWeightedOpenPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).openPrice() * e.getValue());
		double weightedOpenPrice = roundWithPriceTick(rawWeightedOpenPrice);	//通过最小变动价位校准的加权均价
		openPrice = weightedOpenPrice;
		
		//加权前收盘价
		double rawWeightedPreClose = computeWeightedValue(e -> tickMap.get(e.getKey()).preClosePrice() * e.getValue());
		double weightedPreClose = roundWithPriceTick(rawWeightedPreClose);
		preClose = weightedPreClose;
		
		//加权前持仓量
		double rawWeightedPreOpenInterest = computeWeightedValue(e -> tickMap.get(e.getKey()).preOpenInterest() * e.getValue());
		double weightedPreOpenInterest = roundWithPriceTick(rawWeightedPreOpenInterest);
		preOpenInterest = weightedPreOpenInterest;
		
		//加权前结算价
		double rawWeightedPreSettle = computeWeightedValue(e -> tickMap.get(e.getKey()).preSettlePrice() * e.getValue());
		double weightedPreSettle = roundWithPriceTick(rawWeightedPreSettle);
		preSettlePrice = weightedPreSettle;
		
		//加权结算价
		double rawWeightedSettle = computeWeightedValue(e -> tickMap.get(e.getKey()).settlePrice() * e.getValue());
		double weightedSettle = roundWithPriceTick(rawWeightedSettle);
		settlePrice = weightedSettle;
	}
	
	private double computeWeightedValue(ToDoubleFunction<Entry<Contract, Double>> transformer) {
		return weightedMap.reduceEntriesToDouble(
				PARA_THRESHOLD,
				transformer,
				0, 
				(a, b) -> a + b);
	}

	//四舍五入处理
	private double roundWithPriceTick(double weightedPrice) {
		int enlargePrice = (int) (weightedPrice * 1000);
		int enlargePriceTick = (int) (idxContract.contract().priceTick() * 1000);
		int numOfTicks = enlargePrice / enlargePriceTick;
		int tickCarry = (enlargePrice % enlargePriceTick) < (enlargePriceTick / 2) ? 0 : 1;
		  
		return  enlargePriceTick * (numOfTicks + tickCarry) * 1.0 / 1000;
	}
}
