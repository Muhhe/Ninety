/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Muhe
 */
public class IndicatorCalculatorTest {

    public IndicatorCalculatorTest() {
    }

    /**
     * Test of SMA method, of class IndicatorCalculator.
     */
    @Test
    public void testSMA() {
        System.out.println("SMA");
        int count = 10;
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double expResult = 5.5;
        double result = IndicatorCalculator.SMA(count, data);
        assertEquals(expResult, result, 0.0);
    }

    @Test
    public void testSMA200() {
        System.out.println("SMA");
        int count = 200;
        //<editor-fold defaultstate="collapsed" desc="200 values">   
        double[] data = {
            79.68,
            80.68,
            80.65,
            81.13,
            81.75,
            82.46,
            81.82,
            81.66,
            80.716,
            80.626,
            80.577,
            81.193,
            81.998,
            81.938,
            82.664,
            83.201,
            83.101,
            83.986,
            84.363,
            84.99,
            84.065,
            83.757,
            85.964,
            86.669,
            86.948,
            85.904,
            84.304,
            84.811,
            85.139,
            84.085,
            85.248,
            85.954,
            84.781,
            84.91,
            84.284,
            83.439,
            83.688,
            84.115,
            84.92,
            84.99,
            85.854,
            85.636,
            84.761,
            84.602,
            84.98,
            84.761,
            84.642,
            85.606,
            84.493,
            84.264,
            83.956,
            83.837,
            83.449,
            83.916,
            83.996,
            83.6,
            83.047,
            78.966,
            80.093,
            80.32,
            80.429,
            80.528,
            80.735,
            79.045,
            79.144,
            80.073,
            80.192,
            77.771,
            77.721,
            78.927,
            78.986,
            78.887,
            78.798,
            76.911,
            77.524,
            76.832,
            77.761,
            77.297,
            78.255,
            78.65,
            79.065,
            79.263,
            79.352,
            79.51,
            82,
            82.444,
            80.419,
            80.014,
            80.35,
            80.557,
            80.468,
            80.715,
            81.348,
            80.34,
            80.449,
            79.194,
            77.603,
            77.959,
            77.702,
            76.447,
            77.079,
            74.431,
            74.678,
            75.014,
            76.091,
            75.647,
            74.471,
            75.054,
            75.419,
            75.884,
            75.459,
            76.16,
            77.86,
            76.635,
            78.433,
            77.672,
            77.84,
            77.613,
            77.524,
            77.563,
            79.173,
            79.664,
            79.429,
            79.861,
            79.851,
            80.263,
            80.558,
            81.795,
            82.335,
            80.715,
            80.43,
            80.548,
            79.723,
            80.018,
            79.379,
            80.342,
            80.999,
            79.723,
            78.928,
            78.172,
            77.455,
            77.033,
            79.055,
            77.612,
            78.427,
            79.134,
            79.242,
            80.126,
            78.565,
            79.134,
            78.358,
            78.25,
            77.907,
            77.661,
            77.445,
            78.201,
            76.601,
            77.308,
            76.287,
            76.64,
            76.218,
            78.074,
            77.642,
            76.414,
            75.236,
            73.705,
            75.266,
            78.142,
            76.846,
            77.092,
            76.179,
            76.09,
            74.952,
            75.452,
            76.051,
            76.228,
            77.102,
            78.506,
            79.34,
            78.643,
            77.651,
            74.254,
            74.078,
            74.48,
            74.912,
            75.766,
            75.973,
            76.002,
            73.768,
            73.173,
            73.124,
            72.383,
            71.378,
            73.046,
            72.188,
            70.656,
            72.275,
            72.002,
            72.441,
            70.988};
        //</editor-fold>
        
        double expResult = 79.371;
        double result = IndicatorCalculator.SMA(count, data);

        assertEquals(expResult, result, 0.001);
    }

    @Test
    public void testSMA5() {
        System.out.println("SMA");
        int count = 5;
        double[] data = {
            79.68,
            80.68,
            80.65,
            81.13,
            81.75};
        
        double expResult = 80.778;
        double result = IndicatorCalculator.SMA(count, data);

        assertEquals(expResult, result, 0.001);
    }

    /**
     * Test of RSI method, of class IndicatorCalculator.
     */
    @Test
    public void testRSI() {
        System.out.println("RSI");
        double[] values = {
            79.68,
            80.68,
            80.65,
            81.13,
            81.75,
            82.46,
            81.82,
            81.66,
            80.716,
            80.626,
            80.577,
            81.193,
            81.998,
            81.938
        };

        double expResult = 3.52;
        double result = IndicatorCalculator.RSI(values);
        assertEquals(expResult, result, 0.001);
    }

}
