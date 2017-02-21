/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import communication.BrokerIB;
import communication.IBroker;
import data.DataGetterActFile;
import data.DataGetterActGoogle;
import data.DataGetterActIB;
import data.DataGetterHistFile;
import data.DataGetterHistQuandl;
import data.DataGetterHistYahoo;
import data.TickersToTrade;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import strategies.NinetyChecker;
import strategies.NinetyScheduler;
import strategies.StockDataForNinety;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.Settings;
import tradingapp.TradeLogger;
import tradingapp.TradeOrder;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class TestPlatform extends javax.swing.JFrame {
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    
    NinetyScheduler ninetySchedulerNoBroker;

    /**
     * Creates new form TestPlatform
     */
    public TestPlatform() {
        initComponents();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.log(Level.SEVERE, "Uncaught Exception!!!", e);
            }

        });
        
        TradeTimer.SetToday(LocalDate.of(2017, 2, 17));

        TradeLogger.getInstance().initializeTextAreas(logArea, Level.INFO, fineLogArea, Level.FINEST, commArea, Level.FINEST);
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logTabbedPane = new javax.swing.JTabbedPane();
        logScrollPane = new javax.swing.JScrollPane();
        logArea = new javax.swing.JTextArea();
        fineLogScrollPane = new javax.swing.JScrollPane();
        fineLogArea = new javax.swing.JTextArea();
        commScrollPane = new javax.swing.JScrollPane();
        commArea = new javax.swing.JTextArea();
        startButton = new javax.swing.JButton();
        isOnCheckbox = new javax.swing.JCheckBox();
        checkPositionsButton = new javax.swing.JButton();
        startNowButton = new javax.swing.JButton();
        tickerField = new javax.swing.JTextField();
        buyButton = new javax.swing.JButton();
        testDataYahooButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Trading app 90 - TEST PLATFORM");

        logScrollPane.setAutoscrolls(true);

        logArea.setEditable(false);
        logArea.setColumns(20);
        logArea.setRows(5);
        logScrollPane.setViewportView(logArea);

        logTabbedPane.addTab("Info log", logScrollPane);

        fineLogArea.setColumns(20);
        fineLogArea.setRows(5);
        fineLogScrollPane.setViewportView(fineLogArea);

        logTabbedPane.addTab("Detailed log", fineLogScrollPane);

        commArea.setEditable(false);
        commArea.setColumns(20);
        commArea.setRows(5);
        commScrollPane.setViewportView(commArea);

        logTabbedPane.addTab("Communication", commScrollPane);

        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        isOnCheckbox.setText("on");
        isOnCheckbox.setEnabled(false);
        isOnCheckbox.setFocusable(false);

        checkPositionsButton.setText("CheckPositions");
        checkPositionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkPositionsButtonActionPerformed(evt);
            }
        });

        startNowButton.setText("StartNow");
        startNowButton.setMaximumSize(new java.awt.Dimension(80, 23));
        startNowButton.setMinimumSize(new java.awt.Dimension(80, 23));
        startNowButton.setPreferredSize(new java.awt.Dimension(80, 25));
        startNowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startNowButtonActionPerformed(evt);
            }
        });

        tickerField.setText("INTC");

        buyButton.setText("Buy");
        buyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyButtonActionPerformed(evt);
            }
        });

        testDataYahooButton.setText("Test Load Data Yahoo");
        testDataYahooButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testDataYahooButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(isOnCheckbox))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tickerField, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buyButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 373, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkPositionsButton)
                        .addGap(5, 5, 5)
                        .addComponent(startNowButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(testDataYahooButton))
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(logTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 673, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(startNowButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(isOnCheckbox)
                    .addComponent(checkPositionsButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tickerField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buyButton)
                    .addComponent(testDataYahooButton))
                .addGap(0, 538, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(95, Short.MAX_VALUE)
                    .addComponent(logTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 491, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (!ninetySchedulerNoBroker.isStartScheduled) {
            new Thread(() -> {
                ninetySchedulerNoBroker.ScheduleFirstCheck();
            }).start();
            isOnCheckbox.setSelected(true);
            startButton.setText("Stop");
            startNowButton.setText("Stop");
        } else {
            ninetySchedulerNoBroker.Stop();
            isOnCheckbox.setSelected(false);
            startButton.setText("Start");
            startNowButton.setText("StartNow");
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void checkPositionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkPositionsButtonActionPerformed
        new Thread(() -> {
            boolean connected = ninetySchedulerNoBroker.broker.isConnected();
            if (!connected) {
                ninetySchedulerNoBroker.broker.connect();
            }

            NinetyChecker.CheckHeldPositions(ninetySchedulerNoBroker.statusData, ninetySchedulerNoBroker.broker);

            if (!connected) {
                ninetySchedulerNoBroker.broker.disconnect();
            }
        }).start();
    }//GEN-LAST:event_checkPositionsButtonActionPerformed

    private void startNowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startNowButtonActionPerformed
        FilePaths.tradingStatusPathFileInput = "testData/TradingStatus.xml";
        FilePaths.tradingStatusPathFileOutput = "testData/TradingStatusOutput.xml";
        FilePaths.equityPathFile = "testData/Equity.csv";
        FilePaths.tradeLogPathFile = "testData/TradeLog.csv";
        FilePaths.tradeLogDetailedPathFile = "testData/TradeLogDetailed.txt";
        FilePaths.specialTradingDaysPathFile = "testData/specialTradingDays.xml";
        FilePaths.dataLogDirectory = "testData/dataLog/";
        
        Settings.ReadSettings();
        
        IBroker broker = new BrokerIBReadOnly(Settings.port, Settings.clientId);
        
        GlobalConfig.sendMails = false;
        GlobalConfig.ClearGetters();
        
        GlobalConfig.AddDataGetterAct(new DataGetterActIB(broker));
        GlobalConfig.AddDataGetterAct(new DataGetterActGoogle());
        
        GlobalConfig.AddDataGetterHist(new DataGetterHistYahoo());
        GlobalConfig.AddDataGetterHist(new DataGetterHistQuandl());
        
        ninetySchedulerNoBroker = new NinetyScheduler( broker );
        
        if (!ninetySchedulerNoBroker.isStartScheduled) {
            ninetySchedulerNoBroker.RunNow();
            isOnCheckbox.setSelected(true);
            startNowButton.setText("Stop");
            startButton.setText("Stop");
        } else {
            ninetySchedulerNoBroker.Stop();
            isOnCheckbox.setSelected(false);
            startNowButton.setText("StartNow");
            startButton.setText("Start");
        }
    }//GEN-LAST:event_startNowButtonActionPerformed

    private void buyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buyButtonActionPerformed
        new Thread(() -> {
            boolean connected = ninetySchedulerNoBroker.broker.isConnected();
            if (!connected) {
                ninetySchedulerNoBroker.broker.connect();
            }
            
            TradeOrder tradeOrder = new TradeOrder();
            tradeOrder.tickerSymbol = tickerField.getText();
            tradeOrder.position = 10;
            tradeOrder.orderType = TradeOrder.OrderType.BUY;
            
            ninetySchedulerNoBroker.broker.PlaceOrder(tradeOrder);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestPlatform.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (!connected) {
                ninetySchedulerNoBroker.broker.disconnect();
            }
        }).start();
    }//GEN-LAST:event_buyButtonActionPerformed

    private void testDataYahooButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testDataYahooButtonActionPerformed
        LocalDate testDay = LocalDate.of(2017, 2, 17);
        TradeTimer.SetToday(testDay);
        
        FilePaths.tradingStatusPathFileInput = "testData/TradingStatus.xml";
        FilePaths.tradingStatusPathFileOutput = "testData/TradingStatusOutput.xml";
        FilePaths.equityPathFile = "testData/Equity.csv";
        FilePaths.tradeLogPathFile = "testData/TradeLog.csv";
        FilePaths.tradeLogDetailedPathFile = "testData/TradeLogDetailed.txt";
        FilePaths.specialTradingDaysPathFile = "testData/specialTradingDays.xml";
        FilePaths.dataLogDirectory = "testData/dataLog/";
        
        Settings.ReadSettings();
        TradeTimer.LoadSpecialTradingDays();
        
        GlobalConfig.sendMails = false;
        GlobalConfig.ClearGetters();
        
        String pathToSourceData = "testData/Data 2017-02-17/Historic/";
        
        //GlobalConfig.AddDataGetterAct(new DataGetterActFile(pathToSourceData));
        //GlobalConfig.AddDataGetterHist(new DataGetterHistFile(pathToSourceData));
        
        //GlobalConfig.AddDataGetterHist(new DataGetterHistYahoo());
        //GlobalConfig.AddDataGetterHist(new DataGetterHistQuandl());
        
        IBroker broker = new BrokerIBReadOnly(4001, 1);
        broker.connect();
        /*for (String ticker : TickersToTrade.GetTickers()) {
            broker.RequestRealtimeData(ticker);
        }*/
        
        GlobalConfig.AddDataGetterAct(new DataGetterActIB(broker));
        GlobalConfig.AddDataGetterHist(new DataGetterHistYahoo());
        
        StockDataForNinety stockDataForNinety = new StockDataForNinety();
        stockDataForNinety.SubscribeRealtimeData(broker);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestPlatform.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        new Thread(() -> {
            stockDataForNinety.PrepareData();
            broker.disconnect();
            
            stockDataForNinety.SaveHistDataToFiles();
            stockDataForNinety.SaveStockIndicatorsToFiles();
            stockDataForNinety.SaveIndicatorsToCSVFile();

            boolean isOk = true;

            isOk &= TestUtils.CompareDirectories("testData/Data 2017-02-17/Historic/", FilePaths.dataLogDirectory + testDay + "/Historic/", TestUtils.histDataComparator);

            isOk &= TestUtils.CompareDirectories("testData/Data 2017-02-17/Indicators/", FilePaths.dataLogDirectory + testDay + "/Indicators/", TestUtils.indicatorTxtComparator);

            isOk &= TestUtils.CompareFiles("testData/Data 2017-02-17/indicators.csv", FilePaths.dataLogDirectory + testDay + "/indicators.csv", TestUtils.indicatorCsvComparator);

            if (isOk) {
                logger.info("Test finished successfully.");
            } else {
                logger.info("Test FAILED.");
            }
        }).start();
    }//GEN-LAST:event_testDataYahooButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TestPlatform.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TestPlatform.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TestPlatform.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TestPlatform.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TestPlatform().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buyButton;
    private javax.swing.JButton checkPositionsButton;
    private javax.swing.JTextArea commArea;
    private javax.swing.JScrollPane commScrollPane;
    private javax.swing.JTextArea fineLogArea;
    private javax.swing.JScrollPane fineLogScrollPane;
    private javax.swing.JCheckBox isOnCheckbox;
    private javax.swing.JTextArea logArea;
    private javax.swing.JScrollPane logScrollPane;
    private javax.swing.JTabbedPane logTabbedPane;
    private javax.swing.JButton startButton;
    private javax.swing.JButton startNowButton;
    private javax.swing.JButton testDataYahooButton;
    private javax.swing.JTextField tickerField;
    // End of variables declaration//GEN-END:variables
}
