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
    class SingleDayEntry
    {
        LocalDate date = LocalDate.MIN;
        double adjClose = 0;
    }
    
    public CloseData(int count) {
        values = new SingleDayEntry[count];
    }
    
    SingleDayEntry[] values;
}
