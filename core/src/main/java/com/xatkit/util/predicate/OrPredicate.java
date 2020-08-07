package com.xatkit.util.predicate;

import java.util.function.Predicate;

public class OrPredicate<T> extends ComposedPredicate<T> {

    public OrPredicate(Predicate<? super T> p1, Predicate<? super T> p2) {
        super(p1, p2);
    }

    @Override
    public boolean test(T t) {
        return p1.test(t) || p2.test(t);
    }
}
