/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.util.logging.Level;
import java.util.logging.Logger;
import strategies.NinetyChecker;
import strategies.NinetyScheduler;

/**
 *
 * @author Muhe
 */
public class MainWindow extends javax.swing.JFrame {

    public final static String LOGGER_COMM_NAME = "CommLogger";
    public final static String LOGGER_TADELOG_NAME = "TradeLogLogger";
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    
    NinetyScheduler ninetyScheduler;
    
    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
        
        TradeLogger.getInstance().initializeTextAreas(logArea, fineLogArea, commArea);
        
        ninetyScheduler = new NinetyScheduler();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 416, Short.MAX_VALUE)
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
                    .addComponent(checkPositionsButton))
                .addGap(18, 18, 18)
                .addComponent(logTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (!ninetyScheduler.isStartScheduled) {
            new Thread(() -> {
                ninetyScheduler.ScheduleFirstCheck();
            }).start();
            isOnCheckbox.setSelected(true);
            startButton.setText("Stop");
            startNowButton.setText("Stop");
        } else {
            ninetyScheduler.Stop();
            isOnCheckbox.setSelected(false);
            startButton.setText("Start");
            startNowButton.setText("StartNow");
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void checkPositionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkPositionsButtonActionPerformed
        new Thread(() -> {
            boolean connected = ninetyScheduler.broker.connected;
            if (!connected) {
                ninetyScheduler.broker.connect();
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }

            NinetyChecker.PerformChecks(ninetyScheduler.statusData, ninetyScheduler.broker);

            if (!connected) {
                ninetyScheduler.broker.disconnect();
            }
        }).start();
    }//GEN-LAST:event_checkPositionsButtonActionPerformed

    private void startNowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startNowButtonActionPerformed
        if (!ninetyScheduler.isStartScheduled) {
            ninetyScheduler.RunNow();
            isOnCheckbox.setSelected(true);
            startNowButton.setText("Stop");
            startButton.setText("Stop");
        } else {
            ninetyScheduler.Stop();
            isOnCheckbox.setSelected(false);
            startNowButton.setText("StartNow");
            startButton.setText("Start");
        }
    }//GEN-LAST:event_startNowButtonActionPerformed

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
