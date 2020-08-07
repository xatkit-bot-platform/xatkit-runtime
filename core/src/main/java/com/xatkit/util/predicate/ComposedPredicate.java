package com.xatkit.util.predicate;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public abstract class ComposedPredicate<T> implements Predicate<T> {

    @Getter
    protected Predicate<? super T> p1;

    @Getter
    protected Predicate<? super T> p2;

    public ComposedPredicate(Predicate<? super T> p1, Predicate<? super T> p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @NotNull
    @Override
    public Predicate<T> and(@NotNull Predicate<? super T> other) {
        return new AndPredicate<T>(this, other);
    }

    @NotNull
    @Override
    public Predicate<T> or(@NotNull Predicate<? super T> other) {
        return new OrPredicate<T>(this, other);
    }

    @NotNull
    @Override
    public Predicate<T> negate() {
        return new NegatePredicate<>(this);
    }
}
