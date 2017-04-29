/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import data.DataGetterHistQuandl;
import data.DataGetterHistYahoo;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.TextAreaLogHandler;

/**
 *
 * @author Muhe
 */
public final class Backtester extends javax.swing.JFrame {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Creates new form Backtester
     */
    public Backtester() {
        initComponents();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.log(Level.SEVERE, "Uncaught Exception!!!", e);
            }

        });
        TextAreaLogHandler textHandlerInfo = new TextAreaLogHandler(logArea, Level.INFO, Level.SEVERE, false);
        logger.addHandler(textHandlerInfo);
        
        GlobalConfig.AddDataGetterHist(new DataGetterHistYahoo());
        GlobalConfig.AddDataGetterHist(new DataGetterHistQuandl());
        
        LoadBTSettings();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        logArea = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        fromBTField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        toBTField = new javax.swing.JTextField();
        backTest90Button = new javax.swing.JButton();
        logLvlComboBox = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        capitalField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        leverageField = new javax.swing.JTextField();
        reinvestCheckBox = new javax.swing.JCheckBox();
        backTestVIXButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Trading app 90 - BACKTESTER");

        logArea.setEditable(false);
        logArea.setColumns(20);
        logArea.setRows(5);
        jScrollPane1.setViewportView(logArea);

        jLabel1.setText("From");

        fromBTField.setText("2013-01-01");

        jLabel2.setText("To");

        toBTField.setText("2017-02-25");

        backTest90Button.setText("RunBacktest 90");
        backTest90Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backTest90ButtonActionPerformed(evt);
            }
        });

        logLvlComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Short", "Stats only", "Info" }));
        logLvlComboBox.setToolTipText("");

        jLabel3.setText("Log level");

        capitalField.setText("20000");

        jLabel4.setText("Capital");

        jLabel5.setText("Leverage");

        leverageField.setText("3");

        reinvestCheckBox.setText("Reinvest");

        backTestVIXButton.setText("RunBacktest VIX");
        backTestVIXButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backTestVIXButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fromBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(toBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel4)
                                .addGap(5, 5, 5)
                                .addComponent(capitalField, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(leverageField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(184, 184, 184)
                                .addComponent(backTestVIXButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(reinvestCheckBox))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(backTest90Button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(logLvlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fromBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toBTField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(backTest90Button)
                    .addComponent(capitalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(leverageField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(backTestVIXButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(logLvlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(reinvestCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 478, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backTest90ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backTest90ButtonActionPerformed
        Level logLvl;
        int logIndex = logLvlComboBox.getSelectedIndex();

        switch (logIndex) {
            case 0:
                logLvl = BTLogLvl.BACKTEST;
                break;
            case 1:
                logLvl = BTLogLvl.BT_STATS;
                break;
            case 2:
                logLvl = Level.INFO;
                break;
            default:
                logLvl = BTLogLvl.INFO;
        }

        logger.setLevel(logLvl);

        LocalDate start = LocalDate.parse(fromBTField.getText());
        LocalDate end = LocalDate.parse(toBTField.getText());
        
        double capital = Double.parseDouble(capitalField.getText());
        double leverage = Double.parseDouble(leverageField.getText());
        
        FilePaths.tradingStatusPathFileInput = "backtest/TradingStatus.xml";
        FilePaths.tradingStatusPathFileInput = "backtest/TradingStatus.xml";
        
        FilePaths.tradeLogDetailedPathFile = "backtest/TradeLogDetailed.txt";
        FilePaths.tradeLogPathFile = "backtest/TradeLog.csv";
        
        FilePaths.equityPathFile = "backtest/Equity.csv";
        FilePaths.tickerListPathFile = "backtest/tickerList.txt";

        new Thread(() -> {
            BackTesterNinety.RunTest(new BTSettings(start, end, capital, leverage, reinvestCheckBox.isSelected()));
        }).start();
    }//GEN-LAST:event_backTest90ButtonActionPerformed

    private void backTestVIXButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backTestVIXButtonActionPerformed
        Level logLvl;
        int logIndex = logLvlComboBox.getSelectedIndex();

        switch (logIndex) {
            case 0:
                logLvl = BTLogLvl.BACKTEST;
                break;
            case 1:
                logLvl = BTLogLvl.BT_STATS;
                break;
            case 2:
                logLvl = Level.INFO;
                break;
            default:
                logLvl = BTLogLvl.INFO;
        }

        logger.setLevel(logLvl);

        LocalDate start = LocalDate.parse(fromBTField.getText());
        LocalDate end = LocalDate.parse(toBTField.getText());
        
        double capital = Double.parseDouble(capitalField.getText());
        double leverage = Double.parseDouble(leverageField.getText());
        
        BacktesterVXVrVXMT.runBacktest(new BTSettings(start, end, capital, leverage, reinvestCheckBox.isSelected()));
        
    }//GEN-LAST:event_backTestVIXButtonActionPerformed

    public void LoadBTSettings() {
        try {
            File inputFile = new File(FilePaths.backtestSettings);
            
            if (!inputFile.exists()) {
                return;
            }
            
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            Attribute attStart = rootElement.getAttribute("start");
            String start = attStart.getValue();
            fromBTField.setText(start);
            
            Attribute attEnd = rootElement.getAttribute("end");
            String end = attEnd.getValue();
            toBTField.setText(end);
            
            Attribute attCapital = rootElement.getAttribute("capital");
            String capital = attCapital.getValue();
            capitalField.setText(capital);
            
            Attribute attLeverage = rootElement.getAttribute("leverage");
            String leverage = attLeverage.getValue();
            leverageField.setText(leverage);

            Attribute attReinvest = rootElement.getAttribute("reinvest");
            boolean reinvest = Boolean.parseBoolean(attReinvest.getValue());
            reinvestCheckBox.setSelected(reinvest);

        } catch (JDOMException e) {
            e.printStackTrace();
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }
    
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
            java.util.logging.Logger.getLogger(Backtester.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Backtester.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Backtester.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Backtester.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Backtester().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backTest90Button;
    private javax.swing.JButton backTestVIXButton;
    private javax.swing.JTextField capitalField;
    private javax.swing.JTextField fromBTField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField leverageField;
    private javax.swing.JTextArea logArea;
    private javax.swing.JComboBox<String> logLvlComboBox;
    private javax.swing.JCheckBox reinvestCheckBox;
    private javax.swing.JTextField toBTField;
    // End of variables declaration//GEN-END:variables
}
