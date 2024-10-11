package ru.madbrains.smartyard.ui.main.address.models.interfaces


data class AddressIntercomItem(
    val houseId: Int,
    val address: String,
    var boolean: Boolean = false,
    val intercoms: List<IntercomItem>
)

data class IntercomItem(
    private val _hasGates: String,
    private val _contractOwner: String,
    private val _frsEnabled: String,
    val doorId: Int,
    val icon: Int,
    val name: String,
    val domophoneId: Long,
    val title: String,
    val flatId: Int,
    var doorCode: String,
    val flatNumber: Int,
    val id: Int,
    val url: String,
    val clientId: Int,
    val videoUrl: String,
){
    val hasGates: Boolean = _hasGates == "t"
    val contractOwner: Boolean = _contractOwner == "t"
    val frsEnabled: Boolean = _frsEnabled == "t"

    fun setCode(code: String){
        doorCode = code
    }
}
