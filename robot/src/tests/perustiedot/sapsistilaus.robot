*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapsistilaus

*** Test Cases ***
Lähetä kasko perustiedot sapsistilaus
    [Documentation]     KASKO perustiedot ORD_OUT_ / sapsistilaus
    [Tags]   kasko      perustiedot     sapsistilaus
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
    Wait Until Keyword Succeeds      2 minutes   15 seconds      Check SAPSISTILAUS ${KaskoPerustiedotFtpDir}/SAPSISTILAUS.csv
        ...     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}


*** Keywords ***

Check SAPSISTILAUS ${SisTilausFilePath}
    [Documentation]     Opens SAPSISTILAUS.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}  ${AUTYP1}  ${AUFNR2}   ${KTEXT2}  ${STTXT2}   ${AUTYP2}
    ${File}    Get File       ${SisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR1};${KTEXT1};${STTXT1}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR2};${KTEXT2};${STTXT2}
    ${AllLinesCount}    Get Line Count  ${File}
    Should Be Equal As Numbers    ${AllLinesCount}    3

