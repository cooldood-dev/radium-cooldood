package vip.radium.utils;

import java.math.BigDecimal;
import java.math.MathContext;

public final class MathUtils {

    private MathUtils() {
    }

    public static double roundToDecimalPlace(double value, double inc) {
        final double halfOfInc = inc / 2.0D;
        final double floored = StrictMath.floor(value / inc) * inc;
        if (value >= floored + halfOfInc)
            return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).
                    stripTrailingZeros()
                    .doubleValue();
        else
            return new BigDecimal(floored, MathContext.DECIMAL64)
                    .stripTrailingZeros()
                    .doubleValue();
    }

    public static int randomNumber(int max, int min) {
        return (int) (Math.random() * (max - min)) + min;
    }

    public static double roundToPlace(double value, int places) {
        if (Double.isInfinite(value) || Double.isNaN(value) || places < 0) {
            return value;
        }
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, java.math.RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double getIncremental(double val, double inc) {
        double one = 1 / inc;
        return Math.round(val * one) / one;
    }

    public static boolean isInteger(Double variable) {
        return (variable == Math.floor(variable)) && !Double.isInfinite(variable);
    }
}
