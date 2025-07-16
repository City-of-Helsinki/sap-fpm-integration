*** Settings ***
Library         DateTime

*** Keywords ***
Get Current Time Text
    [Documentation]     Returns the current time in format %Y-%m-%d_%H:%M:%S.%f
    RETURN      Get Current Date		result_format=%Y-%m-%d_%H:%M:%S.%f
