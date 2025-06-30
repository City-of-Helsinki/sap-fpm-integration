*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create Kasko ORD
    ${ORD}     Get File	    ${CURDIR}/xml/perustiedot/ORD_OUT_.xml
    ${BUKRS}    Set Variable    1400
    ${Seg1}  Create Segment ORD_ZHKI_TARSISTILAUKSET   ${BUKRS}    3901    3963900001    ktextiä    sttextiä    01
    ${Seg2}  Create Segment ORD_ZHKI_TARSISTILAUKSET    ${BUKRS}    3901    3963900002    ktextiä    sttextiä    02
    ${AllSegments}  Catenate    ${Seg1}     ${Seg2}
    ${ORD}     Replace String 	    ${ORD}  __ZHKI_TARSISTILAUKSET__        ${AllSegments}
    RETURN  ${ORD}

Create Segment ORD_ZHKI_TARSISTILAUKSET
    [Arguments]    ${BUKRS}     ${AUART}    ${AUFNR}    ${KTEXT}    ${STTXT}   ${AUTYP}
    ${SEG}=  catenate  SEPARATOR=
    ...     <ZHKI_TARSISTILAUKSET SEGMENT="1">
    ...     <BUKRS>${BUKRS}</BUKRS><AUART>${AUART}</AUART><AUFNR>${AUFNR}</AUFNR><KTEXT>${KTEXT}</KTEXT>
    ...     <STTXT>${STTXT}</STTXT><AUTYP>${AUTYP}</AUTYP>
    ...     </ZHKI_TARSISTILAUKSET>
    RETURN    ${SEG}