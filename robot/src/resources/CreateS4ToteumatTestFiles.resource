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


Create Segment E1FISEG
   [Arguments]
       ...        ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
       ...      ${EBELN}     ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
       ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
       ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}

    ${SEG}=  catenate  SEPARATOR=
       ...    <E1FISEG SEGMENT="">
       ...        <BUZEI>${BUZEI}</BUZEI>
       ...        <CO_BUZEI>${CO_BUZEI}</CO_BUZEI>
       ...        <AUGDT>00000000</AUGDT>
       ...        <AUGCP>00000000</AUGCP>
       ...        <AUGBL>${AUGBL}</AUGBL>
       ...        <BSCHL>50</BSCHL>
       ...        <KOART>S</KOART>
       ...        <SHKZG>${DRCRK}</SHKZG>
       ...        <GSBER>5000</GSBER>
       ...        <MWSKZ>${MWSKZ}</MWSKZ>
       ...        <DMBTR>${HSL}</DMBTR>
       ...        <DMBE2> 0.00</DMBE2>
       ...        <DMBE3> 0.00</DMBE3>
       ...        <WRBTR> 50.80</WRBTR>
       ...        <KZBTR> 0.00</KZBTR>
       ...        <PSWBT> 50.80</PSWBT>
       ...        <PSWSL>EUR</PSWSL>
       ...        <HWBAS> 0.00</HWBAS>
       ...        <FWBAS> 0.00</FWBAS>
       ...        <VALUT>20221230</VALUT>
       ...        <ZUONR>20221230</ZUONR>
       ...        <VBUND>${RASSC}</VBUND>
       ...        <VORGN>SD00</VORGN>
       ...        <SGTXT>${SGTXT}</SGTXT>
       ...        <FDTAG>00000000</FDTAG>
       ...        <KOKRS>1000</KOKRS>
       ...        <TXGRP>001</TXGRP>
       ...        <KOSTL>${RCNTR}</KOSTL>
       ...        <AUFNR>${AUFNR}</AUFNR>
       ...        <POSN2>000000</POSN2>
       ...        <BZDAT>00000000</BZDAT>
       ...        <PERNR>00000000</PERNR>
       ...        <HKONT>${RACCT}</HKONT>
       ...        <ABPER>000000</ABPER>
       ...        <MATNR>${MATNR}</MATNR>
       ...        <WERKS>3900</WERKS>
       ...        <MENGE> 1.000</MENGE>
       ...        <MEINS>ST</MEINS>
       ...        <ERFMG> 0.000</ERFMG>
       ...        <BPMNG> 0.000</BPMNG>
       ...        <EBELN>${EBELN}</EBELN>
       ...        <EBELP>${EBELP}</EBELP>
       ...        <ZEKKN>00</ZEKKN>
       ...        <BUALT> 0.00</BUALT>
       ...        <PRCTR>${PRCTR}</PRCTR>
       ...        <DABRZ>00000000</DABRZ>
       ...        <FIPOS>3200</FIPOS>
       ...        <AUFPL>0000000000</AUFPL>
       ...        <APLZL>00000000</APLZL>
       ...        <PROJK>${PS_PSPID}</PROJK>
       ...        <PAOBJNR>0000000000</PAOBJNR>
       ...        <FISTL>3900</FISTL>
       ...        <GEBER>31001</GEBER>
       ...        <ALTKT>0000003234</ALTKT>
       ...        <PPRCT>${PPRCTR}</PPRCT>
       ...        <KBLPOS>000</KBLPOS>
       ...        <OBZEI>000</OBZEI>
       ...        <TXDAT>00000000</TXDAT>
       ...        <KURSR> 0.00000</KURSR>
       ...        <GBETR> 0.00</GBETR>
       ...        <E1FISE2 SEGMENT="">
       ...          <SCTAX> 0.00</SCTAX>
       ...          <STTAX> 0.00</STTAX>
       ...          <KSTAR>0000323456</KSTAR>
       ...          <FKBER_LONG>${RFAREA}</FKBER_LONG>
       ...        </E1FISE2>
       ...        <E1FINBU SEGMENT="">
       ...       <MWSTS> 0.00</MWSTS>
       ...       <KUNNR>${KUNNR}</KUNNR>
       ...       <WMWST> 0.00</WMWST>
       ...       <ZFBDT>00000000</ZFBDT>
       ...    <ZBD1T> 0</ZBD1T>
       ...    <ZBD2T> 0</ZBD2T>
       ...    <ZBD3T> 0</ZBD3T>
       ...    <ZBD1P> 0.000</ZBD1P>
       ...    <ZBD2P> 0.000</ZBD2P>
       ...    <SKFBT> 0.00</SKFBT>
       ...    <SKNTO> 0.00</SKNTO>
       ...    <WSKTO> 0.00</WSKTO>
       ...    <REBZJ>0000</REBZJ>
       ...    <REBZZ>000</REBZZ>
       ...    <VRSDT>00000000</VRSDT>
       ...    <MADAT>00000000</MADAT>
       ...    <MANST>0</MANST>
       ...    <QSSHB> 0.00</QSSHB>
       ...    <QSFBT> 0.00</QSFBT>
       ...    <LIFNR>${LIFNR}</LIFNR>
       ...    <KIDNO>39200123456</KIDNO>
       ...    <SKNT2> 0.00</SKNT2>
       ...    <SKNT3> 0.00</SKNT3>
       ...    <PYAMT> 0.00</PYAMT>
       ...    <ABSBT> 0.00</ABSBT>
       ...    <DTWS1>00</DTWS1>
       ...    <DTWS2>00</DTWS2>
       ...    <DTWS3>00</DTWS3>
       ...    <DTWS4>00</DTWS4>
       ...    </E1FINBU>
       ...    </E1FISEG>

       RETURN   ${SEG}