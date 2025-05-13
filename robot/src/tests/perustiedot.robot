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
    ${ORD}      Create Kasko ORD
    Create File     ${KaskoPerustiedotFtpDir}/203/ORD_OUT_1.xml   content=${ORD}
    # ${JSON}     Get File	${CURDIR}/json/${FileName}

      #    ${JSON}     Replace String	${JSON} 	_ATTACHMENT_FILE_NAME_		${AttachmentFileName}
      #    ${JSON}     Replace String	${JSON} 	_ATTACHMENT_URL_		${AttachmentUrl}
    Wait Until Keyword Succeeds      5 minutes   1 minute      Nope

*** Keywords ***
Nope
    Should Be True    False
