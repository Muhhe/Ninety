/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import communication.BrokerIB;
import communication.IBroker;
import data.getters.DataGetterActGoogle;
import data.getters.DataGetterActIB;
import data.getters.DataGetterHistAlpha;
import data.getters.DataGetterHistFile;
import data.getters.DataGetterHistGoogle;
import data.getters.DataGetterHistIB;
import data.getters.DataGetterHistQuandl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import strategyMondayBuyer.MBScheduler;
import test.BrokerIBReadOnly;

/**
 *
 * @author Muhe
 */
public class MainWindowMB extends javax.swing.JFrame {

    public final static String LOGGER_COMM_NAME = "COMM";
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    MBScheduler mbScheduler;

    /**
     * Creates new form MainWindow
     */
    public MainWindowMB() {
        initComponents();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.log(Level.SEVERE, "Uncaught Exception!!!", e);
            }

        });

        TradeLogger.getInstance().initializeTextAreas(logArea, Level.INFO, fineLogArea, Level.FINE, commArea, Level.FINEST);

        Settings.ReadSettings();

        IBroker broker = new BrokerIB(Settings.port, Settings.clientId, IBroker.SecType.STK);

        //GlobalConfig.AddDataGetterAct(new DataGetterActIB(broker));
        //GlobalConfig.AddDataGetterAct(new DataGetterActGoogle());

        GlobalConfig.AddDataGetterHist(new DataGetterHistIB(broker));
        
        //LocalDate today = TradeTimer.GetLocalDateNow();
        //String todayString = today.toString();
        //GlobalConfig.AddDataGetterHist(new DataGetterHistFile("backtest/SP500/", "ddMMyyyy", 4, true));
        //GlobalConfig.AddDataGetterHist(new DataGetterHistQuandl());
        //GlobalConfig.AddDataGetterHist(new DataGetterHistGoogle());

        mbScheduler = new MBScheduler(broker);

        new Thread(MainWindowMB::StartSocketServer).start();
    }

    public static void StartSocketServer() {
        try {
            ServerSocket server = new ServerSocket(4125);
            logger.fine("Opened test port on: " + server.getLocalPort());
            while (true) {
                try (Socket sock = server.accept()) {
                    //InetAddress addr = sock.getInetAddress();
                    //logger.fine("Connection made to " + addr.getHostName() + " (" + addr.getHostAddress() + ")");
                }
                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            logger.warning("Exception in socket server detected: " + e);
        }
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
        isOnCheckbox = new javax.swing.JCheckBox();
        logTabbedPane = new javax.swing.JTabbedPane();
        logScrollPane = new javax.swing.JScrollPane();
        logArea = new javax.swing.JTextArea();
        fineLogScrollPane = new javax.swing.JScrollPane();
        fineLogArea = new javax.swing.JTextArea();
        commScrollPane = new javax.swing.JScrollPane();
        commArea = new javax.swing.JTextArea();
        startNowButton = new javax.swing.JButton();
        checkPositionsButton = new javax.swing.JButton();
        ReportButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Trading app Monday Buyer");
        setMinimumSize(new java.awt.Dimension(300, 300));

        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
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

        startNowButton.setText("StartNow");
        startNowButton.setMaximumSize(new java.awt.Dimension(80, 23));
        startNowButton.setMinimumSize(new java.awt.Dimension(80, 23));
        startNowButton.setPreferredSize(new java.awt.Dimension(80, 25));
        startNowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startNowButtonActionPerformed(evt);
            }
        });

        checkPositionsButton.setText("CheckPositions");
        checkPositionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkPositionsButtonActionPerformed(evt);
            }
        });

        ReportButton.setText("Report");
        ReportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReportButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logTabbedPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(isOnCheckbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 345, Short.MAX_VALUE)
                        .addComponent(ReportButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkPositionsButton)
                        .addGap(5, 5, 5)
                        .addComponent(startNowButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(startNowButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(isOnCheckbox)
                    .addComponent(checkPositionsButton)
                    .addComponent(ReportButton))
                .addGap(18, 18, 18)
                .addComponent(logTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (!mbScheduler.isStartScheduled) {
            new Thread(() -> {
                mbScheduler.ScheduleFirstCheck();
            }).start();
            isOnCheckbox.setSelected(true);
            startButton.setText("Stop");
            startNowButton.setText("Stop");
        } else {
            mbScheduler.Stop();
            isOnCheckbox.setSelected(false);
            startButton.setText("Start");
            startNowButton.setText("StartNow");
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void checkPositionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkPositionsButtonActionPerformed
        /*HeldStock a = null;
        if (a == null) {
            a.purchases.add(new StockPurchase());
        }*/

        new Thread(() -> {
            boolean connected = mbScheduler.broker.isConnected();
            if (!connected) {
                mbScheduler.broker.connect();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainWindowMB.class.getName()).log(Level.SEVERE, null, ex);
            }

            //NinetyChecker.CheckHeldPositions(mbScheduler.statusData, mbScheduler.broker, 10);
            //NinetyChecker.CheckCash(mbScheduler.statusData, mbScheduler.broker);

            if (!connected) {
                mbScheduler.broker.disconnect();
            }
        }).start();
    }//GEN-LAST:event_checkPositionsButtonActionPerformed

    private void startNowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startNowButtonActionPerformed
        if (!mbScheduler.isStartScheduled) {
            mbScheduler.ScheduleForNow();
            isOnCheckbox.setSelected(true);
            startNowButton.setText("Stop");
            startButton.setText("Stop");
        } else {
            mbScheduler.Stop();
            isOnCheckbox.setSelected(false);
            startNowButton.setText("StartNow");
            startButton.setText("Start");
        }
    }//GEN-LAST:event_startNowButtonActionPerformed

    private void ReportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReportButtonActionPerformed
        IBroker broker = new BrokerIB(Settings.port, Settings.clientId, IBroker.SecType.IND);
        broker.connect();
        broker.RequestHistoricalData("SPY", Report.GetNrOfDaysInEquity());
        broker.SubscribeRealtimeData("SPY");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }

        Report.Generate(new DataGetterHistIB(broker), new DataGetterActIB(broker), "SPY", false);
    }//GEN-LAST:event_ReportButtonActionPerformed

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
                new MainWindowMB().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ReportButton;
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
    // End of variables declaration//GEN-END:variables
}