package com.grpcexamples.Resilience4jDemo;
import io.vavr.collection.List;

public class VavrDemo {

    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 3);
        list.tail().prepend(0);
        System.out.println(list.toString());

    }
}
