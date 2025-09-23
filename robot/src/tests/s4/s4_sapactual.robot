*** Settings ***
Documentation   Luo SFTP-palvelimen, luo toteumia palvelimelle, tarkistaa SFTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/S4SFTPServer.py
Library     OperatingSystem
Resource     ../../resources/CreateTestFilesCommon.resource
Resource     ../../resources/CheckToteumatTestFiles.resource
Resource    ../../resources/CreateS4ToteumatTestFiles.resource


Test Tags       toteumat    s4   sapactual

*** Test Cases ***
Send S4 kasko toteumat
    [Documentation]     S4 KASKO toteumat
    [Tags]     kasko
    Set Log Level   DEBUG
    Set Current SFTP USER   %{KASKO_S4_SFTP_USER_TOTEUMAT}
    ${KaskoToteumatFtpDir}   Get SFTP Dir For     %{KASKO_S4_SFTP_USER_TOTEUMAT}
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
    ${SEGMENT}     Set Variable    ${EMPTY}
    ${SGTXT}     Set Variable    SGTXT
    ${DRCRK}     Set Variable    DRCRK
    ${MWSKZ}     Set Variable    MWSKZ
    ${VAT_PERCENT}     Set Variable    ${EMPTY}
    ${HSL}     Set Variable      50.80
    ${PPRCTR}     Set Variable    PPRCTR
    ${MATNR}     Set Variable    MATNR
    ${EBELP}     Set Variable    EBELP
    ${LAST_CHANGE_DATETIME}     Set Variable    ${EMPTY}
    ${AUGBL}     Set Variable    AUGBL
    ${AWTYP}     Set Variable    AWTYP

    ${TOSITE}      Create S4TOSITEFILE         BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}

    Create File     ${KaskoToteumatFtpDir}/ID022_FI_TOSITE_kasko_${CUR_DATE_STR}.xml   content=${TOSITE}
    Sleep    15 seconds
    Set SFtp Connection As Down
    Sleep    15 seconds
    Set SFtp Connection As Up
    # TODO: it takes a long time to send the other SAPACTUAL_ file(s) as well, and before they arrive, the next test is already started
    Wait Until Keyword Succeeds     3 minutes   15 seconds    Should Exist       ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_SIMPLE_MONTH}.csv
    Wait Until Keyword Succeeds     3 minutes   15 Seconds     Check SAPACTUAL ${KaskoToteumatFtpDir}/SAPACTUAL_${TOSITE_YEAR}_${TOSITE_SIMPLE_MONTH}.csv      BUKRS=${BUKRS}     BELNR=${BELNR}    CO_BELNR=${CO_BELNR}    GJAHR=${GJAHR}    POPER=${TOSITE_MONTH}   BLART=${BLART}
    ...   BLDAT=${BLDAT}   BUDAT=${BUDAT}    CPUDT=${CPUDT}   TCODE=${TCODE}   XBLNR=${XBLNR}   KUNNR=${KUNNR}   LIFNR=${LIFNR}   LIFNR_NAME1=${LIFNR_NAME1}
    ...   EBELN=${EBELN}   Attachment=${Attachment}    BUZEI=${BUZEI}   CO_BUZEI=${CO_BUZEI}   RACCT=${RACCT}    RCNTR=${RCNTR}
    ...   PRCTR=${PRCTR}    RFAREA=${RFAREA}    AUFNR=${AUFNR}    PS_PSPID=${PS_PSPID}    RASSC=${RASSC}     SEGMENT=${SEGMENT}
    ...   SGTXT=${SGTXT}    DRCRK=${DRCRK}   MWSKZ=${MWSKZ}    VAT_PERCENT=${VAT_PERCENT}    HSL=${HSL}    PPRCTR=${PPRCTR}
    ...   MATNR=${MATNR}    EBELP=${EBELP}   LAST_CHANGE_DATETIME=${LAST_CHANGE_DATETIME}    AUGBL=${AUGBL}   AWTYP=${AWTYP}
