*** Settings ***
Documentation   Luo FTP-palvelimen, luo co-toteumia palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../../resources/FTPServer.py
Library     OperatingSystem
Resource     ../../../resources/CreateTestFilesCommon.resource
Resource    ../../../resources/CreateCoToteumatTestFiles.resource


Test Tags       cototeumat     sapsisainenlaskenta

*** Test Cases ***
Send palke cototeumat and cut FTP connection for a long time
    Skip    ECC Cotoeumat no longer needed or supported
    [Documentation]     Palke  cototeumat, FTP connection is cut, files should be sent after connection recovers
    [Tags]   palke
    ${PalkeCoToteumatFtpDir}   Get FTP Dir For     %{PALKE_SFTP_USER_ID166}
    # same as toteumat
    ${PalkeResultCoToteumatFtpDir}   Get FTP Dir For     %{PALKE_SFTP_USER_ID025}
    ${COTOSITE_YEAR}    Set Variable    2025
    ${COTOSITE_MONTH}    Set Variable   08
    ${COTOSITE_SIMPLE_MONTH}    Set Variable   8
    ${CUR_DATE_STR}      Get Current Time Text
    ${RandBELNRSuffix}      Generate Random String  10
    ${BELNR}     Set Variable    ${CUR_DATE_STR}_${RandBELNRSuffix}
    ${BLDAT}     Set Variable    BLDAT
    ${BUDAT}     Set Variable     BUDAT
    ${CPUDT}     Set Variable    CPUDT
    ${BLART}     Set Variable    BLART
    ${REFBN}     Set Variable    REFBN
    ${VERSN}     Set Variable    VERSN
    ${AWTYP}     Set Variable    AWTYP
    ${AWORG}     Set Variable    AWORG
    ${BUZEI}     Set Variable    BUZEI
    ${PERIO}     Set Variable    ${COTOSITE_MONTH}
    ${WOGBTR}     Set Variable    WOGBTR
    ${OBJNR}     Set Variable    OBJNR
    ${OBJ_TYPE}     Set Variable    OBJ_TYPE
    ${TYPE_NR}     Set Variable    TYPE_NR
    ${PRCTR}     Set Variable    PRCTR
    ${GJAHR}     Set Variable    ${COTOSITE_YEAR}
    ${KSTAR}     Set Variable    KSTAR
    ${BEKNZ}     Set Variable    BEKNZ
    ${BUKRS}     Set Variable    BUKRS
    ${SGTXT}     Set Variable    SGTXT
    ${FKBER}     Set Variable    FKBER

    ${COTOSITE}      Create COTOSITEFILE     BELNR=${BELNR}    BLDAT=${BLDAT}    BUDAT=${BUDAT}   CPUDT=${CPUDT}   BLART=${BLART}
    ...     REFBN=${REFBN}   VERSN=${VERSN}      AWTYP=${AWTYP}    AWORG=${AWORG}      BUZEI=${BUZEI}
    ...     PERIO=${PERIO}   WOGBTR=${WOGBTR}    OBJNR=${OBJNR}    OBJ_TYPE=${OBJ_TYPE}    TYPE_NR=${TYPE_NR}
    ...     PRCTR=${PRCTR}    GJAHR=${GJAHR}     KSTAR=${KSTAR}     BEKNZ=${BEKNZ}     BUKRS=${BUKRS}    SGTXT=${SGTXT}   FKBER=${FKBER}

    Create File     ${PalkeCoToteumatFtpDir}/ID166_CO_TOSITE_${CUR_DATE_STR}_long_wait.xml   content=${COTOSITE}
    Sleep    20 seconds
    Set Ftp Connection As Down
    Sleep    2 minutes
    Set Ftp Connection As Up

    Wait Until Keyword Succeeds     2 minutes   15 seconds    Should Exist       ${PalkeResultCoToteumatFtpDir}/SAPSISAINENLASKENTA_${COTOSITE_YEAR}_${COTOSITE_SIMPLE_MONTH}.csv
    Wait Until Keyword Succeeds     2 minutes   15 Seconds     Check SAPSISAINENLASKENTA ${PalkeResultCoToteumatFtpDir}/SAPSISAINENLASKENTA_${COTOSITE_YEAR}_${COTOSITE_SIMPLE_MONTH}.csv
    ...     BELNR=${BELNR}    BLDAT=${BLDAT}    BUDAT=${BUDAT}   CPUDT=${CPUDT}   BLART=${BLART}
    ...     REFBN=${REFBN}   VERSN=${VERSN}      AWTYP=${AWTYP}    AWORG=${AWORG}      BUZEI=${BUZEI}
    ...     PERIO=${PERIO}   WOGBTR=${WOGBTR}    OBJNR=${OBJNR}    OBJ_TYPE=${OBJ_TYPE}    TYPE_NR=${TYPE_NR}
    ...     PRCTR=${PRCTR}    GJAHR=${GJAHR}     KSTAR=${KSTAR}     BEKNZ=${BEKNZ}     BUKRS=${BUKRS}    SGTXT=${SGTXT}   FKBER=${FKBER}


*** Keywords ***

Check SAPSISAINENLASKENTA ${FilePath}
    Skip    ECC Cotoeumat no longer needed or supported
    [Documentation]     Opens SAPSISAINENLASKENTA_.csv sent to the FTP and checks it contains the correct lines
    [Arguments]       ${BELNR}    ${BLDAT}    ${BUDAT}   ${CPUDT}   ${BLART}
    ...     ${REFBN}   ${VERSN}    ${AWTYP}    ${AWORG}      ${BUZEI}
    ...     ${PERIO}   ${WOGBTR}    ${OBJNR}    ${OBJ_TYPE}    ${TYPE_NR}
    ...     ${PRCTR}    ${GJAHR}     ${KSTAR}     ${BEKNZ}     ${BUKRS}    ${SGTXT}   ${FKBER}

    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable      BELNR;BLDAT;BUDAT;CPUDT;BLART;REFBN;VERSN;AWTYP;AWORG;BUZEI;PERIO;WOGBTR;OBJNR;OBJ_TYPE;TYPE_NR;PRCTR;GJAHR;KSTAR;BEKNZ;BUKRS;SGTXT;FKBER
    ${FirstLine}  	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${ExpLine}  Set Variable    ${BELNR};${BLDAT};${BUDAT};${CPUDT};${BLART};${REFBN};${VERSN};${AWTYP};${AWORG};${BUZEI};${PERIO};${WOGBTR};${OBJNR};${OBJ_TYPE};${TYPE_NR};${PRCTR};${GJAHR};${KSTAR};${BEKNZ};${BUKRS};${SGTXT};${FKBER}
    ${Lines}    Get Lines Containing String    ${File}   ${ExpLine}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers    ${LineCount}     1    msg=${ExpLine} not found

