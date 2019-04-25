package com.soasta.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * represents a user defined threshold, for a specific transaction
 * @author dbekel
 *
 */
public class TransactionThreshold extends AbstractDescribableImpl<TransactionThreshold>
{
  private final String thresholdname;
  private final String thresholdvalue; 
  private final String transactionname; 
  
  private static final String PCTL_90TH       = "90th Percentile";
  private static final String MIN_MSG_RSP     = "Min Response Time";
  private static final String MAX_MSG_RSP     = "Max Response Time";
  private static final String AVG_MSG_RSP     = "Average Response Time";
  private static final String BYTES_SENT      = "Total Bytes Sent";
  private static final String BYTES_RECEIVED  = "Total Bytes Received";
  private static final String TRANSACTION_ERRORS = "Errors Per Transaction";
  private static final String MIN_TRANSACTION_COUNT = "Minimum Transaction Count";
  private static final String MIN_DURATION     = "Min Duration";
  private static final String MAX_DURATION     = "Max Duration";
  private static final String AVG_DURATION     = "Average Duration";
    
  private static final char   THRESHOLD_STR_SEPERATOR = '/' ; 
  private static final String THRESHOLD_TO_SCOMMAND_FORMAT  = "%s" + THRESHOLD_STR_SEPERATOR + "%s" + THRESHOLD_STR_SEPERATOR + "%s";

  /**
   * filled by the jenkins plugin, when the user enters data in the UI 
   * @param transactionname - name of transaction
   * @param thresholdname - threshold name, picked from a list box
   * @param thresholdvalue - the threshold value
   */
  @DataBoundConstructor
  public TransactionThreshold(String transactionname, String thresholdname, String thresholdvalue) {
      this.thresholdname = thresholdname;
      this.thresholdvalue = thresholdvalue;
      this.transactionname = transactionname;
  }
  
  /**
   * Getters are needed for Jenkins implementation
   */
  public String getTransactionname() {
      return transactionname;
  }

  public String getThresholdname() {
      return thresholdname;
  }

  public String getThresholdvalue() {
      return thresholdvalue;
  }

  public String toScommandString() {
      String WHITESPACE_PATTERN = "\\s"; 
      String trimSpaceThresholdName = thresholdname.replaceAll(WHITESPACE_PATTERN,"");
      String scommandFormatted = String.format(THRESHOLD_TO_SCOMMAND_FORMAT, transactionname,trimSpaceThresholdName,thresholdvalue);
      return scommandFormatted;
  }
  
  @Extension
  public static class DescriptorImpl extends Descriptor<TransactionThreshold> {
    
    @Override
    public String getDisplayName() {
      return "";
    }
    
    /**
     * check for valid transaction name 
     * @param value - the value from jenkins UI
     * @return if "FormValidation.error" is returned, the user is notified of bad input
     */
    public FormValidation doCheckTransactionname(@QueryParameter String value) {
      if (value == null || value.trim().isEmpty()) {
        return FormValidation.error("Paramater 'Transaction Name' is required");
      }
      return FormValidation.ok();
    }
    
    /**
     * checks the validity of the threshold value parameter
     * it should be an alphanumeric number 
     * @param value - entered via jenkins UI 
     */
    public FormValidation doCheckThresholdvalue(@QueryParameter String value) {
      // basic checks, the string must not be empty 
      if (value == null || value.trim().isEmpty()) {
        return FormValidation.error("Parameter 'Threshold Value' is required");
      } 
      
      // verify that the string truly represents a number 
      double dNum=0;
      try{
        dNum = Double.parseDouble(value);
      }
      catch(NumberFormatException e){
        return FormValidation.error("Value must be a positive number");
      }
      
      if (dNum < 0)
        return FormValidation.error("Value must be >= 0");

      return FormValidation.ok();
    }
    
    /**
     * required by the jenkins framework
     * fills the thresholds names into the listbox item. 
     * than the user can select one of them as the desired threshold name.
     * 
     * @return ListBoxModel item filled with the strings to be displayed in the listbox
     */
    public ListBoxModel doFillThresholdnameItems() {
      final ListBoxModel listBox = new ListBoxModel();

      listBox.add(AVG_MSG_RSP, "AverageResponseTime");
      listBox.add(PCTL_90TH, "90thPercentile");
      listBox.add(MIN_MSG_RSP, "MinResponseTime");
      listBox.add(MAX_MSG_RSP, "MaxResponseTime");
      listBox.add(BYTES_SENT, "TotalBytesSent");
      listBox.add(BYTES_RECEIVED, "TotalBytesReceived");
      listBox.add(TRANSACTION_ERRORS, "ErrorsPerTransaction");
      listBox.add(MIN_TRANSACTION_COUNT, "MinimumTransactionCount");   
      listBox.add(MIN_DURATION, "MinDuration");
      listBox.add(MAX_DURATION, "MaxDuration");
      listBox.add(AVG_DURATION, "AverageDuration");
      

      return listBox;
    }

  }  //class
}
