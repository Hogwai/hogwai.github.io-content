package com.hogwai.jit.impl.geometry;

@SuppressWarnings("unused")
public record Circle(double radius) implements Shape {

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }
}
