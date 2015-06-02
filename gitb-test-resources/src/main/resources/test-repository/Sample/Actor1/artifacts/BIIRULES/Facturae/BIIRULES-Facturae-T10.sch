<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to FACTURAE syntax for T10 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="FACTURAE-T10" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T10">
  <param value="(EndDate and  StartDate) and not(number(translate(StartDate,'-','')) &gt; number(translate(EndDate,'-',''))) or number(translate(StartDate,'-','')) = number(translate(EndDate,'-',''))" name="BIIRULE-T10-R001"/>
  <param value="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or
(LegalEntity/OverseasAddress/PostCodeandTown) or
(Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or
(Individual/OverseasAddress/PostCodeandTown))" name="BIIRULE-T10-R002"/>
  <param value="((//CountryCode) and (following::BuyerParty//CountryCode) and ((//CountryCode) = (following::BuyerParty//CountryCode) or ((//CountryCode) != (following::BuyerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not(//CountryCode) or not(following::BuyerParty//CountryCode)" name="BIIRULE-T10-R003"/>
  <param value="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or
(LegalEntity/OverseasAddress/PostCodeandTown) or
(Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or
(Individual/OverseasAddress/PostCodeandTown))" name="BIIRULE-T10-R004"/>
  <param value="((//CountryCode) and (preceding::SellerParty//CountryCode) and  ((//CountryCode) = (preceding::SellerParty//CountryCode) or ((//CountryCode) != (preceding::SellerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not((//CountryCode)) or not((preceding::SellerParty//CountryCode))" name="BIIRULE-T10-R005"/>
  <param value="(InstallmentDueDate and preceding::InvoiceIssueData/IssueDate) and not(number(translate(InstallmentDueDate,'-','')) &lt; number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))) or number(translate(InstallmentDueDate,'-','')) = number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))" name="BIIRULE-T10-R006"/>
  <param value="((PaymentMeans = '31') and (//AccountToBeCredited/IBAN or //AccountToBeCredited/AccountNumber)) or (PaymentMeans != '31')" name="BIIRULE-T10-R007"/>
  <param value="(//AccountToBeCredited/IBAN and //AccountToBeCredited/BIC) or not(//AccountToBeCredited/IBAN)" name="BIIRULE-T10-R008"/>
  <param value="count(Tax)&gt;1 or count(Tax) = 1" name="BIIRULE-T10-R009"/>
  <param value="number(following::InvoiceTotals/TotalTaxOutputs) = number(round(sum(child::Tax/TaxAmount/TotalAmount) * 100) div 100)" name="BIIRULE-T10-R010"/>
  <param value="number(TotalGrossAmount) = number(round(sum(following::Items/InvoiceLine/GrossAmount) * 100) div 100)" name="BIIRULE-T10-R011"/>
  <param value="((TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = (number(TotalGrossAmount) + number(TotalGeneralSurcharges) - number(TotalGeneralDiscounts)))) or (not(TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) - number(TotalGeneralDiscounts))) or ((TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) + number(TotalGeneralSurcharges))) or (not(TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount)))" name="BIIRULE-T10-R012"/>
  <param value="number(InvoiceTotal) = number(TotalGrossAmountBeforeTaxes) - number(TotalTaxesWithheld) + number(TotalTaxOutputs)" name="BIIRULE-T10-R013"/>
  <param value="number(InvoiceTotal) &gt;= 0" name="BIIRULE-T10-R014"/>
  <param value="(TotalGeneralDiscounts) and TotalGeneralDiscounts = (round(sum(//Discount/DiscountAmount) * 100) div 100) or not(TotalGeneralDiscounts)" name="BIIRULE-T10-R015"/>
  <param value="(TotalGeneralSurcharges) and TotalGeneralSurcharges = (round(sum(//Charge/ChargeAmount) * 100) div 100) or not(TotalGeneralSurcharges)" name="BIIRULE-T10-R016"/>
  <param value="(TotalPaymentsOnAccount) and (number(TotalExecutableAmount) = number(InvoiceTotal - TotalPaymentsOnAccount)) or TotalExecutableAmount = InvoiceTotal" name="BIIRULE-T10-R017"/>
  <param value="not(Quantity) or not(UnitPriceWithoutTax) or number(GrossAmount) = (round(number(UnitPriceWithoutTax) * number(Quantity) * 100) div 100)" name="BIIRULE-T10-R018"/>
  <param value="string-length(string(//ItemDescription)) &lt;= 50" name="BIIRULE-T10-R019"/>
  <param value="true" name="BIIRULE-T10-R020"/>
  <param value="not(//ArticleCode)" name="BIIRULE-T10-R021"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T10-R022"/>
  <param value="(//InvoiceIssueData/IssueDate)" name="BIIRULE-T10-R023"/>
  <param value="(//InvoiceHeader/InvoiceNumber)" name="BIIRULE-T10-R024"/>
  <param value="(//ItemDescription)" name="BIIRULE-T10-R025"/>
  <param value="(//SellerParty/LegalEntity/CorporateName) or (//SellerParty/Individual/Name) " name="BIIRULE-T10-R026"/>
  <param value="(//BuyerParty/LegalEntity/CorporateName) or (//BuyerParty/Individual/Name) " name="BIIRULE-T10-R027"/>
  <param value="true=false" name="BIIRULE-T10-R028"/>
  <param value="//SchemaVersion" name="BIIRULE-T10-R029"/>
  <param value="true=false" name="BIIRULE-T10-R030"/>
  <param value="true=false" name="BIIRULE-T10-R031"/>
  <param value="//InvoiceHeader/InvoiceNumber" name="BIIRULE-T10-R032"/>
  <param value="(//Items/InvoiceLine)" name="BIIRULE-T10-R033"/>
  <param value="(//InvoiceIssueData/InvoiceCurrencyCode)" name="BIIRULE-T10-R034"/>
  <param value="true=false" name="BIIRULE-T10-R035"/>
  <param value="true=false" name="BIIRULE-T10-R036"/>
  <param value="true=false" name="BIIRULE-T10-R037"/>
  <param value="//TotalExecutableAmount" name="BIIRULE-T10-R038"/>
  <param value="//InvoiceTotal" name="BIIRULE-T10-R039"/>
  <param value="." name="BIIRULE-T10-R040"/>
  <param value="//RegistrationData" name="BIIRULE-T10-R041"/>
  <param value="//TotalGrossAmountBeforeTaxes" name="BIIRULE-T10-R042"/>
  <param value="//TotalGrossAmount" name="BIIRULE-T10-R043"/>
  <param value="//PaymentMeans" name="BIIRULE-T10-R044"/>
  <param value="//TotalTaxOutputs" name="BIIRULE-T10-R045"/>
  <param value="//Tax/TaxableBase" name="BIIRULE-T10-R046"/>
  <param value="//Tax/TaxAmount" name="BIIRULE-T10-R047"/>
  <param value="false" name="BIIRULE-T10-R048"/>
  <param value="//TaxTypeCode" name="BIIRULE-T10-R049"/>
  <param value="//TotalCost" name="BIIRULE-T10-R050"/>
  <param value="//Parties/BuyerParty" name="Customer"/>
  <param value="//Items/InvoiceLine" name="Invoice_Line"/>
  <param value="//InvoicingPeriod" name="Invoice_Period"/>
  <param value="/ubl:Facturae" name="Invoice"/>
  <param value="//Items/InvoiceLine/UnitPriceWithoutTax" name="Item_Price"/>
  <param value="//Items/InvoiceLine" name="Item"/>
  <param value="//PaymentDetails/Installments" name="Payment_Means"/>
  <param value="//Parties/SellerParty" name="Supplier"/>
  <param value="/ubl:Facturae/Invoices/Invoice/TaxesOutputs" name="Tax_Total"/>
  <param value="//InvoiceTotals" name="Total_Amounts"/>
  <param value="//LegalEntity" name="Party_Legal_Entity"/>
  <param value="//TaxesOutputs" name="Tax_Category"/>
  <param value="//TaxesOutputs" name="Tax_Scheme"/>
  <param value="//cac:AdditionalDocumentReference" name="Annex"/>
  <param value="//CountryCode" name="Country"/>
</pattern>
