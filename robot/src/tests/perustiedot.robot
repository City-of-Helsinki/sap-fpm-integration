*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../resources/CreateTestFiles.resource


Test Tags       perustiedot

*** Test Cases ***
Lähetä kasko perustiedot
    [Documentation]     KASKO perustiedot
    [Tags]   kasko      perustiedot
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}
    ${BUKRS}    Set Variable    1400
    ${AUART}    Set Variable   3901
    ${AUFNR1}   Set Variable    01
    ${KTEXT1}   Set Variable    ktxt1
    ${STTXT1}   Set Variable    st text 1
    ${AUTYP1}   Set Variable    autyp1
    ${AUFNR2}   Set Variable    02
    ${KTEXT2}   Set Variable    ktext2
    ${STTXT2}   Set Variable    sttext 2
    ${AUTYP2}   Set Variable    autyp2
    ${ORD}      Create ORD  ${BUKRS}   ${AUART}     ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}
    Create File     ${KaskoPerustiedotFtpDir}/203/ORD_OUT_1.xml   content=${ORD}
    Wait Until Keyword Succeeds      3 minutes   15 seconds      Check SAPSISTILAUS ${KaskoPerustiedotFtpDir}/SAPSISTILAUS.csv
        ...     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}

Lähetä arch/ kasko perustiedot
    [Documentation]     KASKO perustiedot, varmista että arch/ alta löytyviä käsitellä
    [Tags]   kasko      perustiedot
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}
    ${BUKRS}    Set Variable    -999
    ${AUART}    Set Variable   3901
    ${AUFNR1}   Set Variable    01
    ${KTEXT1}   Set Variable    ktxt1
    ${STTXT1}   Set Variable    st text 1
    ${AUTYP1}   Set Variable    autyp1
    ${AUFNR2}   Set Variable    02
    ${KTEXT2}   Set Variable    ktext2
    ${STTXT2}   Set Variable    sttext 2
    ${AUTYP2}   Set Variable    autyp2
    ${ORD}      Create ORD  ${BUKRS}   ${AUART}     ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}
    Create File     ${KaskoPerustiedotFtpDir}/203/arch/ORD_OUT_should_not_be_handled.xml   content=${ORD}
    Wait Until Keyword Succeeds      1 minutes   15 seconds      Check SAPSISTILAUS not handled ${KaskoPerustiedotFtpDir}/SAPSISTILAUS.csv
        ...     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}

*** Keywords ***

Check SAPSISTILAUS ${SisTilausFilePath}
    [Documentation]     Opens SAPSISTILAUS.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}  ${AUTYP1}  ${AUFNR2}   ${KTEXT2}  ${STTXT2}   ${AUTYP2}
    ${SisTilaus}    Get File       ${SisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${SisTilaus}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Contain   ${SisTilaus}   ${BUKRS};${AUART};${AUFNR1};${KTEXT1};${STTXT1}
    Should Contain   ${SisTilaus}   ${BUKRS};${AUART};${AUFNR2};${KTEXT2};${STTXT2}

Check SAPSISTILAUS not handled ${SisTilausFilePath}
    [Documentation]     Opens SAPSISTILAUS.csv sent to the FTP and checks that it doesn't contain the given lines
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}  ${AUTYP1}  ${AUFNR2}   ${KTEXT2}  ${STTXT2}   ${AUTYP2}
    ${SisTilaus}    Get File       ${SisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${SisTilaus}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Not Contain   ${SisTilaus}   ${BUKRS};${AUART};${AUFNR1};${KTEXT1};${STTXT1}
    Should Not Contain   ${SisTilaus}   ${BUKRS};${AUART};${AUFNR2};${KTEXT2};${STTXT2}
