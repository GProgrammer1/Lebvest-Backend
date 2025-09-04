package com.lebvest.util;

public interface GenericMapper <E,D>{
    static <E, D> E toEntity(D d) {
        return null;
    }

    static <E,D> D toDto(E e) {
        return null;
    }



}
