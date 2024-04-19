package demo.test;

public class TestClass {

    public String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public boolean isPalindrome(String str) {
        if (str == null) {
            return false;
        }
        StringBuilder reverse = new StringBuilder(str).reverse();
        return str.equals(reverse.toString());
    }

    public String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    public boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean nullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public String defaultString(String str, String defaultStr) {
        return (str == null) ? defaultStr : str;
    }

    public String randomAlphanumeric(int length) {
        String alphaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (alphaNum.length() * Math.random());
            sb.append(alphaNum.charAt(index));
        }
        return sb.toString();
    }

    public int countMatches(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    public String removeEnd(String str, String remove) {
        if (str == null || remove == null || !str.endsWith(remove)) {
            return str;
        }
        return str.substring(0, str.length() - remove.length());
    }
}