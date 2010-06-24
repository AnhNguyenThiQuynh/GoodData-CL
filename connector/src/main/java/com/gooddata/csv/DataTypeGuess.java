/*
 * Copyright (c) 2009, GoodData Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and
 *        the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *        and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the GoodData Corporation nor the names of its contributors may be used to endorse
 *        or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gooddata.csv;

import org.apache.log4j.helpers.DateTimeDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * GoodData CSV data type guessing
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class DataTypeGuess {


    /**
     * Tests if the String is integer
     * @param t the tested String
     * @return true if the String is integer, false otherwise
     */
    public static boolean isInteger(String t) {
        try {
            Integer.parseInt(t);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Tests if the String is decimal
     * @param t the tested String
     * @return true if the String is decimal, false otherwise
     */
    public static boolean isDecimal(String t) {
        try {
            if(isInteger(t))
                return false;
            Double.parseDouble(t);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static SimpleDateFormat[] dtf = {new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("M/d/yyyy"), new SimpleDateFormat("MM-dd-yyyy"),new SimpleDateFormat("yyyy-M-d"),
            new SimpleDateFormat("M-d-yyyy")
    };

    /**
     * Tests if the String is date
     * @param t the tested String
     * @return true if the String is date, false otherwise
     */
    public static boolean isDate(String t) {
        for(SimpleDateFormat d : dtf) {
            try {
                d.parse(t);
                return true;
            }
            catch (ParseException e) {
                //NOTHING HERE
            }
        }
        return false;
    }

}
