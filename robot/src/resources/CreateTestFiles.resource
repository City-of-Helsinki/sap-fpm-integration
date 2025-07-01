*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create ORD
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}  ${AUTYP1}  ${AUFNR2}   ${KTEXT2}  ${STTXT2}   ${AUTYP2}
    ${ORD}     Get File	    ${CURDIR}/xml/perustiedot/ORD_OUT_.xml
    ${Seg1}  Create Segment ORD_ZHKI_TARSISTILAUKSET   ${BUKRS}    ${AUART}    ${AUFNR1}    ${KTEXT1}    ${STTXT1}    ${AUTYP1}
    ${Seg2}  Create Segment ORD_ZHKI_TARSISTILAUKSET    ${BUKRS}    ${AUART}    ${AUFNR2}    ${KTEXT2}    ${STTXT2}    ${AUTYP2}
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