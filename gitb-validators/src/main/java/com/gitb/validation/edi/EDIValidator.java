package com.gitb.validation.edi;

import com.gitb.core.Configuration;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by senan on 06/08/2015.
 */
@MetaInfServices(IValidationHandler.class)
public class EDIValidator extends AbstractValidator {

    private static final Logger logger = LoggerFactory.getLogger(EDIValidator.class);

    private final String CONTENT_ARGUMENT_NAME = "edidocument";

    private final String SEGMENT_TERMINATOR = "'";
    private final String DELIMITER = ":|\\+";

    private final String MODULE_DEFINITION_XML = "/edi-validator-definition.xml";

    private EDIReportHandler reportHandler;

    public EDIValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        //get inputs
        BinaryType content   = (BinaryType) inputs.get(CONTENT_ARGUMENT_NAME);
        String stringContent = new String((byte [])content.getValue());
        StringType document  = new StringType(stringContent);

        //create error handler
        reportHandler = new EDIReportHandler(document);

        String[] parsedInvoice= stringContent.split("\\r?\\n");

        boolean una=false, unb=false, unz = false;

        for(int i = 0 ; i < parsedInvoice.length ; i++){
            // checking the first 3 characters of the line in order to
            // find segment type and use the appropriate function for that segment

            String segment= parsedInvoice[i];
            if(segment.lastIndexOf(SEGMENT_TERMINATOR)>0){
                segment = segment.substring(0, segment.lastIndexOf(SEGMENT_TERMINATOR));
            }
            String lead = segment.substring(0,3);

            switch (lead) {
                case "UNA":
                    una = true;
                    break;

                case "UNB":
                    unb = true;
                    break;

                case "UNZ":
                    unz = true;
                    break;

                case "UNH":
                    validateUNH(segment);
                    break;

                case "BGM":
                    validateBGM(segment);
                    break;

                case "DTM":
                    validateDTM(segment);
                    break;

                case "PAI":
                    validatePAI(segment);
                    break;

                case "FTX":
                    validateFTX(segment);
                    break;

                case "NAD":
                    validateNAD(segment);
                    break;

                case "RFF":
                    validateRFF(segment);
                    break;

                case "CTA":
                    validateCTA(segment);
                    break;

                case "CUX":
                    validateCUX(segment);
                    break;

                case "PAT":
                    validatePAT(segment);
                    break;

                case "PCD":
                    validatePCD(segment);
                    break;

                case "MOA":
                    validateMOA(segment);
                    break;

                case "LIN":
                   validateLIN(segment);
                    break;

                case "PIA":
                    validatePIA(segment);
                    break;

                case "IMD":
                    validateIMD(segment);
                    break;

                case "QTY":
                    validateQTY(segment);
                    break;

                case "ALI":
                    validateALI(segment);
                    break;

                case "GIN":
                    validateGIN(segment);
                    break;

                case "PRI":
                    validatePRI(segment);
                    break;

                case "TAX":
                    validateTAX(segment);
                    break;

                case "LOC":
                    validateLOC(segment);
                    break;

                case "UNS":
                    validateUNS(segment);
                    break;

                case "UNT":
                    validateUNT(segment);
                    break;

                case "CNT":
                    validateCNT(segment);
                    break;

                default:
                    reportHandler.addError(lead + " is not a valid Segment Type !");
                    break;
            }
        }

        if(!una ) {
            reportHandler.addError("Missing UNA Filed");
        }

        if(!unb) {
            reportHandler.addError("Missing UNB Filed");
        }

        if(!unz) {
            reportHandler.addError("Missing UNZ Filed");
        }

        return reportHandler.createReport();
    }

    private boolean validateUNH(String segment){
        int result= checkINV(segment,
                new int[][] {{-4},{-14},{-6,-3,-3,-2,6}},
                new String[][]{{"UNH ,UNH"},{""},{"INVOIC","D","96A","UN","A14051"}});

        if(result < 0) {
            String description = "Line starting with \"UNH\" can not be validated.\n" +
                    "An example correct usage:  UNH +0001+INVOIC:D:96A:UN:A14051'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateBGM(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3,3,3,35},{35,3},{3}},
                new String[][]{{"BGM"},{"380,395"},{"",",7,9,"},{"AP"}});

        if(result < 0) {
            String description = "Line starting with \"BGM\" can not be validated.\n" +
                    "An example correct usage:  BGM+380::+12345678:9+AP'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateDTM(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,35,3}},
                new String[][]{{"DTM"},{"137,131,171,134,140,50","","102"}});

        if(result < 0) {
            String description = "Line starting with \"DTM\" can not be validated.\n" +
                    "An example correct usage:  DTM+137:19980610:102'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validatePAI(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3,3,3,3,3,3}},
                new String[][]{{"PAI"},{"","",",1,20,21,31,55,","","","10"}});

        if(result < 0) {
            String description = "Line starting with \"PAI\" can not be validated.\n" +
                    "An example correct usage:  PAI+::31::10'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateFTX(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,3},{3},{3,3,3},{-70,70,70,70,70,3}},
                new String[][]{{"FTX"},{"AAI,AAB,REG,ABL"}});

        if(result < 0) {
            String description = "Line starting with \"FTX\" can not be validated.\n" +
                    "An example correct usage:  FTX+AAI+++On 01.04.99 change to EURO'\n" +
                    "or: FTX+CHG++31::10+Price deterioration in the rubber industry";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateNAD(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{-35,3,3},{-35,35,35,35,35},{-35,35,35,35,35,3},{35,35,35,35},
                        {35},{9},{9},{3}},
                new String[][]{{"NAD"},{"BY,CN,SU,SE"}, {"","","5,10,91,92"}, {},{},{},
                        {},{},{},{"DE,BE,ES,FR,GB,IT,NL,PT,US"}});

        if(result < 0) {
            String description = "Line starting with \"NAD\" can not be validated.\n" +
                    "An example correct usage:  NAD+BY+22334455::91+Street:P.O. Box:Postcode:Place+König Buyer+++++DE'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateRFF(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,35,6,35}},
                new String[][]{{"RFF"},{"ADE,VA,FC,XA,AAK,ACD,ACW,AHL,ON,PP"}});

        if(result < 0) {
            String description = "Line starting with \"RFF\" can not be validated.\n" +
                    "An example correct usage:  RFF+ADE:77477447'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateCTA(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3,17,35}},
                new String[][]{{"CTA"},{"AD,PD,IC,NT"}});

        if(result < 0) {
            String description = "Line starting with \"CTA\" can not be validated.\n" +
                    "An example correct usage:  CTA+AD+:Heinz Tester'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateCUX(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,3,3,4},{3,3,3,4},{12,3}},
                new String[][]{{"CUX"},{"2","DEM,GBP,FRF,USD,EUR","3,4,11"},
                        {"3","DEM,GBP,FRF,USD,EUR","3,4,11"}});

        if(result < 0) {
            String description = "Line starting with \"CUX\" can not be validated.\n" +
                    "An example correct usage:  CUX+2:EUR:4+3:GBP:3+1.44568'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validatePAT(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{17,3,3,35,35},{3,3,3,3}},
                new String[][]{{"PAT"},{",1,22,"}});

        if(result < 0) {
            String description = "Line starting with \"PAT\" can not be validated.\n" +
                    "An example correct usage:  PAT+1'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validatePCD(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3,10,3,3,3}},
                new String[][]{{"PCD"},{"7,12"}});

        if(result < 0) {
            String description = "Line starting with \"PCD\" can not be validated.\n" +
                    "An example correct usage:  PCD+12:2.00'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateMOA(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3,18,3,3,3}},
                new String[][]{{"MOA"},{"124,125,77,79,109,176,8,52,203"}});

        if(result < 0) {
            String description = "Line starting with \"MOA\" can not be validated.\n" +
                    "An example correct usage:  MOA+77:348.00:4'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateLIN(String line){
        int result= checkINV(line,
                new int[][] {{-3},{6},{3},{35,3}},
                new String[][]{{"LIN"},{""},{""},{"","IN"}});

        if(result < 0) {
            String description = "Line starting with \"LIN\" can not be validated.\n" +
                    "An example correct usage:  LIN+1++4711:IN'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validatePIA(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{35,3,3,3},{35,3,3,3},{35,3,3,3},{35,3,3,3},{35,3,3,3}},
                new String[][]{{"PIA"},{""},{"","SA","",""},{"","DR","",""},{"","EC","",""}});

        if(result < 0) {
            String description = "Line starting with \"PIA\" can not be validated.\n" +
                    "An example correct usage:  PIA+1+5822:SA'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateIMD(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3},{3},{17,3,3,35,35}},
                new String[][]{{"IMD"},{""},{""},{"","","","",""}});

        if(result < 0) {
            String description = "Line starting with \"IMD\" can not be validated.\n" +
                    "An example correct usage:  IMD+++:::Wing:'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateQTY(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,-15,3}},
                new String[][]{{"QTY"},{"12,20,47,48,61,119,121,124","","PCE,KGM,MTR,LTR,NPR"}});

        if(result < 0) {
            String description = "Line starting with \"QTY\" can not be validated.\n" +
                    "An example correct usage:  QTY+12:15:PCE'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateALI(String line){
        int result= checkINV(line,
                new int[][] {{-3},{3},{3},{3}},
                new String[][]{{"ALI"},{"BE,DE,ES,FR,GB,IT,NL,PT,US"},{""},{"69"}});

        if(result < 0) {
            String description = "Line starting with \"ALI\" can not be validated.\n" +
                    "An example correct usage:  ALI+DE++'\n" +
                    "or: ALI+DE'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateGIN(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{-35}},
                new String[][]{{"GIN"},{"BN"},{""}});

        if(result < 0) {
            String description = "Line starting with \"GIN\" can not be validated.\n" +
                    "An example correct usage:  GIN+BN+87654321'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validatePRI(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3,15,3,3,9,3}},
                new String[][]{{"PRI"},{"AAA","","","CON,AAK,AAL,CP","","PCE,KGM,MTR,LTR,NPR"}});

        if(result < 0) {
            String description = "Line starting with \"PRI\" can not be validated.\n" +
                    "An example correct usage:  PRI+AAA:1980::CON:100:PCE'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateTAX(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{3},{6},{15},{7,3,3,17},{3}},
                new String[][]{{"TAX"},{"7"},{"VAT"}});

        if(result < 0) {
            String description = "Line starting with \"TAX\" can not be validated.\n" +
                    "An example correct usage:  TAX+7+VAT+++:::16.00+'\n" +
                    "or: TAX+7+VAT:::+++:::16.00:+'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateLOC(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{25,3,3,70}},
                new String[][]{{"LOC"},{"11"}});

        if(result < 0) {
            String description = "Line starting with \"LOC\" can not be validated.\n" +
                    "An example correct usage:  LOC+11+Gate ABC123:::second gate back glass door left'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateUNS(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-1}},
                new String[][]{{"UNS"},{"S"}});

        if(result < 0) {
            String description = "Line starting with \"UNS\" can not be validated.\n" +
                    "An example correct usage:  UNS+S'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateUNT(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-6},{-14}},
                new String[][]{{"UNT"}});

        if(result < 0) {
            String description = "Line starting with \"UNT\" can not be validated.\n" +
                    "An example correct usage:  UNT+147+0001'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    private boolean validateCNT(String line){
        int result= checkINV(line,
                new int[][] {{-3},{-3},{-18}},
                new String[][]{{"CNT"}});

        if(result < 0) {
            String description = "Line starting with \"CNT\" can not be validated.\n" +
                    "An example correct usage: CNT+31:1'";
            this.reportHandler.addError(result, description);
            return false;
        }
        return true;
    }

    /**
     * A function for checking if the line is satisfying the references
     * for checking the length of conditional types just send the length for that field as a parameter
     * for checking the length of mandatory fields add a "-" before sending the length as a parameter
     *
     * @param line the invoice line
     * @param flens lengths of fields
     * @param mstr mandatory strings
     * @return
     */
    protected int checkINV(String line, int[][] flens, String[][] mstr) {
        String[] t1= line.split("\\+");
        int flen;
        for(int i=0;i<flens.length && i<t1.length ;i++) {

            String[] t2= t1[i].split(":");

            for(int j=0;j<flens[i].length;j++) {
                 //check Valid Values for fields
                flen= flens[i][j];

                 //if negative, it is mandatory
                if(flen<0 && t2[j].length()==0) {
                    //mandatory field is null: error
                    return -100*i-j;
                }
                if(j>=t2.length) continue;

                //Conditional fields not given
                if(t2[j].length()==0) continue;

                //Conditional field, NULL value
                if(flen<0) flen= -flen;

                if(flen<t2[j].length()) {
                    //longer than field max length
                    return -100*i-j;
                }
                if(i>=mstr.length || j>= mstr[i].length || mstr[i][j].length()==0)
                    continue;

                if((mstr[i][j]+",").indexOf(t2[j]+",")==-1) {
                    return -100*i-j;
                }
            }
        }
        //no errors
        return 0;
    }
}
