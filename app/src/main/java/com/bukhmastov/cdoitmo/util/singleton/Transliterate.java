package com.bukhmastov.cdoitmo.util.singleton;

public class Transliterate {

    public static String cyr2lat(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (char ch: s.toCharArray()) {
            sb.append(cyr2lat(ch));
        }
        return sb.toString();
    }

    public static String cyr2lat(char ch) {
        switch (ch) {
            case 'А': return "A";   case 'а': return "a";
            case 'Б': return "B";   case 'б': return "b";
            case 'В': return "V";   case 'в': return "v";
            case 'Г': return "G";   case 'г': return "g";
            case 'Д': return "D";   case 'д': return "d";
            case 'Е': return "E";   case 'е': return "e";
            case 'Ё': return "E";   case 'ё': return "e";
            case 'Ж': return "ZH";  case 'ж': return "zh";
            case 'З': return "Z";   case 'з': return "z";
            case 'И': return "I";   case 'и': return "i";
            case 'Й': return "J";   case 'й': return "j";
            case 'К': return "K";   case 'к': return "k";
            case 'Л': return "L";   case 'л': return "l";
            case 'М': return "M";   case 'м': return "m";
            case 'Н': return "N";   case 'н': return "n";
            case 'О': return "O";   case 'о': return "o";
            case 'П': return "P";   case 'п': return "p";
            case 'Р': return "R";   case 'р': return "r";
            case 'С': return "S";   case 'с': return "s";
            case 'Т': return "T";   case 'т': return "t";
            case 'У': return "U";   case 'у': return "u";
            case 'Ф': return "F";   case 'ф': return "f";
            case 'Х': return "KH";  case 'х': return "kh";
            case 'Ц': return "C";   case 'ц': return "c";
            case 'Ч': return "CH";  case 'ч': return "ch";
            case 'Ш': return "SH";  case 'ш': return "sh";
            case 'Щ': return "JSH"; case 'щ': return "jsh";
            case 'Ъ': return "''";  case 'ъ': return "''";
            case 'Ы': return "Y";   case 'ы': return "y";
            case 'Ь': return "'";   case 'ь': return "'";
            case 'Э': return "E";   case 'э': return "e";
            case 'Ю': return "JU";  case 'ю': return "ju";
            case 'Я': return "JA";  case 'я': return "ja";
            default: return String.valueOf(ch);
        }
    }

    public static String letter2lat(String letter) {
        if (StringUtils.isBlank(letter)) {
            return letter;
        }
        char ch = letter.charAt(0);
        switch (ch) {
            case 'А': return "A";  case 'а': return "a";
            case 'Е': return "E";  case 'е': return "e";
            case 'К': return "K";  case 'к': return "k";
            case 'М': return "M";  case 'м': return "m";
            case 'О': return "O";  case 'о': return "o";
            case 'Т': return "T";  case 'т': return "t";
            default: return String.valueOf(ch);
        }
    }
}
