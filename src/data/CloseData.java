/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.time.LocalDate;

/**
 *
 * @author Muhe
 */
public class CloseData {
    public CloseData(double[] adjCloses, LocalDate[] dates) {
        this.adjCloses = adjCloses;
        this.dates = dates;
    }
    public CloseData(int count) {
        dates = new LocalDate[count];
        adjCloses = new double[count];
        
        for (int i = 0; i < count; i++) {
            dates[i] = LocalDate.MIN;
        }
    }
    
    public LocalDate[] dates;
    public double[] adjCloses;
}
