/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.billing.core.dataobjects;

import org.wso2.carbon.billing.core.BillingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Pattern;


public class Cash {
    private static final Log log = LogFactory.getLog(Cash.class);
    public static final String CURRENCY_USD = "$";
    public static final String DEFAULT_CURRENCY = CURRENCY_USD;

    public enum Sign {
        POSITIVE, NEGATIVE
    };

    String currency;
    int wholeNumber;
    int decimalNumber;
    Sign sign = Sign.POSITIVE; // true for positive

    private static final String notNumbersRegEx = "[^0-9]";
    private static final Pattern notNumbersPattern = Pattern.compile(notNumbersRegEx);

    public Cash() {
        // the default constructor for Cash
    }

    public Cash(Cash copyFrom) {
        this.currency = copyFrom.getCurrency();
        this.wholeNumber = copyFrom.getWholeNumber();
        this.decimalNumber = copyFrom.getDecimalNumber();
        this.sign = copyFrom.getSign();
    }

    public Cash(String cashString) throws BillingException {
        if (cashString == null) {
            cashString = "$0";
        }
        if (cashString.startsWith(CURRENCY_USD)) {
            cashString = cashString.substring(CURRENCY_USD.length());
            currency = CURRENCY_USD;
        }
        // possible other currencies
        else {
            currency = DEFAULT_CURRENCY;
        }

        if (cashString.startsWith("-")) {
            sign = Sign.NEGATIVE;
            cashString = cashString.substring(1);
        } else if (cashString.startsWith("+")) {
            sign = Sign.POSITIVE;
            cashString = cashString.substring(1);
        } else {
            sign = Sign.POSITIVE;
        }

        if (cashString.contains(".")) {
            String wholeNumberStr = cashString.substring(0, cashString.indexOf("."));
            if (wholeNumberStr.trim().equals("")) {
                String msg = "Whole number can not be empty";
                throw new BillingException(msg);
            }
            if (notNumbersPattern.matcher(wholeNumberStr).find()) {
                String msg = "The whole number expected to have only 0-9 characters.: " +
                                wholeNumberStr + " is not a number. ";
                throw new BillingException(msg);
            }
            
            String decimalNumberStr = cashString.substring(cashString.indexOf(".") + 1);
            if (notNumbersPattern.matcher(decimalNumberStr).find()) {
                String msg = "The decimal number expected to have only 0-9 characters.: " +
                                decimalNumberStr + " is not a number. ";
                throw new BillingException(msg);
            }
            if (decimalNumberStr.length() == 0) {
                String msg = "String after the decimal point is zero.";
                throw new BillingException(msg);
            } else if (decimalNumberStr.length() > 2) {
                String msg = "String after the decimal point is greater than 2";
                throw new BillingException(msg);
            } else if (decimalNumberStr.length() == 1) {
                decimalNumberStr += "0";
            }
            
            wholeNumber = Integer.parseInt(wholeNumberStr);
            decimalNumber = Integer.parseInt(decimalNumberStr);
            
        } else {
            if (notNumbersPattern.matcher(cashString).find()) {
                String msg = "The cash string to have only 0-9 characters.: " + cashString +
                                " is not a number. ";
                throw new BillingException(msg);
            }
            
            wholeNumber = Integer.parseInt(cashString);
            decimalNumber = 0;
        }
    }

    public Sign getSign() {
        return sign;
    }

    public void setSign(Sign sign) {
        this.sign = sign;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getWholeNumber() {
        return wholeNumber;
    }

    public void setWholeNumber(int wholeNumber) {
        this.wholeNumber = wholeNumber;
    }

    public int getDecimalNumber() {
        return decimalNumber;
    }

    public void setDecimalNumber(int decimalNumber) {
        this.decimalNumber = decimalNumber;
    }

    public String serializeToString() {
        String str = currency;
        if (sign == Sign.NEGATIVE) {
            str += "-";
        }
        str += wholeNumber + ".";
        if (decimalNumber < 10) {
            str += "0" + decimalNumber;
        } else {
            str += decimalNumber;
        }
        return str;
    }

    @Override
    public String toString() {
        return serializeToString();
    }

    public static Cash add(Cash a, Cash b) throws BillingException {
        if (!a.getCurrency().equals(b.getCurrency())) {
            // we still not support this.
            String msg = "Can not add in-similar currencies: " + a.getCurrency() + "!=" +
                            b.getCurrency() + ".";
            log.error(msg);
            throw new BillingException(msg);
        }
        
        if (a.getSign() == Sign.POSITIVE && b.getSign() == Sign.NEGATIVE) {
            Cash b2 = new Cash(b);
            b2.setSign(Sign.POSITIVE);
            return subtract(a, b2);
        }
        
        if (a.getSign() == Sign.NEGATIVE && b.getSign() == Sign.POSITIVE) {
            Cash a2 = new Cash(a);
            a2.setSign(Sign.POSITIVE);
            return subtract(b, a2);
        }
        
        if (a.getSign() == Sign.NEGATIVE && b.getSign() == Sign.NEGATIVE) {
            Cash a2 = new Cash(a);
            Cash b2 = new Cash(b);
            a2.setSign(Sign.POSITIVE);
            b2.setSign(Sign.POSITIVE);
            Cash c2 = add(a2, b2);
            c2.setSign(Sign.NEGATIVE);
            return c2;
        }
        
        int decimalSum = a.getDecimalNumber() + b.getDecimalNumber();
        int wholeSum = a.getWholeNumber() + b.getWholeNumber();

        Cash cash = new Cash();
        cash.setCurrency(a.getCurrency());
        if (decimalSum >= 100) {
            decimalSum -= 100;
            wholeSum += 1;
        }
        cash.setDecimalNumber(decimalSum);
        cash.setWholeNumber(wholeSum);
        cash.setSign(Sign.POSITIVE);
        return cash;
    }

    public static Cash subtract(Cash a, Cash b) throws BillingException {
        if (!a.getCurrency().equals(b.getCurrency())) {
            // we still not support this.
            String msg = "Can not add in-similar currencies: " + a.getCurrency() + "!=" +
                            b.getCurrency() + ".";
            log.error(msg);
            throw new BillingException(msg);
        }
        
        if (a.getSign() == Sign.POSITIVE && b.getSign() == Sign.NEGATIVE) {
            Cash b2 = new Cash(b);
            b2.setSign(Sign.POSITIVE);
            return add(a, b2);
        }
        
        if (a.getSign() == Sign.NEGATIVE && b.getSign() == Sign.POSITIVE) {
            Cash a2 = new Cash(a);
            a2.setSign(Sign.POSITIVE);
            Cash c2 = add(b, a2);
            c2.setSign(Sign.NEGATIVE);
            return c2;
        }
        
        if (a.getSign() == Sign.NEGATIVE && b.getSign() == Sign.NEGATIVE) {
            Cash a2 = new Cash(a);
            Cash b2 = new Cash(b);
            a2.setSign(Sign.POSITIVE);
            b2.setSign(Sign.POSITIVE);
            Cash c2 = subtract(a2, b2);
            if (c2.getSign() == Sign.NEGATIVE) {
                c2.setSign(Sign.POSITIVE);
            } else {
                c2.setSign(Sign.NEGATIVE);
            }
            return c2;
        }
        
        int decimalSum = a.getDecimalNumber() - b.getDecimalNumber();
        int wholeSum = a.getWholeNumber() - b.getWholeNumber();

        if (wholeSum < 0 || (decimalSum < 0 && wholeSum == 0)) {
            // then it is negative value
            Cash c = subtract(b, a);
            c.setSign(Sign.NEGATIVE);
            return c;
        }

        Cash cash = new Cash();
        cash.setCurrency(a.getCurrency());
        if (decimalSum < 0) {
            decimalSum += 100;
            wholeSum -= 1;
        }

        cash.setDecimalNumber(decimalSum);
        cash.setWholeNumber(wholeSum);
        return cash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cash)) {
            return false;
        }
        Cash otherCash = (Cash) o;
        return otherCash.serializeToString().equals(this.serializeToString());
    }

    @Override
    public int hashCode() {
        return serializeToString().hashCode();
    }

    public Cash multiply(double multiplyBy) {
        long answerInCent = (long) (wholeNumber * 100 * multiplyBy + decimalNumber * multiplyBy);
        int newWholeNumber = (int) (answerInCent / 100);
        int newDecimalNumber = (int) (answerInCent % 100);

        Cash cash = new Cash();
        cash.setCurrency(this.getCurrency());
        if (newWholeNumber < 0) {
            cash.setSign(Sign.NEGATIVE);
        }
        cash.setWholeNumber(newWholeNumber);
        cash.setDecimalNumber(newDecimalNumber);
        return cash;
    }
}
