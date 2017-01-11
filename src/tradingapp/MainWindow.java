/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import communication.IBCommunication;
import communication.OrderStatus;
import communication.Position;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import strategies.BackTesterNinety;
import strategies.RunnerNinety;

/**
 *
 * @author Muhe
 */
public class MainWindow extends javax.swing.JFrame {

    public final static String LOGGER_COMM_NAME = "CommLogger";
    public final static String LOGGER_TADELOG_NAME = "TradeLogLogger";
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME );
    private final static Logger loggerTradeLog = Logger.getLogger(LOGGER_TADELOG_NAME );
    
    private final IBCommunication m_comm;
    
    RunnerNinety ninetyRunner;
    private boolean m_connected = false;
    
    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
         
        try {
            logger.setLevel(Level.FINEST);
            
            FileHandler fileTxt = new FileHandler("Logging.txt");
            TextAreaLogHandler textHandler = new TextAreaLogHandler(logArea);
            
            // create a TXT formatter
            SimpleFormatter formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            
            logger.addHandler(fileTxt);
            logger.addHandler(textHandler);
            
            loggerComm.setLevel(Level.FINEST);
            FileHandler fileTxtComm = new FileHandler("LoggingComm.txt");
            TextAreaLogHandler textHandlerComm = new TextAreaLogHandler(commArea);
            
            loggerComm.addHandler(fileTxtComm);
            loggerComm.addHandler(textHandlerComm);
            
            loggerTradeLog.setLevel(Level.FINEST);
            
        } catch (IOException e) {
            throw new RuntimeException("Problems with creating the log files");
        }
        
        m_comm = new IBCommunication();
        
        ninetyRunner = new RunnerNinety(4001);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startButton = new javax.swing.JButton();
        tickSymbolTextField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        portTextField = new javax.swing.JTextField();
        buyButton = new javax.swing.JButton();
        sellButton = new javax.swing.JButton();
        loadStatusButton = new javax.swing.JButton();
        buyStatusButton = new javax.swing.JButton();
        SellAllButton = new javax.swing.JButton();
        saveStatusButton = new javax.swing.JButton();
        LoadStatusFileButton = new javax.swing.JButton();
        printStatusButton = new javax.swing.JButton();
        isOnCheckbox = new javax.swing.JCheckBox();
        logTabbedPane = new javax.swing.JTabbedPane();
        logScrollPane = new javax.swing.JScrollPane();
        logArea = new javax.swing.JTextArea();
        commScrollPane = new javax.swing.JScrollPane();
        commArea = new javax.swing.JTextArea();
        startNowButton = new javax.swing.JButton();
        getPositionsButton = new javax.swing.JButton();
        checkPositionsButton = new javax.swing.JButton();
        backTestButton = new javax.swing.JButton();
        fromBTField = new javax.swing.JTextField();
        toBTField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(300, 300));

        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        tickSymbolTextField.setText("AAPL");
        tickSymbolTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tickSymbolTextFieldActionPerformed(evt);
            }
        });

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        portTextField.setText("4001");
        portTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portTextFieldActionPerformed(evt);
            }
        });

        buyButton.setText("Buy");
        buyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyButtonActionPerformed(evt);
            }
        });

        sellButton.setText("Sell");
        sellButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sellButtonActionPerformed(evt);
            }
        });

        loadStatusButton.setText("LoadStatus");
        loadStatusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadStatusButtonActionPerformed(evt);
            }
        });

        buyStatusButton.setText("BuyStatus");
        buyStatusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyStatusButtonActionPerformed(evt);
            }
        });

        SellAllButton.setText("SellAllPositions");
        SellAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SellAllButtonActionPerformed(evt);
            }
        });

        saveStatusButton.setText("SaveStatus");
        saveStatusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveStatusButtonActionPerformed(evt);
            }
        });

        LoadStatusFileButton.setText("LoadStatusFromFile");
        LoadStatusFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadStatusFileButtonActionPerformed(evt);
            }
        });

        printStatusButton.setText("PrintStatus");
        printStatusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printStatusButtonActionPerformed(evt);
            }
        });

        isOnCheckbox.setText("on");
        isOnCheckbox.setEnabled(false);
        isOnCheckbox.setFocusable(false);

        logScrollPane.setAutoscrolls(true);

        logArea.setEditable(false);
        logArea.setColumns(20);
        logArea.setRows(5);
        logScrollPane.setViewportView(logArea);

        logTabbedPane.addTab("Log", logScrollPane);

        commArea.setEditable(false);
        commArea.setColumns(20);
        commArea.setRows(5);
        commScrollPane.setViewportView(commArea);

        logTabbedPane.addTab("Comm", commScrollPane);

        startNowButton.setText("StartNow");
        startNowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startNowButtonActionPerformed(evt);
            }
        });

        getPositionsButton.setText("GetPositions");
        getPositionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getPositionsButtonActionPerformed(evt);
            }
        });

        checkPositionsButton.setText("CheckPositions");
        checkPositionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkPositionsButtonActionPerformed(evt);
            }
        });

        backTestButton.setText("RunBacktest");
        backTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backTestButtonActionPerformed(evt);
            }
        });

        fromBTField.setText("2013-09-01");

        toBTField.setText("2016-08-01");

        jLabel1.setText("From");

        jLabel2.setText("To");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(startButton)
                            .addComponent(isOnCheckbox))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(tickSymbolTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sellButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(backTestButton)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(fromBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(toBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(connectButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(startNowButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 23, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(loadStatusButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyStatusButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(SellAllButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(saveStatusButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LoadStatusFileButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(printStatusButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(getPositionsButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(checkPositionsButton)))
                        .addGap(0, 13, Short.MAX_VALUE))
                    .addComponent(logTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(tickSymbolTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(connectButton)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadStatusButton)
                    .addComponent(buyStatusButton)
                    .addComponent(SellAllButton)
                    .addComponent(buyButton)
                    .addComponent(sellButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveStatusButton)
                    .addComponent(LoadStatusFileButton)
                    .addComponent(isOnCheckbox)
                    .addComponent(startNowButton)
                    .addComponent(fromBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(printStatusButton)
                    .addComponent(getPositionsButton)
                    .addComponent(checkPositionsButton)
                    .addComponent(backTestButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(logTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (!ninetyRunner.isStartScheduled) {
            ninetyRunner.ScheduleFirstCheck();
            isOnCheckbox.setSelected(true);
        } else {
            ninetyRunner.Stop();
            isOnCheckbox.setSelected(false);
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void tickSymbolTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tickSymbolTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tickSymbolTextFieldActionPerformed

    private void portTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_portTextFieldActionPerformed

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        m_comm.connect(Integer.parseInt(portTextField.getText()));
        m_connected = true;
    }//GEN-LAST:event_connectButtonActionPerformed

    private void buyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buyButtonActionPerformed
        TradeOrder order = new TradeOrder();
        order.orderType = TradeOrder.OrderType.BUY;
        order.tickerSymbol = tickSymbolTextField.getText();
        order.position = 10;
        
        ninetyRunner.broker.connect();
        
        //m_comm.PlaceOrder(order);
        logger.info("Placing " + order.toString());
        
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep((long) (Math.random() * 200));
                ninetyRunner.broker.PlaceOrder(order);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        /*Thread[] threads = new Thread[10];
        for(int i = 0; i < 10; i++) {
            threads[i] = new Thread(new Runnable() {
                            public void run() {
                                ninetyRunner.broker.PlaceOrder(order);
                            }
                         });
            threads[i].start();
        }*/
        
        if (ninetyRunner.broker.waitUntilOrdersClosed(10)) {
            logger.info("Order filled in time");
        } else {
            logger.info("Order NOT filled in time");
        }
        
        ninetyRunner.broker.disconnect();
    }//GEN-LAST:event_buyButtonActionPerformed

    private void sellButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sellButtonActionPerformed
        TradeOrder order = new TradeOrder();
        order.orderType = TradeOrder.OrderType.SELL;
        order.tickerSymbol = tickSymbolTextField.getText();
        order.position = 10;
        
        ninetyRunner.broker.connect();
        
        //m_comm.PlaceOrder(order);
        logger.info("Placing " + order.toString());
        
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep((long) (Math.random() * 200));
                ninetyRunner.broker.PlaceOrder(order);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /*Thread[] threads = new Thread[10];
        for(int i = 0; i < 10; i++) {
            threads[i] = new Thread(new Runnable() {
                            public void run() {
                                ninetyRunner.broker.PlaceOrder(order);
                            }
                         });
            threads[i].start();
        }*/

        if (ninetyRunner.broker.waitUntilOrdersClosed(10)) {
            logger.info("Order filled in time");
        } else {
            logger.info("Order NOT filled in time");
        }
        for (OrderStatus value : ninetyRunner.broker.activeOrdersMap.values()) {
            logger.info("Still active: " + value);
        }
        
        ninetyRunner.broker.disconnect();
    }//GEN-LAST:event_sellButtonActionPerformed

    private void loadStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadStatusButtonActionPerformed
        //ninetyRunner.m_statusDataFor90.LoadStatus();
        ninetyRunner.statusData.PrintStatus();
    }//GEN-LAST:event_loadStatusButtonActionPerformed

    private void buyStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buyStatusButtonActionPerformed
        if (!m_connected) {
            ninetyRunner.broker.connect();
            m_connected = true;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        ninetyRunner.BuyLoadedStatus();
    }//GEN-LAST:event_buyStatusButtonActionPerformed

    private void SellAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SellAllButtonActionPerformed
    }//GEN-LAST:event_SellAllButtonActionPerformed

    private void saveStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveStatusButtonActionPerformed
        ninetyRunner.statusData.SaveHeldPositionsToFile();
        logger.info("Status saved.");
    }//GEN-LAST:event_saveStatusButtonActionPerformed

    private void LoadStatusFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadStatusFileButtonActionPerformed
        ninetyRunner.statusData.ReadHeldPositions();
        ninetyRunner.statusData.PrintStatus();
    }//GEN-LAST:event_LoadStatusFileButtonActionPerformed

    private void printStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printStatusButtonActionPerformed
        ninetyRunner.statusData.PrintStatus();
    }//GEN-LAST:event_printStatusButtonActionPerformed

    private void startNowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startNowButtonActionPerformed
        if (!ninetyRunner.isStartScheduled) {
            ninetyRunner.RunNow();
            isOnCheckbox.setSelected(true);
        } else {
            ninetyRunner.Stop();
            isOnCheckbox.setSelected(false);
        }
    }//GEN-LAST:event_startNowButtonActionPerformed

    private void getPositionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getPositionsButtonActionPerformed
        ninetyRunner.broker.connect();
        List<Position> allPositions = ninetyRunner.broker.getAllPositions();
        
        for (Position position : allPositions) {
            logger.info("Stock :" + position.tickerSymbol + ", position: " + position.pos + ", avgPrice: " + position.avgPrice);
        }
        
        ninetyRunner.broker.disconnect();
    }//GEN-LAST:event_getPositionsButtonActionPerformed

    private void checkPositionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkPositionsButtonActionPerformed
        ninetyRunner.broker.connect();
        
        ninetyRunner.CheckHeldPositions();
        
        ninetyRunner.broker.disconnect();
    }//GEN-LAST:event_checkPositionsButtonActionPerformed

    private void backTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backTestButtonActionPerformed
        //LocalDate start = LocalDate.parse("2015-01-01");                                          
        //LocalDate end = LocalDate.parse("2016-01-01");
        
        LocalDate start = LocalDate.parse(fromBTField.getText());                                          
        LocalDate end = LocalDate.parse(toBTField.getText());
        
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                BackTesterNinety.RunTest(start, end);
            }
        });
        
        thr.start();
    }//GEN-LAST:event_backTestButtonActionPerformed

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
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton LoadStatusFileButton;
    private javax.swing.JButton SellAllButton;
    private javax.swing.JButton backTestButton;
    private javax.swing.JButton buyButton;
    private javax.swing.JButton buyStatusButton;
    private javax.swing.JButton checkPositionsButton;
    private javax.swing.JTextArea commArea;
    private javax.swing.JScrollPane commScrollPane;
    private javax.swing.JButton connectButton;
    private javax.swing.JTextField fromBTField;
    private javax.swing.JButton getPositionsButton;
    private javax.swing.JCheckBox isOnCheckbox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton loadStatusButton;
    private javax.swing.JTextArea logArea;
    private javax.swing.JScrollPane logScrollPane;
    private javax.swing.JTabbedPane logTabbedPane;
    private javax.swing.JTextField portTextField;
    private javax.swing.JButton printStatusButton;
    private javax.swing.JButton saveStatusButton;
    private javax.swing.JButton sellButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton startNowButton;
    private javax.swing.JTextField tickSymbolTextField;
    private javax.swing.JTextField toBTField;
    // End of variables declaration//GEN-END:variables
}
