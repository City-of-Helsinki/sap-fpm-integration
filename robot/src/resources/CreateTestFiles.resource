*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create Kasko ORD
    ${ORD}     Get File	    ${CURDIR}/xml/perustiedot/ORD_OUT_.xml
    ##${ORD}     Replace String Using Regexp	    ${ORD}
    RETURN  ${ORD}

