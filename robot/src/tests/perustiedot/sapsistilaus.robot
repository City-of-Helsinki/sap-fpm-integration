*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource
Resource    ../../resources/CreateTestFilesCommon.resource


Test Tags       perustiedot     sapsistilaus

*** Test Cases ***
Lähetä kasko perustiedot sapsistilaus
    Lähetä kasko perustiedot sapsistilaus      %{KASKO_SFTP_USER_ID137}     BUKRS=1400    AUART=1401

Lähetä sotepe perustiedot sapsistilaus
    Lähetä sotepe perustiedot sapsistilaus      %{SOTEPE_SFTP_USER_ID167}     BUKRS=3900    AUART=3901

Lähetä palke perustiedot sapsistilaus
    Lähetä palke perustiedot sapsistilaus      %{PALKE_SFTP_USER_ID138}     BUKRS=9500    AUART=9501


*** Keywords ***

Lähetä ${TOIMIALA} perustiedot sapsistilaus
    [Documentation]     ${TOIMIALA} perustiedot ORD_OUT_ / sapsistilaus
    [Arguments]          ${SFTP_USER}    ${BUKRS}    ${AUART}
    [Tags]       ${TOIMIALA}      perustiedot     sapsistilaus
    ${PerustiedotFtpDir}   Get FTP Dir For     ${SFTP_USER}
    ${BUKRS_SHOULD_NOT_BE_IN_CSV}    Set Variable    -999
    ${AUART}    Set Variable   3901
    ${AUFNR1}   Set Variable    01
    ${KTEXT1}   Set Variable    ktxt1
    ${STTXT1}   Set Variable    st text 1
    ${AUTYP1}   Set Variable    autyp1
    ${AUFNR2}   Set Variable    02
    ${KTEXT2}   Set Variable    ktext2
    ${STTXT2}   Set Variable    sttext 2
    ${AUTYP2}   Set Variable    autyp2
    ${ORD}      Create ORD  ${BUKRS_SHOULD_NOT_BE_IN_CSV}   ${AUART}     ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}

    ${CUR_DATE_STR}     Get Current Time Text

    Create File     ${PerustiedotFtpDir}/203/arch/ORD_OUT_${CUR_DATE_STR}_should_not_be_handled.xml   content=${ORD}

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
    Create File     ${PerustiedotFtpDir}/203/ORD_OUT_${CUR_DATE_STR}_1.xml   content=${ORD}
    Set Ftp Connection As Down
    Set Ftp Connection As Up
    Wait Until Keyword Succeeds      3 minutes   15 seconds      Check SAPSISTILAUS ${PerustiedotFtpDir}/SAPSISTILAUS.csv
        ...     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}   ${STTXT1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   should_not_contain=${BUKRS_SHOULD_NOT_BE_IN_CSV}


Check SAPSISTILAUS ${SisTilausFilePath}
    [Documentation]     Opens SAPSISTILAUS.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}    ${AUFNR2}   ${KTEXT2}  ${STTXT2}    ${should_not_contain}
    ${File}    Get File       ${SisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR1};${KTEXT1};${STTXT1}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR2};${KTEXT2};${STTXT2}
    Should Not Contain     ${File}             ${should_not_contain}
    ${AllLinesCount}    Get Line Count  ${File}
    # can contain lines from previous runs
    Should Be True    ${AllLinesCount} >= 3

