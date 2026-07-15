package com.myagv.middleware.service;

import java.io.Serializable;
import java.util.function.Predicate;

public class AcceptAllEventFilter implements Predicate<Object>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final AcceptAllEventFilter INSTANCE = new AcceptAllEventFilter();
    
    private AcceptAllEventFilter() {
    }
    
    @Override
    public boolean test(Object o) {
        return true;
    }
}