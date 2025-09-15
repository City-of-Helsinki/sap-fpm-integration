*** Settings ***
Library         String

*** Keywords ***

Check SAPACTUAL ${FilePath}
    [Documentation]     Opens SAPACTUAL_.csv sent to the FTP and checks it contains the correct lines
    [Arguments]      ${BUKRS}     ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     BUKRS;BELNR;CO_BELNR;GJAHR;POPER;BLART;BLDAT;BUDAT;CPUDT;TCODE;XBLNR;KUNNR;LIFNR;LIFNR_NAME1;EBELN;Attachment;BUZEI;CO_BUZEI;RACCT;RCNTR;PRCTR;RFAREA;AUFNR;PS_PSPID;RASSC;SEGMENT;SGTXT;DRCRK;MWSKZ;VAT_PERCENT;HSL;PPRCTR;MATNR;EBELP;LAST_CHANGE_DATETIME;AUGBL;AWTYP
    ${FirstLine}  	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${ExpLine}  Set Variable    ${BUKRS};${BELNR};${CO_BELNR};${GJAHR};${POPER};${BLART};${BLDAT};${BUDAT};${CPUDT};${TCODE};${XBLNR};${KUNNR};${LIFNR};${LIFNR_NAME1};${EBELN};${Attachment};${BUZEI};${CO_BUZEI};${RACCT};${RCNTR};${PRCTR};${RFAREA};${AUFNR};${PS_PSPID};${RASSC};${SEGMENT};${SGTXT};${DRCRK};${MWSKZ};${VAT_PERCENT};${HSL};${PPRCTR};${MATNR};${EBELP};${LAST_CHANGE_DATETIME};${AUGBL};${AWTYP}
    ${Lines}    Get Lines Containing String    ${File}   ${ExpLine}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers    ${LineCount}     1    msg=${ExpLine} not found

