package com.sesameware.domain.model

enum class IssueClass(val value: Int) {
    DontRememberAnythingIssue(1),
    ConfirmAddressByCourierIssue(2),
    ConfirmAddressInOfficeIssue(3),
    DeleteAddressIssue(4),
    ServicesUnavailableIssue(5),
    ComeInOfficeMyselfIssue(6),
    ConnectOnlyNonHousesServices(7),
    OrderCallback(8),
    RequestRec(9)
}
