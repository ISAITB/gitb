# GITB report module

This module contains the templates for the PDF reports used in GITB. This module 
also includes DTO objects as well as supporting resources (e.g. images).

When adapting templates or creating new ones the best approach is to do this via the `ReportGeneratorTest` unit test for
which you can provide a temporary directory to work with and preview produced reports. See the unit test's comments on how
to do this.