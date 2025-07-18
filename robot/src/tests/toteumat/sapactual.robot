*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library     OperatingSystem
Resource     ../../resources/CreateTestFilesCommon.resource
Resource    ../../resources/CreateToteumatTestFiles.resource


Test Tags       toteumat     sapactual

*** Test Cases ***
Send kasko toteumat
    [Documentation]     KASKO toteumat
    [Tags]   kasko      toteumat     sapactual
    ${KaskoToteumatFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID023}
    ${TOSITE_YEAR}    Set Variable    2025
    ${TOSITE_MONTH}    Set Variable   01
    ${TOSITE_SIMPLE_MONTH}     Set Variable    1
    ${CUR_DATE_STR}      Get Current Time Text
    ${BUKRS}    Set Variable    3900
    ${RandBELNRSuffix}      Generate Random String  10
    ${BELNR}    Set Variable    ${CUR_DATE_STR}_${RandBELNRSuffix}
    ${CO_BELNR}     Set Variable    CO_BELNR
    ${GJAHR}    Set Variable    ${TOSITE_YEAR}
    ${BLART}    Set Variable    BLART
    ${BLDAT}     Set Variable    BLDAT
    ${BUDAT}     Set Variable     BUDAT
    ${CPUDT}     Set Variable    CPUDT
    ${TCODE}     Set Variable    TCODE
    ${XBLNR}     Set Variable    XBLNR
    ${KUNNR}     Set Variable    KUNNR
    ${LIFNR}     Set Variable    LIFNR
    # TODO: check LIFNR_NAME1 place in S4/SEC xml
    ${LIFNR_NAME1}     Set Variable    ${EMPTY}
    ${EBELN}    Set Variable     EBELN
    ${Attachment}     Set Variable    Attachment
    ${BUZEI}     Set Variable    BUZEI
    ${CO_BUZEI}     Set Variable    CO_BUZEI
    ${RACCT}     Set Variable    RACCT
    ${RCNTR}     Set Variable    RCNTR
    ${PRCTR}     Set Variable    PRCTR
    ${RFAREA}     Set Variable    RFAREA
    ${AUFNR}     Set Variable    AUFNR
    ${PS_PSPID}     Set Variable    PS_PSPID
    ${RASSC}     Set Variable    RASSC
    # TODO: check SEGMENT in S4, doesn't exist in SEC
    ${SEGMENT}     Set Variable    ${EMPTY}
    ${SGTXT}     Set Variable    SGTXT
    ${DRCRK}     Set Variable    DRCRK
    ${MWSKZ}     Set Variable    MWSKZ
    # TODO: check VAT_PERCENT in S4, doesn't exist in SEC
    ${VAT_PERCENT}     Set Variable    ${EMPTY}
    ${HSL}     Set Variable      50.80
    ${PPRCTR}     Set Variable    PPRCTR
    ${MATNR}     Set Variable    MATNR
    ${EBELP}     Set Variable    EBELP
    # TODO: check LAST_CHANGE_DATETIME in S4, doesn't exist in SEC
    ${LAST_CHANGE_DATETIME}     Set Variable    ${EMPTY}
    ${AUGBL}     Set Variable    AUGBL
    ${AWTYP}     Set Variable    AWTYP

    ${TOSITE}      Create TOSITEFILE     BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}
    Set Ftp Connection As Down
    Create File     ${KaskoToteumatFtpDir}/ID022_FI_TOSITE_kasko_${CUR_DATE_STR}.xml   content=${TOSITE}
    Set Ftp Connection As Up

    Wait Until Keyword Succeeds     2 minutes   15 seconds    Should Exist       ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_SIMPLE_MONTH}.csv
    Check SAPACTUAL ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_SIMPLE_MONTH}.csv      BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}

Send kasko toteumat and cut FTP connection for a long time
    [Documentation]     KASKO toteumat, FTP connection is cut, files should be sent after connection recovers
    [Tags]   kasko      toteumat     sapactual
    [Timeout]    NONE
    ${KaskoToteumatFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID023}
    ${TOSITE_YEAR}    Set Variable    2024
    ${TOSITE_MONTH}    Set Variable   11
    ${CUR_DATE_STR}      Get Current Time Text
    ${BUKRS}    Set Variable    3900
    ${RandBELNRSuffix}      Generate Random String  10
    ${BELNR}    Set Variable    ${CUR_DATE_STR}_${RandBELNRSuffix}
    ${CO_BELNR}     Set Variable    CO_BELNR
    ${GJAHR}    Set Variable    ${TOSITE_YEAR}
    ${BLART}    Set Variable    BLART
    ${BLDAT}     Set Variable    BLDAT
    ${BUDAT}     Set Variable     BUDAT
    ${CPUDT}     Set Variable    CPUDT
    ${TCODE}     Set Variable    TCODE
    ${XBLNR}     Set Variable    XBLNR
    ${KUNNR}     Set Variable    KUNNR
    ${LIFNR}     Set Variable    LIFNR
    ${LIFNR_NAME1}     Set Variable    ${EMPTY}
    ${EBELN}    Set Variable     EBELN
    ${Attachment}     Set Variable    Attachment
    ${BUZEI}     Set Variable    BUZEI
    ${CO_BUZEI}     Set Variable    CO_BUZEI
    ${RACCT}     Set Variable    RACCT
    ${RCNTR}     Set Variable    RCNTR
    ${PRCTR}     Set Variable    PRCTR
    ${RFAREA}     Set Variable    RFAREA
    ${AUFNR}     Set Variable    AUFNR
    ${PS_PSPID}     Set Variable    PS_PSPID
    ${RASSC}     Set Variable    RASSC
    ${SEGMENT}     Set Variable    ${EMPTY}
    ${SGTXT}     Set Variable    SGTXT
    ${DRCRK}     Set Variable    DRCRK
    ${MWSKZ}     Set Variable    MWSKZ
    ${VAT_PERCENT}     Set Variable    ${EMPTY}
    ${HSL}     Set Variable      12.34
    ${PPRCTR}     Set Variable    PPRCTR
    ${MATNR}     Set Variable    MATNR
    ${EBELP}     Set Variable    EBELP
    ${LAST_CHANGE_DATETIME}     Set Variable    ${EMPTY}
    ${AUGBL}     Set Variable    AUGBL
    ${AWTYP}     Set Variable    AWTYP

    ${TOSITE}      Create TOSITEFILE     BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}

    Create File     ${KaskoToteumatFtpDir}/ID022_FI_TOSITE_kasko_${CUR_DATE_STR}_long_wait.xml   content=${TOSITE}
    Sleep    15 seconds
    Set Ftp Connection As Down
    Sleep    1 minutes
    Set Ftp Connection As Up

    Wait Until Keyword Succeeds     2 minutes   15 seconds    Should Exist       ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_MONTH}.csv
    Check SAPACTUAL ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_MONTH}.csv      BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}


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

