/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import java.util.logging.Level;

/**
 *
 * @author Muhe
 */
public class BTLogLvl extends Level {

    public static final Level BACKTEST = new BTLogLvl("BACKTEST", 850);

    protected BTLogLvl(String name, int value) {
        super(name, value, null);
    }
}
