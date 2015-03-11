package com.josh2112.utils.jooq;

import org.jooq.Converter;


public class BooleanConverter implements Converter<Integer, Boolean> {

	@Override
	public Boolean from( Integer intVal ) {
		return intVal != null && intVal > 0;
	}

	@Override
	public Integer to( Boolean boolVal ) {
		return boolVal != null && boolVal ? 1 : 0;
	}

	@Override
	public Class<Integer> fromType() {
		return Integer.class;
	}

	@Override
	public Class<Boolean> toType() {
		return Boolean.class;
	}

}
