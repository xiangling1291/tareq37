package gu.dtalk;

import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import static com.google.common.base.Preconditions.*;

public abstract class BaseOption<T> extends BaseItem implements IOption {
	private T optionValue;
	private T defaultValue;
	private boolean reqiured;
	@JSONField(serialize = false,deserialize = false)
	protected final Type type;
	@JSONField(serialize = false,deserialize = false)
	private Predicate<T> valueValidator = Predicates.alwaysTrue();
	public BaseOption(Type type) {
		super();
		this.type = checkNotNull(type);
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}
	@Override
	public final boolean isContainer() {
		return false;
	}
	public static final boolean isValidString(String value,Type type) {
		try{
			return null == JSON.parseObject(value, type);
		}catch (Exception e) {
			return false;
		}
	}
	@Override
	public final boolean isValid(String value) {
		if(isValidString(value, type)){
			if(valueValidator != null){
				T v = JSON.parseObject(value, type);
				return valueValidator.apply(v);
			}
		}
		return false;
	}

	@Override
	public final String getValue() {
		return	JSON.toJSONString(getObjectValue());
	}
	@Override
	public boolean setValue(String value) {
		try{
			T  v =	JSON.parseObject(value, type);
			optionValue = valueValidator.apply(v) ? v : null;
			return true;
		}catch (Exception e) {
			return false;
		}
	}
	@Override
	public final String getDefaultValue() {
		return JSON.toJSONString(defaultValue);
	}
	public void setDefaultValue(T defaultValue) {
		this.defaultValue = defaultValue;		
	}

	@SuppressWarnings("unchecked")
	@Override
	@JSONField(serialize = false,deserialize = false)
	public <V>V getObjectValue(){
		return (V)((null == optionValue) ? defaultValue : optionValue);
	}

	public void setObjectValue(T value){
		this.optionValue = value;
	}
	public synchronized void setValidator(Predicate<T> validator) {
		if(validator!=null){
			this.valueValidator = validator;
		}
	}

	@Override
	public boolean isReqiured() {
		return reqiured;
	}

	public void setReqiured(boolean reqiured) {
		this.reqiured = reqiured;
	}

	@Override
	public final ItemType getCatalog() {
		return ItemType.OPTION;
	}

}
