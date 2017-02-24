/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Muhe
 */
public class HeldStockTest {
    
    public HeldStockTest() {
    }
    
    private HeldStock createHeldStock() {
        HeldStock held = new HeldStock();
        
        StockPurchase purch = new StockPurchase();
        purch.portions = 1;
        purch.position = 10;
        purch.priceForOne = 50;
        
        held.purchases.add(purch);
        
        purch = new StockPurchase();
        purch.portions = 2;
        purch.position = 20;
        purch.priceForOne = 40;
        
        held.purchases.add(purch);
        
        purch = new StockPurchase();
        purch.portions = 3;
        purch.position = 30;
        purch.priceForOne = 30;
        
        held.purchases.add(purch);
        
        return held;
    }
    
    /**
     * Test of GetPosition method, of class HeldStock.
     */
    @Test
    public void testGetPosition() {
        System.out.println("GetPosition");
        HeldStock instance = createHeldStock();
        int expResult = 60;
        int result = instance.GetPosition();
        assertEquals(expResult, result);
    }

    /**
     * Test of GetPortions method, of class HeldStock.
     */
    @Test
    public void testGetPortions() {
        System.out.println("GetPortions");
        HeldStock instance = createHeldStock();
        int expResult = 6;
        int result = instance.GetPortions();
        assertEquals(expResult, result);
    }

    /**
     * Test of GetTotalPricePaid method, of class HeldStock.
     */
    @Test
    public void testGetTotalPricePaid() {
        System.out.println("GetTotalPricePaid");
        HeldStock instance = createHeldStock();
        double expResult = 500.0 + 800.0 + 900.0;
        double result = instance.GetTotalPricePaid();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of GetAvgPricePaid method, of class HeldStock.
     */
    @Test
    public void testGetAvgPricePaid() {
        System.out.println("GetAvgPricePaid");
        HeldStock instance = createHeldStock();
        double expResult = (500.0 + 800.0 + 900.0) / (10 + 20 + 30);
        double result = instance.GetAvgPricePaid();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of AddToXml method, of class HeldStock.
     */
    /*@Test
    public void testAddToXml() {
        System.out.println("AddToXml");
        Element rootElement = null;
        HeldStock instance = new HeldStock();
        instance.AddToXml(rootElement);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of LoadFromXml method, of class HeldStock.
     */
    /*@Test
    public void testLoadFromXml() {
        System.out.println("LoadFromXml");
        Element heldElement = null;
        HeldStock instance = new HeldStock();
        instance.LoadFromXml(heldElement);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toStringLong method, of class HeldStock.
     */
    /*@Test
    public void testToStringLong() {
        System.out.println("toStringLong");
        HeldStock instance = new HeldStock();
        String expResult = "";
        String result = instance.toStringLong();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of CalculateProfitIfSold method, of class HeldStock.
     */
    @Test
    public void testCalculateProfitIfSold() {
        System.out.println("CalculateProfitIfSold");
        double actValue = 35.0;
        HeldStock instance = createHeldStock();
        double expResult = (35.0 * 60) - (500.0 + 800.0 + 900.0);
        double result = instance.CalculateProfitIfSold(actValue);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of CalculatePercentProfitIfSold method, of class HeldStock.
     */
    @Test
    public void testCalculatePercentProfitIfSold() {
        System.out.println("CalculatePercentProfitIfSold");
        double actValue = 35.0;
        HeldStock instance = createHeldStock();
        double expResult = ((35.0 * 60) - (500.0 + 800.0 + 900.0)) / (500.0 + 800.0 + 900.0) * 100;
        double result = instance.CalculatePercentProfitIfSold(actValue);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of toString method, of class HeldStock.
     */
    /*@Test
    public void testToString() {
        System.out.println("toString");
        HeldStock instance = new HeldStock();
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/
    
}
