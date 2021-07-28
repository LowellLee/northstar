package tech.xuanwu.northstar.strategy.common.constants;

/**
 * 策略模组状态
 * @author KevinHuangwl
 *
 */
public enum ModuleState {

	/**
	 * 空仓
	 */
	EMPTY,
	/**
	 * 持多仓
	 */
	HOLDING_LONG,
	/**
	 * 持空仓
	 */
	HOLDING_SHORT,
	/**
	 * 下单中
	 */
	PLACING_ORDER,
	/**
	 * 等待订单反馈
	 */
	PENDING_ORDER,
	/**
	 * 撤单中
	 */
	RETRIEVING_ORDER;
}
