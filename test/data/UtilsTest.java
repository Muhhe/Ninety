/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Muhe
 */
public class UtilsTest {

    @Test
    public void testGetFirstDaysInWeek() {
        System.out.println("GetFirstDaysInWeek");

        CloseData data = new CloseData(30);

        int index = 0;
        data.adjCloses[0] = 5;
        data.adjCloses[5] = 10;
        data.dates[index++] = LocalDate.parse("22022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("18022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("17022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("16022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("15022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("14022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("11022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("10022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("09022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("08022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("07022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("04022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("03022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("02022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("01022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("31012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("28012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("27012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("26012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("25012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("24012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("21012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("20012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("19012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("18012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("14012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("13012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("12012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("11012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("10012000", DateTimeFormatter.ofPattern("ddMMyyyy"));

        data.adjCloses[index - 1] = 15;

        CloseData weekly = Utils.GetFirstDaysInWeek(data);

        assertEquals(5.0, weekly.adjCloses[0], 0.001);
        assertEquals(10.0, weekly.adjCloses[1], 0.001);
        assertEquals(15.0, weekly.adjCloses[6], 0.001);

        assertEquals(LocalDate.parse("22022000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[0]);
        assertEquals(LocalDate.parse("14022000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[1]);
        assertEquals(LocalDate.parse("10012000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[6]);

        assertEquals(weekly.dates.length, 7);
    }

    @Test
    public void testGetLastDaysInWeekOHLC() {
        System.out.println("GetFirstDaysInWeek");

        OHLCData data = new OHLCData(30);

        int index = 0;
        data.opens[0] = 5;
        data.highs[0] = 10;
        data.lows[0] = 2;
        data.adjCloses[0] = 4;
        data.opens[5] = 10;
        data.highs[5] = 20;
        data.lows[5] = 5;
        data.adjCloses[5] = 8;
        //data.dates[index++] = LocalDate.parse("22022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("18022000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("17022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("16022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("15022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("14022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("11022000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("10022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("09022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("08022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("07022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("04022000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("03022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("02022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("01022000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("31012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        //data.dates[index++] = LocalDate.parse("28012000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("27012000", DateTimeFormatter.ofPattern("ddMMyyyy")); // thursday
        data.dates[index++] = LocalDate.parse("26012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("25012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("24012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("21012000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("20012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("19012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("18012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("14012000", DateTimeFormatter.ofPattern("ddMMyyyy")); // friday
        data.dates[index++] = LocalDate.parse("13012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("12012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("11012000", DateTimeFormatter.ofPattern("ddMMyyyy"));
        data.dates[index++] = LocalDate.parse("10012000", DateTimeFormatter.ofPattern("ddMMyyyy"));

        data.opens[index - 5] = 15;
        data.highs[index - 5] = 30;
        data.lows[index - 5] = 5;
        data.adjCloses[index - 5] = 10;

        OHLCData weekly = Utils.GetLastDaysInWeek(data, 0);

        assertEquals(weekly.dates.length, 6);

        assertEquals(5.0, weekly.opens[0], 0.001);
        assertEquals(10.0, weekly.highs[0], 0.001);
        assertEquals(2.0, weekly.lows[0], 0.001);
        assertEquals(4.0, weekly.adjCloses[0], 0.001);
        assertEquals(10.0, weekly.opens[1], 0.001);
        assertEquals(20.0, weekly.highs[1], 0.001);
        assertEquals(5.0, weekly.lows[1], 0.001);
        assertEquals(8.0, weekly.adjCloses[1], 0.001);
        assertEquals(15.0, weekly.opens[5], 0.001);
        assertEquals(30.0, weekly.highs[5], 0.001);
        assertEquals(5.0, weekly.lows[5], 0.001);
        assertEquals(10.0, weekly.adjCloses[5], 0.001);

        assertEquals(LocalDate.parse("18022000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[0]);
        assertEquals(LocalDate.parse("11022000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[1]);
        assertEquals(LocalDate.parse("27012000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[3]);
        assertEquals(LocalDate.parse("14012000", DateTimeFormatter.ofPattern("ddMMyyyy")), weekly.dates[5]);
    }

    @Test
    public void testReverse() {
        System.out.println("testReverse");

        final double[] arr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        final int maxLength = 5;

        double[] revArr = Utils.reverseDouble(arr, maxLength);

        assertEquals(revArr.length, maxLength);
        for (int i = 0; i < maxLength; ++i) {
            assertEquals(revArr[i], arr[arr.length - 1 - i], 0.001);
        }
    }
}
