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
package org.wso2.carbon.billing.test;

import junit.framework.TestCase;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.dataobjects.Cash;

public class CashTest extends TestCase {
    public void testCash() throws BillingException {
        successCase("$20.58", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 58);
        successCase("$20.5", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 50);
        successCase("$20.05", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 5);
        successCase("20.05", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 5);
        successCase("20", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 0);
        successCase("20.0", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 20, 0);
        successCase("-20.0", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 20, 0);
        successCase("$-21.5", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 21, 50);
        successCase("$-0.05", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 0, 5);

        failCase("2343x");
        failCase("20.");
        failCase("34.34.34");
        failCase("34.2343");
        failCase("34x.23");
        failCase("34.2x");
        failCase("$-.05");

        // always putting
        checkSerialize("$5.8", "$5.80");
        checkSerialize("0.5", "$0.50");
        checkSerialize("$-3", "$-3.00");
        checkSerialize("$-2.5", "$-2.50");
        checkSerialize("-0.5", "$-0.50");

        // checking some adds
        checkAdd("3.54", "3.34", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 6, 88);
        checkAdd("3.54", "3.6", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 7, 14);
        checkAdd("3.8", "-5.8", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 2, 0);
        checkAdd("-23.8", "2.8", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 21, 0);
        checkAdd("-23.8", "-2.8", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 26, 60);


        checkSustract("3.54", "3.34", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 0, 20);
        checkSustract("3.54", "3.6", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 0, 6);
        checkSustract("3.8", "-5.8", Cash.CURRENCY_USD, Cash.Sign.POSITIVE, 9, 60);
        checkSustract("-23.8", "2.8", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 26, 60);
        checkSustract("-23.8", "-2.8", Cash.CURRENCY_USD, Cash.Sign.NEGATIVE, 21, 0);
    }

    private void checkAdd(String cashA, String cashB, String currency,
                          Cash.Sign sign, int wholeNumber, int decimalNumber) throws BillingException {
        successCase(Cash.add(new Cash(cashA), new Cash(cashB)).serializeToString(),
                currency, sign, wholeNumber, decimalNumber);
    }

    private void checkSustract(String cashA, String cashB, String currency,
                               Cash.Sign sign, int wholeNumber, int decimalNumber) throws BillingException {
        successCase(Cash.subtract(new Cash(cashA), new Cash(cashB)).serializeToString(),
                currency, sign, wholeNumber, decimalNumber);
    }

    private void checkSerialize(String cashStr, String cashStrShouldBe) throws BillingException {
        Cash cash = new Cash(cashStr);
        assertEquals("Cash string should be " + cashStrShouldBe, cash.serializeToString(), cashStrShouldBe);
    }

    private void successCase(String cashString, String currency, Cash.Sign sign, int wholeNumber, int decimalNumber)
            throws BillingException {
        Cash cash = new Cash(cashString);
        boolean success = currency.equals(cash.getCurrency()) &&
                sign == cash.getSign() &&
                wholeNumber == cash.getWholeNumber() &&
                decimalNumber == cash.getDecimalNumber();
        assertTrue(cashString + " failed", success);
    }

    public void failCase(String cashString) {
        boolean gotError;
        try {
            new Cash(cashString);
            gotError = false;
        } catch (BillingException e) {
            gotError = true;
        }
        assertTrue(cashString + "is not failed (should have failed)", gotError);
    }
}
