package com.xatkit.util.predicate;

import lombok.Getter;

import java.util.function.Predicate;

public class NegatePredicate<T> implements Predicate<T> {

    @Getter
    protected Predicate<? super T> p1;

    public NegatePredicate(Predicate<? super T> p1) {
        this.p1 = p1;
    }

    @Override
    public boolean test(T t) {
        return !(p1.test(t));
    }
}
