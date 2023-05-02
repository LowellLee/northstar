package org.dromara.northstar.data.jdbc;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.ds.MarketDataServiceImpl;
import org.dromara.northstar.data.jdbc.entity.BarDO;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
public class MarketDataRepoAdapter implements IMarketDataRepository{

	private MarketDataServiceImpl dataServiceDelegate;
	
	private MarketDataRepository delegate;
	
	public MarketDataRepoAdapter(MarketDataRepository delegate, MarketDataServiceImpl dataServiceDelegate) {
		this.delegate = delegate;
		this.dataServiceDelegate = dataServiceDelegate;
	}
	
	@Override
	public void insert(BarField bar) {
		delegate.save(BarDO.convertFrom(bar));
	}

	@Override
	public List<BarField> loadBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate0) {
		log.debug("加载 [{}] 历史行情数据：{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate0.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.isAfter(endDate0) ? endDate0 : today;
		LinkedList<BarField> resultList = new LinkedList<>();
		if(endDate.isAfter(startDate)) {
			List<BarField> list = dataServiceDelegate.loadBars(unifiedSymbol, startDate, endDate)
					.stream()
					.sorted((a, b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1)
					.toList();
			resultList.addAll(list);
		}
		if(today.isAfter(endDate0))	return resultList; 
		
		LocalDate localQueryDate = today;
		if(resultList.isEmpty()) {
			while(resultList.isEmpty() && endDate0.isAfter(localQueryDate)) {
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
				localQueryDate = localQueryDate.plusDays(1);
			}
		} else {			
			if(resultList.peekLast().getTradingDay().equals(today.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					|| today.getDayOfWeek().getValue() > 5) {
				do {					
					localQueryDate = localQueryDate.plusDays(1);
				} while(localQueryDate.getDayOfWeek().getValue() > 5);
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
			} else {
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
			}
		}
		
		return resultList;
	}
	
	private List<BarField> findBarData(LocalDate date, String unifiedSymbol){
		String tradingDay = date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		log.debug("加载 [{}] 本地行情数据：{}", unifiedSymbol, tradingDay);
		return delegate.findByUnifiedSymbolAndTradingDay(unifiedSymbol, tradingDay)
				.stream()
				.map(BarDO::convertTo)
				.filter(Objects::nonNull)
				.toList();
	}
	
	@Override
	public List<BarField> loadDailyBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return dataServiceDelegate.loadDailyBars(unifiedSymbol, startDate, endDate);
	}

	@Override
	public List<LocalDate> findHodidayInLaw(String gatewayType, int year) {
		return dataServiceDelegate.findHodidayInLaw(gatewayType, year);
	}

}
