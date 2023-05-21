package org.dromara.northstar.data;

import java.time.LocalDate;
import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 行情数据持久化
 * @author KevinHuangwl
 *
 */
public interface IMarketDataRepository {

	/**
	 * 保存数据
	 * @param bar
	 */
	void insert(BarField bar);
	
	/**
	 * 加载历史行情分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> loadBars(ContractField contract, LocalDate startDate, LocalDate endDate);
	
	
	/**
	 * 加载历史行情日K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> loadDailyBars(ContractField contract, LocalDate startDate, LocalDate endDate);
	
}
