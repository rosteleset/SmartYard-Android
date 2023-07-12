package com.sesameware.smartyard_oem.ui.main.address.issue

import androidx.lifecycle.ViewModel
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

class IssueViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {
    // TODO: Implement the ViewModel
}
