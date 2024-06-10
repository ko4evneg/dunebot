package ru.trainithard.dunebot.util;

public class DoubleToStringUtil {
    private DoubleToStringUtil() {
    }

    public static String getStrippedZeroesString(double number) {
        String numberString = Double.toString(number);
        return numberString.replaceAll("(0*$|\\.0*$)", "");
    }
}
