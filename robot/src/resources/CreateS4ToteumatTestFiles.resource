*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create S4TOSITEFILE
    [Documentation]     Creates an S4 tosite from the given params + tositteet with BELNR_0 ... BELNR_99
    [Arguments]     ${BUKRS}     ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

    ${Tositteet}   Create S4 Tosite Row      0    ${BUKRS}    ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

    FOR     ${index}    IN RANGE    99
        ${NewTosite}    Create S4 Tosite Row     ${index}    ${BUKRS}    ${BELNR}_${index}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
          ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
          ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
          ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
          ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

        ${Tositteet}  Catenate    ${Tositteet}     ${NewTosite}
    END

    ${TOSITE_FILE}  catenate  SEPARATOR=
    ...    <ROWS xmlns:prx="urn:sap.com:proxy:Q50:/1SAI/TAS517FE8A68214593FC3F3:758">${Tositteet}</ROWS>
    RETURN  ${TOSITE_FILE}

Create S4 Tosite Row
    [Arguments]    ${DOCLN}     ${BUKRS}     ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}   ${AWTYP}

    ${ROW}  catenate  SEPARATOR=
    ...    <ROW>
    ...    <INTERFACEID>023</INTERFACEID>
    ...    <KOKRS>1000</KOKRS>
    ...    <BUKRS>${BUKRS}</BUKRS>
    ...    <BELNR>${BELNR}</BELNR>
    ...    <CO_BELNR>${CO_BELNR}</CO_BELNR>
    ...    <GJAHR>${GJAHR}</GJAHR>
    ...    <POPER>${POPER}</POPER>
    ...    <BLART>${BLART}</BLART>
    ...    <BLDAT>${BLDAT}</BLDAT>
    ...    <BUDAT>${BUDAT}</BUDAT>
    ...    <CPUDT>${CPUDT}</CPUDT>
    ...    <TCODE>${TCODE}</TCODE>
    ...    <XBLNR>${XBLNR}</XBLNR>
    ...    <XREF1_HD/>
    ...    <XREF2_HD/>
    ...    <ZZXREF1/>
    ...    <ZZXREF2/>
    ...    <ZZXREF3/>
    ...    <AWREF>0000000001</AWREF>
    ...    <USNAM>MESIREIR</USNAM>
    ...    <AWTYP>${AWTYP}</AWTYP>
    ...    <VORGN>RFBU</VORGN>
    ...    <KUNNR>${KUNNR}</KUNNR>
    ...    <NAME1/>
    ...    <KDAUF/>
    ...    <LIFNR>${LIFNR}</LIFNR>
    ...    <LIFNR_NAME1>${LIFNR_NAME1}</LIFNR_NAME1>
    ...    <IDNUMBER/>
    ...    <EBELN>${EBELN}</EBELN>
    ...    <Attachment>${Attachment}</Attachment>
    ...    <DOCLN>${DOCLN}</DOCLN>
    ...    <BUZEI>${BUZEI}</BUZEI>
    ...    <CO_BUZEI>${CO_BUZEI}</CO_BUZEI>
    ...    <VRGNG/>
    ...    <RMVCT>220</RMVCT>
    ...    <OBJNR/>
    ...    <ACCASTY/>
    ...    <ACCASTY_TXT/>
    ...    <ARBID>00000000</ARBID>
    ...    <ARBID_TXT/>
    ...    <RACCT>${RACCT}</RACCT>
    ...    <RCNTR>${RCNTR}</RCNTR>
    ...    <PRCTR>${PRCTR}</PRCTR>
    ...    <RFAREA>${RFAREA}</RFAREA>
    ...    <AUFNR>${AUFNR}</AUFNR>
    ...    <PRONR/>
    ...    <PRONR_TXT/>
    ...    <PS_PRJ_PNR>00000000</PS_PRJ_PNR>
    ...    <PS_PSPID>${PS_PSPID}</PS_PSPID>
    ...    <PS_PSP_PNR>00000000</PS_PSP_PNR>
    ...    <PS_POSID/>
    ...    <NPLNR/>
    ...    <NPLNR_VORGN/>
    ...    <RASSC>${RASSC}</RASSC>
    ...    <SEGMENT>${SEGMENT}</SEGMENT>
    ...    <LSTAR/>
    ...    <SGTXT>${SGTXT}</SGTXT>
    ...    <RHCUR>EUR</RHCUR>
    ...    <DRCRK>${DRCRK}</DRCRK>
    ...    <MWSKZ>${MWSKZ}</MWSKZ>
    ...    <VAT_PERCENT>${VAT_PERCENT}</VAT_PERCENT>
    ...    <HSL>${HSL}</HSL>
    ...    <PBUKRS/>
    ...    <SFAREA/>
    ...    <PPRCTR>${PPRCTR}</PPRCTR>
    ...    <KDPOS>000000</KDPOS>
    ...    <MATNR>${MATNR}</MATNR>
    ...    <VKORG/>
    ...    <EBELP>${EBELP}</EBELP>
    ...    <PERNR>00000000</PERNR>
    ...    <WERKS/>
    ...    <RUNIT/>
    ...    <MSL>0.000</MSL>
    ...    <BSCHL>75</BSCHL>
    ...    <KOART>A</KOART>
    ...    <NETDT>00000000</NETDT>
    ...    <PAOBJNR/>
    ...    <ANBWA>500</ANBWA>
    ...    <ANLN1>000000000001</ANLN1>
    ...    <ANLN2>0003</ANLN2>
    ...    <ANLKL>00000900</ANLKL>
    ...    <KTOGR>00000900</KTOGR>
    ...    <AUGDT>00000000</AUGDT>
    ...    <AUGBL>${AUGBL}</AUGBL>
    ...    <PAUFNR/>
    ...    <PPS_PSP_PNR>00000000</PPS_PSP_PNR>
    ...    <PPS_PSPID/>
    ...    <PNPLNR/>
    ...    <PNPLNR_VORGN/>
    ...    <PKDAUF/>
    ...    <PLSTAR/>
    ...    <ZZVERTN/>
    ...    <LAST_CHANGE_DATETIME>${LAST_CHANGE_DATETIME}</LAST_CHANGE_DATETIME>
    ...    <TIMESTAMP>202508250955</TIMESTAMP>
    ...    </ROW>

    RETURN    ${ROW}
