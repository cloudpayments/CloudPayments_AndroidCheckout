package ru.cloudpayments.checkout.sdk.card;

public class CardType {

    public final static int visa = 0;
    public final static int mc = 1;
    public final static int maestro = 2;
    public final static int amex = 3;
    public final static int unknown = -1;
    private final static int AmericanExpress = 4;
    private final static int DinersClub = 5;
    private final static int CarteBlanche = 6;
    private final static int JCB = 7;

    public static String toString(int value) {
        switch (value) {
            case visa:
                return "Visa";
            case mc:
                return "MasterCard";
            case maestro:
                return "Maestro";
            case amex:
                return "Amex";
            case AmericanExpress:
                return "AmericanExpress";
            case DinersClub:
                return "DinersClub";
            case CarteBlanche:
                return "CarteBlanche";
            case JCB:
                return "JCB";
            default:
                return "Unknown";
        }
    }

    public static int fromString(String value) {
        if ("visa".equals(value.toLowerCase())) {
            return visa;
        } else if ("mastercard".equals(value.toLowerCase())) {
            return mc;
        } else if ("maestro".equals(value.toLowerCase())) {
            return maestro;
        } else if ("amex".equals(value.toLowerCase())) {
            return amex;
        } else if ("americanexpress".equals(value.toLowerCase())) {
            return AmericanExpress;
        } else if ("dinersclub".equals(value.toLowerCase())) {
            return DinersClub;
        } else if ("carteblanche".equals(value.toLowerCase())) {
            return CarteBlanche;
        } else if ("jcb".equals(value.toLowerCase())) {
            return JCB;
        } else {
            return unknown;
        }
    }

    public static int getType(String creditCardNumberPart)  {
        if (creditCardNumberPart==null || creditCardNumberPart.length() < 2 || creditCardNumberPart.equals("null")) return unknown;

        int prefix1 = Integer.valueOf(creditCardNumberPart.substring(0, 2));
        //American Express
        if (prefix1 == 34 || prefix1 == 37)
            return AmericanExpress;

        //Diners Club
        if (prefix1 == 36)
            return DinersClub;

        //Carte Blanche
        if (prefix1 == 38)
            return CarteBlanche;

        //MasterCard
        if (prefix1 >= 51 && prefix1 <= 55)
            return mc;

        //Maestro
        if (prefix1 == 50 || (prefix1 >= 56 && prefix1 <= 58) || (prefix1 >= 60 && prefix1 <= 69))
            return maestro;

        int prefix4 = Integer.valueOf(creditCardNumberPart.substring(0, 1));

        //JCB
        if (prefix4 == 3)
            return JCB;

        //Visa
        if (prefix4 == 4)
            return visa;

        return unknown;
    }
}