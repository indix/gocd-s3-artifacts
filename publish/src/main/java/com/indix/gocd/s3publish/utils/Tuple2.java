package com.indix.gocd.s3publish.utils;

public class Tuple2<Left, Right> {
    private Left left;
    private Right right;

    public Tuple2(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    public Left _1() {
        return left;
    }

    public Right _2() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple2 tuple2 = (Tuple2) o;

        if (!left.equals(tuple2.left)) return false;
        if (!right.equals(tuple2.right)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = left.hashCode();
        result = 31 * result + right.hashCode();
        return result;
    }
}
