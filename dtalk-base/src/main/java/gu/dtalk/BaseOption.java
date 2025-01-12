package gu.dtalk;

import java.lang.reflect.Type;
import java.util.Collection;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import static com.google.common.base.Preconditions.*;

public abstract class BaseOption<T> extends BaseItem {
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
	public abstract OptionType getType();
	public boolean isReadOnly() {
		return false;
	}
	@Override
	public final boolean isContainer() {
		return false;
	}

	@Override
	public final ItemType getCatalog() {
		return ItemType.OPTION;
	}
	@Override
	public final BaseItem addChilds(Collection<BaseItem> childs) {
		// DO NOTHING
		return this;
	}
	/**
	 * 验证value是否有效
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean validate(Object value){
		try{
			return valueValidator.apply((T) value);
		}catch (Exception e) {
			return false;
		}
	}
	public final Object getValue() {
		return	optionValue;
	}
	@SuppressWarnings("unchecked")
	public BaseOption<T> setValue(Object value) {
		optionValue = (T) value;
		return this;
	}
	public final T getDefaultValue() {
		return defaultValue;
	}
	public BaseOption<T> setDefaultValue(T defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public synchronized BaseOption<T> setValidator(Predicate<T> validator) {
		if(validator!=null){
			this.valueValidator = validator;
		}
		return this;
	}

	public boolean isReqiured() {
		return reqiured;
	}

	public BaseOption<T> setReqiured(boolean reqiured) {
		this.reqiured = reqiured;
		return this;
	}

}
