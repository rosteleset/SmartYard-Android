package com.sesameware.smartyard_oem.ui.main

import com.sesameware.domain.model.request.CreateIssuesRequest

interface BaseIssueDelegate {
    fun extendBuilder(builder: CreateIssuesRequest.Builder, origin: IssueOrigin)
}