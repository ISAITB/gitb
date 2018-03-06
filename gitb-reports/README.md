# GITB report module

This module contains the templates for the PDF reports used in GITB. This module 
also includes DTO objects as well as supporting resources (e.g. images).

To work with the reports you need the JasperSoft studio [https://community.jaspersoft.com/project/jaspersoft-studio]. 
Create a workspace and import the gitb-reports module as an existing project.

To build the reports for use in GITB and other components (e.g. the XML validator) run
`mvn install` in the current module. This will compile reports and DTO classes and add 
them to the local maven repository.   