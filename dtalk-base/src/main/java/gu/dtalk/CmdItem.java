package gu.dtalk;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import gu.dtalk.exception.CmdExecutionException;
import gu.dtalk.exception.InteractiveCmdStartException;

import static com.google.common.base.Preconditions.*;

/**
 * 设备命令条目
 * @author guyadong
 *
 */
public class CmdItem extends BaseItem {
	private static final Function<BaseItem, BaseOption<Object>> TO_OPTION = new Function<BaseItem,BaseOption<Object>>(){

		@SuppressWarnings("unchecked")
		@Override
		public BaseOption<Object> apply(BaseItem input) {
			return (BaseOption<Object>) input;
		}};
	private static final Function<BaseOption<?>,BaseItem> TO_ITEM = new Function<BaseOption<?>,BaseItem>(){

		@Override
		public BaseItem apply(BaseOption<?> input) {
			return input;
		}};
	private static final Function<BaseItem, Object> TO_VALUE = new Function<BaseItem, Object>() {
		@Override
		public Object apply(BaseItem input) {
			return ((BaseOption<?>) input).fetch();
		}
	};
	@JSONField(serialize = false,deserialize = false)
	private ICmdUnionAdapter cmdAdapter;

	/**
	 * 取消正在执行的设备命令<br>
	 * 此字段默认为{@code null}，为{@code true}时，指示取消正在执行的设备命令,仅对支持交互的设备命令有效
	 */
	private Boolean canceled;
	/**
	 * 执行此命令时会不会导致应用重启
	 */
	private boolean needReset;
	public CmdItem() {
	}

	@Override
	public final boolean isContainer() {
		return true;
	}

	@Override
	public final ItemType getCatalog() {
		return ItemType.CMD;
	}
	@JSONField(serialize = false,deserialize = false)
	public List<BaseOption<Object>> getParameters(){
		return Lists.transform(getChilds(),TO_OPTION);
	}
	@JSONField(serialize = false,deserialize = false)
	public void setParameters(List<BaseOption<?>> parameters){
		items.clear();
		addParameters(parameters);
	}
	public CmdItem addParameters(BaseOption<?> ... parameter){
		return addParameters(Arrays.asList(parameter));
	}
	public CmdItem addParameters(Collection<BaseOption<?>> parameters){
		addChilds(Collections2.transform(parameters, TO_ITEM));
		return this;
	}
	public ICmdUnionAdapter getCmdAdapter() {
		return cmdAdapter;
	}
	public CmdItem setCmdAdapter(ICmdUnionAdapter cmdAdapter) {
		this.cmdAdapter = cmdAdapter;
		return this;
	}
	/**
	 * 设置交互设备命令执行器
	 * @param cmdAdapter 设备命令执行器
	 * @return 当前对象
	 */
	public CmdItem setInteractiveCmdAdapter(ICmdInteractiveAdapter cmdAdapter) {
		this.cmdAdapter = cmdAdapter;
		return this;
	}
	/**
	 * 设置立即设备命令执行器
	 * @param cmdAdapter 设备命令执行器
	 * @return 当前对象
	 */
	public CmdItem setImmediateCmdAdapter(ICmdImmediateAdapter cmdAdapter) {
		this.cmdAdapter = cmdAdapter;
		return this;
	}
    /**
     * 将{@code value}转为{@code type}指定的类型
     * @param <T> 目标参数类型
     * @param value 待转换的值
     * @param type 目标类型
     * @return 目标对象
     * @see TypeUtils#cast(Object, Type, ParserConfig)
     */
    @SuppressWarnings("unchecked")
    public static final <T> T cast(Object value,Type type){
        return (T)TypeUtils.cast(value,type,ParserConfig.getGlobalInstance());
    }
	/**
	 * 更新命令参数
	 * @param parameters 命令参数
	 * @return 当前对象
	 */
	public CmdItem updateParameter(Map<String, ?> parameters){
		parameters = MoreObjects.firstNonNull(parameters, Collections.<String, Object>emptyMap());
		for(BaseOption<Object> param : getParameters()){
			if(parameters.containsKey(param.getName())){
				Object value = cast(parameters.get(param.getName()), param.javaType());
				param.updateFrom(value);
			}
		}	
		return this;
	}

	/**
	 * 参数 检查，如果参数为required,而输入参数中不包含这个参数，则抛出异常
	 * @param input
	 * @return input
	 */
	private Map<String, Object> checkRequired(Map<String, Object> input){
		for(BaseOption<Object> param : getParameters()){
			checkArgument(!param.isRequired() || input.containsKey(param.getName()),
					"MISS REQUIRED PARAM %s",param.getName());
		}
		return input;
	}
	/**
	 * 执行立即命令
	 * @return 执行设备命令的返回结果对象
	 * @throws CmdExecutionException 设备命令执行异常
	 */
	public final Object runImmediateCmd() throws CmdExecutionException{
		return runImmediateCmd(null);
	}
	/**
	 * 执行立即命令
	 * @param parameters 命令参数
	 * @return 执行设备命令的返回结果对象
	 * @throws CmdExecutionException 设备命令执行异常
	 */
	public final Object runImmediateCmd(Map<String, ?> parameters) throws CmdExecutionException{
		checkState(cmdAdapter instanceof ICmdImmediateAdapter,"type of cmdAdapter must be %s",ICmdImmediateAdapter.class.getSimpleName());
		synchronized (items) {			
			if(cmdAdapter !=null){
				updateParameter(parameters);
				try {
					// 将 parameter 转为 Map<String, Object>
					Map<String, Object> objParams = Maps.transformValues(items, TO_VALUE);
					return ((ICmdImmediateAdapter)cmdAdapter).apply(checkRequired(objParams));
				} finally {
					reset();
				}
			}
			return null;
		}
	}
	/**
	 * 启动交互命令
	 * @param statusListener 设备命令状态侦听器
	 * @throws InteractiveCmdStartException 交互命令执行异常，当设备命令被拒绝或出错时抛出此异常
	 */
	public final void startInteractiveCmd(ICmdInteractiveStatusListener statusListener) throws InteractiveCmdStartException{
		checkState(cmdAdapter instanceof ICmdInteractiveAdapter,"type of cmdAdapter must be %s",ICmdInteractiveAdapter.class.getSimpleName());
		synchronized (items) {
			if(cmdAdapter !=null){
				try {
					// 将 parameter 转为 Map<String, Object>
					Map<String, Object> objParams = Maps.transformValues(items, TO_VALUE);
					((ICmdInteractiveAdapter)cmdAdapter).apply(checkRequired(objParams),
							checkNotNull(statusListener,"statusListener is null"));					
				} finally {
					reset();
				}
			}
		}
	}
	/**
	 * 启动交互命令
	 * @param parameters 命令参数
	 * @param statusListener 设备命令状态侦听器
	 * @throws InteractiveCmdStartException 当设备命令被拒绝或不支持或其他出错时抛出此异常,通过{@link InteractiveCmdStartException#getStatus() }获取状态类型
	 */
	final void startInteractiveCmd(Map<String, ?> parameters, ICmdInteractiveStatusListener statusListener) throws InteractiveCmdStartException{
		checkState(cmdAdapter instanceof ICmdInteractiveAdapter,"type of cmdAdapter must be %s",ICmdInteractiveAdapter.class.getSimpleName());
		synchronized (items) {			
			if(cmdAdapter !=null){
				updateParameter(parameters);
				try {
					// 将 parameter 转为 Map<String, Object>
					Map<String, Object> objParams = Maps.transformValues(items, TO_VALUE);
					((ICmdInteractiveAdapter)cmdAdapter).apply(checkRequired(objParams),
							checkNotNull(statusListener,"statusListener is null"));
				} finally {
					reset();
				}
			}
		}
	}
	
	/**
	 * 取消正在执行的交互命令
	 */
	public final void cancelInteractiveCmd(){
		checkState(cmdAdapter instanceof ICmdInteractiveAdapter,"type of cmdAdapter must be %s",ICmdInteractiveAdapter.class.getSimpleName());
		((ICmdInteractiveAdapter)cmdAdapter).cancel();
	}
	@SuppressWarnings("unchecked")
	public <T>BaseOption<T> getParameter(final String name){
		return (BaseOption<T>) getChild(name);
	}
	
	/**
	 * 设置所有参数为{@code null}
	 * @return 返回当前对象
	 */
	public CmdItem reset(){
		for (BaseOption<Object> item : getParameters()) {
			item.setValue(null);
		}
		return this;
	}
	/**
	 * @return canceled
	 */
	public Boolean getCanceled() {
		return canceled;
	}

	/**
	 * @param canceled 要设置的 canceled
	 */
	public void setCanceled(Boolean canceled) {
		this.canceled = canceled;
	}
	
	/**
	 * @return needReset
	 */
	public boolean isNeedReset() {
		return needReset;
	}

	/**
	 * 设置执行此命令时会不会导致应用重启
	 * @param needReset 要设置的 needReset
	 * @return 当前对象
	 */
	public CmdItem setNeedReset(boolean needReset) {
		this.needReset = needReset;
		return this;
	}

	/**
	 * @return 返回是否为交互设备命令
	 */
	@JSONField(serialize = false,deserialize = false)
	public boolean isInteractiveCmd(){
		return cmdAdapter instanceof ICmdInteractiveAdapter;
	}

}
