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
	private ICmdAdapter cmdAdapter;
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
	public ICmdAdapter getCmdAdapter() {
		return cmdAdapter;
	}
	public CmdItem setCmdAdapter(ICmdAdapter cmdAdapter) {
		this.cmdAdapter = cmdAdapter;
		return this;
	}
    /**
     * 将{@code value}转为{@code type}指定的类型
     * @param <T> 目标参数类型
     * @param value
     * @param type
     * @return
     * @see TypeUtils#cast(Object, Type, ParserConfig)
     */
    @SuppressWarnings("unchecked")
    public static final <T> T cast(Object value,Type type){
        return (T)TypeUtils.cast(value,type,ParserConfig.getGlobalInstance());
    }
	/**
	 * 更新命令参数
	 * @param parameters
	 * @return
	 */
	private CmdItem updateParameter(Map<String, ?> parameters){
		parameters = MoreObjects.firstNonNull(parameters, Collections.<String, Object>emptyMap());
		for(BaseOption<Object> param : getParameters()){
			Object value = cast(parameters.get(param.getName()), param.javaType());
			param.updateFrom(value);
		}	
		return this;
	}
	/**
	 * 执行命令
	 * @return
	 * @throws CmdExecutionException
	 */
	public final Object runCmd() throws CmdExecutionException{
		synchronized (items) {
			if(cmdAdapter !=null){
				// 将 parameter 转为 Map<String, Object>
				Map<String, Object> objParams = Maps.transformValues(items, TO_VALUE);
				return cmdAdapter.apply(objParams);
			}
			return null;
		}
	}
	/**
	 * 执行命令
	 * @param parameters 命令参数
	 * @return
	 * @throws CmdExecutionException
	 */
	final Object runCmd(Map<String, ?> parameters) throws CmdExecutionException{
		synchronized (items) {			
			if(cmdAdapter !=null){
				updateParameter(parameters);
				// 将 parameter 转为 Map<String, Object>
				Map<String, Object> objParams = Maps.transformValues(items, TO_VALUE);
				return cmdAdapter.apply(objParams);
			}
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	public <T>BaseOption<T> getParameter(final String name){
		return (BaseOption<T>) getChild(name);
	}
	
	/**
	 * 设置所有参数为{@code null}
	 */
	public void reset(){
		for (BaseOption<Object> item : getParameters()) {
			item.setValue(null);
		}
	}
	public static interface ICmdAdapter {
		Object apply(Map<String, Object> input) throws CmdExecutionException;
	}
}
