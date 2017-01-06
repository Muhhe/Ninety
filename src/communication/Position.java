/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

/**
 *
 * @author Muhe
 */
public class Position {
    public String tickerSymbol;
    public double avgPrice;
    public int pos;

    public Position(String tickerSymbol, double avgPrice, int pos) {
        this.tickerSymbol = tickerSymbol;
        this.avgPrice = avgPrice;
        this.pos = pos;
    }
    
    public String toString() {
        return tickerSymbol + ", position: " + pos + ", avgPrice: " + avgPrice;
    }
}
