package com.hogwai.jit.impl.geometry;

@SuppressWarnings("unused")
public record Square(double side) implements Shape {

    @Override
    public double area() {
        return side * side;
    }

    @Override
    public double perimeter() {
        return 4 * side;
    }
}
