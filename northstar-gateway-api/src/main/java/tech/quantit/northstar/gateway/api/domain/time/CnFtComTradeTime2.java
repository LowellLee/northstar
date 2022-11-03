package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;

/**
 * 国内商品期货二类品种交易时间（有夜盘，1:00收盘）
 * @author KevinHuangwl
 *
 */
public final class CnFtComTradeTime2 implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> getPeriodSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(1, 00)),
				new PeriodSegment(LocalTime.of(9, 1), LocalTime.of(10, 15)),
				new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00))
			);
	}

}
