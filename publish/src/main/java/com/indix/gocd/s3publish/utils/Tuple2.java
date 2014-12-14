package com.indix.gocd.s3publish.utils;

public class Tuple2<Left, Right> {
    private Left _1;
    private Right _2;

    public Tuple2(Left _1, Right _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public Left _1() {
        return _1;
    }

    public Right _2() {
        return _2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple2 tuple2 = (Tuple2) o;

        if (!_1.equals(tuple2._1)) return false;
        if (!_2.equals(tuple2._2)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _1.hashCode();
        result = 31 * result + _2.hashCode();
        return result;
    }
}
